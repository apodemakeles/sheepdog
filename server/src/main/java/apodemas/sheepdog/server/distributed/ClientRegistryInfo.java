package apodemas.sheepdog.server.distributed;

/**
 * @author caozheng
 * @time 2019-03-08 14:26
 **/
public class ClientRegistryInfo {
    private String serverId;
    private String clientId;
    private String username;
    private long upTimestamp;

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public long getUpTimestamp() {
        return upTimestamp;
    }

    public void setUpTimestamp(long upTimestamp) {
        this.upTimestamp = upTimestamp;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
