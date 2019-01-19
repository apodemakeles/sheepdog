package apodemas.sheepdog.http.server;

/**
 * @author caozheng
 * @time 2019-01-18 10:14
 **/
public class HttpServerSetting {
    private int readTimeoutSeconds = DEFAULT_READ_TIMEOUT_SECONDS;

    public static int DEFAULT_READ_TIMEOUT_SECONDS = 10;

    public void setReadTimeoutSeconds(int readTimeoutSeconds) {
        if(readTimeoutSeconds <=0 ){
            throw new IllegalArgumentException("read timeout must be greater than 0");
        }
        this.readTimeoutSeconds = readTimeoutSeconds;
    }

    public int readTimeoutSeconds() {
        return readTimeoutSeconds;
    }
}
