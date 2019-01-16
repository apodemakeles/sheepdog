package apodemas.sheepdog.server;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

/**
 * @author caozheng
 * @time 2019-01-16 14:06
 **/
public class DefaultMessageProcessor implements MessageProcessor {
    public Future<Boolean> authenticate(ConnectInfo connectInfo, Promise<Boolean> promise){
        promise.trySuccess(true);

        return promise;
    }
}
