package apodemas.sheepdog.client;

public class ClientSettings {
    private int connectTimeoutSec;
    private int publishAckTimeoutSec;
    private int pingResponseTimeoutSec;
    private int maxRepubTimes;
    private int maxRepubQueueSize;

    public void setConnectTimeoutSec(int connectTimeoutSec) {
        this.connectTimeoutSec = connectTimeoutSec;
    }

    public void setPublishAckTimeoutSec(int publishAckTimeoutSec) {
        this.publishAckTimeoutSec = publishAckTimeoutSec;
    }

    public void setPingResponseTimeoutSec(int pingResponseTimeoutSec) {
        this.pingResponseTimeoutSec = pingResponseTimeoutSec;
    }

    public void setMaxRepubTimes(int maxRepubTimes) {
        this.maxRepubTimes = maxRepubTimes;
    }

    public void setMaxRepubQueueSize(int maxRepubQueueSize) {
        this.maxRepubQueueSize = maxRepubQueueSize;
    }

    public int getConnectTimeoutSec() {
        return connectTimeoutSec;
    }

    public int getPublishAckTimeoutSec() {
        return publishAckTimeoutSec;
    }

    public int getPingResponseTimeoutSec() {
        return pingResponseTimeoutSec;
    }

    public int getMaxRepubTimes() {
        return maxRepubTimes;
    }

    public int getMaxRepubQueueSize() {
        return maxRepubQueueSize;
    }
}
