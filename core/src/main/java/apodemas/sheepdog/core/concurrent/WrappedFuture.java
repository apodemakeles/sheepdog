package apodemas.sheepdog.core.concurrent;

import apodemas.sheepdog.common.Checks;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author caozheng
 * @time 2019-03-21 10:20
 **/
public abstract class WrappedFuture<T> implements Future<T> {
    private Future<T> future;

    public WrappedFuture(Future<T> future){
        Checks.notNull(future, "future");

        this.future = future;
    }

    public Future<T> future(){
        return future;
    }

    @Override
    public boolean isSuccess(){
        return future.isSuccess();
    }

    @Override
    public boolean isCancellable(){
        return future.isCancellable();
    }

    @Override
    public Throwable cause(){
        return future.cause();
    }

    @Override
    public Future<T> addListener(GenericFutureListener<? extends Future<? super T>> listener){
        future.addListener(listener);

        return this;
    }

    @Override
    public Future<T> addListeners(GenericFutureListener<? extends Future<? super T>>... listeners){
        future.addListeners(listeners);

        return this;
    }

    @Override
    public Future<T> removeListener(GenericFutureListener<? extends Future<? super T>> listener){
        future.removeListener(listener);

        return this;
    }

    @Override
    public Future<T> removeListeners(GenericFutureListener<? extends Future<? super T>>... listeners){
        future.removeListeners(listeners);

        return this;
    }

    @Override
    public Future<T> sync() throws InterruptedException{
        future.sync();

        return this;
    }

    @Override
    public Future<T> syncUninterruptibly(){
        future.syncUninterruptibly();

        return this;
    }

    @Override
    public Future<T> await() throws InterruptedException{
        future.await();

        return this;
    }

    @Override
    public Future<T> awaitUninterruptibly(){
        future.awaitUninterruptibly();

        return this;
    }

    @Override
    public T getNow(){
        return future.getNow();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning){
        return future.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled(){
        return future.isCancelled();
    }

    @Override
    public boolean isDone(){
        return future.isDone();
    }

    @Override
    public boolean await(long timeout, TimeUnit unit) throws InterruptedException{
        return future.await(timeout, unit);
    }

    @Override
    public boolean await(long timeoutMillis) throws InterruptedException{
        return future.await(timeoutMillis);
    }

    @Override
    public boolean awaitUninterruptibly(long timeout, TimeUnit unit){
        return future.awaitUninterruptibly(timeout, unit);
    }

    @Override
    public boolean awaitUninterruptibly(long timeoutMillis){
        return future.awaitUninterruptibly(timeoutMillis);
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        return future.get();
    }

    @Override
    public T get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return future.get(timeout, unit);
    }


}
