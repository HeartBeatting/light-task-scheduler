package com.github.ltsopensource.jobtracker.processor;

import com.github.ltsopensource.core.logger.Logger;
import com.github.ltsopensource.core.logger.LoggerFactory;
import com.github.ltsopensource.core.protocol.JobProtos;
import com.github.ltsopensource.core.protocol.command.JobPullRequest;
import com.github.ltsopensource.jobtracker.domain.JobTrackerAppContext;
import com.github.ltsopensource.jobtracker.support.JobPusher;
import com.github.ltsopensource.remoting.Channel;
import com.github.ltsopensource.remoting.exception.RemotingCommandException;
import com.github.ltsopensource.remoting.protocol.RemotingCommand;

/**
 * @author Robert HG (254963746@qq.com) on 7/24/14.
 *         处理 TaskTracker的 Job pull 请求
 */
public class JobPullProcessor extends AbstractRemotingProcessor {

    private JobPusher jobPusher;

    private static final Logger LOGGER = LoggerFactory.getLogger(JobPullProcessor.class);

    public JobPullProcessor(JobTrackerAppContext appContext) {  //用于当前节点下,创建全局的job pull processer,可以重复利用的
        super(appContext);

        jobPusher = new JobPusher(appContext);  //lts里面各个节点对象 都会持有appContext的引用,方便获取各种配置文件,或者访问全局的对象
    }

    //处理job pull 请求,无状态的
    @Override
    public RemotingCommand processRequest(final Channel ctx, final RemotingCommand request) throws RemotingCommandException {

        JobPullRequest requestBody = request.getBody();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("taskTrackerNodeGroup:{}, taskTrackerIdentity:{} , availableThreads:{}", requestBody.getNodeGroup(), requestBody.getIdentity(), requestBody.getAvailableThreads());
        }
        jobPusher.push(requestBody);  //异步并发的处理,分发任务到请求任务的taskTracker节点

        return RemotingCommand.createResponseCommand(JobProtos.ResponseCode.JOB_PULL_SUCCESS.code(), "");   //返回任务pull成功
    }
}
