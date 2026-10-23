package com.ruoyi.framework.config;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 关闭 Magic API 自带的全局 CORS 过滤器。
 *
 * <p>Magic API 2.2.2 的过滤器会无条件回显请求 Origin，导致项目自己的跨域白名单失效。
 * 本平台统一使用 {@link ResourcesConfig#corsFilter()} 处理跨域，因此关闭该重复过滤器。</p>
 *
 * @author itjialu
 */
@Configuration
public class MagicCorsCompatibilityConfig
{
    private static final String MAGIC_CORS_BEAN = "magicCorsFilterRegistrationBean";

    @Bean
    public static BeanPostProcessor disableMagicCorsFilterPostProcessor()
    {
        return new BeanPostProcessor()
        {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName)
            {
                if (MAGIC_CORS_BEAN.equals(beanName) && bean instanceof FilterRegistrationBean<?> registration)
                {
                    registration.setEnabled(false);
                }
                return bean;
            }
        };
    }
}
