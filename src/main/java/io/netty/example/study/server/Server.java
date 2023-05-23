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
import io.netty.example.study.server.handler.AuthHandler;
import io.netty.example.study.server.handler.MetricsHandler;
import io.netty.example.study.server.handler.OrderServerProcessHandler;
import io.netty.example.study.server.handler.ServerIdleCheckHandler;
import io.netty.handler.flush.FlushConsolidationHandler;
import io.netty.handler.ipfilter.IpFilterRuleType;
import io.netty.handler.ipfilter.IpSubnetFilterRule;
import io.netty.handler.ipfilter.RuleBasedIpFilter;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.handler.traffic.GlobalTrafficShapingHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.UnorderedThreadPoolEventExecutor;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;

/**
 * @author pangjiawei
 */
@Slf4j
public class Server {

    public static void main(String[] args) throws InterruptedException, CertificateException, SSLException {
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

        IpSubnetFilterRule ipSubnetFilterRule = new IpSubnetFilterRule("127.1.0.1", 16, IpFilterRuleType.REJECT);
        RuleBasedIpFilter ruleBasedIpFilter = new RuleBasedIpFilter(ipSubnetFilterRule);

        AuthHandler handler = new AuthHandler();

        // ssl
        SelfSignedCertificate selfSignedCertificate = new SelfSignedCertificate();
        log.info("self signed certificate location: {}", selfSignedCertificate.certificate().toString());
        SslContext sslContext = SslContextBuilder.forServer(selfSignedCertificate.certificate(), selfSignedCertificate.privateKey()).build();

        serverBootstrap.childHandler(new ChannelInitializer<NioSocketChannel>() {
            @Override
            protected void initChannel(NioSocketChannel ch) throws Exception {

                ChannelPipeline pipeline = ch.pipeline();

                pipeline.addLast("debugLogger", new LoggingHandler(LogLevel.DEBUG));

                pipeline.addLast("ipFilter", ruleBasedIpFilter);

                pipeline.addLast("trafficShaping", tsHandler);
                pipeline.addLast("idlerCheck", new ServerIdleCheckHandler());

                pipeline.addLast("metricsHandler", metricsHandler);

                pipeline.addLast("ssl", sslContext.newHandler(ch.alloc()));

                pipeline.addLast("frameDecoder", new OrderFrameDecoder());
                pipeline.addLast("frameEncoder", new OrderFrameEncoder());

                pipeline.addLast("protocolEncoder", new OrderProtocolEncoder());
                pipeline.addLast("protocolDecoder", new OrderProtocolDecoder());

                pipeline.addLast("infoLogger", new LoggingHandler(LogLevel.INFO));

                pipeline.addLast("authHandler", handler);

                pipeline.addLast("flushEnhance", new FlushConsolidationHandler(10, true));

                pipeline.addLast(businessEventExecutor, "processHandler", new OrderServerProcessHandler());
            }
        });

        ChannelFuture channelFuture = serverBootstrap.bind(8090).sync();
        channelFuture.channel().closeFuture().sync();
    }
}
