package apodemas.sheepdog.client;

/**
 * @author caozheng
 * @time 2018-12-10 16:31
 **/

import apodemas.sheepdog.core.FixedLinkedQueue;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.DefaultEventExecutor;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class RetrySchedulerTest {
    private static class Message{
        private final Integer id;
        private int count;

        public Message(Integer id) {
            this.id = id;
        }

        public Integer getId() {
            return id;
        }

        public void increase(){
            count += 1;
        }

        public int count(){
            return count;
        }
    }

    private static class TestingRetryScheduler extends RetryScheduler<Message, Integer>{
        public TestingRetryScheduler(EventExecutor executor, int maxRetryTimes, FixedLinkedQueue queue) {
            super(executor, maxRetryTimes, queue);
        }

        @Override
        protected void retry(ChannelHandlerContext ctx, Integer id, Message value) {
            value.increase();
        }
    }

    private RetryFailReason getReason(Throwable e){
        return ((RetryException)e).reason();
    }

    private Message getValue(Throwable e){
        return (Message)(((RetryException)e).value());
    }

    @Test
    public void testTimeExceedLimit() throws Exception{
        DefaultEventExecutor executor = new DefaultEventExecutor();
        FixedLinkedQueue queue = new FixedLinkedQueue(5);
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class, RETURNS_DEEP_STUBS);
        when(ctx.channel().isOpen()).thenReturn(true);
        when(ctx.executor()).thenReturn(executor);

        TestingRetryScheduler scheduler = new TestingRetryScheduler(executor, 3, queue);

        Future<Message> f1 = scheduler.schedule(ctx, 1, new Message(1),  10, TimeUnit.MILLISECONDS);
        Future<Message> f2 = scheduler.schedule(ctx, 2, new Message(2),  20, TimeUnit.MILLISECONDS);
        Future<Message> f3 = scheduler.schedule(ctx, 3, new Message(3),  30, TimeUnit.MILLISECONDS);

        f1.await();
        f2.await();
        f3.await();

        assertEquals(RetryFailReason.RETRY_EXCEED_LIMIT, getReason(f1.cause()));
        assertEquals(3, getValue(f1.cause()).count());
        assertEquals(RetryFailReason.RETRY_EXCEED_LIMIT, getReason(f2.cause()));
        assertEquals(3, getValue(f2.cause()).count());
        assertEquals(RetryFailReason.RETRY_EXCEED_LIMIT, getReason(f3.cause()));
        assertEquals(3, getValue(f3.cause()).count());

        assertEquals(0, queue.size());
    }

    @Test
    public void testAck() throws Exception{
        DefaultEventExecutor executor = new DefaultEventExecutor();
        FixedLinkedQueue queue = new FixedLinkedQueue(5);
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class, RETURNS_DEEP_STUBS);
        when(ctx.channel().isOpen()).thenReturn(true);
        when(ctx.executor()).thenReturn(executor);

        TestingRetryScheduler scheduler = new TestingRetryScheduler(executor, 10000, queue);

        Future<Message> f1 = scheduler.schedule(ctx, 1, new Message(1),  10, TimeUnit.MILLISECONDS);
        Future<Message> f2 = scheduler.schedule(ctx, 2, new Message(2),  20, TimeUnit.MILLISECONDS);
        Future<Message> f3 = scheduler.schedule(ctx, 3, new Message(3),  30, TimeUnit.MILLISECONDS);

        scheduler.done(1);
        scheduler.done(2);
        scheduler.done(3);

        f1.await();
        f2.await();
        f3.await();
        assertTrue(f1.isSuccess());
        assertEquals(1, f1.get().getId().intValue());
        assertTrue(f2.isSuccess());
        assertEquals(2, f2.get().getId().intValue());
        assertTrue(f3.isSuccess());
        assertEquals(3, f3.get().getId().intValue());
    }

    @Test
    public void testQueueFull() throws Exception{
        DefaultEventExecutor executor = new DefaultEventExecutor();
        FixedLinkedQueue queue = new FixedLinkedQueue(5);
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class, RETURNS_DEEP_STUBS);
        when(ctx.channel().isOpen()).thenReturn(true);
        when(ctx.executor()).thenReturn(executor);

        TestingRetryScheduler scheduler = new TestingRetryScheduler(executor, 1, queue);
        Future<Message> f1 = scheduler.schedule(ctx, 1, new Message(1),  10000, TimeUnit.MILLISECONDS);
        scheduler.schedule(ctx, 2, new Message(2),  20000, TimeUnit.MILLISECONDS);
        scheduler.schedule(ctx, 3, new Message(3),  20000, TimeUnit.MILLISECONDS);
        scheduler.schedule(ctx, 4, new Message(4),  20000, TimeUnit.MILLISECONDS);
        scheduler.schedule(ctx, 5, new Message(5),  20000, TimeUnit.MILLISECONDS);
        scheduler.schedule(ctx, 6, new Message(6),  20000, TimeUnit.MILLISECONDS);
        f1.await();
        assertEquals(RetryFailReason.QUEUE_EXCEED_LIMIT, getReason(f1.cause()));
    }

    @Test
    public void testDispose() throws Exception{
        DefaultEventExecutor executor = new DefaultEventExecutor();
        FixedLinkedQueue queue = new FixedLinkedQueue(5);
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class, RETURNS_DEEP_STUBS);
        when(ctx.channel().isOpen()).thenReturn(true);
        when(ctx.executor()).thenReturn(executor);

        TestingRetryScheduler scheduler = new TestingRetryScheduler(executor, 1, queue);

        Future<Message> f1 = scheduler.schedule(ctx, 1, new Message(1),  10000, TimeUnit.MILLISECONDS);
        Future<Void> fut = scheduler.dispose(executor.newPromise());
        fut.await();
        f1.await();
        assertEquals(RetryFailReason.DISPOSE, getReason(f1.cause()));
    }

    @Test
    public void testDuplicateId() throws Exception{
        DefaultEventExecutor executor = new DefaultEventExecutor();
        FixedLinkedQueue queue = new FixedLinkedQueue(5);
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class, RETURNS_DEEP_STUBS);
        when(ctx.channel().isOpen()).thenReturn(true);
        when(ctx.executor()).thenReturn(executor);

        TestingRetryScheduler scheduler = new TestingRetryScheduler(executor, 1, queue);

        scheduler.schedule(ctx, 1, new Message(1),  10000, TimeUnit.MILLISECONDS);
        Future<Message> f1 = scheduler.schedule(ctx, 1, new Message(1),  10000, TimeUnit.MILLISECONDS);
        f1.await();
        assertEquals(RetryFailReason.DUPLICATE_ID, getReason(f1.cause()));
    }

}
