package apodemas.sheepdog.http.server.requst;

import apodemas.sheepdog.http.server.HttpContext;
import apodemas.sheepdog.http.server.JsonErrorMessage;
import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * @author caozheng
 * @time 2019-01-20 18:01
 **/
public abstract class JSONBaseRequestHandler extends BaseRequestHandler{
    @Override
    public String[] supportContentTypes() {
        return new String[]{ "application/json" };
    }

    protected void NOT_FOUND(HttpContext context, String message){
        context.json(HttpResponseStatus.NOT_FOUND, new JsonErrorMessage(404, message + " is not found"));
    }

    protected void INTERNAL_SERVER_ERROR(HttpContext context, Throwable e){
        context.json(HttpResponseStatus.INTERNAL_SERVER_ERROR, new JsonErrorMessage(500, e.getMessage()));
    }

    protected void BAD_REQUEST(HttpContext context, String message){
        context.json(HttpResponseStatus.BAD_REQUEST, new JsonErrorMessage(400, message));
    }
}
