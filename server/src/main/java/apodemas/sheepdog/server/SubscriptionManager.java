package apodemas.sheepdog.server;

import apodemas.sheepdog.common.CollectionUtils;

import java.util.*;

/**
 * @author caozheng
 * @time 2019-01-15 15:50
 **/
public class SubscriptionManager {
    private final Map<String, List<Subscription>> sessionTopicMap = new HashMap<>(256);
    private final Map<String, List<Session>> topicSessionMap = new HashMap<>(256);

    public void clean(Session session){
        String clientId = session.clientId();
        if (sessionTopicMap.containsKey(clientId)) {
            for(Subscription sub : sessionTopicMap.get(clientId)){
                topicSessionMap.remove(sub.getTopic(), session);
            }
        }
    }

    public List<Integer> subscribe(Session session, List<Subscription> subscriptions) {
        if (subscriptions == null || subscriptions.size() == 0) {
            return new ArrayList<>();
        }

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

        return results;
    }

    public void unsubscribe(Session session, List<String> topics){
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
    }

    public List<Subscription> getSessionSubscriptions(Session session){
        return sessionTopicMap.get(session.clientId());
    }

    public List<Session> getTopicSubSessions(String topic){
        return topicSessionMap.get(topic);
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
