package apodemas.sheepdog.server.api;

import apodemas.sheepdog.http.server.HttpContext;
import apodemas.sheepdog.http.server.requst.JSONGetRequestHandler;
import apodemas.sheepdog.server.ClientSessionInfo;
import apodemas.sheepdog.server.SessionManager;
import io.netty.util.concurrent.Future;

import java.util.List;

/**
 * @author caozheng
 * @time 2019-01-19 11:26
 **/
public class ClientsHandler extends JSONGetRequestHandler {
    private final SessionManager manager;

    public ClientsHandler(SessionManager manager) {
        this.manager = manager;
    }

    @Override
    public void handle(HttpContext context) {
        manager.sessions(context.newPromise())
                .addListener((Future<List<ClientSessionInfo>> fut)->{
                    if(fut.isSuccess()){
                        context.json(fut.get());
                    }else{
                        INTERNAL_SERVER_ERROR(context, fut.cause());
                    }
                });
    }
}
