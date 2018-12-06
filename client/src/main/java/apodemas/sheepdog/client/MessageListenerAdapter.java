package apodemas.sheepdog.client;

import io.netty.handler.codec.mqtt.MqttPublishMessage;

/**
 * @author caozheng
 * @time 2018-12-05 16:32
 **/
public class MessageListenerAdapter implements MessageListener {
    public boolean onPublish(ClientContext ctx, MqttPublishMessage msg){
        return true;
    }

    public void onConnectSuccess(ClientContext ctx){

    }
}
