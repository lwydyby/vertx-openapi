package cn.lwydyby.openapi.scanner.util;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author: liwei
 * @description: 线程池工具类
 */
public enum ExecutorUtil {
    ;
    /**
     * 线程池
     */
    private static ThreadPoolExecutor executor = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors()*2,
            Integer.MAX_VALUE,
            5,
            TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            new NamedThreadFactory("class-scanner-thread-"));


    /**
     * 在线程池执行线程
     *
     * @param thread
     */
    public static void executeInPool(Thread thread) {
        executor.execute(thread);
    }
}
