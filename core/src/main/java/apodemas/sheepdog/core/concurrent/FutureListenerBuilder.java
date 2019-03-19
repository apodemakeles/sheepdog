package apodemas.sheepdog.core.concurrent;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;

import java.util.function.Function;

/**
 * @author caozheng
 * @time 2019-03-08 15:07
 **/
public class FutureListenerBuilder {

    public static <T,K> GenericFutureListener<Future<T>> successThen(Function<T, K> function, Promise promise){
        return new GenericFutureListener<Future<T>>(){
            @Override
            public void operationComplete(Future<T> fut) throws Exception {
                if(fut.isSuccess()){
                    try {
                        K result = function.apply(fut.get());
                        promise.trySuccess(result);
                    }catch (Throwable e){
                        promise.setFailure(e);
                    }

                }else{
                    promise.tryFailure(fut.cause());
                }
            }
        };
    }
}
