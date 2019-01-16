package apodemas.sheepdog.server;

import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttUnsubscribeMessage;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

import java.util.List;

/**
 * @author caozheng
 * @time 2019-01-02 16:52
 **/
public interface Session {
    String clientId();
    boolean isConnected();
    void writeAndFlush(MqttMessage message);
    void disconnect();
    Future<List<Integer>> subscribe(MqttSubscribeMessage message, Promise<List<Integer>> promise);
    Future<Void> unsubscribe(MqttUnsubscribeMessage message, Promise<Void> promise);
}

