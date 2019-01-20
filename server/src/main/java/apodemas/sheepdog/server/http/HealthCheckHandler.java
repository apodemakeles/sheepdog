package apodemas.sheepdog.server.http;

import apodemas.sheepdog.http.server.HttpContext;
import apodemas.sheepdog.http.server.HttpRequestHandler;
import io.netty.handler.codec.http.HttpMethod;

/**
 * @author caozheng
 * @time 2019-01-19 11:04
 **/
public class HealthCheckHandler implements HttpRequestHandler {
    @Override
    public HttpMethod[] supportMethods() {
        return new HttpMethod[]{ HttpMethod.GET };
    }

    @Override
    public String[] supportContentTypes() {
        return new String[]{"application/json"};
    }

    @Override
    public Class<?> valueClazz() {
        return null;
    }

    @Override
    public void handle(HttpContext context) {
        context.json(null);
    }
}
