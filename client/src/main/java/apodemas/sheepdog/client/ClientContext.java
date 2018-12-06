package apodemas.sheepdog.client;

import apodemas.sheepdog.core.concurrent.EventLoopPromise;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;

import java.util.concurrent.TimeUnit;

/**
 * @author caozheng
 * @time 2018-12-05 16:24
 **/
public class ClientContext {
    private final ChannelHandlerContext handlerContext;
    private final EventLoopPromise shutdownHandler;
    private final RepublishScheduler scheduler;
    private final ByteBufAllocator alloc;
    private final ClientSettings settings;

    public ClientContext(ChannelHandlerContext handlerContext,
                         EventLoopPromise shutdownHandler,
                         RepublishScheduler scheduler,
                         ByteBufAllocator alloc,
                         ClientSettings settings) {
        this.handlerContext = handlerContext;
        this.shutdownHandler = shutdownHandler;
        this.scheduler = scheduler;
        this.alloc = alloc;
        this.settings = settings;
    }

    public void close(){
        shutdownHandler.shutdownGracefully();
    }

    public void close(Throwable cause){
        shutdownHandler.setFailure(cause);
    }

    public void publish(MqttPublishMessage msg){
        boolean repub = msg.fixedHeader().qosLevel().value() > MqttQoS.AT_MOST_ONCE.value();
        if(repub){
            msg.retain();
        }
        ChannelFuture fut = handlerContext.write(msg);
        if (repub) {
            fut.addListener(f -> {
                if (f.isSuccess()) {
                    scheduler.schedule(handlerContext, msg,
                            settings.getPingResponseTimeoutSec(), TimeUnit.SECONDS);
                } else {
                    msg.release();
                }
            });
        }
    }

    public ByteBufAllocator alloc(){
        return alloc;
    }

}

