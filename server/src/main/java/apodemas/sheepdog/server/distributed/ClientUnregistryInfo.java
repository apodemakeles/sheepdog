package apodemas.sheepdog.server.distributed;

/**
 * @author caozheng
 * @time 2019-03-08 16:06
 **/
public class ClientUnregistryInfo {
    private String serverId;
    private String clientId;


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
}
