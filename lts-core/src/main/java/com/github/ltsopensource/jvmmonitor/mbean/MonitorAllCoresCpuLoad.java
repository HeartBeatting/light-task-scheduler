package com.github.ltsopensource.jvmmonitor.mbean;

import com.sun.management.OperatingSystemMXBean;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

// for test
public class MonitorAllCoresCpuLoad {

    private static volatile boolean hasAnyWorkerFinished = false;

    public static void main(String[] args) throws Exception {
        OperatingSystemMXBean operatingSystemMXBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        int workersCount = Runtime.getRuntime().availableProcessors();

        CyclicBarrier barrier = new CyclicBarrier(workersCount + 1); // + 1 to include main thread measuring CPU load
        for (int i = 0; i < workersCount; i++) {
            createAndStartWorker(barrier); //use barrier to start all workers at the same time as main thread
        }
        barrier.await();
        System.out.println("All workers and main thread started");
        while (!hasAnyWorkerFinished) { // stop measuring if at least one of workers finished
            getAndPrintCpuLoad(operatingSystemMXBean);
            TimeUnit.MILLISECONDS.sleep(100);
        }
        System.out.println("One of workers finished");
    }

    private static void createAndStartWorker(final CyclicBarrier cyclicBarrier) {
        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            cyclicBarrier.await();
                            for (long i = 0L; i < 99999999999L; i++) { // 6s
                                // Thread 100% time as RUNNABLE, taking 1/(n cores) of JVM/System overall CPU
                            }
                            hasAnyWorkerFinished = true;
                            System.out.println(Thread.currentThread().getName() + " finished");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
        ).start();
    }

    private static void getAndPrintCpuLoad(OperatingSystemMXBean mxBean) {
        // need to use reflection as the impl class is not visible
        for (Method method : mxBean.getClass().getDeclaredMethods()) {
            method.setAccessible(true);
            String methodName = method.getName();
            if (methodName.startsWith("get") && methodName.contains("Cpu") && methodName.contains("Load")
                    && Modifier.isPublic(method.getModifiers())) {

                Object value;
                try {
                    value = method.invoke(mxBean);
                } catch (Exception e) {
                    value = e;
                }
                System.out.println(methodName + " = " + value);
            }
        }
//        System.out.println(mxBean.getSystemCpuLoad());
        System.out.println("");
    }

}
