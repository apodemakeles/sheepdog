package apodemas.sheepdog.client;

import io.netty.handler.codec.mqtt.MqttConnectReturnCode;

/**
 * @author caozheng
 * @time 2018-12-05 16:00
 **/
public class ClientConnectException extends ClientException {
    private final MqttConnectReturnCode returnCode;

    public ClientConnectException(MqttConnectReturnCode code) {
        super(String.format("failed to connect to remote server (return code  %x)", code.byteValue()));
        this.returnCode = code;
    }

    public MqttConnectReturnCode returnCode(){
        return returnCode;
    }
}
