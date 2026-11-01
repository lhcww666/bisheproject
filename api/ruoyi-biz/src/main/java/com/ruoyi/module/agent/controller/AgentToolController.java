package com.ruoyi.module.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.module.agent.domain.AgentContextClaims;
import com.ruoyi.module.agent.domain.AgentReservationSummary;
import com.ruoyi.module.agent.service.AgentContextService;
import com.ruoyi.module.agent.service.AgentToolPolicyService;
import com.ruoyi.module.agent.service.AgentToolAuditService;
import com.ruoyi.module.reservation.domain.Reservation;
import com.ruoyi.module.reservation.domain.ReservationView;
import com.ruoyi.module.reservation.service.IReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent 内部业务工具接口。仅允许携带签名上下文的服务端调用。
 */
@Anonymous
@RestController
@RequestMapping("/internal/agent/tools")
@RequiredArgsConstructor
public class AgentToolController extends BaseController
{
    private final AgentContextService contextService;

    private final AgentToolPolicyService toolPolicyService;

    private final AgentToolAuditService auditService;

    private final IReservationService reservationService;

    @GetMapping("/reservations/my")
    public AjaxResult myReservations(
            @RequestHeader(value = "X-Agent-Context", required = false) String contextToken,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "20") long pageSize)
    {
        long startedAt = System.nanoTime();
        AgentContextClaims context = null;
        Map<String, Object> arguments = Map.of(
                "status", status == null ? "" : status,
                "pageNum", pageNum,
                "pageSize", pageSize);
        try
        {
            context = contextService.verify(contextToken, requestId);
            toolPolicyService.requireAllowed(context.getRole(), "queryMyReservations");
            Page<Reservation> page = new Page<>(
                    Math.max(pageNum, 1),
                    Math.min(Math.max(pageSize, 1), 50));
            IPage<ReservationView> result = reservationService.pageMyReservationForUser(
                    page,
                    context.getUserId(),
                    status,
                    null,
                    null);
            List<AgentReservationSummary> summaries = result.getRecords().stream()
                    .map(this::toSummary)
                    .toList();
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("list", summaries);
            data.put("total", result.getTotal());
            data.put("pageNum", result.getCurrent());
            data.put("pageSize", result.getSize());
            auditService.success(
                    context,
                    requestId,
                    "queryMyReservations",
                    arguments,
                    Map.of(
                            "returned", summaries.size(),
                            "total", result.getTotal()),
                    elapsedMs(startedAt));
            return success(data);
        }
        catch (ServiceException exception)
        {
            auditService.failure(
                    context,
                    requestId,
                    "queryMyReservations",
                    arguments,
                    "AGENT_TOOL_FAILED",
                    exception.getMessage(),
                    elapsedMs(startedAt));
            throw exception;
        }
    }

    private long elapsedMs(long startedAt)
    {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    private AgentReservationSummary toSummary(ReservationView reservation)
    {
        return new AgentReservationSummary(
                reservation.getId(),
                reservation.getReservationNo(),
                reservation.getRoomName(),
                reservation.getExperimentName(),
                reservation.getStartTime(),
                reservation.getEndTime(),
                reservation.getStatus(),
                reservation.getReviewComment());
    }
}
