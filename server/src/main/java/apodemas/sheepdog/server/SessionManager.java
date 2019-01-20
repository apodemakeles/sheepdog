package apodemas.sheepdog.server;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

import java.util.List;


/**
 * @author caozheng
 * @time 2019-01-02 16:41
 **/
public interface SessionManager {
    Future<Session> createSession(ConnectInfo connectInfo, Promise<Session> promise);
    Future<List<ClientSessionInfo>> sessions(Promise<List<ClientSessionInfo>> promise);
    Future<ClientSessionInfo> findSession(String clientId, Promise<ClientSessionInfo> promise);
}
