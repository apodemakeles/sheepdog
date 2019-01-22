package apodemas.sheepdog.server;

import apodemas.sheepdog.core.retry.RetryException;
import apodemas.sheepdog.core.retry.RetryFailReason;
import apodemas.sheepdog.core.retry.RetryScheduler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundInvoker;
import io.netty.handler.codec.mqtt.MqttMessageBuilders;
import io.netty.handler.codec.mqtt.MqttPubAckMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author caozheng
 * @time 2019-01-21 19:22
 **/
public class PublishController {
    private final ChannelHandlerContext ctx;
    private final RepubScheduler repubScheduler;
    private final ServerSettings settings;

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(PublishController.class);

    private AtomicInteger idCounter = new AtomicInteger(1);

    public PublishController(ChannelHandlerContext ctx, ServerSettings settings) {
        this.ctx = ctx;
        this.settings = settings;
        this.repubScheduler = new RepubScheduler(ctx.executor(), settings, ctx);
    }

    public void publish(PublishMessageTemplate template){
//        if(ctx.executor().inEventLoop()){
//            doPublish(message);
//        }else{
//            ctx.executor().submit(()->{
//                doPublish(message);
//            });
//        }

        doPublish(template);
    }

    public void ack(MqttPubAckMessage msg){
        repubScheduler.done(msg.variableHeader().messageId());
    }

    private void doPublish(PublishMessageTemplate template){
        MqttPublishMessage msg = buildMessage(template);

        MqttQoS qos = msg.fixedHeader().qosLevel();
        boolean repub = qos.value() > MqttQoS.AT_MOST_ONCE.value();
        if(repub){
            msg.retain();
            Future<MqttPublishMessage> repubFut = repubScheduler.schedule(
                    ctx,
                    msg.variableHeader().packetId(),
                    msg,
                    settings.publishAckTimeoutSec(),
                    TimeUnit.SECONDS);

            repubFut.addListener((Future<MqttPublishMessage> fut)->{
                if(fut.isSuccess()){
                    MqttPublishMessage message = fut.get();
                    message.release();
                }else{
                    if (fut.cause() instanceof RetryException){
                        RetryException e = (RetryException)fut.cause();
                        MqttPublishMessage message = (MqttPublishMessage)e.value();
                        message.release();
                        int id = message.variableHeader().packetId();
                        if(e.reason() == RetryFailReason.QUEUE_EXCEED_LIMIT){
                            logger.warn("republish queue is full, message (id:{}) be removed", id);
                        }else if(logger.isInfoEnabled()) {
                            logger.debug("republish message (id: {}) failed due to %s", id, e.reason());
                        }
                    }
                }
            });
        }

        ctx.write(msg);
    }

    private MqttPublishMessage buildMessage(PublishMessageTemplate template){
        int id = idCounter.getAndIncrement();
        ByteBuf byteBuf = ctx.alloc().buffer(template.payload().length);
        byteBuf.writeBytes(template.payload());

        return MqttMessageBuilders
                .publish()
                .retained(template.retained())
                .topicName(template.topic())
                .qos(template.qos())
                .payload(byteBuf)
                .messageId(id)
                .build();
    }

    private static class RepubScheduler extends RetryScheduler<MqttPublishMessage, Integer>{
        private final ChannelOutboundInvoker outInvoker;

        public RepubScheduler(EventExecutor executor, ServerSettings settings, ChannelOutboundInvoker outInvoker) {
            super(executor, settings.maxRepubTimes(), settings.maxRepubQueueSize());
            this.outInvoker = outInvoker;
        }

        @Override
        protected void retry(ChannelHandlerContext ctx, Integer id, MqttPublishMessage value) {
            outInvoker.write(value);
        }
    }
}
