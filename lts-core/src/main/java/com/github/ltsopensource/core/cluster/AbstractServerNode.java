package com.github.ltsopensource.core.cluster;

import com.github.ltsopensource.core.AppContext;
import com.github.ltsopensource.core.constant.Constants;
import com.github.ltsopensource.core.constant.ExtConfig;
import com.github.ltsopensource.core.factory.NamedThreadFactory;
import com.github.ltsopensource.core.remoting.RemotingServerDelegate;
import com.github.ltsopensource.core.spi.ServiceLoader;
import com.github.ltsopensource.remoting.RemotingProcessor;
import com.github.ltsopensource.remoting.RemotingServer;
import com.github.ltsopensource.remoting.RemotingServerConfig;
import com.github.ltsopensource.remoting.RemotingTransporter;

import java.util.concurrent.Executors;

/**
 * @author Robert HG (254963746@qq.com) on 8/18/14.
 *         抽象服务端
 */
public abstract class AbstractServerNode<T extends Node, App extends AppContext> extends AbstractJobNode<T, App> {

    protected RemotingServerDelegate remotingServer;    //remotingServer的代理

    protected void remotingStart() {

        remotingServer.start(); //netty server启动??

        RemotingProcessor defaultProcessor = getDefaultProcessor(); //创建当前节点下各种processer
        if (defaultProcessor != null) {
            int processorSize = config.getParameter(ExtConfig.PROCESSOR_THREAD, Constants.DEFAULT_PROCESSOR_THREAD);
            remotingServer.registerDefaultProcessor(defaultProcessor,   //创建各种processer公共的线程池
                    Executors.newFixedThreadPool(processorSize, new NamedThreadFactory(AbstractServerNode.class.getSimpleName(), true)));
        }
    }

    public void setListenPort(int listenPort) {
        config.setListenPort(listenPort);
    }

    protected void remotingStop() {
        remotingServer.shutdown();
    }

    @Override
    protected void beforeRemotingStart() {
        RemotingServerConfig remotingServerConfig = new RemotingServerConfig();
        // config 配置
        if (config.getListenPort() == 0) {
            config.setListenPort(Constants.JOB_TRACKER_DEFAULT_LISTEN_PORT);
            node.setPort(config.getListenPort());
        }
        remotingServerConfig.setListenPort(config.getListenPort());

        remotingServer = new RemotingServerDelegate(getRemotingServer(remotingServerConfig), appContext);

        beforeStart();
    }

    private RemotingServer getRemotingServer(RemotingServerConfig remotingServerConfig) {
        return ServiceLoader.load(RemotingTransporter.class, config).getRemotingServer(appContext, remotingServerConfig);
    }

    @Override
    protected void afterRemotingStart() {
        afterStart();
    }

    @Override
    protected void beforeRemotingStop() {
        beforeStop();
    }

    @Override
    protected void afterRemotingStop() {
        afterStop();
    }

    /**
     * 得到默认的处理器
     */
    protected abstract RemotingProcessor getDefaultProcessor();

    protected abstract void beforeStart();

    protected abstract void afterStart();

    protected abstract void afterStop();

    protected abstract void beforeStop();


}
