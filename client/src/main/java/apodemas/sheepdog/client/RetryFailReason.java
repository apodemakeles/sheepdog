package apodemas.sheepdog.client;

/**
 * @author caozheng
 * @time 2018-12-10 17:09
 **/
public enum RetryFailReason{
    DUPLICATE_ID,
    QUEUE_EXCEED_LIMIT,
    RETRY_EXCEED_LIMIT,
    DISPOSE
}
