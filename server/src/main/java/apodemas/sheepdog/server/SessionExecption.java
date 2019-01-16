package apodemas.sheepdog.server;

/**
 * @author caozheng
 * @time 2019-01-15 10:04
 **/
public class SessionExecption extends ServerExecption {
    public SessionExecption(String message){
        super(message);
    }
    public SessionExecption(String message, Throwable cause){
        super(message, cause);
    }
}
