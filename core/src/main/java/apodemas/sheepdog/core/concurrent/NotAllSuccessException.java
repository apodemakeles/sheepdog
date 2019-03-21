package apodemas.sheepdog.core.concurrent;

import io.netty.util.concurrent.Future;

/**
 * @author caozheng
 * @time 2019-03-21 10:23
 **/
public class NotAllSuccessException extends RuntimeException {
    private final Future<?>[] futures;

    public NotAllSuccessException(Future<?>[] futures) {
        this.futures = futures;
    }

    public Future<?>[] getFailedFutures() {
        return futures;
    }
}
