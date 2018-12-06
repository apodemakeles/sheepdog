package apodemas.sheepdog.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.util.concurrent.Future;

import java.util.concurrent.TimeUnit;

/**
 * @author caozheng
 * @time 2018-12-06 08:40
 **/
public interface RepublishScheduler {
    Future<Void> schedule(ChannelHandlerContext ctx, MqttPublishMessage msg, long delay, TimeUnit unit);
}
