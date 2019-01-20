package apodemas.sheepdog.http.server;

import apodemas.sheepdog.common.CollectionUtils;
import apodemas.sheepdog.common.StringUtils;
import apodemas.sheepdog.common.url.URL;
import apodemas.sheepdog.common.url.URLParameters;
import apodemas.sheepdog.common.url.URLParser;
import apodemas.sheepdog.core.bytebuf.ByteBufDeserializer;
import apodemas.sheepdog.core.bytebuf.JsonByteBufDeserializer;
import apodemas.sheepdog.core.bytebuf.PlainTextByteBufDeserializer;
import apodemas.sheepdog.http.HttpUtils;
import apodemas.sheepdog.http.server.requst.HttpRequestHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * @author caozheng
 * @time 2019-01-19 09:24
 **/
public class DefaultHttpDispatcher implements HttpDispatcher{
    private final ExceptionHandler exceptionHandler;
    private final Router router;
    private static final String DEFAULT_CHARSET = "utf-8";
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(HttpDispatcher.class);

    public DefaultHttpDispatcher(Router router){
        this(router, null);
    }

    public DefaultHttpDispatcher(Router router, ExceptionHandler exceptionHandler) {
        this.router = router;
        this.exceptionHandler = new WrappedExceptionHandler(exceptionHandler);
    }

    public void handle(ChannelHandlerContext ctx, FullHttpRequest request) {
        if (!request.decoderResult().isSuccess()) {
            logger.warn("FullHttpRequest decoded failed, remote address:"
                    + ctx.channel().remoteAddress(), request.decoderResult().cause());
            return;
        }

        try {
            String originalPath = request.uri();
            URL url = URLParser.parseFormPath(originalPath);
            String path = url.path();
            URLParameters queryParams = url.parameters();
            ByteBuf content = null;
            String contentType = null;
            boolean hasContent = false;
            String charset = null;

            RouteMatchResult result = router.match(path);

            if (!result.isMatche()) {
                throw new HttpException.NotFoundException(path);
            }

            HttpMethod method = request.method();
            HttpRequestHandler handler = result.handler();
            HttpMethod[] supportedMethod = handler.supportMethods();
            HttpHeaders headers = request.headers();
            content = request.content();

            if (content != null && content.readableBytes() != 0) {
                hasContent = true;
            }
            if (hasContent) {
                contentType = headers.get(HttpHeaderNames.CONTENT_TYPE);
                if (StringUtils.empty(contentType)) {
                    throw new HttpServerExecption("Message body is not empty but there is no Content-Type in headers");
                }

                if (StringUtils.notEmpty(contentType)) {
                    contentType = contentType.toLowerCase();
                    String[] cc = HttpUtils.trySplitCharsetFromContentType(contentType);
                    contentType = cc[0];
                    charset = cc[1];
                    if (StringUtils.empty(charset)) {
                        charset = DEFAULT_CHARSET;
                    }
                }

                String[] supportedContentTypes = handler.supportContentTypes();

                if (supportedContentTypes != null && !StringUtils.containsIgnoreCase(contentType, supportedContentTypes)) {
                    throw new HttpException.MediaTypeNotSupportException(contentType);
                }
            }

            if (supportedMethod != null && CollectionUtils.indexOf(supportedMethod, method) == -1) {
                throw new HttpException.MethodNotAllowedException(method, path);
            }

            Object msg = null;
            if (hasContent) {
                try {
                    msg = deserializeBody(contentType, handler.valueClazz() == null ? String.class : handler.valueClazz(), charset, content);
                } catch (Throwable e) {
                    throw new HttpException.BodyDeserializingException(e);
                }
            }

            DefaultHttpContext context = new DefaultHttpContext(
                    path,
                    method,
                    headers,
                    msg,
                    result.params(),
                    queryParams,
                    ctx,
                    new DefaultHttpResponseWriter(ctx, ctx.alloc())
            );

            handler.handle(context);


        } catch (Throwable e) {
            if (!exceptionHandler.handle(ctx, e)) {
                throw e;
            }
        } finally {

        }
    }

    private Object deserializeBody(String contentType, Class<?> exceptedClazz, String charset, ByteBuf byteBuf){
        ByteBufDeserializer deserializer = null;
        if(contentType.equals("text/plain")){
            if(charset.equals(DEFAULT_CHARSET)){
                deserializer =  PlainTextByteBufDeserializer.UTF8;
            }else{
                deserializer = new PlainTextByteBufDeserializer(charset);
            }

            return deserializer.deserialize(byteBuf, String.class);
        }
        else if(contentType.equals("application/json")){
            if(charset.equals(DEFAULT_CHARSET)){
                deserializer =  JsonByteBufDeserializer.DEFAULT;
            }else{
                deserializer = new JsonByteBufDeserializer(charset);
            }

            return deserializer.deserialize(byteBuf, exceptedClazz);
        }else{
            throw new HttpException.MediaTypeNotSupportException(contentType);
        }

    }

    public class WrappedExceptionHandler implements ExceptionHandler{
        private final ExceptionHandler innerHandler;

        public WrappedExceptionHandler(){
            innerHandler = null;
        }

        public WrappedExceptionHandler(ExceptionHandler handler){
            innerHandler = handler;
        }

        JsonErrorMessage messag = new JsonErrorMessage();

        public boolean handle(ChannelHandlerContext ctx, Throwable e){
            if(innerHandler != null && innerHandler.handle(ctx, e)){
                return true;
            }

            HttpResponseStatus status = null;
            if(e instanceof HttpException){
                HttpException ee = (HttpException) e;
                status = ee.getStatus();
                messag.setCode(ee.getStatus().code());
                messag.setMessage(ee.getMessage());
            }else {
                status = HttpResponseStatus.INTERNAL_SERVER_ERROR;
                messag.setCode(500);
                messag.setMessage(e.getMessage());
            }
            new DefaultHttpResponseWriter(ctx, ctx.alloc()).json(status, messag);

            return true;
        }
    }
}
