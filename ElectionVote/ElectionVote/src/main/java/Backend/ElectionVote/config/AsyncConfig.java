package Backend.ElectionVote.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async configuration for recompute worker. Tune pool sizes for production workload.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Executor used by @Async("voteExecutor") throughout the vote submission/tally flow.
     * Tune pool sizes for your environment.
     */
    @Bean(name = "voteExecutor")
    public Executor voteExecutor() {
        ThreadPoolTaskExecutor t = new ThreadPoolTaskExecutor();
        t.setCorePoolSize(8);
        t.setMaxPoolSize(50);
        t.setQueueCapacity(500);
        t.setThreadNamePrefix("vote-recompute-");
        t.initialize();
        return t;
    }
}