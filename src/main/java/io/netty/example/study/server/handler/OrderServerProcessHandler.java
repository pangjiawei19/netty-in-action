package io.netty.example.study.server.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.example.study.common.Operation;
import io.netty.example.study.common.OperationResult;
import io.netty.example.study.common.RequestMessage;
import io.netty.example.study.common.ResponseMessage;
import lombok.extern.slf4j.Slf4j;

/**
 * @author pangjiawei
 */
@Slf4j
public class OrderServerProcessHandler extends SimpleChannelInboundHandler<RequestMessage> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RequestMessage requestMessage) {
        Operation operation = requestMessage.getMessageBody();
        OperationResult operationResult = operation.execute();

        ResponseMessage responseMessage = new ResponseMessage();
        responseMessage.setMessageBody(operationResult);
        responseMessage.setMessageHeader(requestMessage.getMessageHeader());

        if (ctx.channel().isActive() && ctx.channel().isWritable()) {
            ctx.writeAndFlush(responseMessage);
        } else {
            log.error("message dropped");
        }
    }
}
