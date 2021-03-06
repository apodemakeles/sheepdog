package apodemas.sheepdog.http.server;

import apodemas.sheepdog.http.server.requst.HttpRequestHandler;

/**
 * @author caozheng
 * @time 2019-01-18 10:21
 **/
public interface Router {
    void add(String path, HttpRequestHandler handler);
    RouteMatchResult match(String path);
}
