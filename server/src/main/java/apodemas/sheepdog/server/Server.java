package apodemas.sheepdog.server;

import apodemas.sheepdog.common.StringUtils;
import apodemas.sheepdog.core.concurrent.EventLoopPromise;
import apodemas.sheepdog.http.server.DefaultHttpDispatcher;
import apodemas.sheepdog.http.server.HttpServer;
import apodemas.sheepdog.http.server.HttpServerSetting;
import apodemas.sheepdog.http.server.ParameterizedPatriciaTrieRouter;
import apodemas.sheepdog.server.api.*;
import apodemas.sheepdog.server.auth.SuccessAuthenticator;
import apodemas.sheepdog.server.distributed.DefaultRemotingService;
import apodemas.sheepdog.server.mq.DefaultMessageConsumer;
import apodemas.sheepdog.server.mq.MQConsumerStartupException;
import apodemas.sheepdog.server.mq.MessageQueueConsumer;
import apodemas.sheepdog.server.mq.rocket.RocketMQConsumer;
import apodemas.sheepdog.server.sub.SubscriptionController;
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

import java.util.UUID;

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
        LOG.info("server {} startup", settings.id());

        SubscriptionController subscriptionController = new SubscriptionController();
        SessionController sessionController = new SessionController("server_id",
                new DefaultEventLoop(), new DefaultRemotingService(), settings, subscriptionController);

        ChannelFuture channelFuture = bootstrap.group(bossGroup, workerGroup)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception{
                        ChannelPipeline pipe = ch.pipeline();
                        pipe.addLast("mqtt encoder", MqttEncoder.INSTANCE);
                        pipe.addLast("mqtt decoder", new MqttDecoder());
                        ServerHandler serverHandler = new ServerHandler(settings, sessionController, subscriptionController, new SuccessAuthenticator());
                        pipe.addLast(ServerHandler.NAME, serverHandler);
                    }
                }).bind(inetHost, inetPort);

        startupMQConsumer(subscriptionController);

        HttpServer server = buildHttpServer(inetHost, sessionController, subscriptionController);
        server.start();
        LOG.info("http server started");
        LOG.info("server {} start successfully", settings.id());

        return channelFuture;
    }

    private void checkId(){
        if(StringUtils.empty(settings.id())){
            String id = UUID.randomUUID().toString();
            LOG.info("select a id {}", id);
            settings.setId(id);
        }
    }

    private void startupMQConsumer(SubscriptionController subscriptionController) throws MQConsumerStartupException {
        mqConsumer.initWithSettings(this.settings);
        mqConsumer.start(new DefaultMessageConsumer(subscriptionController));
        LOG.info("mq consumer started");
    }

    private HttpServer buildHttpServer(String inetHost, SessionController sessionController, SubscriptionController subController){
        ParameterizedPatriciaTrieRouter router = new ParameterizedPatriciaTrieRouter();
        router.add("/healthcheck", new HealthCheckHandler());
        router.add("/clients", new ClientsHandler(sessionController, subController));
        router.add("/clients/:id", new ClientHandler(sessionController, subController));
        router.add("/topics/:topic", new SubscriptionsHandler(subController));
        router.add("/disconnect/:id", new DisconnectHandler(sessionController));
        router.add("/admin/pub/:topic/:msg", new PublishHandler(subController));

        return new HttpServer(inetHost, 1885, new HttpServerSetting(), new DefaultHttpDispatcher(router));
    }

    public Future<?> terminationFuture(){
        return shutdownHandler.terminationFuture();
    }

    public void shutdownGracefully(){
        shutdownHandler.shutdownGracefully();
    }
}
