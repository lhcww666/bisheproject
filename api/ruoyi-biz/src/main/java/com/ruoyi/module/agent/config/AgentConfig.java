package com.ruoyi.module.agent.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ruoyi.module.agent.tool.LabAgentTools;

/**
 * Spring AI Agent 配置。
 *
 * <p>这里集中做两件事：
 * 1. 定义 Agent 的系统边界和行为约束；
 * 2. 把 Spring AI 自动装配的 {@link ChatClient.Builder} 与当前项目的
 *    {@link LabAgentTools} 绑定，后续聊天请求统一复用这个 ChatClient。</p>
 *
 * <p>业务规则不写在提示词里，提示词只负责约束模型行为；真正的权限、冲突、
 * 考试和信用校验仍由 Java Service 完成。</p>
 *
 * @author itjialu
 */
@Configuration
public class AgentConfig
{
    /**
     * 系统提示词。
     *
     * <p>提示词只约束“如何表达”和“不能做什么”，不替代后端校验。
     * 模型即使生成错误参数，工具和业务 Service 仍会再次校验。</p>
     */
    private static final String SYSTEM_PROMPT = """
            你是“校园实验室智能助手”，服务于实验室预约与安全管理平台。
            必须遵守以下规则：
            1. 只能根据工具返回的真实数据回答，不得编造实验室、设备、预约、成绩或信用分。
            2. 查询个人信息时不要要求用户提供 userId，工具会自动使用当前登录身份。
            3. 你不能直接修改数据库，也不能承诺预约一定成功；预约写操作必须由用户在前端确认，并由后端重新校验。
            4. 工具没有返回数据时，明确告诉用户“未查询到”，不要猜测。
            5. 回答使用简洁中文，优先分点说明，并给出下一步建议。
            6. 不回答与实验室预约、设备、安全考试、信用分和实验室管理无关的问题。
            7. 安全问题必须优先调用 searchSafetyKnowledge，并在回答中说明资料标题或来源；没有检索到可靠资料时明确提示咨询管理员。
            8. 预约写操作只能先调用 createReservationDraft 生成草稿，并提醒用户在页面点击确认；不能把草稿说成已经预约成功。
            """;

    /**
     * 创建项目统一的 Agent ChatClient。
     *
     * <p>Spring AI 根据 spring-ai-starter-model-openai 自动创建 Builder；
     * 这里通过 defaultTools 注册 @Tool 方法，模型只能调用这些白名单工具，
     * 不能直接访问数据库或执行 SQL。</p>
     */
    @Bean
    public ChatClient labAgentChatClient(ChatClient.Builder builder, LabAgentTools tools)
    {
        return builder.defaultSystem(SYSTEM_PROMPT).defaultTools(tools).build();
    }
}
