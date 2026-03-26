package com.example.business.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class ThreadPoolConfig {

    /**
     * 通用业务线程池
     */
    @Bean("bizExecutor")
    public Executor bizExecutor() {
        return new ThreadPoolExecutor(
                8,                          // 核心线程数
                16,                         // 最大线程数
                60, TimeUnit.SECONDS,       // 空闲线程存活时间
                new LinkedBlockingQueue<>(1024),  // 队列容量
                new NamedThreadFactory("biz"),
                new ThreadPoolExecutor.CallerRunsPolicy()  // 拒绝策略：调用方线程执行
        );
    }

    /**
     * 查询专用线程池（IO密集型，线程数可以多一些）
     */
    @Bean("queryExecutor")
    public Executor queryExecutor() {
        return new ThreadPoolExecutor(
                16,
                32,
                60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(2048),
                new NamedThreadFactory("query"),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }



    // 实现 ThreadFactory 接口，线程池每次需要创建新线程时会调用这个工厂
    static class NamedThreadFactory implements ThreadFactory {

        // 原子计数器，线程安全地递增，保证每个线程编号唯一
        // 用 AtomicInteger 而不是普通 int，是因为多个线程可能同时请求创建新线程
        private final AtomicInteger counter = new AtomicInteger(0);

        // 线程名前缀，比如传入 "query"，创建的线程就叫 query-thread-1, query-thread-2...
        private final String prefix;

        // 构造方法，传入前缀名，不同线程池传不同前缀来区分
        NamedThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        // 线程池内部在需要新线程时会调用这个方法
        // 参数 r 就是线程池要执行的任务（Runnable）
        @Override
        public Thread newThread(Runnable r) {
            // 创建线程，拼接名字：前缀 + "-thread-" + 递增编号
            // counter.incrementAndGet() 原子地 +1 并返回新值，所以编号从 1 开始
            Thread t = new Thread(r, prefix + "-thread-" + counter.incrementAndGet());

            // 设为守护线程
            // 守护线程：当所有非守护线程（比如 main 线程）结束后，JVM 会自动退出，不会等守护线程执行完
            // 如果不设置，线程池里的线程默认是用户线程，应用关闭时可能卡住
            t.setDaemon(true);

            // 返回创建好的线程给线程池使用
            return t;
        }
    }
}
