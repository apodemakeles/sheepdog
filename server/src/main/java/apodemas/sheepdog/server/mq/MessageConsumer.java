package apodemas.sheepdog.server.mq;

import java.util.function.Consumer;

/**
 * @author caozheng
 * @time 2019-02-25 16:36
 **/
@FunctionalInterface
public interface MessageConsumer extends Consumer<MQMessageProtos.MQMessage> {

}
