package apodemas.sheepdog.server;


/**
 * @author caozheng
 * @time 2019-01-02 11:22
 **/
public class ServerSettings {
    private String idPrefix;
    private double timeoutFactor = DEFAULT_TIMEOUT_FACTOR;
    private int publishAckTimeoutSec = DEFAULT_PUBLISH_ACK_TIMEOUT_SEC;
    private int maxRepubTimes = DEFAULT_MAX_REPUB_TIMES;
    private int maxRepubQueueSize = DEFAULT_MAX_REPUB_QUEUE_SIZE;

    public static int DEFAULT_PUBLISH_ACK_TIMEOUT_SEC = 15;
    public static double DEFAULT_TIMEOUT_FACTOR = 1.5;
    public static int DEFAULT_MAX_REPUB_QUEUE_SIZE = 1024;
    public static int DEFAULT_MAX_REPUB_TIMES = 3;

    static ServerSettings DEFAULT;

    static{
        ServerSettings serverSettings = new ServerSettings();

        DEFAULT = serverSettings;
    }

    public void setIdPrefix(String idPrefix) {
        this.idPrefix = idPrefix;
    }

    public void setTimeoutFactor(double timeoutFactor) {
        if (timeoutFactor < 1) {
            throw new IllegalArgumentException("timeout factor must equal or greater than 1");
        }

        this.timeoutFactor = timeoutFactor;
    }

    public void setPublishAckTimeoutSec(int publishAckTimeoutSec) {
        if(publishAckTimeoutSec <= 0){
            throw new IllegalArgumentException("publish ack timeout must greater than 0");
        }

        this.publishAckTimeoutSec = publishAckTimeoutSec;
    }

    public void setMaxRepubTimes(int maxRepubTimes) {
        if(maxRepubTimes < 1){
            throw new IllegalArgumentException("max republish times must greater than 1");
        }

        this.maxRepubTimes = maxRepubTimes;
    }

    public void setMaxRepubQueueSize(int maxRepubQueueSize) {
        if(maxRepubQueueSize < 1){
            throw new IllegalArgumentException("max republish times queue size must greater than 1");
        }

        this.maxRepubQueueSize = maxRepubQueueSize;
    }

    public String idPrefix() {
        return idPrefix;
    }

    public double timeoutFactor() {
        return timeoutFactor;
    }

    public int publishAckTimeoutSec() {
        return publishAckTimeoutSec;
    }

    public int maxRepubTimes() {
        return maxRepubTimes;
    }

    public int maxRepubQueueSize() {
        return maxRepubQueueSize;
    }

}