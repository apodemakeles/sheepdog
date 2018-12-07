package apodemas.sheepdog.example.client;

import apodemas.sheepdog.client.Client;
import io.netty.util.concurrent.Future;


/**
 * @author caozheng
 * @time 2018-12-07 14:19
 **/
public class Application {
    public static void main(String[] args) throws Exception{
        String prefix = "ABCDEF1234567890";
        String id = "6340299262395416576";

        Client client = new Client();
        client.setClientId(prefix + id);
        client.setKeepAliveSec(10);
        client.setAuth(id, "abc".getBytes());
        Future<?> fut = client.connect("mqtt.united-iot.com", 3883);
        fut.await();
        System.out.println(fut.isSuccess());
        System.out.println(fut.cause());
    }
}
