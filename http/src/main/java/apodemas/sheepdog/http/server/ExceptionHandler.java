package apodemas.sheepdog.http.server;

import io.netty.channel.ChannelHandlerContext;

/**
 * @author caozheng
 * @time 2019-01-19 09:21
 **/
public interface ExceptionHandler {
    boolean handle(ChannelHandlerContext ctx, Throwable e);
}
