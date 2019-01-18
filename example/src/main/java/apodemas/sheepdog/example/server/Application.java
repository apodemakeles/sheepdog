package apodemas.sheepdog.example.server;


import apodemas.sheepdog.server.Server;
import apodemas.sheepdog.server.ServerSettings;
import io.netty.util.concurrent.Future;

/**
 * @author caozheng
 * @time 2019-01-16 14:47
 **/
public class Application {
    public static void main(String[] args) throws Exception{
        ServerSettings serverSettings = new ServerSettings();
        serverSettings.setIdPrefix("ABCDEF1234567890");

        Server server = new Server(serverSettings);
        Future<Void> terFuture = server.bind("127.0.0.1", 3883);
        terFuture.await();
    }
}
