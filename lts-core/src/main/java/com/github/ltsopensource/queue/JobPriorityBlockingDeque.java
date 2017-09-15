package com.github.ltsopensource.queue;

import com.github.ltsopensource.queue.domain.JobPo;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 优先级 有界 去重 双向队列
 * @author Robert HG (254963746@qq.com) on 8/5/16.
 *
 * 实现线程安全的数据结构,最好的办法是基于现有的数据结构
 */
public class JobPriorityBlockingDeque {

    private final int capacity;             //实现有界的

    private final LinkedList<JobPo> list;   //实现有序的
    private final ReentrantLock lock = new ReentrantLock();         //实现线程安全的

    // Key: jobId     value:gmtModified
    private Map<String, Long> jobs = new ConcurrentHashMap<String, Long>();     //保存jobs

    private Comparator<JobPo> comparator;                           //比较优先级

    public JobPriorityBlockingDeque(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException();
        this.capacity = capacity;
        this.list = new LinkedList<JobPo>();
        this.comparator = new Comparator<JobPo>() {
            @Override
            public int compare(JobPo left, JobPo right) {
                if (left.getJobId().equals(right.getJobId())) {     //jobId相同,代表是相同的任务,优先级相同
                    return 0;
                }
                int compare = left.getPriority().compareTo(right.getPriority());    //比较优先级
                if (compare != 0) {
                    return compare;
                }
                compare = left.getTriggerTime().compareTo(right.getTriggerTime());  //比较触发时间
                if (compare != 0) {
                    return compare;
                }
                compare = left.getGmtCreated().compareTo(right.getGmtCreated());    //比较创建时间
                if (compare != 0) {
                    return compare;
                }
                return -1;
            }
        };
    }

    public JobPo pollFirst() {
        lock.lock();
        try {
            JobPo f = list.pollFirst();
            if (f == null)
                return null;
            jobs.remove(f.getJobId());
            return f;
        } finally {
            lock.unlock();
        }
    }

    public JobPo pollLast() {
        lock.lock();
        try {
            JobPo l = list.pollLast();
            if (l == null)
                return null;
            jobs.remove(l.getJobId());
            return l;
        } finally {
            lock.unlock();
        }
    }

    public boolean offer(JobPo e) {
        if (e == null) throw new NullPointerException();
        if (list.size() >= capacity)
            return false;

        lock.lock();
        try {
            if (jobs.containsKey(e.getJobId())) {
                // 如果已经在内存中了，check下是否和内存中的一致
                Long gmtModified = jobs.get(e.getJobId());
                if (gmtModified != null && !gmtModified.equals(e.getGmtModified())) {
                    // 删除原来的
                    removeOld(e);
                }
            }

            int insertionPoint = Collections.binarySearch(list, e, comparator);     //二分查找法
            if (insertionPoint < 0) {
                // this means the key didn't exist, so the insertion point is negative minus 1.
                insertionPoint = -insertionPoint - 1;
            }

            list.add(insertionPoint, e);    //为什么要用LinkedList 就是因为要在中间插入节点,LinkedList适合这种操作
            jobs.put(e.getJobId(), e.getGmtModified());
            return true;
        } finally {
            lock.unlock();
        }
    }

    private void removeOld(JobPo e) {
        Iterator<JobPo> i = iterator();
        int index = 0;
        while (i.hasNext()) {
            JobPo o = i.next();
            if (o.getJobId().equals(e.getJobId())) {
                list.remove(index);
                jobs.remove(e.getJobId());
                return;
            }
            index++;
        }
    }

    public JobPo poll() {   //预加载数据的线程很少,这种比较粗的同步已经够了
        return pollFirst();
    }

    public int size() {
        lock.lock();
        try {
            return list.size();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String toString() {
        lock.lock();
        try {
            Iterator<JobPo> i = iterator();
            if (!i.hasNext())
                return "[]";

            StringBuilder sb = new StringBuilder();
            sb.append('[');
            for (; ; ) {
                JobPo e = i.next();
                sb.append(e);
                if (!i.hasNext())
                    return sb.append(']').toString();
                sb.append(", ");
            }
        } finally {
            lock.unlock();
        }
    }

    public Iterator<JobPo> iterator() {
        return list.iterator();
    }

}
