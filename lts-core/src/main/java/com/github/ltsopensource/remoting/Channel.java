package com.github.ltsopensource.remoting;


import java.net.SocketAddress;

/**
 * @author Robert HG (254963746@qq.com) on 11/3/15.
 */
public interface Channel {  // 这个接口是抽象出了LTS通信的channel的接口
    // 不是直接依赖netty等中间件的接口, 这样可以抽象出一般的通信操作,对修改关闭,对扩展开放
    SocketAddress localAddress();

    SocketAddress remoteAddress();

    ChannelHandler writeAndFlush(Object msg);

    ChannelHandler close();

    boolean isConnected();

    boolean isOpen();

    boolean isClosed();
}
