package com.ruoyi.module.assistant.controller;

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
import com.ruoyi.module.assistant.domain.ChatRequest;
import com.ruoyi.module.assistant.service.IAssistantService;

import lombok.RequiredArgsConstructor;

/**
 * 智能助手Controller。
 *
 * @author itjialu
 */
@RestController
@RequestMapping("/module/assistant")
@RequiredArgsConstructor
public class AssistantController extends BaseController
{
    private final IAssistantService assistantService;

    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    @PreAuthorize("@ss.hasPermi('module:assistant:use')")
    @GetMapping("/status")
    public AjaxResult status()
    {
        boolean configured = apiKey != null && !apiKey.isBlank() && !"not-configured".equals(apiKey.trim());
        return success(Map.of("configured", configured));
    }

    @PreAuthorize("@ss.hasPermi('module:assistant:use')")
    @GetMapping("/sessions")
    public AjaxResult sessions()
    {
        return success(assistantService.listSessions());
    }

    @PreAuthorize("@ss.hasPermi('module:assistant:use')")
    @PostMapping("/sessions")
    public AjaxResult createSession(@RequestParam(required = false) String title)
    {
        return success(assistantService.createSession(title));
    }

    @PreAuthorize("@ss.hasPermi('module:assistant:use')")
    @GetMapping("/sessions/{id}/messages")
    public AjaxResult messages(@PathVariable Long id)
    {
        return success(assistantService.listMessages(id));
    }

    @PreAuthorize("@ss.hasPermi('module:assistant:use')")
    @PostMapping("/chat")
    public AjaxResult chat(@Validated @RequestBody ChatRequest request)
    {
        return success(assistantService.chat(request));
    }

    @PreAuthorize("@ss.hasPermi('module:assistant:use')")
    @DeleteMapping("/sessions/{id}")
    public AjaxResult deleteSession(@PathVariable Long id)
    {
        assistantService.deleteSession(id);
        return success();
    }
}
