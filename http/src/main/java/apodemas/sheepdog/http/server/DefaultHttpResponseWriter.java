package apodemas.sheepdog.http.server;

import apodemas.sheepdog.core.bytebuf.JsonUTF8ByteBufSerializer;
import apodemas.sheepdog.core.bytebuf.StringByteBufSerializer;
import apodemas.sheepdog.http.HttpUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;

import java.util.concurrent.atomic.AtomicBoolean;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpHeaderValues.TEXT_PLAIN;

/**
 * @author caozheng
 * @time 2019-01-19 09:25
 **/
public class DefaultHttpResponseWriter implements HttpResponseWriter {

    private final ChannelHandlerContext ctx;
    private final ByteBufAllocator allocator;
    private final AtomicBoolean output = new AtomicBoolean(false);

    public DefaultHttpResponseWriter(ChannelHandlerContext ctx, ByteBufAllocator allocator) {
        this.ctx = ctx;
        this.allocator = allocator;
    }

    public void writeResponse(HttpResponseStatus status, HttpHeaders headers, ByteBuf content) {
        if (!output.compareAndSet(false, true)) {
            throw new HttpServerExecption("HttpContext already output");
        }

        if(content == null){
            content = Unpooled.buffer(0);
        }

        headers.setInt(CONTENT_LENGTH, content.readableBytes());

        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                status,
                content,
                headers,
                new DefaultHttpHeaders()
        );

        ctx.writeAndFlush(response);
    }

    public void string(HttpResponseStatus status, HttpHeaders headers, Object msg) {
        ByteBuf content = null;
        if (msg != null) {
            headers.set(CONTENT_TYPE,
                    HttpUtils.appendCharsetToContentType(TEXT_PLAIN.toString(), "utf-8"));
            content = StringByteBufSerializer.UTF8.serialize(msg, allocator);
        } else {
            content = Unpooled.buffer(0);
        }

        writeResponse(status, headers, content);
    }

    public void json(HttpResponseStatus status, HttpHeaders headers, Object msg) {
        ByteBuf content = null;
        if (msg != null) {
            headers.set(CONTENT_TYPE,
                    HttpUtils.appendCharsetToContentType(APPLICATION_JSON.toString(), "utf-8"));
            content = JsonUTF8ByteBufSerializer.DEFAULT.serialize(msg, allocator);
        } else {
            content = Unpooled.buffer(0);
        }

        writeResponse(status, headers, content);
    }
}
