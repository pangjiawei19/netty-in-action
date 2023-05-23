package io.netty.example.study.server.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.example.study.common.Operation;
import io.netty.example.study.common.RequestMessage;
import io.netty.example.study.common.auth.AuthOperation;
import io.netty.example.study.common.auth.AuthOperationResult;
import lombok.extern.slf4j.Slf4j;

/**
 * @author pangjiawei
 */
@Slf4j
@ChannelHandler.Sharable
public class AuthHandler extends SimpleChannelInboundHandler<RequestMessage> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RequestMessage message) {
        try {
            Operation operation = message.getMessageBody();
            if (operation instanceof AuthOperation) {
                AuthOperationResult authOperationResult = ((AuthOperation) operation).execute();
                if (authOperationResult.isPassAuth()) {
                    log.info("auth success");
                } else {
                    log.error("auth failed");
                    ctx.close();
                }
            } else {
                log.error("first message is not auth");
                ctx.close();
            }
        } catch (Exception e) {
            log.error("auth exception", e);
            ctx.close();
        } finally {
            ctx.pipeline().remove(this);
        }
    }
}
