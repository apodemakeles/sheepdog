package apodemas.sheepdog.server;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

/**
 * @author caozheng
 * @time 2019-01-02 15:02
 **/
public interface MessageProcessor {
    Future<Boolean> authenticate(ConnectInfo connectInfo, Promise<Boolean> promise);
}
