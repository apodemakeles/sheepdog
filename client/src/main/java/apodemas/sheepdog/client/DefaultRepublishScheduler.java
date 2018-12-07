package apodemas.sheepdog.client;

import apodemas.sheepdog.core.FixedLinkedQueue;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttPubAckMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.ScheduledFuture;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * @author caozheng
 * @time 2018-12-06 08:46
 **/
class DefaultRepublishScheduler implements RepublishScheduler {
    private static final InternalLogger logger
            = InternalLoggerFactory.getInstance(DefaultRepublishScheduler.class);

    private final int maxRepubTimes;
    private final EventExecutor executor;
    private final FixedLinkedQueue<Entry> queue;

    public DefaultRepublishScheduler(EventExecutor executor, ClientSettings settings) {
        this(executor, settings, null);
    }

    public DefaultRepublishScheduler(EventExecutor executor, ClientSettings settings, FixedLinkedQueue<Entry> queue) {
        this.executor = executor;
        this.maxRepubTimes = settings.maxRepubTimes();
        if(queue == null) {
            this.queue = new FixedLinkedQueue<>(settings.maxRepubQueueSize());
        }else{
            this.queue = queue;
        }
    }

    public void ack(MqttPubAckMessage msg){
        if(executor.inEventLoop()){
            doAck(msg);
        }else{
            executor.submit(()->{
               doAck(msg);
            });
        }
    }

    private void doAck(MqttPubAckMessage msg){
        Entry entry;
        if((entry = queue.findFirst(new IdPredict(msg.variableHeader().messageId()), true)) != null) {
            entry.task.cancel(false);
            if (entry.promise != null) {
                entry.promise.trySuccess(null);
            }
            entry.msg.release();
        }
    }

    @Override
    public Future<Void> schedule(ChannelHandlerContext ctx, MqttPublishMessage msg, long delay, TimeUnit unit) {
        Promise<Void> promise = executor.newPromise();
        if(executor.inEventLoop()){
            doSchedule(ctx, msg, delay, unit, promise);
        }else{
            executor.submit(()->{
                doSchedule(ctx, msg, delay, unit, promise);
            });
        }

        return promise;
    }

    private Future<Void> doSchedule(ChannelHandlerContext ctx, MqttPublishMessage msg, long delay, TimeUnit unit, Promise<Void> promise){
        int id = msg.variableHeader().packetId();
        if(queue.findFirst(new IdPredict(id), false) != null){
            if(logger.isWarnEnabled()){
                logger.warn("message (%d) is already in republish queue", id);
            }

            return executor.newFailedFuture(null);
        }else {
            Entry entry = new Entry();
            entry.msg = msg;
            entry.count = maxRepubTimes;
            entry.packetId = msg.variableHeader().packetId();
            entry.nextTimeNano = System.nanoTime() + unit.toNanos(delay);
            entry.promise = promise;

            Entry oldEntry = queue.enqueue(entry);
            if(oldEntry != null){
                oldEntry.msg.release();
                if(oldEntry.promise != null){
                    oldEntry.promise.tryFailure(ClientRepubExecption.BUFFERED_EXCEED_LIMIT);
                }
            }

            entry.task = executor.schedule(new RepublishTask(ctx, entry, delay, unit), delay, unit);

            return promise;
        }
    }

    private class IdPredict implements Predicate<Entry>{
        private int id;

        public IdPredict(int id) {
            this.id = id;
        }

        @Override
        public boolean test(Entry entry) {
            return entry != null && entry.packetId == id;
        }
    }

    public Future<Void> close(){
        Promise<Void> promise = executor.newPromise();
        if(executor.inEventLoop()){
            doClose(promise);
        }else{
            executor.submit(()->{
                doClose(promise);
            });
        }

        return promise;
    }

    public void doClose(Promise<Void> promise){
        for(Entry entry : queue){
            entry.task.cancel(false);
            if(entry.promise != null){
                entry.promise.tryFailure(ClientRepubExecption.CANCEL);
            }
            entry.msg.release();
        }

        promise.trySuccess(null);
    }

    static class Entry{
        private int packetId;
        private MqttPublishMessage msg;
        private int count;
        private ScheduledFuture<?> task;
        private long nextTimeNano;
        private Promise<Void> promise;
    }

    private class RepublishTask extends AbstractScheduleTask{
        private final Entry entry;
        private final long delay;
        private final TimeUnit unit;

        public RepublishTask(ChannelHandlerContext ctx, Entry entry, long delay, TimeUnit unit) {
            super(ctx);
            this.entry = entry;
            this.delay = delay;
            this.unit = unit;
        }

        @Override
        protected void run(ChannelHandlerContext ctx) {
            if(entry.count != 0){
                entry.count--;
                long delay = unit.toNanos(this.delay) - (System.nanoTime() - entry.nextTimeNano) ;
                entry.nextTimeNano = entry.nextTimeNano + unit.toNanos(this.delay);
                entry.task = executor.schedule(new RepublishTask(ctx, entry, this.delay, this.unit),
                        delay, TimeUnit.NANOSECONDS);
            }else{
                if (queue.findFirst(new IdPredict(entry.packetId), true) != null) {
                    if(entry.promise != null){
                        entry.promise.tryFailure(ClientRepubExecption.RETRY_EXCEED_LIMIT);
                    }
                    entry.msg.release();
                    if (logger.isDebugEnabled()){
                        logger.debug("send publish message (%d) failed");
                    }
                }
            }
        }
    }
}
