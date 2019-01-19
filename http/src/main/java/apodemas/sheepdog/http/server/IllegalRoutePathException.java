package apodemas.sheepdog.http.server;

/**
 * @author caozheng
 * @time 2019-01-19 09:07
 **/
public class IllegalRoutePathException extends HttpServerExecption {
    public IllegalRoutePathException(String message, String path) {
        super(message + " route path : " + path);
    }
}
