package apodemas.sheepdog.http.server.requst;

import io.netty.handler.codec.http.HttpMethod;

/**
 * @author caozheng
 * @time 2019-01-20 17:26
 **/
public abstract class JSONGetRequestHandler extends JSONBaseRequestHandler {
    @Override
    public HttpMethod[] supportMethods() {
        return new HttpMethod[]{ HttpMethod.GET };
    }

    @Override
    public Class<?> valueClazz() {
        return null;
    }
}
