package apodemas.sheepdog.core;

import apodemas.sheepdog.common.Checks;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author caozheng
 * @time 2018-12-05 16:59
 **/
public abstract class AbstractScheduleTask implements Runnable {
    private final ChannelHandlerContext ctx;

    public AbstractScheduleTask(ChannelHandlerContext ctx) {
        this.ctx = Checks.notNull(ctx, "ctx");
    }

    @Override
    public void run() {
        if (!ctx.channel().isOpen()) {
            return;
        }

        run(ctx);
    }

    protected abstract void run(ChannelHandlerContext ctx);
}
