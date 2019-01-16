package apodemas.sheepdog.server;

/**
 * @author caozheng
 * @time 2019-01-15 10:04
 **/
public class SessionNotFoundExecption extends SessionExecption {
    public SessionNotFoundExecption(String cliendId){
        super(String.format("session (%s) is not found in session manager", cliendId));
    }
}
