package apodemas.sheepdog.example.server;


import apodemas.sheepdog.server.Server;
import apodemas.sheepdog.server.ServerSettings;
import apodemas.sheepdog.server.mq.rocket.RocketMQSettings;
import io.netty.util.concurrent.Future;

/**
 * @author caozheng
 * @time 2019-01-16 14:47
 **/
public class ServerApplication {
    public static void main(String[] args) throws Exception{
        ServerSettings serverSettings = new ServerSettings();
        serverSettings.setClientIdPrefix("ABCDEF1234567890");
        RocketMQSettings rocketMQSettings = new RocketMQSettings();
        serverSettings.setCustomSetting(rocketMQSettings);

        Server server = new Server(serverSettings);
        Runtime.getRuntime().addShutdownHook(
                new Thread(){
                    @Override
                    public void run() {
                        server.shutdown();
                    }
                }
        );
        server.start();
        Thread t = new Thread(()->{
            try {
                Thread.sleep(1000);
            }catch (Throwable e){

            }
           server.shutdown();
        });
//        t.start();
        server.terminationFuture().await();
        System.out.println("server over");

    }
}
