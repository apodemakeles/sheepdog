package apodemas.sheepdog.http.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.concurrent.Future;

/**
 * @author caozheng
 * @time 2019-01-19 09:45
 **/
public class HttpServer {
    private final String ip;
    private final int port;
    private final HttpDispatcher dispatcher;
    private final HttpServerSetting setting;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    public HttpServer(String ip, int port, HttpServerSetting setting, HttpDispatcher dispatcher){
        this.ip = ip;
        this.port = port;
        this.setting = setting;
        this.dispatcher = dispatcher;
        this.bossGroup = new NioEventLoopGroup(1);
        this.workerGroup = new NioEventLoopGroup();
    }

    public String id(){
        return "Chopin Http Server";
    }

    public Future<Void> start(){
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipe = ch.pipeline();
                        pipe.addLast(new ReadTimeoutHandler(setting.readTimeoutSeconds()));
                        pipe.addLast(new HttpServerCodec());
                        pipe.addLast(new HttpObjectAggregator(512 * 1024));
                        pipe.addLast(new HttpInternalHandler(dispatcher));
                    }
                });

        return bootstrap.bind(this.ip, this.port);
    }

//    public Future<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit){
//        Future<?> boss = bossGroup.shutdownGracefully(quietPeriod, timeout, unit);
//        Future<?> worker = workerGroup.shutdownGracefully(quietPeriod, timeout, unit);
//
//        return new AllFuture(GlobalEventExecutor.INSTANCE, boss, worker);
//    }
}
