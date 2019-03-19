package apodemas.sheepdog.server.auth;

import apodemas.sheepdog.server.ConnectInfo;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

/**
 * @author caozheng
 * @time 2019-03-18 20:21
 **/
public class SuccessAuthenticator implements Authenticator {
    public Future<Boolean> authenticate(ConnectInfo connectInfo, Promise<Boolean> promise){
        promise.trySuccess(true);

        return promise;
    }
}
