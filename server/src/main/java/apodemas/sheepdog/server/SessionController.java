package apodemas.sheepdog.server;

import apodemas.sheepdog.core.concurrent.FutureListenerBuilder;
import apodemas.sheepdog.server.distributed.ClientRegistryInfo;
import apodemas.sheepdog.server.distributed.ClientRegistryResult;
import apodemas.sheepdog.server.distributed.ClientUnregistryInfo;
import apodemas.sheepdog.server.distributed.RemotingService;
import apodemas.sheepdog.server.pub.PubSettings;
import apodemas.sheepdog.server.pub.PublishController;
import apodemas.sheepdog.server.sub.SubscriptionController;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoop;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author caozheng
 * @time 2019-03-08 13:37
 **/
public class SessionController implements SessionService{
    private static final InternalLogger LOG = InternalLoggerFactory.getInstance(SessionController.class);

    private final String serverId;
    private final Map<String, LocalMemorySession> sessions = new HashMap<>(256);
    private final ReadWriteLock mapLock = new ReentrantReadWriteLock();
    private final EventLoop eventLoop;
    private final RemotingService remotingService;
    private final ServerSettings serverSettings;
    private final SubscriptionController subscriptionController;

    public SessionController(String serverId, EventLoop eventLoop, RemotingService remotingService,
                             ServerSettings settings, SubscriptionController subscriptionController) {
        this.serverId = serverId;
        this.eventLoop = eventLoop;
        this.remotingService = remotingService;
        this.serverSettings = settings;
        this.subscriptionController = subscriptionController;
    }

    public Future<Session> registerClient(ConnectInfo connectInfo, Promise<Session> promise) {
        ClientRegistryInfo regInfo = new ClientRegistryInfo();
        regInfo.setClientId(connectInfo.clientId());
        regInfo.setUpTimestamp(System.currentTimeMillis());
        regInfo.setUsername(connectInfo.username());
        regInfo.setServerId(serverId);
        Future<ClientRegistryResult> regResult = remotingService.RegisterClient(regInfo, eventLoop.newPromise());
        regResult.addListener(FutureListenerBuilder.successThen(result -> createSession(connectInfo), promise));

        return promise;
    }

    @Override
    public void removeSession(Session session){
        String clientId = session.clientId();
        mapLock.writeLock().lock();
        if(sessions.containsKey(clientId)){
            sessions.remove(clientId);
        }
        mapLock.writeLock().unlock();
        unregisterClient(clientId);
    }

    private void unregisterClient(String clientId){
        ClientUnregistryInfo unregInfo = new ClientUnregistryInfo();
        unregInfo.setClientId(clientId);
        unregInfo.setServerId(serverId);
        remotingService.UnregisterClient(unregInfo, eventLoop.newPromise());
    }

    private Session createSession(ConnectInfo connectInfo){
        String clientId = connectInfo.clientId();
        ChannelHandlerContext ctx = connectInfo.handlerContext();
        mapLock.writeLock().lock();
        if (sessions.containsKey(clientId)) {
            LOG.warn("duplicated client id({})", clientId);
            mapLock.writeLock().unlock();

            throw new ClientConnectedException(clientId, serverId);
        }
        PubSettings pubSettings = new PubSettings();
        pubSettings.setMaxRepubQueueSize(serverSettings.maxRepubQueueSize());
        pubSettings.setMaxRepubTimes(serverSettings.maxRepubTimes());
        pubSettings.setPublishAckTimeoutSec(serverSettings.publishAckTimeoutSec());
        PublishController publishController = new PublishController(ctx, pubSettings);

        LocalMemorySession session = new LocalMemorySession(ctx, clientId,
                publishController,this, subscriptionController);

        sessions.put(clientId, session);

        mapLock.writeLock().unlock();

        return session;
    }

    public List<Session> sessions(){
        mapLock.readLock().lock();
        List<Session> sessionList = new ArrayList<>();
        for(Session session : sessions.values()){
            sessionList.add(session);
        }
        mapLock.readLock().unlock();

        return sessionList;
    }

    public Session findSession(String clientId){
        mapLock.readLock().lock();
        Session session = sessions.get(clientId);
        mapLock.readLock().unlock();

        return session;
    }
}
