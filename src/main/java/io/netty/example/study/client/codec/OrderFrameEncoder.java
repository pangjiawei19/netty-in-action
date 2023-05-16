package io.netty.example.study.client.codec;

import io.netty.handler.codec.LengthFieldPrepender;

/**
 * @author pangjiawei
 */
public class OrderFrameEncoder extends LengthFieldPrepender {
    public OrderFrameEncoder() {
        super(2);
    }
}
