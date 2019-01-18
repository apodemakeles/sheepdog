package apodemas.sheepdog.server;

/**
 * @author caozheng
 * @time 2019-01-02 11:22
 **/
public class ServerSettings {
    private String idPrefix;
    private double timeoutFactor = DEFAULT_TIMEOUT_FACTOR;

    public static double DEFAULT_TIMEOUT_FACTOR = 1.5;

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

    public String idPrefix() {
        return idPrefix;
    }

    public double timeoutFactor() {
        return timeoutFactor;
    }

}