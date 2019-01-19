package apodemas.sheepdog.server;

import apodemas.sheepdog.common.StringUtils;
import apodemas.sheepdog.core.mqtt.ProMqttMessageFactory;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.*;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.List;

import static io.netty.handler.codec.mqtt.MqttMessageType.CONNECT;

/**
 * @author caozheng
 * @time 2019-01-02 13:27
 **/
public class ServerHandler extends SimpleChannelInboundHandler<MqttMessage> {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ServerHandler.class);
    public static final String NAME = "Sheepdog Handler";
    static int UNCONNECTED = 0;
    static int AUTHENTICATING = 1;
    static int CONNECTED = 2;

    private int state;
    private Session session;

    private final ServerSettings settings;
    private final MessageProcessor processor;
    private final SessionManager sessionManager;

    public ServerHandler(ServerSettings settings, MessageProcessor processor, SessionManager sessionManager){
        this.settings = settings;
        this.processor = processor;
        this.sessionManager = sessionManager;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        String clientId = "unknown";
        if(state == CONNECTED && session != null){
            session.disconnect();
            clientId = session.clientId();
        }else{
            ctx.close();
        }

        if(logger.isWarnEnabled()){
            logger.warn("client ({}) connection disconnected due to exceptions", clientId, cause);
        }
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MqttMessage msg) throws Exception {
        MqttMessageType msgType = msg.fixedHeader().messageType();
        if (msgType == CONNECT){
            onConnect(ctx, (MqttConnectMessage)msg);
        }else if(state == CONNECTED && session != null){
            switch (msgType){
                case PUBLISH:
                    break;
                case SUBSCRIBE:
                    onSubscribe(ctx, (MqttSubscribeMessage)msg);
                    break;
                case UNSUBSCRIBE:
                    onUnsubscribe(ctx, (MqttUnsubscribeMessage)msg);
                    break;
                case PINGREQ:
                    session.writeAndFlush(ProMqttMessageFactory.newPingresp());
                    if(logger.isInfoEnabled()){
                        logger.info("client ({}) ping", session.clientId());
                    }
                    break;
                case DISCONNECT:
                    session.disconnect();
                    break;
            }
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent){
            IdleStateEvent idleEvent = (IdleStateEvent) evt;
            if (idleEvent == IdleStateEvent.FIRST_READER_IDLE_STATE_EVENT
                    || idleEvent == IdleStateEvent.READER_IDLE_STATE_EVENT){
                if(state == CONNECTED && session != null){
                    session.disconnect();
                    if(logger.isInfoEnabled()){
                        logger.warn("client ({}) ping timeout", session.clientId());
                    }
                }
            }
        }
    }

    private void onConnect(ChannelHandlerContext ctx, MqttConnectMessage msg) {
        if (state != UNCONNECTED){
            return;
        }

        ConnectInfo connectInfo = new ConnectInfo();
        connectInfo.setClientId(msg.payload().clientIdentifier());
        connectInfo.setPrefix(settings.idPrefix());
        if(msg.variableHeader().hasUserName()){
            connectInfo.setUsername(msg.payload().userName());
            if(msg.variableHeader().hasPassword()){
                connectInfo.setPassword(msg.payload().passwordInBytes());
            }
        }
        connectInfo.setKeepAliveTimeSeconds(msg.variableHeader().keepAliveTimeSeconds());
        connectInfo.setHandlerContext(ctx);

        if(StringUtils.notEmpty(connectInfo.prefix())
                && !connectInfo.clientId().startsWith(connectInfo.prefix())){
            if(logger.isDebugEnabled()){
                logger.debug("prefix is required before client id({})", connectInfo.clientId());
            }

            ctx.writeAndFlush(connackMessage(MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED));

            return;
        }else{
            String id = connectInfo.clientId();
            String prefix = connectInfo.prefix();
            if(StringUtils.notEmpty(prefix)) {
                id = id.substring(id.indexOf(prefix) + prefix.length());
                connectInfo.setClientId(id);
            }
        }

        state = AUTHENTICATING;

        Future<Boolean> futAuth = processor.authenticate(connectInfo, ctx.executor().newPromise());
        futAuth.addListener((Future<Boolean> fut)->{
            if(fut.isSuccess() && fut.get()){
                onAuthSuccess(connectInfo, ctx);
            }else {
                if (!fut.isSuccess()) {
                    logger.info("exception {} occurred in authentication (client id : {})", fut.cause(), connectInfo.clientId());
                }
                ctx.writeAndFlush(connackMessage(MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD));
                ctx.close();
            }
        });
    }

    private void onAuthSuccess(ConnectInfo connectInfo, ChannelHandlerContext ctx){
        sessionManager.createSession(connectInfo, ctx.executor().newPromise())
                .addListener((Future<Session> sessFut)->{
                    if(sessFut.isSuccess()){
                        session = sessFut.get();

                        if(logger.isInfoEnabled()){
                            logger.info("client ({}) connect successfully, keep alive={}s", session.clientId(), connectInfo.keepAliveTimeSeconds());
                        }

                        int timeoutInSec = (int)(connectInfo.keepAliveTimeSeconds() * settings.timeoutFactor());
                        IdleStateHandler idleStateHandler =
                                new IdleStateHandler(timeoutInSec, 0, 0);
                        ctx.channel().pipeline().addBefore(ServerHandler.NAME, "keep alive", idleStateHandler);
                        ctx.writeAndFlush(connackMessage(MqttConnectReturnCode.CONNECTION_ACCEPTED));
                        state = CONNECTED;
                    }else{
                        ctx.close();
                    }
                });
    }

    private void onSubscribe(ChannelHandlerContext ctx, MqttSubscribeMessage msg){
        int id = msg.variableHeader().messageId();
        List<MqttTopicSubscription> subscriptions = msg.payload().topicSubscriptions();

        session.subscribe(msg, ctx.executor().newPromise())
                .addListener((Future<List<Integer>> topicFut)->{
                    if(topicFut.isSuccess()){
                        if(logger.isInfoEnabled()){
                            logger.info("client ({}) subscribe (message id: {}) topics {}", session.clientId(), id, subscriptions);
                        }
                        List<Integer> topics = topicFut.get();
                        MqttSubAckMessage ackMsg = ProMqttMessageFactory.newSubAck(id, topics);
                        session.writeAndFlush(ackMsg);
                    }
                });
    }

    private void onUnsubscribe(ChannelHandlerContext ctx, MqttUnsubscribeMessage msg){
        int id = msg.variableHeader().messageId();
        List<String> topics = msg.payload().topics();

        session.unsubscribe(msg, ctx.executor().newPromise())
                .addListener((Future<Void> fut)->{
                   if(fut.isSuccess()){
                       if(logger.isInfoEnabled()){
                           logger.info("client ({}) unsubscribe (message id: {}) topics {}", session.clientId(), id, topics);
                       }
                       MqttUnsubAckMessage ackMsg = ProMqttMessageFactory.newUnsubAck(id);
                       session.writeAndFlush(ackMsg);
                   }
                });
    }


    private MqttConnAckMessage connackMessage(MqttConnectReturnCode returnCode){
        return MqttMessageBuilders.connAck().sessionPresent(false)
                .returnCode(returnCode)
                .build();
    }


}
