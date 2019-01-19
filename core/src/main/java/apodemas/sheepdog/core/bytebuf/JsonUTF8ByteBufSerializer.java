package apodemas.sheepdog.core.bytebuf;

import com.alibaba.fastjson.JSON;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;

/**
 * @author caozheng
 * @time 2019-01-19 09:30
 **/
public class JsonUTF8ByteBufSerializer implements ByteBufSerializer {

    public static final JsonUTF8ByteBufSerializer DEFAULT = new JsonUTF8ByteBufSerializer();

    public ByteBuf serialize(Object value, ByteBufAllocator allocator){
        if (value == null) {
            return Unpooled.buffer(0);
        }

        //fastjson seems to force using utf-8
        byte[] bytes = JSON.toJSONBytes(value);
        ByteBuf byteBuf = allocator.buffer(bytes.length);
        byteBuf.writeBytes(bytes);

        return byteBuf;
    }
}
