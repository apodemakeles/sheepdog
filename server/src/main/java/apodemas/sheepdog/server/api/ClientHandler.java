package apodemas.sheepdog.server.api;

import apodemas.sheepdog.common.StringUtils;
import apodemas.sheepdog.http.server.HttpContext;
import apodemas.sheepdog.http.server.requst.JSONGetRequestHandler;
import apodemas.sheepdog.server.ClientSessionInfo;
import apodemas.sheepdog.server.Session;
import apodemas.sheepdog.server.SessionController;
import apodemas.sheepdog.server.SessionManager;
import apodemas.sheepdog.server.sub.SubscriptionController;
import io.netty.util.concurrent.Future;

/**
 * @author caozheng
 * @time 2019-01-20 17:24
 **/
public class ClientHandler extends JSONGetRequestHandler {
    private final SessionController sessionController;
    private final SubscriptionController subscriptionController;

    public ClientHandler(SessionController sessionController, SubscriptionController subscriptionController) {
        this.sessionController = sessionController;
        this.subscriptionController = subscriptionController;
    }

    @Override
    public void handle(HttpContext context) {
        String clientId = context.pathParams().get("id");
        if(StringUtils.empty(clientId)){
            BAD_REQUEST(context, "id is required");
        }else{
            Session session = sessionController.findSession(clientId);
            if(session == null){
                NOT_FOUND(context, clientId);
            }else{
                ClientSessionInfo info = new ClientSessionInfo(session.clientId(),
                        subscriptionController.getSessionSubscriptions(session));
                context.json(info);
            }
        }
    }
}
