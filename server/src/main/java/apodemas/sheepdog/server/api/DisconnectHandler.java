package apodemas.sheepdog.server.api;

import apodemas.sheepdog.common.StringUtils;
import apodemas.sheepdog.http.server.HttpContext;
import apodemas.sheepdog.http.server.requst.JSONGetRequestHandler;
import apodemas.sheepdog.server.ClientSessionInfo;
import apodemas.sheepdog.server.Session;
import apodemas.sheepdog.server.SessionController;
import apodemas.sheepdog.server.SessionManager;
import io.netty.util.concurrent.Future;

/**
 * @author caozheng
 * @time 2019-01-20 20:44
 **/
public class DisconnectHandler extends JSONGetRequestHandler {
    private final SessionController sessionController;

    public DisconnectHandler(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    @Override
    public void handle(HttpContext context) {
        String clientId = context.pathParams().get("id");
        if(StringUtils.empty(clientId)) {
            BAD_REQUEST(context, "id is required");
        }else {
            Session session = sessionController.findSession(clientId);
            if(session == null){
                NOT_FOUND(context, clientId);
            }else {
                session.disconnect();
                context.json(new ClientSessionInfo(clientId, null));
            }
        }
    }
}
