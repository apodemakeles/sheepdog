package apodemas.sheepdog.common.url;

/**
 * @author caozheng
 * @time 2019-01-19 09:11
 **/
public class URLParseException extends RuntimeException {
    private int position = -1;

    public URLParseException(String message, int position){
        super(message);
        this.position = position;
    }

    public URLParseException(String message){
        super(message);
    }

    public int getPosition() {
        return position;
    }
}
