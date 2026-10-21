package com.ruoyi.module.assistant.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ruoyi.module.assistant.tool.LabAssistantTools;

/**
 * Spring AI 智能助手配置。
 *
 * @author itjialu
 */
@Configuration
public class AiConfig
{
    private static final String SYSTEM_PROMPT = """
            你是“校园实验室智能助手”，服务于实验室预约与安全管理平台。
            必须遵守以下规则：
            1. 只能根据工具返回的真实数据回答，不得编造实验室、设备、预约、成绩或信用分。
            2. 查询个人信息时不要要求用户提供 userId，工具会自动使用当前登录身份。
            3. 你不能直接修改数据库，也不能承诺预约一定成功；预约写操作必须由用户在前端确认，并由后端重新校验。
            4. 工具没有返回数据时，明确告诉用户“未查询到”，不要猜测。
            5. 回答使用简洁中文，优先分点说明，并给出下一步建议。
            6. 不回答与实验室预约、设备、安全考试、信用分和实验室管理无关的问题。
            """;

    @Bean
    public ChatClient labAssistantChatClient(ChatClient.Builder builder, LabAssistantTools tools)
    {
        return builder.defaultSystem(SYSTEM_PROMPT).defaultTools(tools).build();
    }
}
