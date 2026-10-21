package com.ruoyi.web.core.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

/**
 * IDEA/JRebel 未复制 resources 时的国际化消息兜底。
 *
 * <p>正常打包运行时读取 classpath；target/classes 缺少 i18n/messages.properties
 * 时继续从项目源码目录读取，避免登录、退出等接口返回 500。</p>
 *
 * @author itjialu
 */
@Configuration
public class MessageSourceFallbackConfig
{
    @Bean
    public MessageSource messageSource()
    {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasenames(
                "classpath:i18n/messages",
                "file:./src/main/resources/i18n/messages",
                "file:./ruoyi-admin/src/main/resources/i18n/messages",
                "file:./api/ruoyi-admin/src/main/resources/i18n/messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);
        messageSource.setUseCodeAsDefaultMessage(false);
        return messageSource;
    }
}
