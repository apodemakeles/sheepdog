package apodemas.sheepdog.core.bytebuf;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;

import java.io.UnsupportedEncodingException;

/**
 * @author caozheng
 * @time 2019-01-19 09:31
 **/
public class StringByteBufSerializer implements ByteBufSerializer {
    private final String charset;
    public static final StringByteBufSerializer UTF8 = new StringByteBufSerializer("utf-8");

    public StringByteBufSerializer(String charset) {
        this.charset = charset;
    }

    public ByteBuf serialize(Object value, ByteBufAllocator allocator){
        try {
            if (value == null) {
                return Unpooled.buffer(0);
            }

            String str;
            if (value instanceof String) {
                str = (String)value;
            }else{
                str = value.toString();
            }

            byte[] bytes = str.getBytes(charset);
            ByteBuf byteBuf = allocator.buffer(bytes.length);
            byteBuf.writeBytes(bytes);

            return byteBuf;

        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

}
