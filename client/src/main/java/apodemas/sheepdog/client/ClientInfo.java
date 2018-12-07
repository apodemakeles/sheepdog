package apodemas.sheepdog.client;

/**
 * @author caozheng
 * @time 2018-12-07 10:01
 **/
class ClientInfo {
    private int keepAliveSec;
    private String username;
    private byte[] password;
    private String clientId;

    public void setKeepAliveSec(int keepAliveSec) {
        this.keepAliveSec = keepAliveSec;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(byte[] password) {
        this.password = password;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public int keepAliveSec() {
        return keepAliveSec;
    }

    public String username() {
        return username;
    }

    public byte[] password() {
        return password;
    }

    public String clientId() {
        return clientId;
    }
}
