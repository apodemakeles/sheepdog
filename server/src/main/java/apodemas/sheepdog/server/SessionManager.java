package apodemas.sheepdog.server;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

/**
 * @author caozheng
 * @time 2019-01-02 16:41
 **/
public interface SessionManager {
    Future<Session> createSession(ConnectInfo connectInfo, Promise<Session> promise);
}
