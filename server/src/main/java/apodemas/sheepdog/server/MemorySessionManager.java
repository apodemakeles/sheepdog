package apodemas.sheepdog.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultEventLoop;
import io.netty.channel.EventLoop;
import io.netty.handler.codec.mqtt.*;
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
    private static final InternalLogger LOG = InternalLoggerFactory.getInstance(MemorySessionManager.class);

    private final Map<String, MemorySession> sessions = new HashMap<>(256);
    private final SubscriptionManager subManager = new SubscriptionManager();
    private final EventLoop eventLoop;
    private final ServerSettings serverSettings;

    public MemorySessionManager(ServerSettings serverSettings) {
        this.eventLoop = new DefaultEventLoop();
        this.serverSettings = serverSettings;
    }

    public Future<Session> createSession(ConnectInfo connectInfo, Promise<Session> promise){
        safeExecute(()->{
            doCreate(connectInfo, promise);
        });

        return promise;
    }

    public Future<List<ClientSessionInfo>> sessions(Promise<List<ClientSessionInfo>> promise){
        safeExecute(()->{
            List<ClientSessionInfo> results = new ArrayList<>();
            for(MemorySession item : sessions.values()){
                List<Subscription> subscriptions = subManager.getSessionSubscriptions(item);
                ClientSessionInfo info = new ClientSessionInfo(item.clientId, subscriptions);
                results.add(info);
            }

            promise.trySuccess(results);
        });

        return promise;
    }

    public Future<Session> findSession(String clientId){
        return findSession(clientId, eventLoop.newPromise());
    }

    public Future<Session> findSession(String clientId, Promise<Session> promise){
        safeExecute(()->{
            promise.trySuccess(sessions.get(clientId));
        });

        return promise;
    }

    public Future<ClientSessionInfo> getClientInfo(String clientId, Promise<ClientSessionInfo> promise){
        safeExecute(()->{
            MemorySession session = sessions.get(clientId);
            if(session != null) {
                List<Subscription> subscriptions = subManager.getSessionSubscriptions(session);
                promise.trySuccess(new ClientSessionInfo(clientId, Subscription.clone(subscriptions)));
            }else{
                promise.trySuccess(null);
            }
        });

        return promise;
    }

    @Override
    public Future<List<ClientSessionInfo>> findSubscription(String topic, Promise<List<ClientSessionInfo>> promise) {
        safeExecute(()->{
            List<Session> sess = subManager.getTopicSubSessions(topic);
            if(sess == null){
                sess = new ArrayList<>();
            }

            List<ClientSessionInfo> results = new ArrayList<>();
            for(Session session : sess){
                results.add(new ClientSessionInfo(session.clientId(), null));
            }

            promise.trySuccess(results);

        });

        return promise;
    }

    @Override
    public Future<List<Session>> findSessionByTopic(String topic, Promise<List<Session>> promise){
        safeExecute(()->{
            promise.trySuccess(subManager.getTopicSubSessions(topic));
        });

        return promise;
    }

    private void doCreate(ConnectInfo connectInfo, Promise<Session> promise){
        String clientId = connectInfo.clientId();
        ChannelHandlerContext ctx = connectInfo.handlerContext();
        try {
            PublishController pubCtrl = new PublishController(ctx, serverSettings);
            MemorySession session = new MemorySession(ctx, clientId, pubCtrl);
            if (sessions.containsKey(clientId)) {
                MemorySession oldSession = sessions.get(clientId);
                LOG.warn("overlapping occurred client {}, stopping old one", clientId);
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
        private final PublishController pubCtrl;

        public MemorySession(ChannelHandlerContext ctx, String clientId, PublishController pubCtrl) {
            this.ctx = ctx;
            this.clientId = clientId;
            this.pubCtrl = pubCtrl;
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
        public void publish(PublishMessageTemplate template){
            pubCtrl.publish(template);
        }

        @Override
        public void ack(MqttPubAckMessage message){
            pubCtrl.ack(message);
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
