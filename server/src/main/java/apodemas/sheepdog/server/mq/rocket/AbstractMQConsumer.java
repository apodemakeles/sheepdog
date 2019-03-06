package apodemas.sheepdog.server.mq.rocket;

import apodemas.sheepdog.server.mq.MQMessageProtos;
import apodemas.sheepdog.server.mq.MQMessageProtos.MQMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.apache.rocketmq.common.message.MessageExt;

import java.util.ArrayList;
import java.util.List;

/**
 * @author caozheng
 * @time 2019-02-25 17:28
 **/
public abstract class AbstractMQConsumer {

    private static final InternalLogger LOG = InternalLoggerFactory.getInstance(AbstractMQConsumer.class);

    protected List<MQMessage> deserializeMessage(List<MessageExt> messages){
        List<MQMessage> msgList = new ArrayList<>();
        for(MessageExt msg : messages) {
            try {
                MQMessage mqMsg = MQMessageProtos.MQMessage.newBuilder().mergeFrom(msg.getBody()).build();
                if(mqMsg.getType().equals(MQMessage.MessageType.FAILURE)){
                    LOG.warn("MQConsumer deserialize message ({}) with invalid type 0", msg.getMsgId());
                }else{
                    msgList.add(mqMsg);
                }
            } catch (InvalidProtocolBufferException ipbEx) {
                LOG.warn("MQConsumer deserialize message failed via protobuf", ipbEx);
            }
        }

        return msgList;
    }
}
