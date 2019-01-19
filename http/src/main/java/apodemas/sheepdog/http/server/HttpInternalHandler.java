package apodemas.sheepdog.http.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * @author caozheng
 * @time 2019-01-19 09:44
 **/
public class HttpInternalHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private final HttpDispatcher dispatcher;

    private static InternalLogger logger = InternalLoggerFactory.getInstance(HttpInternalHandler.class);

    public HttpInternalHandler(HttpDispatcher dispatcher){
        this.dispatcher = dispatcher;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        if(!msg.decoderResult().isSuccess()){
            logger.warn("FullHttpRequest decoded failed");
            ctx.close();
        }else{
            dispatcher.handle(ctx, msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.warn("Dispatcher un-caught exception, closing the channel", cause);
        ctx.close();
    }
}
