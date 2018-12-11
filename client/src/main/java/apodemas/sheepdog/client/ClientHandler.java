package apodemas.sheepdog.client;

import apodemas.common.Checks;
import apodemas.common.StringUtils;
import apodemas.sheepdog.core.concurrent.EventLoopPromise;
import apodemas.sheepdog.core.mqtt.ProMqttMessageFactory;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.*;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;


import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static io.netty.handler.codec.mqtt.MqttMessageType.CONNACK;

public class ClientHandler extends SimpleChannelInboundHandler<MqttMessage> implements Session{
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
    private final Promise<Session> connectFuture;

    private long lastPingRespTime;
    private ScheduledFuture<?> pingResponseTimeout;
    private RepubScheduler repubScheduler;
    private ResubScheduler resubScheduler;
    private ReunsubScheduler reunsubScheduler;

    private volatile ChannelHandlerContext cachedCtx;

    public ClientHandler(EventLoopPromise shutdownHandler, ClientInfo info, ClientSettings settings, MessageListener listener, Promise<Session> connectFuture){
        super(true);
        this.shutdownHandler = Checks.notNull(shutdownHandler, "shutdownHandler");
        this.info = Checks.notNull(info, "info");
        this.settings = Checks.notNull(settings, "settings");
        this.listener = Checks.notNull(listener, "listener");
        this.connectFuture = connectFuture;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if(state == UNCONNECTED){
            state = CONNECTING;
            ctx.writeAndFlush(connectMessage());
            if (repubScheduler == null){
                repubScheduler = new RepubScheduler(ctx.executor(), settings);
            }
            if (resubScheduler == null){
                resubScheduler = new ResubScheduler(ctx.executor());
            }
            if (reunsubScheduler == null){
                reunsubScheduler =  new ReunsubScheduler(ctx.executor());
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        //closeChannel(cachedCtx, new ServerCloseConnectionException());
        closeChannel(ctx, null);
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
                ChannelFuture channelFuture = ctx.writeAndFlush(ProMqttMessageFactory.newPingreq());
                if (settings.pingResponseTimeoutSec() > 0){
                    channelFuture.addListener(fut->{
                        if(fut.isSuccess() && pingResponseTimeout == null){
                            int timeoutSec = settings.pingResponseTimeoutSec();
                            pingResponseTimeout = schedule(ctx,
                                    new RingrespTimeoutTask(ctx, new Long(timeoutSec) * 1000 * 1000 * 1000),
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

    @Override
    public Future<Void> publish(MqttPublishMessage msg) {
        if(state == CLOSED || !(cachedCtx != null && cachedCtx.channel().isOpen())){
            return GlobalEventExecutor.INSTANCE.newFailedFuture(new ClientClosedException());
        }

        boolean repub = msg.fixedHeader().qosLevel().value() > MqttQoS.AT_MOST_ONCE.value();
        if(repub){
            msg.retain();
        }
        ChannelFuture writeFut = cachedCtx.write(msg);
        if(!repub){
            return writeFut;
        }

        Promise<Void> promise = cachedCtx.newPromise();
        writeFut.addListener(f -> {
            if (f.isSuccess()) {
                Future<MqttPublishMessage> repubFut = repubScheduler.schedule(
                        cachedCtx,
                        msg.variableHeader().packetId(),
                        msg,
                        settings.publishAckTimeoutSec(),
                        TimeUnit.SECONDS);
                repubFut.addListener((Future<MqttPublishMessage> fut)->{
                   if(fut.isSuccess()){
                       fut.get().release();
                       promise.trySuccess(null);
                   }else{
                       if (fut.cause() instanceof RetryException && logger.isDebugEnabled()){
                           RetryException e = (RetryException)fut.cause();
                           MqttPublishMessage message = (MqttPublishMessage)e.value();
                           logger.debug("republish message (id: d%) failed due to %s",
                                   message.variableHeader().packetId(), e.reason());
                       }
                       promise.tryFailure(fut.cause());
                   }
                });
            } else {
                msg.release();
                promise.setFailure(f.cause());
            }
        });

        return promise;
    }

    @Override
    public Future<?> subscribe(MqttSubscribeMessage msg) {
        if (state == CLOSED || !(cachedCtx != null && cachedCtx.channel().isOpen())) {
            return GlobalEventExecutor.INSTANCE.newFailedFuture(new ClientClosedException());
        }
        Integer id = msg.variableHeader().messageId();
        ChannelFuture writeFut = cachedCtx.writeAndFlush(msg);
        Promise<Void> promise = cachedCtx.newPromise();
        writeFut.addListener(f->{
            if (f.isSuccess()) {
                Future<MqttSubscribeMessage> repubFut = resubScheduler.schedule(cachedCtx,id,msg,10,TimeUnit.SECONDS);
                repubFut.addListener((Future<MqttSubscribeMessage> fut)->{
                    if(fut.isSuccess()){
                        promise.trySuccess(null);
                    }else{
                        if (fut.cause() instanceof RetryException && logger.isDebugEnabled()){
                            RetryException e = (RetryException)fut.cause();
                            MqttSubscribeMessage message = (MqttSubscribeMessage)e.value();
                            logger.debug("resubscribe message (id: d%) failed due to %s",
                                    message.variableHeader().messageId(), e.reason());
                        }
                        promise.tryFailure(fut.cause());
                    }
                });
            } else {
                promise.tryFailure(f.cause());
            }
        });

        return promise;
    }

    @Override
    public Future<?> unsubscribe(MqttUnsubscribeMessage msg) {
        if (state == CLOSED || !(cachedCtx != null && cachedCtx.channel().isOpen())) {
            return GlobalEventExecutor.INSTANCE.newFailedFuture(new ClientClosedException());
        }

        Integer id = msg.variableHeader().messageId();
        ChannelFuture writeFut = cachedCtx.writeAndFlush(msg);
        Promise<Void> promise = cachedCtx.newPromise();
        writeFut.addListener(f->{
            if (f.isSuccess()) {
                Future<MqttUnsubscribeMessage> repubFut = reunsubScheduler.schedule(cachedCtx,id,msg,10,TimeUnit.SECONDS);
                repubFut.addListener((Future<MqttUnsubscribeMessage> fut)->{
                    if(fut.isSuccess()){
                        promise.trySuccess(null);
                    }else{
                        if (fut.cause() instanceof RetryException && logger.isDebugEnabled()){
                            RetryException e = (RetryException)fut.cause();
                            MqttUnsubscribeMessage message = (MqttUnsubscribeMessage)e.value();
                            logger.debug("re-unsubscribe message (id: d%) failed due to %s",
                                    message.variableHeader().messageId(), e.reason());
                        }
                        promise.tryFailure(fut.cause());
                    }
                });
            } else {
                promise.tryFailure(f.cause());
            }
        });

        return promise;
    }

    @Override
    public ByteBufAllocator allocator() {
        if(cachedCtx != null) {
            return cachedCtx.alloc();
        }

        return null;
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

    private void onConack(ChannelHandlerContext ctx, MqttConnAckMessage msg) {
        if (state == CONNECTING) {
            MqttConnectReturnCode code = msg.variableHeader().connectReturnCode();
            if (code == MqttConnectReturnCode.CONNECTION_ACCEPTED){
                this.cachedCtx = ctx;
                if(logger.isInfoEnabled()) {
                    logger.info("receive connack from server");
                }

                state = CONNECTED;
                connectFuture.trySuccess(this);
            }else {
                if (logger.isInfoEnabled()) {
                    logger.info("failed to connect to remote server (return code  %x)", code.byteValue());
                }
                ClientConnectException cause = new ClientConnectException(code);
                connectFuture.tryFailure(cause);
                closeChannel(ctx, cause);
            }
        }
    }

    private void onPub(ChannelHandlerContext ctx, MqttPublishMessage msg){
        boolean ack = listener.onPublish(this, msg);
        MqttQoS qos = msg.fixedHeader().qosLevel();
        int messageId = msg.variableHeader().packetId();
        if (ack && qos.value() > MqttQoS.AT_MOST_ONCE.value() && messageId > 0) {
            MqttPubAckMessage pubAckMessage = ProMqttMessageFactory.newPubAck(messageId);
            ctx.write(pubAckMessage);
        }
    }

    private void onPuback(ChannelHandlerContext ctx, MqttPubAckMessage msg) {
        repubScheduler.done(msg.variableHeader().messageId());
    }

    private void onSuback(ChannelHandlerContext ctx, MqttSubAckMessage msg){
        resubScheduler.done(msg.variableHeader().messageId());
    }

    private void onUnsuback(ChannelHandlerContext ctx, MqttUnsubAckMessage msg){
        reunsubScheduler.done(msg.variableHeader().messageId());
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

    private void closeChannel(ChannelHandlerContext ctx, Throwable cause) {
        if(state != CLOSED) {
            state = CLOSED;
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

            if (pingResponseTimeout != null) {
                pingResponseTimeout.cancel(false);
                pingResponseTimeout = null;
            }

            if (repubScheduler != null) {
                repubScheduler.dispose(ctx.executor().newPromise());
            }
            if(resubScheduler != null){
                reunsubScheduler.dispose(ctx.executor().newPromise());
            }
            if(reunsubScheduler != null){
                reunsubScheduler.dispose(ctx.executor().newPromise());
            }

            this.cachedCtx = null;

        }
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

    private static class RepubScheduler extends RetryScheduler<MqttPublishMessage, Integer>{
        public RepubScheduler(EventExecutor executor, ClientSettings settings) {
            super(executor, settings.maxRepubTimes(), settings.maxRepubQueueSize());
        }

        @Override
        protected void retry(ChannelHandlerContext ctx, Integer id, MqttPublishMessage value) {
            ctx.write(value);
        }
    }

    private static class ResubScheduler extends RetryScheduler<MqttSubscribeMessage, Integer>{
        public ResubScheduler(EventExecutor executor) {
            super(executor, 3, 16);
        }

        @Override
        protected void retry(ChannelHandlerContext ctx, Integer id, MqttSubscribeMessage value) {
           ctx.writeAndFlush(value);
        }
    }

    private static class ReunsubScheduler extends RetryScheduler<MqttUnsubscribeMessage, Integer>{
        public ReunsubScheduler(EventExecutor executor) {
            super(executor, 3, 16);
        }

        @Override
        protected void retry(ChannelHandlerContext ctx, Integer id, MqttUnsubscribeMessage value) {
            ctx.writeAndFlush(value);
        }
    }
}
