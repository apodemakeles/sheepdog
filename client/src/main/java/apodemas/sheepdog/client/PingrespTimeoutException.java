package apodemas.sheepdog.client;

/**
 * @author caozheng
 * @time 2018-12-05 16:05
 **/
public class PingrespTimeoutException extends ClientException {
    public PingrespTimeoutException(){
        super("waiting for ping response is timeout");
    }
}
