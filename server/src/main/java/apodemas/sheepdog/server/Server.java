package apodemas.sheepdog.server;

import apodemas.sheepdog.core.concurrent.EventLoopPromise;
import apodemas.sheepdog.http.server.DefaultHttpDispatcher;
import apodemas.sheepdog.http.server.HttpServer;
import apodemas.sheepdog.http.server.HttpServerSetting;
import apodemas.sheepdog.http.server.ParameterizedPatriciaTrieRouter;
import apodemas.sheepdog.server.api.*;
import apodemas.sheepdog.server.mq.DefaultMessageConsumer;
import apodemas.sheepdog.server.mq.MQConsumerStartupException;
import apodemas.sheepdog.server.mq.MessageQueueConsumer;
import apodemas.sheepdog.server.mq.rocket.RocketMQConsumer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.apache.rocketmq.common.message.MessageExt;

/**
 * @author caozheng
 * @time 2019-01-02 11:26
 **/
public class Server {
    private static final InternalLogger LOG = InternalLoggerFactory.getInstance(Server.class);
    private final ServerSettings settings;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private final EventLoopPromise shutdownHandler;
    private final MessageQueueConsumer mqConsumer;

    public Server(){
        this(ServerSettings.DEFAULT);
    }

    public Server(ServerSettings settings){
        this.settings = settings;
        this.bossGroup = new NioEventLoopGroup(1);
        this.workerGroup = new NioEventLoopGroup();
        this.shutdownHandler = new EventLoopPromise(GlobalEventExecutor.INSTANCE);
        this.mqConsumer = new RocketMQConsumer();
    }

    public Future<Void> bind(String inetHost, int inetPort) throws Exception{
        ServerBootstrap bootstrap = new ServerBootstrap();
        LOG.info("server startup");

        MemorySessionManager manager = new MemorySessionManager(settings);
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

        startupMQConsumer(manager);

        HttpServer server = buildHttpServer(inetHost, manager);
        server.start();
        LOG.info("http server started");
        LOG.info("server start successfully");

        return channelFuture;
    }

    private void startupMQConsumer(SessionManager manager) throws MQConsumerStartupException {
        mqConsumer.initWithSettings(this.settings);
        mqConsumer.start(new DefaultMessageConsumer(manager));
        LOG.info("mq consumer started");
    }

    private HttpServer buildHttpServer(String inetHost, SessionManager manager){
        ParameterizedPatriciaTrieRouter router = new ParameterizedPatriciaTrieRouter();
        router.add("/healthcheck", new HealthCheckHandler());
        router.add("/clients", new ClientsHandler(manager));
        router.add("/clients/:id", new ClientHandler(manager));
        router.add("/topics/:topic", new SubscriptionsHandler(manager));
        router.add("/disconnect/:id", new DisconnectHandler(manager));
        router.add("/admin/pub/:topic/:msg", new PublishHandler(manager));

        return new HttpServer(inetHost, 1885, new HttpServerSetting(), new DefaultHttpDispatcher(router));
    }

    public Future<?> terminationFuture(){
        return shutdownHandler.terminationFuture();
    }

    public void shutdownGracefully(){
        shutdownHandler.shutdownGracefully();
    }
}
