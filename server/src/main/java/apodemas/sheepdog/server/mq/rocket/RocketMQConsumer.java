package apodemas.sheepdog.server.mq.rocket;

import apodemas.sheepdog.common.Checks;
import apodemas.sheepdog.server.ServerSettings;
import apodemas.sheepdog.server.mq.AbstractMQConsumer;
import apodemas.sheepdog.server.mq.MQConsumerStartupException;
import apodemas.sheepdog.server.mq.MQMessageProtos.MQMessage;
import apodemas.sheepdog.server.mq.MessageConsumer;
import apodemas.sheepdog.server.mq.MessageQueueConsumer;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;

import java.util.List;

/**
 * @author caozheng
 * @time 2019-02-25 17:02
 **/
public class RocketMQConsumer extends AbstractMQConsumer implements MessageQueueConsumer {
    private RocketMQSettings settings;
    private DefaultMQPushConsumer mqConsumer;

    private static final InternalLogger LOG = InternalLoggerFactory.getInstance(RocketMQConsumer.class);

    public void initWithSettings(ServerSettings settings){
        this.settings = settings.getCustomizeSetting(RocketMQSettings.class);
        if(this.settings == null){
            this.settings = RocketMQSettings.DEFAULT;
        }
    }

    @Override
    public void start(MessageConsumer consumer) throws MQConsumerStartupException {
        Checks.notNull(consumer, "consumer");

        try {
            mqConsumer = new DefaultMQPushConsumer(settings.groupName());
            mqConsumer.setNamesrvAddr(settings.nameSrvAddr());
            mqConsumer.setConsumeFromWhere(settings.consumeFromWhere());
            mqConsumer.subscribe(settings.topic(), settings.subExpression());
            mqConsumer.registerMessageListener(new MessageListenerConcurrently(){
                @Override
                public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> messages, ConsumeConcurrentlyContext consumeOrderlyContext) {
                    List<MQMessage> msgList = deserializeMessage(messages);
                    System.out.println("consume " + msgList.size() + " messages");
                    for(MQMessage msg : msgList) {
                        try {
                            consumer.accept(msg);
                        }catch (Exception e){
                            LOG.warn("MessageConsumer consume message failed", e);
                        }
                    }

                    return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                }
            });

            mqConsumer.start();
        }catch (MQClientException e){
            throw new MQConsumerStartupException("rocket mq client startup failed", e);
        }
    }

    public void shutdown() {
        if (mqConsumer != null) {
            mqConsumer.shutdown();
        }
    }






}
