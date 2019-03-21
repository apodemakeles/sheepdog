package apodemas.sheepdog.server.api;

import apodemas.sheepdog.http.server.HttpContext;
import apodemas.sheepdog.http.server.requst.JSONGetRequestHandler;
import apodemas.sheepdog.server.ClientSessionInfo;
import apodemas.sheepdog.server.Session;
import apodemas.sheepdog.server.SessionManager;
import apodemas.sheepdog.server.sub.SubscriptionManager;

import java.util.ArrayList;
import java.util.List;

/**
 * @author caozheng
 * @time 2019-01-19 11:26
 **/
public class ClientsHandler extends JSONGetRequestHandler {
    private final SessionManager sessionManager;
    private final SubscriptionManager subscriptionManager;

    public ClientsHandler(SessionManager sessionManager,
                          SubscriptionManager subscriptionManager) {
        this.sessionManager = sessionManager;
        this.subscriptionManager = subscriptionManager;
    }

    @Override
    public void handle(HttpContext context) {
        List<ClientSessionInfo> infos = new ArrayList<>();
        for(Session session : sessionManager.sessions()){
            ClientSessionInfo info = new ClientSessionInfo(session.clientId(), subscriptionManager.getSessionSubscriptions(session));
            infos.add(info);
        }

        context.json(infos);
    }
}
