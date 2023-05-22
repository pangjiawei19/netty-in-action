package io.netty.example.study.client.handler;

import io.netty.handler.timeout.IdleStateHandler;

/**
 * @author pangjiawei
 */
public class ClientIdleCheckHandler extends IdleStateHandler {
    public ClientIdleCheckHandler() {
        super(0, 5, 0);
    }
}
