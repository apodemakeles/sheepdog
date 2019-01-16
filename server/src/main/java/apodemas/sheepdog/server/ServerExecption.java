package apodemas.sheepdog.server;

/**
 * @author caozheng
 * @time 2019-01-15 10:01
 **/
public class ServerExecption extends RuntimeException {
    public ServerExecption(String message){
        super(message);
    }
    public ServerExecption(String message, Throwable cause){
        super(message, cause);
    }
}
