package apodemas.sheepdog.core.bytebuf;

import io.netty.buffer.ByteBuf;

/**
 * @author caozheng
 * @time 2019-01-19 09:29
 **/
public interface ByteBufDeserializer{
    <T> T deserialize(ByteBuf byteBuf, Class<T> clazz);
}