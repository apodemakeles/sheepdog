package apodemas.sheepdog.http.server;

import apodemas.sheepdog.common.url.URLParameters;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * @author caozheng
 * @time 2019-01-19 09:22
 **/
public class DefaultHttpContext implements HttpContext {
    private final String path;
    private final HttpMethod method;
    private final HttpHeaders headers;
    private final Object msg;
    private final PathParams params;
    private final URLParameters queryParams;
    private final ChannelHandlerContext ctx;
    private final HttpResponseWriter writer;

    public DefaultHttpContext(String path, HttpMethod method, HttpHeaders headers,
                              Object msg, PathParams params, URLParameters queryParams,
                              ChannelHandlerContext ctx, HttpResponseWriter writer) {
        this.path = path;
        this.method = method;
        this.headers = headers;
        this.msg = msg;
        this.params = params;
        this.queryParams = queryParams;
        this.ctx = ctx;
        this.writer = writer;
    }

    public String path() {
        return this.path;
    }

    public HttpMethod method() {
        return this.method;
    }

    public HttpHeaders headers() {
        return this.headers;
    }

    public Object msg() {
        return this.msg;
    }

    public PathParams pathParams() {
        return this.params;
    }

    public URLParameters queryParams(){
        return this.queryParams;
    }

    public void writeResponse(HttpResponseStatus status, HttpHeaders headers, ByteBuf content) {
        writer.writeResponse(status, headers, content);
    }

    public void string(HttpResponseStatus status, HttpHeaders headers, Object msg) {
        writer.string(status, headers, msg);
    }

    public void json(HttpResponseStatus status, HttpHeaders headers, Object msg) {
        writer.json(status, headers, msg);
    }
}
