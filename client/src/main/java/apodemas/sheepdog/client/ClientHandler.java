package apodemas.sheepdog.client;

import apodemas.common.Checks;
import apodemas.common.StringUtils;
import apodemas.sheepdog.core.concurrent.EventLoopPromise;
import apodemas.sheepdog.core.mqtt.ProMqttMessageFactory;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.*;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static io.netty.handler.codec.mqtt.MqttMessageType.CONNACK;

public class ClientHandler extends SimpleChannelInboundHandler<MqttMessage> {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ClientHandler.class);
    static int UNCONNECTED = 0;
    static int CONNECTING = 1;
    static int CONNECTED = 2;
    static int CLOSED = 3;

    private int state;

    private final EventLoopPromise shutdownHandler;
    private final ClientInfo info;
    private final ClientSettings settings;
    private final MessageListener listener;

    private long lastPingRespTime;
    private ScheduledFuture<?> pingResponseTimeout;
    private DefaultRepublishScheduler repubScheduler;

    public ClientHandler(EventLoopPromise shutdownHandler, ClientInfo info, ClientSettings settings, MessageListener listener){
        super(true);
        this.shutdownHandler = Checks.notNull(shutdownHandler, "shutdownHandler");
        this.info = Checks.notNull(info, "info");
        this.settings = Checks.notNull(settings, "settings");
        this.listener = Checks.notNull(listener, "listener");
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if(state == UNCONNECTED){
            state = CONNECTING;
            ctx.writeAndFlush(connectMessage());
            if (repubScheduler == null){
                repubScheduler = new DefaultRepublishScheduler(ctx.executor(), settings);
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        destroy();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        closeChannel(ctx, cause);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MqttMessage msg) throws Exception {
        MqttMessageType msgType = msg.fixedHeader().messageType();
        if (msgType == CONNACK){
            onConack(ctx, (MqttConnAckMessage)msg);
        }else if (state == CONNECTED){
            switch (msg.fixedHeader().messageType()){
                case PUBLISH:
                    onPub(ctx, (MqttPublishMessage)msg);
                    break;
                case PUBACK:
                    onPuback(ctx, (MqttPubAckMessage)msg);
                    break;
                case SUBACK:
                    onSuback(ctx, (MqttSubAckMessage)msg);
                    break;
                case UNSUBACK:
                    onUnsuback(ctx, (MqttUnsubAckMessage)msg);
                    break;
                case PINGRESP:
                    onPingresp(ctx);
                    break;
            }
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent){
            IdleStateEvent idleEvent = (IdleStateEvent) evt;
            if (idleEvent == IdleStateEvent.FIRST_WRITER_IDLE_STATE_EVENT
                    || idleEvent == IdleStateEvent.WRITER_IDLE_STATE_EVENT){
                if(logger.isDebugEnabled()){
                    logger.debug("send a ping to server");
                }
                ChannelFuture channelFuture = ctx.write(ProMqttMessageFactory.newPingreq());
                if (settings.pingResponseTimeoutSec() > 0){
                    channelFuture.addListener(fut->{
                        if(fut.isSuccess() && pingResponseTimeout == null){
                            int timeoutSec = settings.pingResponseTimeoutSec();
                            pingResponseTimeout = schedule(ctx,
                                    new RingrespTimeoutTask(ctx, timeoutSec * 1000 * 1000 * 1000),
                                    timeoutSec,
                                    TimeUnit.SECONDS);
                        }
                    });
                }
            }
        }else{
            ctx.fireUserEventTriggered(evt);
        }
    }

    private MqttConnectMessage connectMessage(){
        MqttMessageBuilders.ConnectBuilder builder = MqttMessageBuilders.connect();
        builder.protocolVersion(MqttVersion.MQTT_3_1_1)
                .clientId(info.clientId())
                .keepAlive(info.keepAliveSec());
        if (StringUtils.notEmpty(info.username())){
            builder.username(info.username());
            builder.password(info.password());
        }

        return builder.build();
    }

    private ClientContext clientContext(ChannelHandlerContext ctx){
        return new ClientContext(ctx, shutdownHandler, repubScheduler, ctx.alloc(), settings);
    }

    private void onConack(ChannelHandlerContext ctx, MqttConnAckMessage msg) {
        if (state == CONNECTING) {
            MqttConnectReturnCode code = msg.variableHeader().connectReturnCode();
            if (code == MqttConnectReturnCode.CONNECTION_ACCEPTED){
                state = CONNECTED;
                if(logger.isInfoEnabled()) {
                    logger.info("receive connack from server");
                }
                listener.onConnectSuccess(clientContext(ctx));
                return;
            }
            if(logger.isInfoEnabled()) {
                logger.info("failed to connect to remote server (return code  %x)", code.byteValue());
            }
            closeChannel(ctx, new ClientConnectException(code));
        }
    }

    private void onPub(ChannelHandlerContext ctx, MqttPublishMessage msg){
        boolean ack = listener.onPublish(clientContext(ctx), msg);
        MqttQoS qos = msg.fixedHeader().qosLevel();
        int messageId = msg.variableHeader().packetId();
        if (ack && qos.value() > MqttQoS.AT_MOST_ONCE.value() && messageId > 0) {
            MqttPubAckMessage pubAckMessage = ProMqttMessageFactory.newPubAck(messageId);
            ctx.write(pubAckMessage);
        }
    }

    private void onPuback(ChannelHandlerContext ctx, MqttPubAckMessage msg) {
        repubScheduler.ack(msg);
    }

    private void onSuback(ChannelHandlerContext ctx, MqttSubAckMessage msg){
    }

    private void onUnsuback(ChannelHandlerContext ctx, MqttUnsubAckMessage msg){

    }

    private void onPingresp(ChannelHandlerContext ctx){
        lastPingRespTime = ticksInNanos();
    }

    private ScheduledFuture<?> schedule(ChannelHandlerContext ctx, Runnable task, long delay, TimeUnit unit) {
        return ctx.executor().schedule(task, delay, unit);
    }

    private long ticksInNanos() {
        return System.nanoTime();
    }

    private void destroy() {
        state = CLOSED;

        if (pingResponseTimeout != null){
            pingResponseTimeout.cancel(false);
            pingResponseTimeout = null;
        }

        if (repubScheduler != null){
            repubScheduler.close();
        }
    }

    private void closeChannel(ChannelHandlerContext ctx, Throwable cause) {
        if (cause != null) {
            if(logger.isErrorEnabled()){
                logger.warn("exception occurred", cause);
            }
        }
        ctx.close().addListener(fut->{
            if(cause != null) {
                shutdownHandler.setFailure(cause);
            }else{
                shutdownHandler.shutdownGracefully();
            }
        });
    }

    private final class RingrespTimeoutTask extends AbstractScheduleTask {
        private final long timeoutNano;

        public RingrespTimeoutTask(ChannelHandlerContext ctx, long timeoutNano){
            super(ctx);
            this.timeoutNano = timeoutNano;
        }

        @Override
        protected void run(ChannelHandlerContext ctx) {
            ClientHandler.this.pingResponseTimeout = null;
            if (ticksInNanos() - lastPingRespTime > timeoutNano){
                if(logger.isInfoEnabled()){
                    logger.info("waiting for ping response is timeout");
                }
                closeChannel(ctx, new PingrespTimeoutException());
            }
        }
    }

}
