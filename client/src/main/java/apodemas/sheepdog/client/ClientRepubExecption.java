package apodemas.sheepdog.client;

import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.internal.ThrowableUtil;

import java.util.concurrent.CancellationException;

/**
 * @author caozheng
 * @time 2018-12-06 20:34
 **/
public class ClientRepubExecption extends ClientException {
    public static ClientRepubExecption RETRY_EXCEED_LIMIT =
            ThrowableUtil.unknownStackTrace(new ClientRepubExecption("times of retrying exceed the limit"), ClientRepubExecption.class, "retry(...)");
    public static ClientRepubExecption BUFFERED_EXCEED_LIMIT =
            ThrowableUtil.unknownStackTrace(new ClientRepubExecption("number of message buffered exceed limit"), ClientRepubExecption.class, "enqueue(...)");
    public static CancellationException CANCEL =
            ThrowableUtil.unknownStackTrace(
                    new CancellationException(), ClientRepubExecption.class, "cancel(...)");

    private ClientRepubExecption(String message) {
        super(message);
    }


}
