package apodemas.sheepdog.example.testcode;

import apodemas.sheepdog.server.mq.MQMessageProtos;
import com.google.protobuf.ByteString;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;

/**
 * @author caozheng
 * @time 2019-03-06 13:21
 **/
public class RocketMQPublishMessageProducer {

    public static void main(String[] args) throws Exception{
        DefaultMQProducer producer = new
                DefaultMQProducer("mqtt_producer");

        producer.setNamesrvAddr("127.0.0.1:9876");
        producer.start();

        for (int i = 0; i < 100; i++) {
            String payload = "message " + i;
            MQMessageProtos.MQMessage msg = MQMessageProtos.MQMessage
                    .newBuilder()
                    .setType(MQMessageProtos.MQMessage.MessageType.PUBLISH)
                    .setPayload(ByteString.copyFromUtf8(payload))
                    .setQos(MQMessageProtos.MQMessage.QosType.AT_LEAST_ONCE)
                    .setTopic("6340299262395416576")
                    .build();
            Message mqMsg = new Message("mqtt-topic", msg.toByteArray());
            SendResult result = producer.send(mqMsg);
            System.out.println(result.getSendStatus());
        }
    }
}
