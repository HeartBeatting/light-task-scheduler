package com.github.ltsopensource.jvmmonitor.mbean;

import com.github.ltsopensource.core.logger.Logger;
import com.github.ltsopensource.core.logger.LoggerFactory;
import com.sun.management.OperatingSystemMXBean;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.math.BigDecimal;
import java.math.MathContext;

/**
 * @author Robert HG (254963746@qq.com) on 9/15/15.
 */
@SuppressWarnings("restriction")
public class JVMThread implements JVMThreadMBean {

    private final static Logger LOGGER = LoggerFactory.getLogger(JVMThread.class);

    private volatile long lastCPUTime;              //volatile 在只有一个线程修改,其他线程都是读取的情况下是线程安全的!
    private volatile long lastCPUUpTime;
	private OperatingSystemMXBean OperatingSystem;
    private RuntimeMXBean Runtime;

    private static final JVMThread instance = new JVMThread();  //这里是一个静态变量,所以所有的线程都是共享同一个变量

    public static JVMThread getInstance() {
        return instance;
    }

    private ThreadMXBean threadMXBean;

	private JVMThread() {
        threadMXBean = ManagementFactory.getThreadMXBean();
        OperatingSystem = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        Runtime = ManagementFactory.getRuntimeMXBean();

        try {
            lastCPUTime = OperatingSystem.getProcessCpuTime();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @Override
    public BigDecimal getProcessCpuTimeRate() {
        /**
         * JVMMonitor.getAttribute(JVMConstants.JMX_JVM_THREAD_NAME, "ProcessCpuTimeRate")会调用getProcessCpuTimeRate()方法.
         * 这个方法也只有一个线程会调用,就是com.github.ltsopensource.tasktracker.support.JobPullMachine#isMachineResEnough()方法,
         * 这里需要每秒获取一次,用于判断当前节点系统资源是否充足的.
         * lts-admin里的监控都是每分钟收集一次数据, 是由com.github.ltsopensource.core.monitor.MStatReportWorker完成的
          */
        long cpuTime = OperatingSystem.getProcessCpuTime();
        long upTime = Runtime.getUptime();

        long elapsedCpu = cpuTime - lastCPUTime;
        long elapsedTime = upTime - lastCPUUpTime;

        lastCPUTime = cpuTime;
        lastCPUUpTime = upTime;

        BigDecimal cpuRate;
        if (elapsedTime <= 0) {
            return new BigDecimal(0);
        }

        float cpuUsage = elapsedCpu / (elapsedTime * 10000F);
        cpuRate = new BigDecimal(cpuUsage, new MathContext(4));

        return cpuRate;
    }

    @Override
    public int getDaemonThreadCount() {
        return threadMXBean.getDaemonThreadCount();
    }

    @Override
    public int getThreadCount() {
        return threadMXBean.getThreadCount();
    }

    @Override
    public long getTotalStartedThreadCount() {
        return threadMXBean.getTotalStartedThreadCount();
    }

    @Override
    public int getDeadLockedThreadCount() {
        try {
            long[] deadLockedThreadIds = threadMXBean.findDeadlockedThreads();
            if (deadLockedThreadIds == null) {
                return 0;
            }
            return deadLockedThreadIds.length;
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

}
