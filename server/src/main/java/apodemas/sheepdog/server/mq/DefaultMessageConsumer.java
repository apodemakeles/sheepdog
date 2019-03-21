package apodemas.sheepdog.server.mq;

import apodemas.sheepdog.server.PublishMessageTemplate;
import apodemas.sheepdog.server.Session;
import apodemas.sheepdog.server.mq.MQMessageProtos.MQMessage;
import apodemas.sheepdog.server.sub.SubscriptionManager;
import io.netty.handler.codec.mqtt.MqttQoS;

import java.util.List;

/**
 * @author caozheng
 * @time 2019-03-06 12:54
 **/
public class DefaultMessageConsumer implements MessageConsumer {
    private final SubscriptionManager subscriptionManager;

    public DefaultMessageConsumer(SubscriptionManager subscriptionManager) {
        this.subscriptionManager = subscriptionManager;
    }

    @Override
    public void accept(MQMessage mqMessage) {
        if (mqMessage.getType().equals(MQMessage.MessageType.PUBLISH)) {
            PublishMessageTemplate template = getPublishTemplate(mqMessage);
            List<Session> sessions = this.subscriptionManager.getTopicSubSessions(template.topic());
            if(sessions!= null) {
                for (Session session : sessions) {
                    session.publish(template);
                }
            }
        }
    }

    private PublishMessageTemplate getPublishTemplate(MQMessage mqMessage){
        PublishMessageTemplate template = new PublishMessageTemplate();
        template.setTopic(mqMessage.getTopic());
        template.setPayload(mqMessage.getPayload().toByteArray());
        MqttQoS qos = MqttQoS.valueOf(mqMessage.getQos().getNumber());
        template.setQos(qos);

        return template;
    }
}
