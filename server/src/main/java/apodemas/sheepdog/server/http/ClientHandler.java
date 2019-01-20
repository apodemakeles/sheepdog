package apodemas.sheepdog.server.http;

import apodemas.sheepdog.common.StringUtils;
import apodemas.sheepdog.http.server.HttpContext;
import apodemas.sheepdog.http.server.JsonErrorMessage;
import apodemas.sheepdog.http.server.requst.JSONGetRequestHandler;
import apodemas.sheepdog.server.ClientSessionInfo;
import apodemas.sheepdog.server.SessionManager;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.concurrent.Future;

/**
 * @author caozheng
 * @time 2019-01-20 17:24
 **/
public class ClientHandler extends JSONGetRequestHandler {
    private final SessionManager manager;

    public ClientHandler(SessionManager manager) {
        this.manager = manager;
    }

    @Override
    public void handle(HttpContext context) {
        String clientId = context.pathParams().get("id");
        if(StringUtils.empty(clientId)){
            BAD_REQUEST(context, "id is required");
        }else{
            manager.findSession(clientId, context.newPromise())
                    .addListener((Future<ClientSessionInfo> fut)->{
                       if(fut.isSuccess()){
                           ClientSessionInfo info = fut.get();
                           if(info == null){
                               NOT_FOUND(context, "client");
                           }else {
                               context.json(info);
                           }
                       }else{
                           INTERNAL_SERVER_ERROR(context, fut.cause());
                       }
                    });
        }
    }
}
