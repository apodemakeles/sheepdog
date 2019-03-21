package apodemas.sheepdog.server;

import apodemas.sheepdog.common.StringUtils;
import apodemas.sheepdog.core.ServiceState;
import apodemas.sheepdog.core.concurrent.AllFuture;
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
import apodemas.sheepdog.server.sub.SubscriptionManager;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.UUID;

import static apodemas.sheepdog.core.ServiceState.*;

/**
 * @author caozheng
 * @time 2019-01-02 11:26
 **/
public class Server {
    private static final InternalLogger LOG = InternalLoggerFactory.getInstance(Server.class);
    private final ServerSettings settings;
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private final Promise<?> terminationFuture = new DefaultPromise(GlobalEventExecutor.INSTANCE);
    private final MessageQueueConsumer mqConsumer;
    private final SubscriptionManager subscriptionManager;
    private final SessionManager sessionManager;
    private final HttpServer httpServer;
    private ServiceState state = CREATE_JUST;


    public Server(){
        this(ServerSettings.DEFAULT);
    }

    public Server(ServerSettings settings){
        this.settings = settings;
        this.bossGroup = new NioEventLoopGroup(1);
        this.workerGroup = new NioEventLoopGroup();
        this.mqConsumer = new RocketMQConsumer();
        this.subscriptionManager = new SubscriptionManager();
        this.sessionManager = new SessionManager(
                new DefaultEventLoop(), new DefaultRemotingService(), settings, subscriptionManager);
        this.httpServer = buildHttpServer();
    }

    public void start(){
        synchronized (this){
            switch (state){
                case CREATE_JUST:
                    try {
                        this.state = ServiceState.START_FAILED;
                        checkId();
                        LOG.info("trying to start server[{}]'s mqtt server", settings.id());
                        startMqttServer().await();
                        LOG.info("server[{}]'s mqtt server start OK");
                        LOG.info("trying to start server[{}]'s mq consumer", settings.id());
                        startupMQConsumer();
                        LOG.info("server[{}]'s mq consumer start OK");
                        LOG.info("trying to start server[{}]'s http server", settings.id());
                        httpServer.start().await();
                        LOG.info("server[{}]'s http server start OK", settings.id());
                        this.state = ServiceState.RUNNING;
                        LOG.info("server[{}] startup successfully", settings.id());
                    }catch (Throwable e) {
                        LOG.error("server[" + settings.id() + " ] startup failed due to an error", e);
                        clear();
                    }
                    break;
                case RUNNING:
                    break;
                case SHUTDOWN_ALREADY:
                    break;
                case START_FAILED:
                    throw new ServerExecption(String.format("server [{}] has been created before, and failed.", settings.id()));

            }
        }
    }

    public Future<Void> startMqttServer(){
        ServerBootstrap bootstrap = new ServerBootstrap();
        ChannelFuture channelFuture = bootstrap.group(bossGroup, workerGroup)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception{
                        ChannelPipeline pipe = ch.pipeline();
                        pipe.addLast("mqtt encoder", MqttEncoder.INSTANCE);
                        pipe.addLast("mqtt decoder", new MqttDecoder());
                        ServerHandler serverHandler = new ServerHandler(settings, sessionManager, subscriptionManager, new SuccessAuthenticator());
                        pipe.addLast(ServerHandler.NAME, serverHandler);
                    }
                }).bind(settings.mqttHost(), settings.mqttPort());


        return channelFuture;
    }

    private void checkId(){
        if(StringUtils.empty(settings.id())){
            String id = UUID.randomUUID().toString();
            LOG.info("select a id {} for server", id);
            settings.setId(id);
        }
    }

    private void startupMQConsumer() throws MQConsumerStartupException {
        mqConsumer.initWithSettings(this.settings);
        mqConsumer.start(new DefaultMessageConsumer(subscriptionManager));
    }

    private HttpServer buildHttpServer(){
        ParameterizedPatriciaTrieRouter router = new ParameterizedPatriciaTrieRouter();
        router.add("/healthcheck", new HealthCheckHandler());
        router.add("/clients", new ClientsHandler(sessionManager, subscriptionManager));
        router.add("/clients/:id", new ClientHandler(sessionManager, subscriptionManager));
        router.add("/topics/:topic", new SubscriptionsHandler(subscriptionManager));
        router.add("/disconnect/:id", new DisconnectHandler(sessionManager));
        router.add("/admin/pub/:topic/:msg", new PublishHandler(subscriptionManager));

        return new HttpServer(settings.httpHost(), settings.htttPort(), new HttpServerSetting(), new DefaultHttpDispatcher(router));
    }

    public void clear(){
        switch (this.state){
            case RUNNING:
                LOG.info("trying to shutdown server[{}]'s http server", settings.id());
                try {
                    httpServer.shutdownGracefully().await();
                    LOG.info("server[{}]'s http server shutdown ok", settings.id());
                }catch (Throwable e){
                    LOG.warn("server[" + settings.id() + "]'s http server due to an error", e);
                }
                LOG.info("trying to shutdown server[{}]'s mq consumer", settings.id());
                try{
                    mqConsumer.shutdown();
                    LOG.info("server[{}]'s http server shutdown ok", settings.id());
                }catch (Throwable e){
                    LOG.warn("server[" + settings.id() + "]'s mq consumer due to an error", e);
                }
                try{
                    Future<?> allShutdown = new AllFuture(this.workerGroup.shutdownGracefully(), this.bossGroup.shutdownGracefully());
                    allShutdown.await();
                    LOG.info("server[{}]'s mqtt server shutdown ok", settings.id());
                }catch (Throwable e){
                    LOG.warn("server[" + settings.id() + "]'s mqtt server due to an error", e);
                }

            default:
                break;

        }
    }

    public void shutdown(){
        synchronized (this) {
            clear();
            switch (this.state){
                case RUNNING:
                    this.state = SHUTDOWN_ALREADY;
                    LOG.info("server[{}] shutdown successfully", settings.id());
                    terminationFuture.trySuccess(null);
            }
        }
    }

    public Future<?> terminationFuture(){
        return terminationFuture;
    }

}
