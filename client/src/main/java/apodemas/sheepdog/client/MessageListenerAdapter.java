package apodemas.sheepdog.client;

import io.netty.handler.codec.mqtt.MqttPublishMessage;

/**
 * @author caozheng
 * @time 2018-12-05 16:32
 **/
public class MessageListenerAdapter implements MessageListener {
    public boolean onPublish(Session ctx, MqttPublishMessage msg){
        return true;
    }
}
