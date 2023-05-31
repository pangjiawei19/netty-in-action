package io.netty.example.study.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioChannelOption;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.example.study.client.codec.OrderFrameDecoder;
import io.netty.example.study.client.codec.OrderFrameEncoder;
import io.netty.example.study.client.codec.OrderProtocolDecoder;
import io.netty.example.study.client.codec.OrderProtocolEncoder;
import io.netty.example.study.client.handler.ClientIdleCheckHandler;
import io.netty.example.study.client.handler.KeepaliveHandler;
import io.netty.example.study.common.RequestMessage;
import io.netty.example.study.common.auth.AuthOperation;
import io.netty.example.study.common.order.OrderOperation;
import io.netty.example.study.util.IdUtil;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import javax.net.ssl.SSLException;

/**
 * @author pangjiawei
 */
public class ClientV0 {

    public static void main(String[] args) throws InterruptedException, SSLException {

        Bootstrap bootstrap = new Bootstrap();

        bootstrap.channel(NioSocketChannel.class);
        bootstrap.option(NioChannelOption.CONNECT_TIMEOUT_MILLIS, 10 * 1000);

        NioEventLoopGroup group = new NioEventLoopGroup();

        try {
            bootstrap.group(group);

            LoggingHandler loggingHandler = new LoggingHandler(LogLevel.INFO);
            KeepaliveHandler keepaliveHandler = new KeepaliveHandler();

            // ssl
            SslContextBuilder sslContextBuilder = SslContextBuilder.forClient();
            // 直接信任自签证书，也可以自己安装证书，安装方法参考 resources/ssl.txt
            sslContextBuilder.trustManager(InsecureTrustManagerFactory.INSTANCE);
            SslContext sslContext = sslContextBuilder.build();

            bootstrap.handler(new ChannelInitializer<NioSocketChannel>() {
                @Override
                protected void initChannel(NioSocketChannel ch) throws Exception {
                    ChannelPipeline pipeline = ch.pipeline();

                    pipeline.addLast(new ClientIdleCheckHandler());

                    pipeline.addLast(sslContext.newHandler(ch.alloc()));

                    pipeline.addLast(new OrderFrameDecoder());
                    pipeline.addLast(new OrderFrameEncoder());

                    pipeline.addLast(new OrderProtocolEncoder());
                    pipeline.addLast(new OrderProtocolDecoder());

                    pipeline.addLast(loggingHandler);

                    pipeline.addLast(keepaliveHandler);
                }
            });

            ChannelFuture channelFuture = bootstrap.connect("127.0.0.1", 8090);

            channelFuture.sync();

            AuthOperation authOperation = new AuthOperation("admin", "password");
            RequestMessage authRequest = new RequestMessage(IdUtil.nextId(), authOperation);
            channelFuture.channel().writeAndFlush(authRequest);

            RequestMessage requestMessage = new RequestMessage(IdUtil.nextId(), new OrderOperation(1001, "Tomato"));
            channelFuture.channel().writeAndFlush(requestMessage);

            channelFuture.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }
}
