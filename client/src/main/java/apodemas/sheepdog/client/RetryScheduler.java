package apodemas.sheepdog.client;

import apodemas.sheepdog.core.FixedLinkedQueue;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.ScheduledFuture;

import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * @author caozheng
 * @time 2018-12-10 14:48
 **/
public abstract class RetryScheduler<T, K> {
    private final EventExecutor executor;
    private final int maxRetryTimes;
    private final FixedLinkedQueue<Entry> queue;

    RetryScheduler(EventExecutor executor, int maxRetryTimes, FixedLinkedQueue queue){
        this.executor = executor;
        this.maxRetryTimes = maxRetryTimes;
        this.queue = queue;
    }

    public RetryScheduler(EventExecutor executor, int maxRetryTimes, int maxRetryQueueSize) {
        this(executor, maxRetryTimes, new FixedLinkedQueue<>(maxRetryQueueSize));
    }

    public void done(K id){
        if(executor.inEventLoop()){
            doneInternal(id);
        }else{
            executor.submit(()->{
                doneInternal(id);
            });
        }
    }

    public Future<T> schedule(ChannelHandlerContext ctx, K id, T value, long delay, TimeUnit unit){
        return schedule(ctx, id, value, delay, unit, ctx.executor().newPromise());
    }

    public Future<T> schedule(ChannelHandlerContext ctx, K id, T value, long delay, TimeUnit unit, Promise<T> promise){
        if(executor.inEventLoop()){
            scheduleInternal(ctx, id, value, delay, unit, promise);
        }else{
            executor.submit(()->{
                scheduleInternal(ctx, id, value, delay, unit, promise);
            });
        }

        return promise;
    }

    public Future<Void> dispose(Promise<Void> promise){
        if(executor.inEventLoop()){
            disposeInternal(promise);
        }else{
            executor.submit(()->{
                disposeInternal(promise);
            });
        }

        return promise;
    }

    private void doneInternal(K id){
        Entry entry;
        if((entry = queue.findFirst(new EqualPredict(id), true)) != null) {
            entry.task.cancel(false);
            if (entry.promise != null) {
                entry.promise.trySuccess(null);
            }
        }
    }

    private void scheduleInternal(ChannelHandlerContext ctx, K id, T value, long delay, TimeUnit unit, Promise<T> promise){
        if(queue.findFirst(new EqualPredict(id), false) != null){
            promise.tryFailure(new RetryException(value, RetryFailReason.DUPLICATE_ID));
        }else{
            Entry entry = new Entry();
            entry.id = id;
            entry.value = value;
            entry.count = maxRetryTimes;
            entry.nextTimeNano = System.nanoTime() + unit.toNanos(delay);
            entry.promise = promise;

            Entry oldEntry = queue.enqueue(entry);
            if(oldEntry != null){
                if(oldEntry.promise != null){
                    oldEntry.promise.tryFailure(new RetryException(value, RetryFailReason.QUEUE_EXCEED_LIMIT));
                }
            }

            entry.task = executor.schedule(new RetryTask(ctx, entry, delay, unit), delay, unit);
        }
    }

    private void disposeInternal(Promise<Void> promise){
        for(Entry entry : queue){
            entry.task.cancel(false);
            if(entry.promise != null){
                entry.promise.tryFailure(new RetryException(entry.value, RetryFailReason.DISPOSE));
            }
        }

        promise.trySuccess(null);
    }

    protected abstract void retry(ChannelHandlerContext ctx, K id, T value);

    private class EqualPredict implements Predicate<Entry> {
        private K id;

        public EqualPredict(K id) {
            this.id = id;
        }

        @Override
        public boolean test(Entry entry) {
            return entry != null && entry.id.equals(id);
        }
    }

    private class RetryTask extends AbstractScheduleTask{
        private final Entry entry;
        private final long delay;
        private final TimeUnit unit;

        public RetryTask(ChannelHandlerContext ctx, Entry entry, long delay, TimeUnit unit) {
            super(ctx);
            this.entry = entry;
            this.delay = delay;
            this.unit = unit;
        }

        @Override
        protected void run(ChannelHandlerContext ctx) {
            if(entry.count != 0){
                try{
                    retry(ctx, entry.id, entry.value);
                }catch(Throwable e){
                }
                entry.count--;
                long delay = unit.toNanos(this.delay) - (System.nanoTime() - entry.nextTimeNano) ;
                entry.nextTimeNano = entry.nextTimeNano + unit.toNanos(this.delay);
                entry.task = executor.schedule(new RetryTask(ctx, entry, this.delay, this.unit),
                        delay, TimeUnit.NANOSECONDS);
            }else{
                if (queue.findFirst(new EqualPredict(entry.id), true) != null) {
                    if(entry.promise != null){
                        entry.promise.tryFailure(new RetryException(entry.value, RetryFailReason.RETRY_EXCEED_LIMIT));
                    }
                }
            }
        }
    }

    class Entry{
        private K id;
        private T value;
        private int count;
        private ScheduledFuture<?> task;
        private long nextTimeNano;
        private Promise<T> promise;
    }

}
