package apodemas.sheepdog.http.server;

import io.netty.handler.codec.http.HttpMethod;

/**
 * @author caozheng
 * @time 2019-01-20 08:38
 **/
public abstract class GetRequestHandler implements HttpRequestHandler{
    @Override
    public HttpMethod[] supportMethods() {
        return new HttpMethod[]{ HttpMethod.GET };
    }

    @Override
    public Class<?> valueClazz() {
        return null;
    }
}
