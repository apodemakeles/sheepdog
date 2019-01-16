package apodemas.sheepdog.client;

import apodemas.common.Checks;
import apodemas.common.StringUtils;
import apodemas.sheepdog.core.concurrent.EventLoopPromise;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.*;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

public class Client {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(Client.class);
    private static AtomicReferenceFieldUpdater STATE_UPDATER
            = AtomicReferenceFieldUpdater.newUpdater(Client.class, State.class, "state");
    private static int KEEP_ALIVE_SEC = 30;

    private final EventLoop eventLoop;
    private final EventLoopPromise shutdownHandler;

    private volatile State state = State.UNCONNECTED;
    private ClientInfo info = new ClientInfo();
    private ClientSettings settings =  ClientSettings.DEFAULT;
    private MessageListener listener = new MessageListenerAdapter();

    public Client(){
        this.eventLoop = new NioEventLoopGroup(1).next();
        this.shutdownHandler = new EventLoopPromise(this.eventLoop);
    }

    public void setClientId(String clientId){
        if(state != State.UNCONNECTED){
            throw new IllegalStateException();
        }
        info.setClientId(Checks.notEmpty(clientId, "clientId"));
    }

    public void setKeepAliveSec(int keepAliveSec){
        if(state != State.UNCONNECTED){
            throw new IllegalStateException();
        }
        if(keepAliveSec <= 0){
            throw new IllegalArgumentException("keep alive value must greater than 0");
        }
        info.setKeepAliveSec(keepAliveSec);
    }

    public void setAuth(String username){
        if(state != State.UNCONNECTED){
            throw new IllegalStateException();
        }
        info.setUsername(Checks.notEmpty(username, "username"));
    }

    public void setAuth(String username, byte[] password){
        if(state != State.UNCONNECTED){
            throw new IllegalStateException();
        }
        info.setUsername(Checks.notEmpty(username, "username"));
        info.setPassword(Checks.notNull(password, "password"));
    }

    public void setSettings(ClientSettings settings){
        if(state != State.UNCONNECTED){
            throw new IllegalStateException();
        }
        this.settings = Checks.notNull(settings, "settings");
    }

    public void setMessageListener(MessageListener listener){
        if(state != State.UNCONNECTED){
            throw new IllegalStateException();
        }
        this.listener = Checks.notNull(listener, "listener");
    }

    private void check(){
        if(StringUtils.empty(info.clientId())){
            throw new IllegalArgumentException("client id is required");
        }
        if(StringUtils.notEmpty(info.username()) && info.password() == null){
            info.setPassword(new byte[0]);
        }
        if(info.keepAliveSec() <= 0){
            info.setKeepAliveSec(KEEP_ALIVE_SEC);
        }
    }

    public Future<Session> connect(String host, int port){
        if(state != State.UNCONNECTED){
            throw new IllegalStateException();
        }

        logger.info("client startup");
        try {
            check();
            state = State.CONNECTING;
            Bootstrap bootstrap = new Bootstrap();

            Promise<Session> connectFuture = eventLoop.newPromise();
            Promise<Session> connectResult = eventLoop.newPromise();
            connectFuture.addListener((Future<Session> sessionFut)->{
                if(sessionFut.isSuccess()){
                    STATE_UPDATER.compareAndSet(this, State.CONNECTING, State.CONNECTED);
                    connectResult.setSuccess(sessionFut.getNow());
                }else{
                    connectResult.setFailure(sessionFut.cause());
                    shutdownForException(sessionFut.cause());
                }
            });

            ClientHandler handler = new ClientHandler(shutdownHandler, info, settings, listener, connectFuture);
            ChannelFuture connectFut = bootstrap.group(eventLoop)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, settings.connectTimeoutSec() * 1000)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipe = ch.pipeline();
                            //pipe.addLast(new LoggingHandler(LogLevel.INFO));
                            pipe.addLast(MqttEncoder.INSTANCE);
                            pipe.addLast(new MqttDecoder());
                            pipe.addLast(new IdleStateHandler(0, info.keepAliveSec(), 0, TimeUnit.SECONDS));
                            pipe.addLast(handler);
                        }
                    }).connect(host, port);
            logger.info("client try to connect remote server...");
            connectFut.addListener(fut->{
                if(!fut.isSuccess()){
                    Throwable cause = fut.cause();
                    shutdownForException(fut.cause());
                    connectFuture.tryFailure(cause);
                }
            });

            return connectResult;
        }catch(Throwable e){
            state = State.CLOSED;
            throw  e;
        }
    }

    private void shutdownForException(Throwable cause){
        state = State.CLOSED;
        shutdownHandler.setFailure(cause);
    }

    public void shutdownGracefully(){
        state = State.CLOSED;
        shutdownHandler.shutdownGracefully();
    }

    public Future<?> terminationFuture(){
        return shutdownHandler.terminationFuture();
    }

    public State state(){
        return state;
    }

    public enum State {
        UNCONNECTED,
        CONNECTING,
        CONNECTED,
        CLOSED
    }
}
