package apodemas.sheepdog.http.server.requst;

/**
 * @author caozheng
 * @time 2019-01-20 17:26
 **/
public abstract class JSONGetRequestHandler extends GetRequestHandler {
    @Override
    public String[] supportContentTypes() {
        return new String[]{ "application/json" };
    }
}
