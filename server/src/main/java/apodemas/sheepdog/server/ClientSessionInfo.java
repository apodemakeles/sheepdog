package apodemas.sheepdog.server;

import java.util.List;

/**
 * @author caozheng
 * @time 2019-01-19 11:43
 **/
public class ClientSessionInfo {
    private final String clientId;
    private final List<Subscription> subscriptions;

    public ClientSessionInfo(String clientId, List<Subscription> subscriptions) {
        this.clientId = clientId;
        this.subscriptions = subscriptions;
    }

    public String getClientId() {
        return clientId;
    }

    public List<Subscription> getSubscriptions() {
        return subscriptions;
    }
}
