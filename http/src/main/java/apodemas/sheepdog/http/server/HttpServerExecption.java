package apodemas.sheepdog.http.server;

/**
 * @author caozheng
 * @time 2019-01-18 10:24
 **/
public class HttpServerExecption extends RuntimeException{
    public HttpServerExecption(String message){
        super(message);
    }
    public HttpServerExecption(String message, Throwable cause){
        super(message, cause);
    }
}
