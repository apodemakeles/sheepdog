package apodemas.sheepdog.server.api;

import apodemas.sheepdog.common.StringUtils;
import apodemas.sheepdog.http.server.HttpContext;
import apodemas.sheepdog.http.server.requst.JSONGetRequestHandler;
import apodemas.sheepdog.server.ClientSessionInfo;
import apodemas.sheepdog.server.SessionManager;
import io.netty.util.concurrent.Future;

import java.util.List;

/**
 * @author caozheng
 * @time 2019-01-20 20:26
 **/
public class SubscriptionsHandler extends JSONGetRequestHandler {
    private final SessionManager manager;

    public SubscriptionsHandler(SessionManager manager) {
        this.manager = manager;
    }

    @Override
    public void handle(HttpContext context) {
        String topic = context.pathParams().get("topic");
        if(StringUtils.empty(topic)){
            BAD_REQUEST(context, "topic is required");
            return;
        }

        manager.findSubscription(topic, context.newPromise())
                .addListener((Future<List<ClientSessionInfo>> fut)->{
                    if(fut.isSuccess()){
                        context.json(fut.get());
                    }else{
                        INTERNAL_SERVER_ERROR(context, fut.cause());
                    }
                });
    }

}
