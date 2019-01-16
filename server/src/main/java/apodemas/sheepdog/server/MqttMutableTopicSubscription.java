package apodemas.sheepdog.server;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import io.netty.util.internal.StringUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author caozheng
 * @time 2019-01-15 17:24
 **/
public class MqttMutableTopicSubscription {
    private final String topicFilter;
    private MqttQoS qualityOfService;

    public MqttMutableTopicSubscription(String topicFilter, MqttQoS qualityOfService) {
        this.topicFilter = topicFilter;
        this.qualityOfService = qualityOfService;
    }

    public MqttMutableTopicSubscription(MqttTopicSubscription subscription){
        this.topicFilter = subscription.topicName();
        this.qualityOfService = subscription.qualityOfService();
    }

    public String topicName() {
        return topicFilter;
    }

    public MqttQoS qualityOfService() {
        return qualityOfService;
    }

    public void setQualityOfService(MqttQoS qos){
        qualityOfService = qos;
    }

    @Override
    public String toString() {
        return new StringBuilder(StringUtil.simpleClassName(this))
                .append('[')
                .append("topicFilter=").append(topicFilter)
                .append(", qualityOfService=").append(qualityOfService)
                .append(']')
                .toString();
    }

    public static List<MqttMutableTopicSubscription> form(List<MqttTopicSubscription> originalSubs){
        List<MqttMutableTopicSubscription> results = new ArrayList<>();
        for(MqttTopicSubscription orgSub : originalSubs){
            MqttMutableTopicSubscription sub
                    = new MqttMutableTopicSubscription(orgSub.topicName(), orgSub.qualityOfService());
            results.add(sub);
        }

        return results;
    }
}
