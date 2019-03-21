package apodemas.sheepdog.server.sub;

import apodemas.sheepdog.common.CollectionUtils;
import apodemas.sheepdog.server.Session;
import apodemas.sheepdog.server.Subscription;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author caozheng
 * @time 2019-03-19 13:45
 **/
public class SubscriptionManager {
    private static final InternalLogger LOG = InternalLoggerFactory.getInstance(SubscriptionManager.class);

    private final Map<String, List<Subscription>> sessionTopicMap = new HashMap<>(256);
    private final Map<String, List<Session>> topicSessionMap = new HashMap<>(256);
//    private final ExecutorService executorService = Executors.newFixedThreadPool(3, new ThreadFactoryImpl("SubscriptionExecutorThread_"));
    private final ReadWriteLock mapLock = new ReentrantReadWriteLock();

    public void clean(Session session){
        String clientId = session.clientId();
        mapLock.writeLock().lock();

        if (sessionTopicMap.containsKey(clientId)) {
            for(Subscription sub : sessionTopicMap.get(clientId)){
                topicSessionMap.remove(sub.getTopic(), session);
            }
        }
        mapLock.writeLock().unlock();
    }

    public List<Integer> subscribe(Session session, List<Subscription> subscriptions){
        if (subscriptions == null || subscriptions.size() == 0) {
            return new ArrayList<>();
        }

        mapLock.writeLock().lock();

        List<Subscription> subs = sessionTopicMap.get(session.clientId());
        if (subs == null) {
            subs = new ArrayList<>();
            sessionTopicMap.put(session.clientId(), subs);
        }
        List<Integer> results = new ArrayList<>(subscriptions.size());
        for (Subscription toSub : subscriptions) {
            Optional<Subscription> fromSub = CollectionUtils.findFirst(subs,
                    (Subscription item) -> item.topicEquals(toSub));
            if (fromSub.isPresent()) {
                fromSub.get().setQos(toSub.getQos()); //override getQos
            } else {
                subs.add(toSub);
                saveTopicSessionMapping(toSub.getTopic(), session);
            }
            results.add(toSub.qosValue());
        }

        mapLock.writeLock().unlock();

        return results;
    }

    public void unsubscribe(Session session, List<String> topics){
        mapLock.writeLock().lock();

        for(String topic : topics){
            List<Session> sessions = topicSessionMap.get(topic);
            if(sessions != null) {
                if(sessions.removeIf(item -> item.clientId().equals(session.clientId()))){
                    List<Subscription> subscriptions = sessionTopicMap.get(session.clientId());
                    if(subscriptions != null){
                        subscriptions.removeIf(sub->sub.getQos().equals(topic));
                    }
                }
            }
        }

        mapLock.writeLock().unlock();
    }

    public List<Subscription> getSessionSubscriptions(Session session){
        mapLock.readLock().lock();
        List<Subscription> results = sessionTopicMap.get(session.clientId());
        mapLock.readLock().unlock();

        return results;
    }

    public List<Session> getTopicSubSessions(String topic){
        mapLock.readLock().lock();
        List<Session> results = topicSessionMap.get(topic);
        mapLock.readLock().unlock();

        return results;
    }

    private void saveTopicSessionMapping(String topic, Session session){
        List<Session> subSessions = topicSessionMap.get(topic);
        if(subSessions == null){
            subSessions = new ArrayList<>();
            topicSessionMap.put(topic, subSessions);
        }
        Optional<Session> fromSess = CollectionUtils.findFirst(subSessions,
                (Session item)->item.clientId().equals(session.clientId()));
        if(!fromSess.isPresent()){
            subSessions.add(session);
        }
    }

}
