package apodemas.sheepdog.client;

import apodemas.sheepdog.core.concurrent.EventLoopPromise;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.*;

public class Client {
    private final EventLoop eventLoop;
    private final EventLoopPromise shutdownHandler;
    private int KeepAliveSec;
    private String username;
    private String password;
    private ClientSettings settings;

    public Client(){
        this.eventLoop = new NioEventLoopGroup(1).next();
        this.shutdownHandler = new EventLoopPromise(this.eventLoop);
    }

    public Future<?> connect(String host, int port){
        Bootstrap bootstrap = new Bootstrap();
        ChannelFuture connectFut = bootstrap.group(eventLoop)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, settings.getConnectTimeoutSec() * 1000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipe = ch.pipeline();
                        pipe.addLast(new LoggingHandler(LogLevel.INFO));
                        pipe.addLast(MqttEncoder.INSTANCE);
                        pipe.addLast(new MqttDecoder());
                    }
                }).connect(host, port);
        connectFut.addListener(fut->{
            if(!fut.isSuccess()){
                shutdownHandler.setFailure(fut.cause());
            }
        });

        return shutdownHandler.terminationFuture();
    }

    public void shutdownGracefully(){
        shutdownHandler.shutdownGracefully();
    }
}
