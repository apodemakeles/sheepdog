package apodemas.sheepdog.server.api;

import apodemas.sheepdog.common.StringUtils;
import apodemas.sheepdog.http.server.HttpContext;
import apodemas.sheepdog.http.server.requst.JSONGetRequestHandler;
import apodemas.sheepdog.server.ClientSessionInfo;
import apodemas.sheepdog.server.Session;
import apodemas.sheepdog.server.SessionManager;
import apodemas.sheepdog.server.sub.SubscriptionController;
import io.netty.util.concurrent.Future;

import java.util.ArrayList;
import java.util.List;

/**
 * @author caozheng
 * @time 2019-01-20 20:26
 **/
public class SubscriptionsHandler extends JSONGetRequestHandler {
    private final SubscriptionController subscriptionController;

    public SubscriptionsHandler(SubscriptionController subscriptionController) {
        this.subscriptionController = subscriptionController;
    }

    @Override
    public void handle(HttpContext context) {
        String topic = context.pathParams().get("topic");
        if(StringUtils.empty(topic)){
            BAD_REQUEST(context, "topic is required");
            return;
        }

        List<ClientSessionInfo> infos = new ArrayList<>();
        List<Session> sessions = subscriptionController.getTopicSubSessions(topic);
        for (Session session : sessions) {
            ClientSessionInfo info = new ClientSessionInfo(session.clientId(), null);
            infos.add(info);
        }

        context.json(infos);
    }

}
