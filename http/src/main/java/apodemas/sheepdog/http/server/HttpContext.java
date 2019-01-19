package apodemas.sheepdog.http.server;

import apodemas.sheepdog.common.url.URLParameters;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;

/**
 * @author caozheng
 * @time 2019-01-18 10:23
 **/
public interface HttpContext extends HttpResponseWriter {
    String path();
    HttpMethod method();
    HttpHeaders headers();
    Object msg();
    PathParams pathParams();
    URLParameters queryParams();
}
