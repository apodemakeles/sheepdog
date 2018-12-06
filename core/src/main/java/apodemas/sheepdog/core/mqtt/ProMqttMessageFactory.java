package apodemas.sheepdog.core.mqtt;

import io.netty.handler.codec.mqtt.*;

/**
 * @author caozheng
 * @time 2018-12-04 14:49
 **/
public class ProMqttMessageFactory {
    private static MqttFixedHeader defaultHeader(MqttMessageType type, int remainingLength){
        return new MqttFixedHeader(type, false, MqttQoS.AT_MOST_ONCE, false, remainingLength);
    }

    public static MqttPubAckMessage newPubAck(int messageId){
        return (MqttPubAckMessage)MqttMessageFactory.newMessage(
                defaultHeader(MqttMessageType.PUBACK, 2),
                MqttMessageIdVariableHeader.from(messageId),
                null
                );
    }

    public static MqttMessage newPingreq(){
        return MqttMessageFactory.newMessage(
                defaultHeader(MqttMessageType.PINGREQ, 0),
                null,
                null
        );
    }
}
