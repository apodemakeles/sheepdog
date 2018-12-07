package apodemas.sheepdog.client;

/**
 * @author caozheng
 * @time 2018-12-07 14:56
 **/
public class ServerCloseConnectionException extends ClientException {
    public ServerCloseConnectionException(){
        super("server close the connection");
    }
}
