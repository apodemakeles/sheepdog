package apodemas.sheepdog.http.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

/**
 * @author caozheng
 * @time 2019-01-19 09:04
 **/
public interface HttpDispatcher {
    void handle(ChannelHandlerContext ctx, FullHttpRequest request);
}
