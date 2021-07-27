package com.delong.essynchelper.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class ThreadPoolConfig {

    @Value("${threadpool.core-pool-size}")
    private int corePoolSize;

    @Value("${threadpool.max-pool-size}")
    private int maxPoolSize;


    @Bean("taskExecutor")
    public ThreadPoolTaskExecutor defaultThreadPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        //核心线程数目
        executor.setCorePoolSize(corePoolSize);
        //指定最大线程数
        executor.setMaxPoolSize(maxPoolSize);
        //队列中最大的数目
        executor.setQueueCapacity(Integer.MAX_VALUE);
        //线程名称前缀
        executor.setThreadNamePrefix("defaultThreadPool_");
        //rejection-policy：当pool已经达到max size的时候，如何处理新任务
        //CALLER_RUNS：不在新线程中执行任务，而是由调用者所在的线程来执行
        //对拒绝task的处理策略
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        //线程空闲后的最大存活时间
        executor.setKeepAliveSeconds(60);
        // 等待所有任务结束后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        //加载
        executor.initialize();
        return executor;
    }
}
