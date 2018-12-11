package apodemas.sheepdog.client;

import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttUnsubscribeMessage;
import io.netty.util.concurrent.Future;

/**
 * @author caozheng
 * @time 2018-12-10 09:41
 **/
public interface Session {
    ByteBufAllocator allocator();
    Future<Void> publish(MqttPublishMessage msg);
    Future<?> subscribe(MqttSubscribeMessage msg);
    Future<?> unsubscribe(MqttUnsubscribeMessage msg);
}
