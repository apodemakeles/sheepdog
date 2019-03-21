package apodemas.sheepdog.core.concurrent;

import apodemas.sheepdog.common.Checks;
import io.netty.util.concurrent.*;

/**
 * @author caozheng
 * @time 2019-03-21 10:22
 **/
public class AllFuture<T> extends WrappedFuture<Future<T>[]> {
    private final Future<T>[] futures;
    private final EventExecutor executor;
    private volatile int completed = 0;
    private volatile boolean success = true;

    public AllFuture(Future<T>... futures){
        this(GlobalEventExecutor.INSTANCE, futures);
    }

    public AllFuture(EventExecutor executor, Future<T>... futures) {
        super(Checks.notNull(executor, "executor").newPromise());

        if (futures == null || futures.length == 0) {
            throw new IllegalArgumentException("futures");
        }

        this.futures = futures;
        this.executor = executor;

        for(Future<T> f : futures) {
            Future<T> item = f;
            if (item.isDone()) {
                completed++;
                if(!item.isSuccess() && success){
                    success = false;
                }
            }else{
                FutureListener listener = future ->{
                    if(executor.inEventLoop()){
                        completed++;
                        checkCompleted(item);
                    }else{
                        executor.execute(new Runnable() {
                            @Override
                            public void run() {
                                checkCompleted(item);
                            }
                        });
                    }
                };

                try {
                    item.addListener(listener);

                }catch (Throwable e){

                }
            }

            if (completed == futures.length){
                if(success){
                    promise().trySuccess(futures);
                }else{
                    promise().tryFailure(new NotAllSuccessException(futures));
                }
            }

        }
    }

    private Promise<Future<T>[]> promise(){
        return (Promise<Future<T>[]>)future();
    }

    private void checkCompleted(Future<T> future){
        if(!future.isSuccess() && success){
            success = false;
        }

        if (++completed == futures.length){
            if(success){
                promise().trySuccess(futures);
            }else{
                promise().tryFailure(new NotAllSuccessException(futures));
            }
        }

    }

}
