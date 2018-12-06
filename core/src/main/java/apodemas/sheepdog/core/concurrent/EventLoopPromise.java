package apodemas.sheepdog.core.concurrent;

import io.netty.channel.EventLoop;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * @author caozheng
 * @time 2018-12-05 14:47
 **/
public class EventLoopPromise {
    private final EventLoop eventLoop;
    private final Promise<?> terminationFuture = new DefaultPromise(GlobalEventExecutor.INSTANCE);
//    private static final AtomicReferenceFieldUpdater<EventLoopPromise, Boolean> STATE_UPDATER =
//            AtomicReferenceFieldUpdater.newUpdater(EventLoopPromise.class, Boolean.class, "closed");
//
//    private volatile Boolean closed = false;

    public EventLoopPromise(EventLoop eventLoop) {
        this.eventLoop = eventLoop;
    }

    public boolean shutdownGracefully(){
//        if (!STATE_UPDATER.compareAndSet(this, false, true)){
//            return false;
//        }

        this.eventLoop.terminationFuture().addListener(fut->{
            terminationFuture.trySuccess(null);
        });
        this.eventLoop.shutdownGracefully();

        return true;
    }

    public boolean setFailure(Throwable cause){
//        if (!STATE_UPDATER.compareAndSet(this, false, true)){
//            return false;
//        }

        this.eventLoop.terminationFuture().addListener(fut->{
            terminationFuture.tryFailure(cause);
        });
        this.eventLoop.shutdownGracefully();

        return true;
    }

    public Promise<?> terminationFuture(){
        return terminationFuture;
    }
}
