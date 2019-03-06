package apodemas.sheepdog.server.mq.rocket;

import apodemas.sheepdog.server.CustomSetting;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;

/**
 * @author caozheng
 * @time 2019-02-25 16:54
 **/
public class RocketMQSettings implements CustomSetting {
    private String groupName = "mqtt-consumer";
    private String nameSrvAddr = "127.0.0.1:9876";
    private ConsumeFromWhere consumeFromWhere = ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET;
    private String topic = "mqtt-topic";
    private String subExpression = "*";

    static RocketMQSettings DEFAULT;

    static{
        RocketMQSettings settings = new RocketMQSettings();

        DEFAULT = settings;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public void setNameSrvAddr(String nameSrvAddr) {
        this.nameSrvAddr = nameSrvAddr;
    }

    public void setConsumeFromWhere(ConsumeFromWhere consumeFromWhere) {
        this.consumeFromWhere = consumeFromWhere;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public void setSubExpression(String subExpression) {
        this.subExpression = subExpression;
    }

    public String groupName() {
        return groupName;
    }

    public String nameSrvAddr() {
        return nameSrvAddr;
    }

    public ConsumeFromWhere consumeFromWhere() {
        return consumeFromWhere;
    }

    public String topic() {
        return topic;
    }

    public String subExpression() {
        return subExpression;
    }
}
