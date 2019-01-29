package apodemas.sheepdog.example.client;

import apodemas.sheepdog.client.Client;
import apodemas.sheepdog.client.MessageListener;
import apodemas.sheepdog.client.Session;
import io.netty.handler.codec.mqtt.MqttMessageBuilders;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.util.concurrent.Future;

import java.nio.charset.Charset;


/**
 * @author caozheng
 * @time 2018-12-07 14:19
 **/
public class ClientApplication {
    public static void main(String[] args) throws Exception{
        String prefix = "ABCDEF1234567890";
        String id = "6340299262395416576";

        Client client = new Client();
        client.setClientId(prefix + id);
        client.setKeepAliveSec(10);
        client.setAuth(id, "abc".getBytes());
        client.setMessageListener(new MessageListener() {
            @Override
            public boolean onPublish(Session session, MqttPublishMessage msg) {
                System.out.printf("topic %s payload %s %n",
                        msg.variableHeader().topicName(),
                        msg.content().toString(Charset.forName("utf-8")));

                return true;
            }
        });
        Future<Session> connectFut = client.connect("127.0.0.1", 3883);

        try {
            Session session = connectFut.get();
            System.out.println("connect success ");
            MqttSubscribeMessage msg = MqttMessageBuilders.subscribe().messageId(1)
                    .addSubscription(MqttQoS.AT_LEAST_ONCE, "6340299262395416576")
                    .build();
            session.subscribe(msg).await();
            System.out.println("sub success");

        }catch (Throwable e){
            System.out.println("connect exception : " + e);
        }

        Future<?> terFuture = client.terminationFuture();
        terFuture.await();
        if(terFuture.cause() != null){
            System.out.println(terFuture.cause());
        }

    }
}
