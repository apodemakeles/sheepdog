package apodemas.sheepdog.core.bytebuf;

import com.alibaba.fastjson.JSON;
import io.netty.buffer.ByteBuf;

import java.nio.charset.Charset;

/**
 * @author caozheng
 * @time 2019-01-19 09:30
 **/
public class JsonByteBufDeserializer implements ByteBufDeserializer {
    private final String charset;
    public static final ByteBufDeserializer DEFAULT = new JsonByteBufDeserializer("utf-8");

    public JsonByteBufDeserializer(String charset) {
        this.charset = charset;
    }

    public <T> T deserialize(ByteBuf byteBuf, Class<T> clazz){
        if (byteBuf == null) {
            return null;
        }

        if (byteBuf.hasArray()){
            return JSON.parseObject(
                    byteBuf.array(),
                    byteBuf.arrayOffset(),
                    byteBuf.readableBytes(),
                    Charset.forName(charset),
                    clazz);
        }else{
            byte[] bytes = new byte[byteBuf.readableBytes()];
            byteBuf.getBytes(byteBuf.readerIndex(), bytes);

            return JSON.parseObject(
                    bytes,
                    0,
                    bytes.length,
                    Charset.forName(charset),
                    clazz);
        }

    }
}
