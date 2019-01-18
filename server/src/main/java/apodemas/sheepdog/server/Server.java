package apodemas.sheepdog.server;

import apodemas.sheepdog.core.concurrent.EventLoopPromise;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * @author caozheng
 * @time 2019-01-02 11:26
 **/
public class Server {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(Server.class);
    private final ServerSettings settings;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private final EventLoopPromise shutdownHandler;

    public Server(){
        this(ServerSettings.DEFAULT);
    }

    public Server(ServerSettings settings){
        this.settings = settings;
        this.bossGroup = new NioEventLoopGroup(1);
        this.workerGroup = new NioEventLoopGroup();
        this.shutdownHandler = new EventLoopPromise(GlobalEventExecutor.INSTANCE);
    }

    public Future<Void> bind(String inetHost, int inetPort){
        ServerBootstrap bootstrap = new ServerBootstrap();
        logger.info("server startup");

        return bootstrap.group(bossGroup, workerGroup)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception{
                        ChannelPipeline pipe = ch.pipeline();
                        pipe.addLast("mqtt encoder", MqttEncoder.INSTANCE);
                        pipe.addLast("mqtt decoder", new MqttDecoder());
                        ServerHandler serverHandler = new ServerHandler(settings, new DefaultMessageProcessor(), new MemorySessionManager());
                        pipe.addLast(ServerHandler.NAME, serverHandler);
                    }
                }).bind(inetHost, inetPort);
    }

    public Future<?> terminationFuture(){
        return shutdownHandler.terminationFuture();
    }

    public void shutdownGracefully(){
        shutdownHandler.shutdownGracefully();
    }
}
