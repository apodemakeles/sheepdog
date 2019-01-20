package apodemas.sheepdog.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultEventLoop;
import io.netty.channel.EventLoop;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import io.netty.handler.codec.mqtt.MqttUnsubscribeMessage;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author caozheng
 * @time 2019-01-07 15:54
 **/
public class MemorySessionManager implements SessionManager {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(MemorySessionManager.class);

    private final Map<String, MemorySession> sessions = new HashMap<>(256);
    private final SubscriptionManager subManager = new SubscriptionManager();
    private final EventLoop eventLoop;

    public MemorySessionManager(){
        eventLoop = new DefaultEventLoop();
    }

    public Future<Session> createSession(ConnectInfo connectInfo, Promise<Session> promise){
        safeExecute(()->{
            doCreate(connectInfo, promise);
        });

        return promise;
    }

    public Future<List<ClientSessionInfo>> sessions(Promise<List<ClientSessionInfo>> promise){
        safeExecute(()->{
            promise.trySuccess(doGetAllSessionInfo());
        });

        return promise;
    }

    private List<ClientSessionInfo> doGetAllSessionInfo(){
        List<ClientSessionInfo> results = new ArrayList<>();
        for(MemorySession item : sessions.values()){
            List<Subscription> subscriptions = subManager.getSessionSubscriptions(item);
            ClientSessionInfo info = new ClientSessionInfo(item.clientId, subscriptions);
            results.add(info);
        }

        return results;
    }

    private void doCreate(ConnectInfo connectInfo, Promise<Session> promise){
        String clientId = connectInfo.clientId();
        try {
            MemorySession session = new MemorySession(connectInfo.handlerContext(), clientId);
            if (sessions.containsKey(clientId)) {
                MemorySession oldSession = sessions.get(clientId);
                logger.warn("overlapping occurred client {}, stopping old one", clientId);
                removeSession(oldSession);
                oldSession.closeContext();
            }
            sessions.put(clientId, session);

            promise.trySuccess(session);
        }catch (Exception e){
            promise.tryFailure(e);
        }
    }

    private void doSub(MemorySession session, List<Subscription> subscriptions,
                       Promise<List<Integer>> promise){
        String clientId = session.clientId();
        if(!sessions.containsKey(clientId)){
            promise.tryFailure(new SessionNotFoundExecption(clientId));
        }else{
            List<Integer> results = subManager.subscribe(session, subscriptions);
            promise.trySuccess(results);
        }
    }

    private void doUnsub(MemorySession session, List<String> topics, Promise<Void> promise){
        String clientId = session.clientId();
        if(!sessions.containsKey(clientId)){
            promise.tryFailure(new SessionNotFoundExecption(clientId));
        }else{
            subManager.unsubscribe(session, topics);
            promise.trySuccess(null);
        }
    }

    private void removeSession(MemorySession session){
        sessions.remove(session.clientId());
        subManager.clean(session);
    }

    private void disconnectSession(MemorySession session){
        safeExecute(()->{
            removeSession(session);
            session.closeContext();
        });
    }

    private void subscribeTopic(MemorySession session, List<Subscription> subscriptions,
                           Promise<List<Integer>> promise){
        safeExecute(()->{
            doSub(session, subscriptions, promise);
        });
    }

    private void unsubscribeTopic(MemorySession session, List<String> topics, Promise<Void> promise){
        safeExecute(()->{
            doUnsub(session, topics, promise);
        });
    }

    private void safeExecute(Runnable runnable){
        if(eventLoop.inEventLoop()){
            runnable.run();
        }else{
            eventLoop.submit(runnable);
        }
    }

    public class MemorySession implements Session{
        private final ChannelHandlerContext ctx;
        private final String clientId;
        private final AtomicBoolean disconnected = new AtomicBoolean(false);

        public MemorySession(ChannelHandlerContext ctx, String clientId) {
            this.ctx = ctx;
            this.clientId = clientId;
        }

        public void closeContext(){
            ctx.close();
        }

        @Override
        public String clientId(){
            return clientId;
        }

        @Override
        public boolean isConnected(){
            return !disconnected.get();
        }

        @Override
        public void writeAndFlush(MqttMessage message){
            ctx.writeAndFlush(message);
        }

        @Override
        public void disconnect(){
            if(disconnected.compareAndSet(false,true)){
                MemorySessionManager.this.disconnectSession(this);
            }
        }

        @Override
        public Future<List<Integer>> subscribe(MqttSubscribeMessage message, Promise<List<Integer>> promise){
            List<MqttTopicSubscription> originalSubs = message.payload().topicSubscriptions();
            List<Subscription> subs = Subscription.fromMqttTopicSubscription(originalSubs);
            MemorySessionManager.this.subscribeTopic(this, subs, promise);

            return promise;
        }

        @Override
        public Future<Void> unsubscribe(MqttUnsubscribeMessage message, Promise<Void> promise){
            List<String> topics = message.payload().topics();
            MemorySessionManager.this.unsubscribeTopic(this, topics, promise);

            return promise;
        }

    }
}
