package apodemas.sheepdog.server;

import apodemas.sheepdog.core.concurrent.EventLoopPromise;
import apodemas.sheepdog.http.server.*;
import apodemas.sheepdog.server.http.ClientHandler;
import apodemas.sheepdog.server.http.ClientsHandler;
import apodemas.sheepdog.server.http.HealthCheckHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
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

        MemorySessionManager manager = new MemorySessionManager();
        ChannelFuture channelFuture = bootstrap.group(bossGroup, workerGroup)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception{
                        ChannelPipeline pipe = ch.pipeline();
                        pipe.addLast("mqtt encoder", MqttEncoder.INSTANCE);
                        pipe.addLast("mqtt decoder", new MqttDecoder());
                        ServerHandler serverHandler = new ServerHandler(settings, new DefaultMessageProcessor(), manager);
                        pipe.addLast(ServerHandler.NAME, serverHandler);
                    }
                }).bind(inetHost, inetPort);

        HttpServer server = buildHttpServer(inetHost, manager);
        server.start();

        return channelFuture;
    }

    private HttpServer buildHttpServer(String inetHost, SessionManager manager){
        ParameterizedPatriciaTrieRouter router = new ParameterizedPatriciaTrieRouter();
        router.add("/healthcheck", new HealthCheckHandler());
        router.add("/clients", new ClientsHandler(manager));
        router.add("/clients/:id", new ClientHandler(manager));

        return new HttpServer(inetHost, 1885, new HttpServerSetting(), new DefaultHttpDispatcher(router));
    }

    public Future<?> terminationFuture(){
        return shutdownHandler.terminationFuture();
    }

    public void shutdownGracefully(){
        shutdownHandler.shutdownGracefully();
    }
}
