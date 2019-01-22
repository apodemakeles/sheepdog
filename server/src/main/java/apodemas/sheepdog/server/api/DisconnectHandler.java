package apodemas.sheepdog.server.api;

import apodemas.sheepdog.common.StringUtils;
import apodemas.sheepdog.http.server.HttpContext;
import apodemas.sheepdog.http.server.requst.JSONGetRequestHandler;
import apodemas.sheepdog.server.ClientSessionInfo;
import apodemas.sheepdog.server.Session;
import apodemas.sheepdog.server.SessionManager;
import io.netty.util.concurrent.Future;

/**
 * @author caozheng
 * @time 2019-01-20 20:44
 **/
public class DisconnectHandler extends JSONGetRequestHandler {
    private final SessionManager manager;

    public DisconnectHandler(SessionManager manager) {
        this.manager = manager;
    }

    @Override
    public void handle(HttpContext context) {
        String clientId = context.pathParams().get("id");
        if(StringUtils.empty(clientId)) {
            BAD_REQUEST(context, "id is required");
        }else {
            manager.findSession(clientId, context.newPromise())
                    .addListener((Future<Session> fut) -> {
                        if (fut.isSuccess()) {
                            Session session = fut.get();
                            if (session != null) {
                                session.disconnect();
                                context.json(new ClientSessionInfo(clientId, null));
                            }else{
                                NOT_FOUND(context, clientId);
                            }
                        } else {
                            INTERNAL_SERVER_ERROR(context, fut.cause());
                        }
                    });
        }
    }
}
