package apodemas.sheepdog.core.bytebuf;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

/**
 * @author caozheng
 * @time 2019-01-19 09:29
 **/
public interface ByteBufSerializer  {
    ByteBuf serialize(Object value, ByteBufAllocator allocator);
}
