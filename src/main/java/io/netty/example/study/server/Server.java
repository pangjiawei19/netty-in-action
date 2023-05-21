package io.netty.example.study.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioChannelOption;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.example.study.server.codec.OrderFrameDecoder;
import io.netty.example.study.server.codec.OrderFrameEncoder;
import io.netty.example.study.server.codec.OrderProtocolDecoder;
import io.netty.example.study.server.codec.OrderProtocolEncoder;
import io.netty.example.study.server.handler.MetricsHandler;
import io.netty.example.study.server.handler.OrderServerProcessHandler;
import io.netty.handler.flush.FlushConsolidationHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.traffic.GlobalTrafficShapingHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.UnorderedThreadPoolEventExecutor;

/**
 * @author pangjiawei
 */
public class Server {

    public static void main(String[] args) throws InterruptedException {
        ServerBootstrap serverBootstrap = new ServerBootstrap();

        serverBootstrap.channel(NioServerSocketChannel.class);
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(0, new DefaultThreadFactory("bossGroup"));
        NioEventLoopGroup workerGroup = new NioEventLoopGroup(0, new DefaultThreadFactory("workerGroup"));
        serverBootstrap.group(bossGroup, workerGroup);
        serverBootstrap.handler(new LoggingHandler(LogLevel.INFO));

        serverBootstrap.option(NioChannelOption.SO_BACKLOG, 1024);
        serverBootstrap.childOption(NioChannelOption.TCP_NODELAY, true);

        MetricsHandler metricsHandler = new MetricsHandler();
        GlobalTrafficShapingHandler tsHandler = new GlobalTrafficShapingHandler(new NioEventLoopGroup(), 10 * 1024 * 1025, 10 * 1024 * 1024);

        UnorderedThreadPoolEventExecutor businessEventExecutor = new UnorderedThreadPoolEventExecutor(10, new DefaultThreadFactory("business"));

        serverBootstrap.childHandler(new ChannelInitializer<NioSocketChannel>() {
            @Override
            protected void initChannel(NioSocketChannel ch) throws Exception {

                ChannelPipeline pipeline = ch.pipeline();

                pipeline.addLast("debugLogger", new LoggingHandler(LogLevel.DEBUG));

                pipeline.addLast("trafficShaping", tsHandler);

                pipeline.addLast("metricsHandler", metricsHandler);

                pipeline.addLast("frameDecoder", new OrderFrameDecoder());
                pipeline.addLast("frameEncoder", new OrderFrameEncoder());

                pipeline.addLast("protocolEncoder", new OrderProtocolEncoder());
                pipeline.addLast("protocolDecoder", new OrderProtocolDecoder());

                pipeline.addLast("infoLogger", new LoggingHandler(LogLevel.INFO));

                pipeline.addLast("flushEnhance", new FlushConsolidationHandler(10, true));


                pipeline.addLast(businessEventExecutor, "processHandler", new OrderServerProcessHandler());
            }
        });

        ChannelFuture channelFuture = serverBootstrap.bind(8090).sync();
        channelFuture.channel().closeFuture().sync();
    }
}
