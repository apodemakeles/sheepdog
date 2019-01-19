package apodemas.sheepdog.http.server;

import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * @author caozheng
 * @time 2019-01-19 09:04
 **/
public abstract class HttpException extends HttpServerExecption{
    private final HttpResponseStatus status;

    public HttpException(String message, HttpResponseStatus status) {
        super(message);
        this.status = status;
    }

    public HttpResponseStatus getStatus() {
        return status;
    }

    public static class NotFoundException extends HttpException{
        public NotFoundException(String url){
            super(String.format("%s is not found", url), HttpResponseStatus.NOT_FOUND);
        }
    }

    public static class MediaTypeNotSupportException extends HttpException{
        public MediaTypeNotSupportException(String mime){
            super(String.format("%s is not supported", mime), HttpResponseStatus.UNSUPPORTED_MEDIA_TYPE);
        }
    }

    public static class MethodNotAllowedException extends HttpException{
        public MethodNotAllowedException(HttpMethod method, String path){
            super(String.format("%s is not allowed for path %s", method, path), HttpResponseStatus.METHOD_NOT_ALLOWED);
        }
    }

    public static class BodyDeserializingException extends HttpException{
        public BodyDeserializingException(Throwable cause){
            super(String.format("Message body deserializing failed: %s", cause), HttpResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
