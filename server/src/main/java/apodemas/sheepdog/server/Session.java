package apodemas.sheepdog.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.*;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

import java.util.List;

/**
 * @author caozheng
 * @time 2019-01-02 16:52
 **/
public interface Session {
    String clientId();
    boolean isOpen();
    void writeAndFlush(MqttMessage message);
    void publish(PublishMessageTemplate template);
    void ack(MqttPubAckMessage message);
    void disconnect();
    void subscribe(MqttSubscribeMessage message);
    void unsubscribe(MqttUnsubscribeMessage message);
}

