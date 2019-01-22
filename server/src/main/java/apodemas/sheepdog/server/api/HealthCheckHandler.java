package apodemas.sheepdog.server.api;

import apodemas.sheepdog.http.server.HttpContext;
import apodemas.sheepdog.http.server.requst.JSONGetRequestHandler;

/**
 * @author caozheng
 * @time 2019-01-19 11:04
 **/
public class HealthCheckHandler extends JSONGetRequestHandler {

    @Override
    public void handle(HttpContext context) {
        context.json(null);
    }
}
