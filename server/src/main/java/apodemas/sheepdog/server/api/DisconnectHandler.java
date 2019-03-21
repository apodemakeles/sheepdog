package apodemas.sheepdog.server.api;

import apodemas.sheepdog.common.StringUtils;
import apodemas.sheepdog.http.server.HttpContext;
import apodemas.sheepdog.http.server.requst.JSONGetRequestHandler;
import apodemas.sheepdog.server.ClientSessionInfo;
import apodemas.sheepdog.server.Session;
import apodemas.sheepdog.server.SessionManager;

/**
 * @author caozheng
 * @time 2019-01-20 20:44
 **/
public class DisconnectHandler extends JSONGetRequestHandler {
    private final SessionManager sessionManager;

    public DisconnectHandler(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public void handle(HttpContext context) {
        String clientId = context.pathParams().get("id");
        if(StringUtils.empty(clientId)) {
            BAD_REQUEST(context, "id is required");
        }else {
            Session session = sessionManager.findSession(clientId);
            if(session == null){
                NOT_FOUND(context, clientId);
            }else {
                session.disconnect();
                context.json(new ClientSessionInfo(clientId, null));
            }
        }
    }
}
