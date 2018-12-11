package apodemas.sheepdog.client;

import io.netty.handler.codec.mqtt.MqttPublishMessage;

/**
 * @author caozheng
 * @time 2018-12-05 16:27
 **/
public interface MessageListener {
    boolean onPublish(Session session, MqttPublishMessage msg);
}
