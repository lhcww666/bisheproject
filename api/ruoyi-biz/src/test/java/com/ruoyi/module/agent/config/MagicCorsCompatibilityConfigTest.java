package com.ruoyi.module.agent.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.web.filter.OncePerRequestFilter;

import com.ruoyi.framework.config.MagicCorsCompatibilityConfig;

import jakarta.servlet.Filter;

/**
 * Magic API 重复 CORS 过滤器关闭逻辑测试。
 *
 * @author itjialu
 */
class MagicCorsCompatibilityConfigTest
{
    @Test
    void disablesOnlyMagicCorsFilterRegistration()
    {
        BeanPostProcessor processor = MagicCorsCompatibilityConfig.disableMagicCorsFilterPostProcessor();
        Filter filter = new OncePerRequestFilter()
        {
            @Override
            protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest request,
                    jakarta.servlet.http.HttpServletResponse response, jakarta.servlet.FilterChain chain)
            {
                // 测试不需要真正执行过滤器。
            }
        };
        FilterRegistrationBean<Filter> magicCors = new FilterRegistrationBean<>(filter);
        FilterRegistrationBean<Filter> businessFilter = new FilterRegistrationBean<>(filter);

        processor.postProcessAfterInitialization(magicCors, "magicCorsFilterRegistrationBean");
        processor.postProcessAfterInitialization(businessFilter, "businessFilterRegistrationBean");

        assertFalse(magicCors.isEnabled());
        assertTrue(businessFilter.isEnabled());
    }
}
