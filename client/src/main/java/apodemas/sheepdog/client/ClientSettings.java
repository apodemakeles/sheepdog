package apodemas.sheepdog.client;

import java.util.Optional;

public class ClientSettings {
    private Optional<Integer> connectTimeoutSec = Optional.empty();
    private Optional<Integer> publishAckTimeoutSec = Optional.empty();
    private Optional<Integer> pingResponseTimeoutSec = Optional.empty();
    private Optional<Integer> maxRepubTimes = Optional.empty();
    private Optional<Integer> maxRepubQueueSize = Optional.empty();

    public static int DEFAULT_CONNECT_TIMEOUT_SEC = 15;
    public static int DEFAULT_PUBLISH_ACK_TIMEOUT_SEC = 15;
    public static int DEFAULT_PING_RESPONSE_TIMEOUT_SEC = 90;
    public static int DEFAULT_MAX_REPUB_TIMES = 3;
    public static int DEFAULT_MAX_REPUB_QUEUE_SIZE = 20;

    static ClientSettings DEFAULT;

    static{
        ClientSettings settings = new ClientSettings();
        settings.setConnectTimeoutSec(DEFAULT_CONNECT_TIMEOUT_SEC);
        settings.setPublishAckTimeoutSec(DEFAULT_PUBLISH_ACK_TIMEOUT_SEC);
        settings.setPingResponseTimeoutSec(DEFAULT_PING_RESPONSE_TIMEOUT_SEC);
        settings.setMaxRepubTimes(DEFAULT_MAX_REPUB_TIMES);
        settings.setMaxRepubQueueSize(DEFAULT_MAX_REPUB_QUEUE_SIZE);

        DEFAULT = settings;
    }

    public void setConnectTimeoutSec(int connectTimeoutSec) {
        if (connectTimeoutSec <= 0){
            throw new IllegalArgumentException("connect timeout must greater than 0");
        }
        this.connectTimeoutSec = Optional.of(connectTimeoutSec);
    }

    public void setPublishAckTimeoutSec(int publishAckTimeoutSec) {
        if(publishAckTimeoutSec <= 0){
            throw new IllegalArgumentException("publish ack timeout must greater than 0");
        }
        this.publishAckTimeoutSec = Optional.of(publishAckTimeoutSec);
    }

    public void setPingResponseTimeoutSec(int pingResponseTimeoutSec) {
        if(pingResponseTimeoutSec <= 0){
            throw new IllegalArgumentException("ping response timeout must greater than 0");
        }
        this.pingResponseTimeoutSec = Optional.of(pingResponseTimeoutSec);
    }

    public void setMaxRepubTimes(int maxRepubTimes) {
        if(maxRepubTimes < 1){
            throw new IllegalArgumentException("max republish times must greater than 1");
        }
        this.maxRepubTimes = Optional.of(maxRepubTimes);
    }

    public void setMaxRepubQueueSize(int maxRepubQueueSize) {
        if(maxRepubQueueSize < 1){
            throw new IllegalArgumentException("max republish times queue size must greater than 1");
        }
        this.maxRepubQueueSize = Optional.of(maxRepubQueueSize);
    }

    public int connectTimeoutSec() {
        return connectTimeoutSec.orElse(DEFAULT_CONNECT_TIMEOUT_SEC);
    }

    public int publishAckTimeoutSec() {
        return publishAckTimeoutSec.orElse(DEFAULT_PUBLISH_ACK_TIMEOUT_SEC);
    }

    public int pingResponseTimeoutSec() {
        return pingResponseTimeoutSec.orElse(DEFAULT_PING_RESPONSE_TIMEOUT_SEC);
    }

    public int maxRepubTimes() {
        return maxRepubTimes.orElse(DEFAULT_MAX_REPUB_TIMES);
    }

    public int maxRepubQueueSize() {
        return maxRepubQueueSize.orElse(DEFAULT_MAX_REPUB_QUEUE_SIZE);
    }



}
