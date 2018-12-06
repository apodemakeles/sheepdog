package apodemas.sheepdog.client;

/**
 * @author caozheng
 * @time 2018-12-05 15:57
 **/
public class ClientException extends RuntimeException{
    public ClientException(String message){
        super(message);
    }

    public ClientException(String message, Throwable cause){
        super(message, cause);
    }
}
