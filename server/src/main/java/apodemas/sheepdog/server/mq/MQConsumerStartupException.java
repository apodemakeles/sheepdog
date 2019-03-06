package apodemas.sheepdog.server.mq;

/**
 * @author caozheng
 * @time 2019-02-25 17:13
 **/
public class MQConsumerStartupException extends Exception{
    public MQConsumerStartupException(String message) {
        super(message);
    }

    public MQConsumerStartupException(String message, Throwable cause) {
        super(message, cause);
    }
}
