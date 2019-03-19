package apodemas.sheepdog.server.pub;

/**
 * @author caozheng
 * @time 2019-03-18 20:52
 **/
public class PubSettings {
    private int publishAckTimeoutSec;
    private int maxRepubTimes;
    private int maxRepubQueueSize;

    public int publishAckTimeoutSec() {
        return publishAckTimeoutSec;
    }

    public void setPublishAckTimeoutSec(int publishAckTimeoutSec) {
        this.publishAckTimeoutSec = publishAckTimeoutSec;
    }

    public int maxRepubTimes() {
        return maxRepubTimes;
    }

    public void setMaxRepubTimes(int maxRepubTimes) {
        this.maxRepubTimes = maxRepubTimes;
    }

    public int maxRepubQueueSize() {
        return maxRepubQueueSize;
    }

    public void setMaxRepubQueueSize(int maxRepubQueueSize) {
        this.maxRepubQueueSize = maxRepubQueueSize;
    }
}
