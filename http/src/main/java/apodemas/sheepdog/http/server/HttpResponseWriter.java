package apodemas.sheepdog.http.server;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * @author caozheng
 * @time 2019-01-18 10:22
 **/
public interface HttpResponseWriter {
    void writeResponse(HttpResponseStatus status, HttpHeaders headers, ByteBuf content);
    void string(HttpResponseStatus status, HttpHeaders headers, Object msg);
    void json(HttpResponseStatus status, HttpHeaders headers, Object msg);

    default void empty(HttpResponseStatus status){
        writeResponse(status, new DefaultHttpHeaders(), null);
    }

    default void empty(){
        writeResponse(HttpResponseStatus.OK, new DefaultHttpHeaders(), null);
    }

    default void json(HttpResponseStatus status, Object msg){
        json(status, new DefaultHttpHeaders(), msg);
    }

    default void json(Object msg){
        json(HttpResponseStatus.OK, new DefaultHttpHeaders(), msg);
    }

    default void string(HttpResponseStatus status, Object msg){
        string(status, new DefaultHttpHeaders(), msg);
    }

    default void string(String msg){
        string(HttpResponseStatus.OK, new DefaultHttpHeaders(), msg);
    }
}
