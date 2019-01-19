package apodemas.sheepdog.core.bytebuf;

import io.netty.buffer.ByteBuf;

import java.nio.charset.Charset;

/**
 * @author caozheng
 * @time 2019-01-19 09:30
 **/
public class PlainTextByteBufDeserializer implements ByteBufDeserializer {
    private final String charset;
    public static final ByteBufDeserializer UTF8 = new PlainTextByteBufDeserializer("utf-8");

    public PlainTextByteBufDeserializer(String charset) {
        this.charset = charset;
    }

    public <T> T deserialize(ByteBuf byteBuf, Class<T> clazz){
        if (!clazz.isAssignableFrom(String.class)){
            return null;
        }

        return (T)byteBuf.toString(Charset.forName(charset));
    }
}
