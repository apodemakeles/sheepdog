package apodemas.sheepdog.server;

import apodemas.sheepdog.common.CollectionUtils;

import java.util.*;

/**
 * @author caozheng
 * @time 2019-01-15 15:50
 **/
public class SubscriptionManager {
    private final Map<String, List<MqttMutableTopicSubscription>> sessionTopicMap = new HashMap<>(256);
    private final Map<String, List<Session>> topicSessionMap = new HashMap<>(256);

    public void clean(Session session){
        String clientId = session.clientId();
        if (sessionTopicMap.containsKey(clientId)) {
            for(MqttMutableTopicSubscription sub : sessionTopicMap.get(clientId)){
                topicSessionMap.remove(sub.topicName(), session);
            }
        }
    }

    public List<Integer> subscribe(Session session, List<MqttMutableTopicSubscription> subscriptions) {
        List<MqttMutableTopicSubscription> subs = sessionTopicMap.get(session.clientId());
        if(subs == null) {
            subs = new ArrayList<>();
        }
        List<Integer> results = new ArrayList<>(subscriptions.size());
        for(MqttMutableTopicSubscription toSub : subscriptions){
            Optional<MqttMutableTopicSubscription> fromSub = CollectionUtils.findFirst(subs,
                    (MqttMutableTopicSubscription item)-> item.topicName().equals(toSub.topicName()));
            if(fromSub.isPresent()){
                fromSub.get().setQualityOfService(toSub.qualityOfService()); //override qos
            }else{
                subs.add(toSub);
                saveTopicSessionMapping(toSub.topicName(), session);
            }
            results.add(toSub.qualityOfService().value());
        }

        return results;
    }

    public void unsubscribe(Session session, List<String> topics){
        for(String topic : topics){
            List<Session> sessions = topicSessionMap.get(topic);
            if(sessions != null) {
                if(sessions.removeIf(item -> item.clientId().equals(session.clientId()))){
                    List<MqttMutableTopicSubscription> subscriptions = sessionTopicMap.get(session.clientId());
                    if(subscriptions != null){
                        subscriptions.removeIf(sub->sub.topicName().equals(topic));
                    }
                }
            }
        }
    }

    public List<MqttMutableTopicSubscription> getSessionSubscriptions(Session session){
        return sessionTopicMap.get(session.clientId());
    }

    public List<Session> getTopicSubSessions(String topic){
        return topicSessionMap.get(topic);
    }

    private void saveTopicSessionMapping(String topic, Session session){
        List<Session> subSessions = topicSessionMap.get(topic);
        if(subSessions == null){
            subSessions = new ArrayList<>();
        }
        Optional<Session> fromSess = CollectionUtils.findFirst(subSessions,
                (Session item)->item.clientId().equals(session.clientId()));
        if(!fromSess.isPresent()){
            subSessions.add(session);
        }
    }

}
