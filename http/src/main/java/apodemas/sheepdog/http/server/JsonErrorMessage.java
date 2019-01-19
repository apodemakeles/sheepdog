package apodemas.sheepdog.http.server;

/**
 * @author caozheng
 * @time 2019-01-19 09:36
 **/
public class JsonErrorMessage {
    private int code;
    private String message;

    public JsonErrorMessage(){

    }

    public JsonErrorMessage(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
