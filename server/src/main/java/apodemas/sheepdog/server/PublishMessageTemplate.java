package apodemas.sheepdog.server;

import io.netty.handler.codec.mqtt.MqttQoS;

/**
 * @author caozheng
 * @time 2019-01-22 09:12
 **/
public class PublishMessageTemplate {
    private boolean retained = false;
    private byte[] payload = EMPTY_PAYLOAD;
    private String topic;
    private MqttQoS qos = MqttQoS.AT_LEAST_ONCE;

    private static byte[] EMPTY_PAYLOAD = new byte[0];

    public void setRetained(boolean retained) {
        this.retained = retained;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public void setQos(MqttQoS qos) {
        this.qos = qos;
    }

    public boolean retained() {
        return retained;
    }

    public byte[] payload() {
        return payload;
    }

    public String topic() {
        return topic;
    }

    public MqttQoS qos() {
        return qos;
    }
}
