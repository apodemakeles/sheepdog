package apodemas.sheepdog.server.auth;

import apodemas.sheepdog.server.ConnectInfo;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

/**
 * @author caozheng
 * @time 2019-03-18 20:13
 **/
public interface Authenticator {
    Future<Boolean> authenticate(ConnectInfo connectInfo, Promise<Boolean> promise);
}
