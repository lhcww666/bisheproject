package com.ruoyi.module.agent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Agent 流式任务线程池。
 */
@Configuration
public class AgentAsyncConfig
{
    @Bean(destroyMethod = "shutdown")
    public ExecutorService agentStreamExecutor()
    {
        return Executors.newFixedThreadPool(4);
    }
}
