package apodemas.sheepdog.server;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import io.netty.util.internal.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author caozheng
 * @time 2019-01-19 11:45
 **/
public class Subscription {
    private final String topic;
    private MqttQoS qos;

    public Subscription(String topic, MqttQoS qos) {
        this.topic = topic;
        this.qos = qos;
    }

    public void setQos(MqttQoS qos) {
        this.qos = qos;
    }

    public String getTopic() {
        return topic;
    }

    public MqttQoS getQos() {
        return qos;
    }

    public int qosValue(){
        return qos.value();
    }

    public boolean topicEquals(Subscription that){
        return topic.equals(that.topic);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Subscription that = (Subscription) o;
        return Objects.equals(topic, that.topic) &&
                qos == that.qos;
    }

    @Override
    public int hashCode() {
        return Objects.hash(topic, qos);
    }

    @Override
    public String toString() {
        return new StringBuilder(StringUtil.simpleClassName(this))
                .append('[')
                .append("getTopic=").append(topic)
                .append(", getQos=").append(qos)
                .append(']')
                .toString();
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return new Subscription(topic, qos);
    }

    public static List<Subscription> fromMqttTopicSubscription(List<MqttTopicSubscription> originalSubs){
        List<Subscription> results = new ArrayList<>();
        for(MqttTopicSubscription orgSub : originalSubs){
            Subscription sub
                    = new Subscription(orgSub.topicName(), orgSub.qualityOfService());
            results.add(sub);
        }

        return results;
    }

    public static List<Subscription> clone(List<Subscription> originalSubs){
        List<Subscription> results = new ArrayList<>();
        for(Subscription sub : originalSubs){
            try {
                results.add((Subscription) sub.clone());
            }catch (CloneNotSupportedException e){

            }
        }

        return results;
    }
}
