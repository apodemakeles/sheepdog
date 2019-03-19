package apodemas.sheepdog.server;

import apodemas.sheepdog.core.mqtt.ProMqttMessageFactory;
import apodemas.sheepdog.server.pub.PublishController;
import apodemas.sheepdog.server.sub.SubscriptionController;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.*;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Promise;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.List;

/**
 * @author caozheng
 * @time 2019-03-08 16:35
 **/
public class LocalMemorySession implements Session{
    private final ChannelHandlerContext ctx;
    private final EventExecutor eventExecutor;
    private final String clientId;
    private volatile boolean open = true;
    private final PublishController pubCtrl;
    private final SessionService sessionService;
    private final SubscriptionController subController;

    private static final InternalLogger LOG = InternalLoggerFactory.getInstance(LocalMemorySession.class);

    public LocalMemorySession(ChannelHandlerContext ctx, String clientId, PublishController pubCtrl,
                              SessionService sessionService, SubscriptionController subController) {
        this.ctx = ctx;
        this.eventExecutor = ctx.executor();
        this.clientId = clientId;
        this.pubCtrl = pubCtrl;
        this.sessionService = sessionService;
        this.subController = subController;
    }

    public String clientId(){
        return clientId;
    }

    public boolean isOpen(){
        return open;
    }

    public void writeAndFlush(MqttMessage message){
        safeExecute(()->{
            if(!open){
                LOG.info("{}'s connection's already closed.", clientId);
                return;
            }
            ctx.writeAndFlush(message);
        });
    }

    public void publish(PublishMessageTemplate template){
        safeExecute(()->{
            if(!open){
                LOG.info("{}'s connection's already closed.", clientId);
                return;
            }
            pubCtrl.publish(template);
        });
    }

    public void ack(MqttPubAckMessage message){
        safeExecute(()-> {
            if (!open) {
                LOG.info("{}'s connection's already closed.");
                return;
            }
            pubCtrl.ack(message);
        });
    }

    private void safeExecute(Runnable runnable){
        if(eventExecutor.inEventLoop()){
            runnable.run();
        }else{
            eventExecutor.submit(runnable);
        }
    }

    @Override
    public void disconnect(){
        safeExecute(()-> {
            open = false;
            ctx.close();
            subController.clean(this);
            sessionService.removeSession(this);
        });
    }

    @Override
    public void subscribe(MqttSubscribeMessage message){
        int id = message.variableHeader().messageId();
        List<MqttTopicSubscription> originalSubs = message.payload().topicSubscriptions();
        List<Subscription> subs = Subscription.fromMqttTopicSubscription(originalSubs);
        safeExecute(()-> {
            if(!open){
                LOG.info("{}'s connection's already closed.", clientId);
                return;
            }

            List<Integer> subResults = subController.subscribe(this, subs);
            MqttSubAckMessage ackMsg = ProMqttMessageFactory.newSubAck(id, subResults);
            ctx.writeAndFlush(ackMsg);
            LOG.info("client ({}) subscribe (message id: {}) topics {}", clientId, id, subs);
        });
    }

    @Override
    public void unsubscribe(MqttUnsubscribeMessage message){
        int id = message.variableHeader().messageId();
        List<String> topics = message.payload().topics();
        safeExecute(()-> {
            if (!open) {
                LOG.info("{}'s connection's already closed.", clientId);
                return;
            }

            subController.unsubscribe(this, topics);
            MqttUnsubAckMessage ackMsg = ProMqttMessageFactory.newUnsubAck(id);
            ctx.writeAndFlush(ackMsg);
            LOG.info("client ({}) unsubscribe (message id: {}) topics {}", clientId, id, topics);
        });
    }

}
