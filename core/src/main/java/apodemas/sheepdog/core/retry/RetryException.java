package apodemas.sheepdog.core.retry;

/**
 * @author caozheng
 * @time 2018-12-10 17:09
 **/
public class RetryException extends RuntimeException{
    private final Object value;
    private final RetryFailReason reason;

    public RetryException(Object value, RetryFailReason reason){
        this.value = value;
        this.reason = reason;
    }

    public Object value(){
        return value;
    }

    public RetryFailReason reason(){
        return reason;
    }
}
