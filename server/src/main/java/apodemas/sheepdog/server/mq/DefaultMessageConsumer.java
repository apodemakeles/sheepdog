package apodemas.sheepdog.server.mq;

import apodemas.sheepdog.server.PublishMessageTemplate;
import apodemas.sheepdog.server.Session;
import apodemas.sheepdog.server.SessionManager;
import apodemas.sheepdog.server.mq.MQMessageProtos.MQMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.util.concurrent.Future;

/**
 * @author caozheng
 * @time 2019-03-06 12:54
 **/
public class DefaultMessageConsumer implements MessageConsumer {
    private final SessionManager sessionManager;

    public DefaultMessageConsumer(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public void accept(MQMessage mqMessage) {
        if(mqMessage.getType().equals(MQMessage.MessageType.PUBLISH)){
            PublishMessageTemplate template = getPublishTemplate(mqMessage);
            sessionManager.findSession(template.topic()).addListener((Future<Session> fut)->{
                if(fut.isSuccess()){
                    Session session = fut.get();
                    if(session != null) {
                        session.publish(template);
                    }
                }
            }) ;
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
