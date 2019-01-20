package apodemas.sheepdog.server.http;

import apodemas.sheepdog.http.server.GetRequestHandler;
import apodemas.sheepdog.http.server.HttpContext;
import apodemas.sheepdog.http.server.JsonErrorMessage;
import apodemas.sheepdog.server.ClientSessionInfo;
import apodemas.sheepdog.server.SessionManager;
import io.netty.util.concurrent.Future;

import java.util.List;

/**
 * @author caozheng
 * @time 2019-01-19 11:26
 **/
public class ClientsHandler extends GetRequestHandler {
    private final SessionManager manager;

    public ClientsHandler(SessionManager manager) {
        this.manager = manager;
    }

    @Override
    public String[] supportContentTypes() {
        return new String[]{"application/json"};
    }

    @Override
    public void handle(HttpContext context) {
        manager.sessions(context.<List<ClientSessionInfo>>newPromise())
                .addListener((Future<List<ClientSessionInfo>> fut)->{
                    if(fut.isSuccess()){
                        context.json(fut.get());
                    }else{
                        context.json(new JsonErrorMessage(500, fut.cause().getMessage()));
                    }
                });
    }
}
