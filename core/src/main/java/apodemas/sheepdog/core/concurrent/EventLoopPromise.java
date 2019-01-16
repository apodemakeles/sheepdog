package apodemas.sheepdog.core.concurrent;

import io.netty.channel.EventLoop;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;


/**
 * @author caozheng
 * @time 2018-12-05 14:47
 **/
public class EventLoopPromise {
    private final EventExecutor eventExecutor;
    private final Promise<?> terminationFuture = new DefaultPromise(GlobalEventExecutor.INSTANCE);

    public EventLoopPromise(EventExecutor eventExecutor) {
        this.eventExecutor = eventExecutor;
    }

    public boolean shutdownGracefully(){
        this.eventExecutor.terminationFuture().addListener(fut->{
            terminationFuture.trySuccess(null);
        });
        this.eventExecutor.shutdownGracefully();

        return true;
    }

    public boolean setFailure(Throwable cause){
        this.eventExecutor.terminationFuture().addListener(fut->{
            terminationFuture.tryFailure(cause);
        });
        this.eventExecutor.shutdownGracefully();

        return true;
    }

    public Promise<?> terminationFuture(){
        return terminationFuture;
    }
}
