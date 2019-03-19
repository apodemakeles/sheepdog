package apodemas.sheepdog.server.api;

import apodemas.sheepdog.http.server.HttpContext;
import apodemas.sheepdog.http.server.requst.JSONGetRequestHandler;
import apodemas.sheepdog.server.ClientSessionInfo;
import apodemas.sheepdog.server.Session;
import apodemas.sheepdog.server.SessionController;
import apodemas.sheepdog.server.SessionManager;
import apodemas.sheepdog.server.sub.SubscriptionController;
import io.netty.util.concurrent.Future;

import java.util.ArrayList;
import java.util.List;

/**
 * @author caozheng
 * @time 2019-01-19 11:26
 **/
public class ClientsHandler extends JSONGetRequestHandler {
    private final SessionController sessionController;
    private final SubscriptionController subscriptionController;

    public ClientsHandler(SessionController sessionController,
                          SubscriptionController subscriptionController) {
        this.sessionController = sessionController;
        this.subscriptionController = subscriptionController;
    }

    @Override
    public void handle(HttpContext context) {
        List<ClientSessionInfo> infos = new ArrayList<>();
        for(Session session : sessionController.sessions()){
            ClientSessionInfo info = new ClientSessionInfo(session.clientId(), subscriptionController.getSessionSubscriptions(session));
            infos.add(info);
        }

        context.json(infos);
    }
}
