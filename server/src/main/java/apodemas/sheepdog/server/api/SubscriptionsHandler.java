package apodemas.sheepdog.server.api;

import apodemas.sheepdog.common.StringUtils;
import apodemas.sheepdog.http.server.HttpContext;
import apodemas.sheepdog.http.server.requst.JSONGetRequestHandler;
import apodemas.sheepdog.server.ClientSessionInfo;
import apodemas.sheepdog.server.Session;
import apodemas.sheepdog.server.sub.SubscriptionManager;

import java.util.ArrayList;
import java.util.List;

/**
 * @author caozheng
 * @time 2019-01-20 20:26
 **/
public class SubscriptionsHandler extends JSONGetRequestHandler {
    private final SubscriptionManager subscriptionManager;

    public SubscriptionsHandler(SubscriptionManager subscriptionManager) {
        this.subscriptionManager = subscriptionManager;
    }

    @Override
    public void handle(HttpContext context) {
        String topic = context.pathParams().get("topic");
        if(StringUtils.empty(topic)){
            BAD_REQUEST(context, "topic is required");
            return;
        }

        List<ClientSessionInfo> infos = new ArrayList<>();
        List<Session> sessions = subscriptionManager.getTopicSubSessions(topic);
        for (Session session : sessions) {
            ClientSessionInfo info = new ClientSessionInfo(session.clientId(), null);
            infos.add(info);
        }

        context.json(infos);
    }

}
