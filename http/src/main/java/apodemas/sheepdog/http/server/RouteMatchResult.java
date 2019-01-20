package apodemas.sheepdog.http.server;

import apodemas.sheepdog.http.server.requst.HttpRequestHandler;

/**
 * @author caozheng
 * @time 2019-01-18 10:21
 **/
public class RouteMatchResult {
    private final boolean match;
    private final HttpRequestHandler handler;
    private final PathParams params;

    public static final RouteMatchResult NOT_MATCH = new RouteMatchResult(false);

    private RouteMatchResult(boolean match) {
        this.match = false;
        this.handler = null;
        this.params = null;
    }

    public RouteMatchResult(HttpRequestHandler handler){
        this(null, new PathParams());
    }


    public RouteMatchResult(HttpRequestHandler handler, PathParams params) {
        this.match = true;
        this.handler = handler;
        this.params = params;
    }

    public boolean isMatche(){
        return match;
    }

    public HttpRequestHandler handler(){
        return handler;
    }

    public PathParams params(){
        return params;
    }
}
