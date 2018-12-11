package apodemas.sheepdog.client;

/**
 * @author caozheng
 * @time 2018-12-10 11:41
 **/
public class ClientClosedException extends ClientException {
    public ClientClosedException(){
        super("client already closed");
    }
}
