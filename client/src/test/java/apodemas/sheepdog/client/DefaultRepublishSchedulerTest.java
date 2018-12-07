package apodemas.sheepdog.client;

import apodemas.sheepdog.core.FixedLinkedQueue;
import apodemas.sheepdog.core.mqtt.ProMqttMessageFactory;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessageBuilders;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.util.concurrent.DefaultEventExecutor;
import io.netty.util.concurrent.Future;
import net.bytebuddy.build.ToStringPlugin;
import org.junit.Test;

import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * @author caozheng
 * @time 2018-12-06 16:34
 **/
public class DefaultRepublishSchedulerTest {


    private MqttPublishMessage getMessage(int id){
        return MqttMessageBuilders.publish()
                .messageId(id).qos(MqttQoS.AT_LEAST_ONCE)
                .topicName("schedule").payload(Unpooled.buffer()).build();
    }


    @Test
    public void testTimeExceedLimit() throws Exception{
        ClientSettings settings = new ClientSettings();
        settings.setMaxRepubTimes(1);
        DefaultEventExecutor executor = new DefaultEventExecutor();

        FixedLinkedQueue queue = new FixedLinkedQueue(5);
        DefaultRepublishScheduler scheduler = new DefaultRepublishScheduler(executor, settings, queue);

        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class, RETURNS_DEEP_STUBS);
        when(ctx.channel().isOpen()).thenReturn(true);
        when(ctx.executor()).thenReturn(executor);

        Future<Void> f1 = scheduler.schedule(ctx, getMessage(1), 10, TimeUnit.MILLISECONDS);
        Future<Void> f2 = scheduler.schedule(ctx, getMessage(2), 20, TimeUnit.MILLISECONDS);
        Future<Void> f3 = scheduler.schedule(ctx, getMessage(3), 30, TimeUnit.MILLISECONDS);
        f1.await();
        f2.await();
        f3.await();
        assertEquals(ClientRepubExecption.RETRY_EXCEED_LIMIT, f1.cause());
        assertEquals(ClientRepubExecption.RETRY_EXCEED_LIMIT, f2.cause());
        assertEquals(ClientRepubExecption.RETRY_EXCEED_LIMIT, f3.cause());

        assertEquals(0, queue.size());
    }

    @Test
    public void testAck() throws Exception{
        ClientSettings settings = new ClientSettings();
        settings.setMaxRepubTimes(10000);
        DefaultEventExecutor executor = new DefaultEventExecutor();

        FixedLinkedQueue queue = new FixedLinkedQueue(5);
        DefaultRepublishScheduler scheduler = new DefaultRepublishScheduler(executor, settings, queue);

        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class, RETURNS_DEEP_STUBS);
        when(ctx.channel().isOpen()).thenReturn(true);
        when(ctx.executor()).thenReturn(executor);

        Future<Void> f1 = scheduler.schedule(ctx, getMessage(1), 10, TimeUnit.MILLISECONDS);
        Future<Void> f2 = scheduler.schedule(ctx, getMessage(2), 20, TimeUnit.MILLISECONDS);
        Future<Void> f3 = scheduler.schedule(ctx, getMessage(3), 30, TimeUnit.MILLISECONDS);
        scheduler.ack(ProMqttMessageFactory.newPubAck(1));
        scheduler.ack(ProMqttMessageFactory.newPubAck(2));
        scheduler.ack(ProMqttMessageFactory.newPubAck(3));
        f1.await();
        f2.await();
        f3.await();
        assertTrue(f1.isSuccess());
        assertTrue(f2.isSuccess());
        assertTrue(f3.isSuccess());
    }

    @Test
    public void testQueueFull() throws Exception{
        ClientSettings settings = new ClientSettings();
        settings.setMaxRepubTimes(1);
        DefaultEventExecutor executor = new DefaultEventExecutor();

        FixedLinkedQueue queue = new FixedLinkedQueue(5);
        DefaultRepublishScheduler scheduler = new DefaultRepublishScheduler(executor, settings, queue);

        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class, RETURNS_DEEP_STUBS);
        when(ctx.channel().isOpen()).thenReturn(true);
        when(ctx.executor()).thenReturn(executor);

        Future<Void> f1 = scheduler.schedule(ctx, getMessage(1), 10000, TimeUnit.MILLISECONDS);
        scheduler.schedule(ctx, getMessage(2), 10000, TimeUnit.MILLISECONDS);
        scheduler.schedule(ctx, getMessage(3), 10000, TimeUnit.MILLISECONDS);
        scheduler.schedule(ctx, getMessage(4), 10000, TimeUnit.MILLISECONDS);
        scheduler.schedule(ctx, getMessage(5), 10000, TimeUnit.MILLISECONDS);
        scheduler.schedule(ctx, getMessage(6), 10000, TimeUnit.MILLISECONDS);
        f1.await();
        assertEquals(ClientRepubExecption.BUFFERED_EXCEED_LIMIT, f1.cause());
    }

    @Test(expected = CancellationException.class)
    public void testClose() throws Exception{
        ClientSettings settings = new ClientSettings();
        settings.setMaxRepubTimes(1);
        DefaultEventExecutor executor = new DefaultEventExecutor();

        FixedLinkedQueue queue = new FixedLinkedQueue(5);
        DefaultRepublishScheduler scheduler = new DefaultRepublishScheduler(executor, settings, queue);

        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class, RETURNS_DEEP_STUBS);
        when(ctx.channel().isOpen()).thenReturn(true);
        when(ctx.executor()).thenReturn(executor);

        Future<Void> f1 = scheduler.schedule(ctx, getMessage(1), 10000, TimeUnit.MILLISECONDS);
        scheduler.close();
        f1.sync();
    }
}
