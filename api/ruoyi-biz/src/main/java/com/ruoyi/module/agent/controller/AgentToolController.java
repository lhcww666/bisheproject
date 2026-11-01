package com.ruoyi.module.agent.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.module.agent.domain.AgentContextClaims;
import com.ruoyi.module.agent.domain.AgentAvailableDeviceRequest;
import com.ruoyi.module.agent.domain.AgentAvailableRoomRequest;
import com.ruoyi.module.agent.domain.AgentDeviceSummary;
import com.ruoyi.module.agent.domain.AgentReservationSummary;
import com.ruoyi.module.agent.domain.AgentReservationDraftCreateRequest;
import com.ruoyi.module.agent.domain.AgentRoomSummary;
import com.ruoyi.module.agent.service.AgentContextService;
import com.ruoyi.module.agent.service.AgentToolPolicyService;
import com.ruoyi.module.agent.service.AgentToolAuditService;
import com.ruoyi.module.agent.service.IReservationDraftService;
import com.ruoyi.module.credit.domain.CreditSummary;
import com.ruoyi.module.credit.service.ICreditService;
import com.ruoyi.module.device.domain.Device;
import com.ruoyi.module.room.domain.Room;
import com.ruoyi.module.reservation.domain.Reservation;
import com.ruoyi.module.reservation.domain.ReservationView;
import com.ruoyi.module.reservation.service.IReservationService;
import com.ruoyi.module.safety.domain.SafetyExamStatusView;
import com.ruoyi.module.safety.service.ISafetyExamService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

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

    private final ISafetyExamService safetyExamService;

    private final ICreditService creditService;

    private final IReservationDraftService reservationDraftService;

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

    @GetMapping("/safety-exam/status")
    public AjaxResult safetyExamStatus(
            @RequestHeader(value = "X-Agent-Context", required = false) String contextToken,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId)
    {
        long startedAt = System.nanoTime();
        AgentContextClaims context = null;
        try
        {
            context = contextService.verify(contextToken, requestId);
            toolPolicyService.requireAllowed(context.getRole(), "checkSafetyExamStatus");
            SafetyExamStatusView status = safetyExamService.getStatus(context.getUserId());
            auditService.success(
                    context,
                    requestId,
                    "checkSafetyExamStatus",
                    Map.of(),
                    Map.of("hasValidPass", Boolean.TRUE.equals(status.getHasValidPass())),
                    elapsedMs(startedAt));
            return success(status);
        }
        catch (ServiceException exception)
        {
            auditService.failure(
                    context,
                    requestId,
                    "checkSafetyExamStatus",
                    Map.of(),
                    "AGENT_TOOL_FAILED",
                    exception.getMessage(),
                    elapsedMs(startedAt));
            throw exception;
        }
    }

    @GetMapping("/credit/current")
    public AjaxResult currentCredit(
            @RequestHeader(value = "X-Agent-Context", required = false) String contextToken,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId)
    {
        long startedAt = System.nanoTime();
        AgentContextClaims context = null;
        try
        {
            context = contextService.verify(contextToken, requestId);
            toolPolicyService.requireAllowed(context.getRole(), "queryMyCredit");
            CreditSummary summary = creditService.getSummary(context.getUserId());
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("score", summary.getScore());
            data.put("minScore", summary.getMinScore());
            data.put("canBook", summary.getCanBook());
            auditService.success(
                    context,
                    requestId,
                    "queryMyCredit",
                    Map.of(),
                    Map.of(
                            "score", summary.getScore(),
                            "canBook", Boolean.TRUE.equals(summary.getCanBook())),
                    elapsedMs(startedAt));
            return success(data);
        }
        catch (ServiceException exception)
        {
            auditService.failure(
                    context,
                    requestId,
                    "queryMyCredit",
                    Map.of(),
                    "AGENT_TOOL_FAILED",
                    exception.getMessage(),
                    elapsedMs(startedAt));
            throw exception;
        }
    }

    @PostMapping("/rooms/available")
    public AjaxResult availableRooms(
            @RequestHeader(value = "X-Agent-Context", required = false) String contextToken,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @Valid @RequestBody AgentAvailableRoomRequest request)
    {
        long startedAt = System.nanoTime();
        AgentContextClaims context = null;
        Map<String, Object> arguments = Map.of(
                "startTime", request.getStartTime(),
                "endTime", request.getEndTime(),
                "participantCount", request.getParticipantCount() == null
                        ? ""
                        : request.getParticipantCount());
        try
        {
            context = contextService.verify(contextToken, requestId);
            toolPolicyService.requireAllowed(context.getRole(), "queryAvailableRooms");
            List<AgentRoomSummary> rooms = reservationService.selectAvailableRooms(
                    context.getUserId(),
                    parseDateTime(request.getStartTime()),
                    parseDateTime(request.getEndTime()),
                    request.getParticipantCount()).stream()
                    .map(this::toRoomSummary)
                    .toList();
            auditService.success(
                    context,
                    requestId,
                    "queryAvailableRooms",
                    arguments,
                    Map.of("returned", rooms.size()),
                    elapsedMs(startedAt));
            return success(Map.of("list", rooms, "total", rooms.size()));
        }
        catch (ServiceException exception)
        {
            auditService.failure(
                    context,
                    requestId,
                    "queryAvailableRooms",
                    arguments,
                    "AGENT_TOOL_FAILED",
                    exception.getMessage(),
                    elapsedMs(startedAt));
            throw exception;
        }
    }

    @PostMapping("/devices/available")
    public AjaxResult availableDevices(
            @RequestHeader(value = "X-Agent-Context", required = false) String contextToken,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @Valid @RequestBody AgentAvailableDeviceRequest request)
    {
        long startedAt = System.nanoTime();
        AgentContextClaims context = null;
        Map<String, Object> arguments = Map.of("roomId", request.getRoomId());
        try
        {
            context = contextService.verify(contextToken, requestId);
            toolPolicyService.requireAllowed(context.getRole(), "queryAvailableDevices");
            List<AgentDeviceSummary> devices = reservationService
                    .selectAvailableDevices(request.getRoomId())
                    .stream()
                    .map(this::toDeviceSummary)
                    .toList();
            auditService.success(
                    context,
                    requestId,
                    "queryAvailableDevices",
                    arguments,
                    Map.of("returned", devices.size()),
                    elapsedMs(startedAt));
            return success(Map.of("list", devices, "total", devices.size()));
        }
        catch (ServiceException exception)
        {
            auditService.failure(
                    context,
                    requestId,
                    "queryAvailableDevices",
                    arguments,
                    "AGENT_TOOL_FAILED",
                    exception.getMessage(),
                    elapsedMs(startedAt));
            throw exception;
        }
    }

    @PostMapping("/reservation-drafts")
    public AjaxResult createReservationDraft(
            @RequestHeader(value = "X-Agent-Context", required = false) String contextToken,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody AgentReservationDraftCreateRequest request)
    {
        long startedAt = System.nanoTime();
        AgentContextClaims context = null;
        Map<String, Object> arguments = Map.of(
                "sessionId", request.getSessionId(),
                "roomId", request.getRoomId(),
                "startTime", request.getStartTime(),
                "endTime", request.getEndTime(),
                "participantCount", request.getParticipantCount());
        try
        {
            context = contextService.verify(contextToken, requestId);
            toolPolicyService.requireAllowed(context.getRole(), "createReservationDraft");
            if (!context.getSessionId().equals(request.getSessionId()))
            {
                throw new ServiceException("预约草稿会话上下文不匹配");
            }
            var draft = reservationDraftService.create(
                    context.getUserId(),
                    idempotencyKey,
                    request);
            auditService.success(
                    context,
                    requestId,
                    "createReservationDraft",
                    arguments,
                    Map.of("draftId", draft.getId(), "status", draft.getStatus()),
                    elapsedMs(startedAt));
            return success(draft);
        }
        catch (ServiceException exception)
        {
            auditService.failure(
                    context,
                    requestId,
                    "createReservationDraft",
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

    private AgentRoomSummary toRoomSummary(Room room)
    {
        return new AgentRoomSummary(
                room.getId(),
                room.getRoomCode(),
                room.getRoomName(),
                room.getRoomType(),
                room.getLocation(),
                room.getCapacity(),
                room.getSafetyLevel());
    }

    private AgentDeviceSummary toDeviceSummary(Device device)
    {
        return new AgentDeviceSummary(
                device.getId(),
                device.getDeviceCode(),
                device.getDeviceName(),
                device.getDeviceType(),
                device.getQuantity(),
                device.getRequiresTraining());
    }

    private Date parseDateTime(String value)
    {
        String[] patterns = { "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd" };
        for (String pattern : patterns)
        {
            try
            {
                SimpleDateFormat formatter = new SimpleDateFormat(pattern);
                formatter.setLenient(false);
                return formatter.parse(value);
            }
            catch (ParseException ignored)
            {
            }
        }
        throw new ServiceException("时间格式不正确：" + value);
    }
}
