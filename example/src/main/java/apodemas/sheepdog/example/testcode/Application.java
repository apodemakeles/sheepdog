package apodemas.sheepdog.example.testcode;

import apodemas.sheepdog.server.ClientSessionInfo;
import apodemas.sheepdog.server.Subscription;
import com.alibaba.fastjson.JSON;
import io.netty.handler.codec.mqtt.MqttQoS;

import java.util.ArrayList;
import java.util.List;

/**
 * @author caozheng
 * @time 2019-01-20 09:05
 **/
public class Application {
    public static void main(String[] args){
        List<ClientSessionInfo> lists = new ArrayList<>();
        List<Subscription> subscriptions = new ArrayList<>();
        subscriptions.add(new Subscription("abc", MqttQoS.AT_LEAST_ONCE));
        lists.add(new ClientSessionInfo("631",subscriptions));
        byte[] bytes = JSON.toJSONBytes(lists);
        System.out.println(bytes.length);
    }
}
