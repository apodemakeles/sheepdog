package apodemas.sheepdog.server;

import io.netty.channel.ChannelHandlerContext;

/**
 * @author caozheng
 * @time 2019-01-07 13:52
 **/
public class ConnectInfo {
    private String clientId;
    private String username;
    private byte[] password;
    private int keepAliveTimeSeconds;
    private String prefix;
    private ChannelHandlerContext ctx;


    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(byte[] password) {
        this.password = password;
    }

    public void setKeepAliveTimeSeconds(int keepAliveTimeSeconds) {
        this.keepAliveTimeSeconds = keepAliveTimeSeconds;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void setHandlerContext(ChannelHandlerContext ctx){
        this.ctx = ctx;
    }

    public String clientId() {
        return clientId;
    }

    public String username() {
        return username;
    }

    public byte[] password() {
        return password;
    }

    public int keepAliveTimeSeconds() {
        return keepAliveTimeSeconds;
    }

    public String prefix() {
        return prefix;
    }

    public ChannelHandlerContext handlerContext(){
        return ctx;
    }
}
