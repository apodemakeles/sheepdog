package apodemas.sheepdog.server;

/**
 * @author caozheng
 * @time 2019-01-02 11:22
 **/
public class ServerSettings {
    private String idPrefix;

    static ServerSettings DEFAULT;

    static{
        ServerSettings serverSettings = new ServerSettings();

        DEFAULT = serverSettings;
    }

    public void setIdPrefix(String idPrefix) {
        this.idPrefix = idPrefix;
    }

    public String idPrefix() {
        return idPrefix;
    }
}