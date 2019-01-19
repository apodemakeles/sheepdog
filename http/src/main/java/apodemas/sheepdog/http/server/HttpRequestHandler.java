package apodemas.sheepdog.http.server;

import io.netty.handler.codec.http.HttpMethod;

/**
 * @author caozheng
 * @time 2019-01-18 10:21
 **/
public interface HttpRequestHandler {
    HttpMethod[] supportMethods();
    String[] supportContentTypes();
    Class<?> valueClazz();
    void handle(HttpContext context);
}
