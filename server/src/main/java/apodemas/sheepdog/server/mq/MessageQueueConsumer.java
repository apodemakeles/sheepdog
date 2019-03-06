package apodemas.sheepdog.server.mq;

import apodemas.sheepdog.server.SettingsAware;

/**
 * @author caozheng
 * @time 2019-02-25 16:32
 **/
public interface MessageQueueConsumer extends SettingsAware {
    void start(MessageConsumer consumer) throws MQConsumerStartupException;
    void shutdown();
}
