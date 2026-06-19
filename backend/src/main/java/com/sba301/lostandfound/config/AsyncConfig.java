package com.sba301.lostandfound.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Bật @Async cho toàn project + cấu hình thread pool riêng cho AI enrichment.
 * Tách khỏi ForkJoinPool mặc định vì Qwen-VL chạy blocking I/O, cần pool riêng.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "aiAnalysisExecutor")
    public Executor aiAnalysisExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("ai-analysis-");
        // Quan trọng: caller sẽ không block khi queue đầy, exception vẫn ném về caller
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
