package com.github.ltsopensource.remoting.netty;


import com.github.ltsopensource.remoting.ChannelHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;

import java.net.SocketAddress;

/**
 * @author Robert HG (254963746@qq.com) on 11/3/15.
 */
public class NettyChannel implements com.github.ltsopensource.remoting.Channel {

    private Channel channel;    // 内部聚合了一个netty的channel

    public NettyChannel(ChannelHandlerContext ctx) {
        this.channel = ctx.channel();
    }

    public NettyChannel(Channel channel) {
        this.channel = channel;
    }

    @Override
    public SocketAddress localAddress() {
        return channel.localAddress();
    }

    @Override
    public SocketAddress remoteAddress() {
        return channel.remoteAddress();
    }

    @Override
    public ChannelHandler writeAndFlush(Object msg) {
        ChannelFuture channelFuture = channel.writeAndFlush(msg);
        return new NettyChannelHandler(channelFuture);  // NettyChannelHandler可以添加回调方法的.
    }

    @Override
    public ChannelHandler close() {
        return new NettyChannelHandler(channel.close());    // 这个返回的也是一个NettyChannelHandler,可以添加listener
    }

    public boolean isConnected() {
        return channel.isActive();
    }

    @Override
    public boolean isOpen() {
        return channel.isOpen();
    }

    @Override
    public boolean isClosed() {
        return !isOpen();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NettyChannel that = (NettyChannel) o;

        return !(channel != null ? !channel.equals(that.channel) : that.channel != null);

    }

    @Override
    public int hashCode() {
        return channel != null ? channel.hashCode() : 0;
    }
}
