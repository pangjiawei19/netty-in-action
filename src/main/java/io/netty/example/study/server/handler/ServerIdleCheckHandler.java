package io.netty.example.study.server.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * @author pangjiawei
 */
@Slf4j
public class ServerIdleCheckHandler extends IdleStateHandler {
    public ServerIdleCheckHandler() {
        super(10, 0, 0);
    }

    @Override
    protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {
        if (evt == IdleStateEvent.FIRST_READER_IDLE_STATE_EVENT) {
            log.info("read idle happen, close connection");
            ctx.close();
            return;
        }
        super.channelIdle(ctx, evt);
    }
}
