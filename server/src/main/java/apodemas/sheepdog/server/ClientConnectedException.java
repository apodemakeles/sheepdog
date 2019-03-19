package apodemas.sheepdog.server;

/**
 * @author caozheng
 * @time 2019-03-08 17:34
 **/
public class ClientConnectedException extends ServerExecption {
    public ClientConnectedException(String cliendId, String serveerId){
        super(String.format("client [%s] is already connected to server [%s]", cliendId, serveerId));
    }
}
