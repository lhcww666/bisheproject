package com.ruoyi.module.agent.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.module.agent.domain.ChatRequest;
import com.ruoyi.module.agent.service.IAgentService;

import lombok.RequiredArgsConstructor;

/**
 * 智能助手 HTTP 入口。
 *
 * <p>Controller 只负责接收请求和返回 AjaxResult，不在这里写模型调用和业务规则。
 * 所有接口都要求 module:agent:use 权限，会话归属校验在 Service 中完成。</p>
 *
 * @author itjialu
 */
@RestController
@RequestMapping("/module/agent")
@RequiredArgsConstructor
public class AgentController extends BaseController
{
    private final IAgentService agentService;

    /**
     * 只用于检查模型是否配置，不返回真实 Key。
     * 占位值 not-configured 表示本地没有设置 DEEPSEEK_API_KEY。
     */
    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    /**
     * 返回前端展示所需的模型配置状态。
     */
    @PreAuthorize("@ss.hasPermi('module:agent:use')")
    @GetMapping("/status")
    public AjaxResult status()
    {
        // 只返回布尔值，绝不把 apiKey 返回给前端。
        boolean configured = apiKey != null && !apiKey.isBlank() && !"not-configured".equals(apiKey.trim());
        return success(Map.of("configured", configured));
    }

    /**
     * 查询当前登录用户的会话列表。
     */
    @PreAuthorize("@ss.hasPermi('module:agent:use')")
    @GetMapping("/sessions")
    public AjaxResult sessions()
    {
        return success(agentService.listSessions());
    }

    /**
     * 新建一个空会话。标题为空时由 Service 使用“新对话”。
     */
    @PreAuthorize("@ss.hasPermi('module:agent:use')")
    @PostMapping("/sessions")
    public AjaxResult createSession(@RequestParam(required = false) String title)
    {
        return success(agentService.createSession(title));
    }

    /**
     * 查询指定会话的历史消息。
     * Service 会校验该会话是否属于当前登录用户。
     */
    @PreAuthorize("@ss.hasPermi('module:agent:use')")
    @GetMapping("/sessions/{id}/messages")
    public AjaxResult messages(@PathVariable Long id)
    {
        return success(agentService.listMessages(id));
    }

    /**
     * 发送一条用户消息，并同步等待 Agent 回复。
     *
     * <p>当前阶段使用同步调用，后续可以替换为 SSE/Flux 流式返回；
     * Controller 的职责和前端协议可以保持不变。</p>
     */
    @PreAuthorize("@ss.hasPermi('module:agent:use')")
    @PostMapping("/chat")
    public AjaxResult chat(@Validated @RequestBody ChatRequest request)
    {
        return success(agentService.chat(request));
    }

    /**
     * 删除会话及其消息、工具调用日志。
     */
    @PreAuthorize("@ss.hasPermi('module:agent:use')")
    @DeleteMapping("/sessions/{id}")
    public AjaxResult deleteSession(@PathVariable Long id)
    {
        agentService.deleteSession(id);
        return success();
    }
}
