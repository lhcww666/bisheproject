package com.ruoyi.module.agent.tool;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.function.Supplier;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.module.agent.service.AgentContext;
import com.ruoyi.module.agent.service.ToolAuditService;
import com.ruoyi.module.agent.knowledge.domain.KnowledgeSearchResult;
import com.ruoyi.module.agent.knowledge.service.IKnowledgeSearchService;
import com.ruoyi.module.agent.draft.domain.ReservationDraftRequest;
import com.ruoyi.module.agent.draft.domain.ReservationDraftView;
import com.ruoyi.module.agent.draft.service.IReservationDraftService;
import com.ruoyi.module.credit.domain.CreditSummary;
import com.ruoyi.module.credit.service.ICreditService;
import com.ruoyi.module.device.domain.Device;
import com.ruoyi.module.reservation.domain.Reservation;
import com.ruoyi.module.reservation.domain.ReservationStatus;
import com.ruoyi.module.reservation.domain.ReservationView;
import com.ruoyi.module.reservation.mapper.ReservationMapper;
import com.ruoyi.module.reservation.service.IReservationService;
import com.ruoyi.module.room.domain.Room;
import com.ruoyi.module.safety.domain.SafetyExamStatusView;
import com.ruoyi.module.safety.service.ISafetyExamService;

import lombok.RequiredArgsConstructor;

/**
 * 实验室 Agent 的只读工具集合。
 *
 * <p>工具是模型和业务 Service 之间唯一的桥：
 * 模型负责决定“要查什么”，工具负责用当前 JWT 用户身份调用现有 Service。
 * 工具不会接收 userId，也不允许模型直接写 SQL。</p>
 *
 * <p>每个工具都通过 {@link #execute(String, String, Supplier)} 包装，
 * 自动记录调用参数、返回结果、成功状态和耗时。</p>
 *
 * @author itjialu
 */
@Component
@RequiredArgsConstructor
public class LabAgentTools
{
    private final IReservationService reservationService;

    private final ReservationMapper reservationMapper;

    private final ISafetyExamService safetyExamService;

    private final ICreditService creditService;

    private final ToolAuditService toolAuditService;

    private final IKnowledgeSearchService knowledgeSearchService;

    private final IReservationDraftService reservationDraftService;

    /**
     * 查询当前用户在某时段可预约的实验室。
     */
    @Tool(name = "queryAvailableRooms",
            description = "查询指定时间段内当前用户可以预约的实验室，返回实验室编号、名称、容量、预约模式和安全等级。")
    public String queryAvailableRooms(
            @ToolParam(description = "开始时间，格式 yyyy-MM-dd HH:mm:ss") String startTime,
            @ToolParam(description = "结束时间，格式 yyyy-MM-dd HH:mm:ss") String endTime,
            @ToolParam(required = false, description = "参与人数，可为空") Integer participantCount)
    {
        return execute("queryAvailableRooms", startTime + "~" + endTime + ", people=" + participantCount, () -> {
            Long userId = AgentContext.require().getUserId();
            List<Room> rooms = reservationService.selectAvailableRooms(userId, parse(startTime, false),
                    parse(endTime, true), participantCount == null ? null : participantCount.longValue());
            if (rooms.isEmpty())
            {
                return "该时段没有符合条件且可预约的实验室。";
            }
            StringBuilder result = new StringBuilder("可用实验室：\n");
            for (Room room : rooms)
            {
                result.append("- ID ").append(room.getId()).append("：").append(room.getRoomCode())
                        .append(" ").append(room.getRoomName())
                        .append("，容量 ").append(room.getCapacity()).append(" 人")
                        .append("，预约模式 ").append(room.getReservationMode())
                        .append("，安全等级 ").append(room.getSafetyLevel()).append("\n");
            }
            return result.toString();
        });
    }

    /**
     * 查询实验室的设备。返回 ID，便于模型继续调用设备相关工具。
     */
    @Tool(name = "queryAvailableDevices",
            description = "查询指定实验室当前可预约的设备，返回设备编号、名称、数量和是否需要培训。")
    public String queryAvailableDevices(
            @ToolParam(description = "实验室ID，可从可用实验室结果或实验室档案查询") Long roomId)
    {
        return execute("queryAvailableDevices", "roomId=" + roomId, () -> {
            List<Device> devices = reservationService.selectAvailableDevices(roomId);
            if (devices.isEmpty())
            {
                return "该实验室当前没有可预约设备。";
            }
            StringBuilder result = new StringBuilder("可用设备：\n");
            for (Device device : devices)
            {
                result.append("- ").append(device.getDeviceCode()).append(" ").append(device.getDeviceName())
                        .append("，数量 ").append(device.getQuantity())
                        .append("，需要培训 ").append("1".equals(device.getRequiresTraining()) ? "是" : "否")
                        .append("\n");
            }
            return result.toString();
        });
    }

    /**
     * 查询当前学生的考试资格，不返回其他学生的成绩。
     */
    @Tool(name = "checkSafetyExamStatus",
            description = "查询当前学生的安全考试状态，包括是否有效通过、最近成绩和有效期。")
    public String checkSafetyExamStatus()
    {
        return execute("checkSafetyExamStatus", "currentUser", () -> {
            SafetyExamStatusView status = safetyExamService.getStatus(AgentContext.require().getUserId());
            return "安全考试：" + (status.getHasValidPass() ? "有效通过" : "没有有效通过记录")
                    + "；最近成绩 " + (status.getLatestScore() == null ? "暂无" : status.getLatestScore())
                    + "；最近结果 " + (status.getLatestPassed() == null ? "暂无" : (status.getLatestPassed() ? "通过" : "未通过"))
                    + "；有效期至 " + formatDate(status.getLatestValidUntil());
        });
    }

    /**
     * 查询当前学生信用分和是否能预约。
     */
    @Tool(name = "queryMyCredit",
            description = "查询当前学生的信用分、预约门槛以及当前是否可以提交预约。")
    public String queryMyCredit()
    {
        return execute("queryMyCredit", "currentUser", () -> {
            CreditSummary summary = creditService.getSummary(AgentContext.require().getUserId());
            return "当前信用分 " + summary.getScore() + " 分，预约门槛 " + summary.getMinScore()
                    + " 分，" + (summary.getCanBook() ? "当前可以提交预约。" : "当前信用分不足，不能提交预约。");
        });
    }

    /**
     * 查询当前用户最近 10 条预约，可按状态过滤。
     */
    @Tool(name = "queryMyReservations",
            description = "查询当前用户最近的预约记录，可按状态筛选：0待审核，1已通过，2已驳回，3已取消，4已完成，5已签到，6已爽约。")
    public String queryMyReservations(
            @ToolParam(required = false, description = "预约状态，可为空") String status)
    {
        return execute("queryMyReservations", "status=" + status, () -> {
            Long userId = AgentContext.require().getUserId();
            Page<Reservation> page = new Page<>(1, 10);
            List<ReservationView> records = reservationService.pageMyReservation(page, status, null, null)
                    .getRecords();
            if (records.isEmpty())
            {
                return "当前用户没有符合条件的预约记录。";
            }
            StringBuilder result = new StringBuilder("最近预约：\n");
            for (ReservationView item : records)
            {
                result.append("- ").append(item.getReservationNo()).append("，")
                        .append(item.getRoomName()).append("，").append(item.getExperimentName()).append("，")
                        .append(formatDate(item.getStartTime())).append(" 至 ").append(formatDate(item.getEndTime()))
                        .append("，状态 ").append(statusLabel(item.getStatus())).append("\n");
            }
            return result.toString();
        });
    }

    /**
     * 按预约编号查询当前用户自己的审核结果和驳回原因。
     */
    @Tool(name = "queryReservationReview",
            description = "按预约编号查询当前用户自己的预约审核结果和驳回原因。")
    public String queryReservationReview(
            @ToolParam(description = "预约编号，例如 RES20261021-001") String reservationNo)
    {
        return execute("queryReservationReview", "reservationNo=" + reservationNo, () -> {
            Long userId = AgentContext.require().getUserId();
            QueryWrapper<Reservation> query = new QueryWrapper<>();
            query.eq("applicant_id", userId).eq("reservation_no", reservationNo).last("limit 1");
            Reservation reservation = reservationMapper.selectOne(query);
            if (reservation == null)
            {
                return "没有找到编号为 " + reservationNo + " 的本人预约。";
            }
            ReservationView detail = reservationService.selectReservationDetail(reservation.getId());
            StringBuilder result = new StringBuilder();
            result.append("预约 ").append(detail.getReservationNo()).append(" 当前状态：")
                    .append(statusLabel(detail.getStatus())).append("；审核状态：")
                    .append(detail.getCheckStatus()).append("；审核人：")
                    .append(detail.getCheckName() == null ? "暂无" : detail.getCheckName()).append("。");
            if (detail.getReviewList() != null && !detail.getReviewList().isEmpty())
            {
                result.append("\n审核历史：");
                detail.getReviewList().forEach(review -> result.append("\n- ")
                        .append(formatDate(review.getReviewTime())).append("：")
                        .append(review.getReviewComment() == null ? "无意见" : review.getReviewComment()));
            }
            return result.toString();
        });
    }

    /**
     * 检索实验室安全知识库，返回带来源的片段，供模型组织带依据的回答。
     */
    @Tool(name = "searchSafetyKnowledge",
            description = "检索实验室安全知识库。回答化学品、消防、个人防护、设备安全等问题前应优先调用此工具。")
    public String searchSafetyKnowledge(
            @ToolParam(description = "要检索的安全问题，例如：酒精灯打翻着火怎么办") String question,
            @ToolParam(required = false, description = "返回条数，默认3") Integer topK)
    {
        return execute("searchSafetyKnowledge", "question=" + question + ", topK=" + topK, () -> {
            List<KnowledgeSearchResult> results = knowledgeSearchService.search(question,
                    topK == null ? 3 : topK);
            if (results.isEmpty())
            {
                return "未检索到可靠的安全知识资料，请提示用户咨询实验室管理员或查阅正式安全制度。";
            }
            StringBuilder result = new StringBuilder("安全知识检索结果：\n");
            for (KnowledgeSearchResult item : results)
            {
                result.append("【来源：").append(item.getDocumentTitle())
                        .append(" / ").append(item.getSource()).append("】\n")
                        .append(item.getContent()).append("\n")
                        .append("相关度：").append(item.getScore()).append("\n\n");
            }
            return result.toString();
        });
    }

    /**
     * 创建预约草稿。
     *
     * <p>工具只落草稿，不直接创建预约；创建成功后会提示用户在前端确认。</p>
     */
    @Tool(name = "createReservationDraft",
            description = "创建实验室预约草稿。只是待确认草稿，不代表预约成功；用户必须在前端确认后才会提交。")
    public String createReservationDraft(
            @ToolParam(description = "实验室ID") Long roomId,
            @ToolParam(description = "实验名称") String experimentName,
            @ToolParam(description = "开始时间，格式 yyyy-MM-dd HH:mm:ss") String startTime,
            @ToolParam(description = "结束时间，格式 yyyy-MM-dd HH:mm:ss") String endTime,
            @ToolParam(description = "参与人数") Integer participantCount,
            @ToolParam(required = false, description = "设备ID列表，可为空") List<Long> deviceIds,
            @ToolParam(required = false, description = "实验说明，可为空") String remark)
    {
        return execute("createReservationDraft", "roomId=" + roomId + ", " + startTime + "~" + endTime, () -> {
            AgentContext.Context context = AgentContext.require();
            ReservationDraftRequest request = new ReservationDraftRequest();
            request.setRoomId(roomId);
            request.setExperimentName(experimentName);
            request.setStartTime(startTime);
            request.setEndTime(endTime);
            request.setParticipantCount(participantCount);
            request.setDeviceIds(deviceIds);
            request.setRemark(remark);
            ReservationDraftView draft = reservationDraftService.createDraft(context.getUserId(),
                    context.getSessionId(), request);
            context.setDraft(draft);
            return "预约草稿已创建，草稿ID " + draft.getId() + "。"
                    + "这不是预约成功；请核对页面上的实验室、时间、人数和设备，然后点击“确认提交预约”。"
                    + "草稿将在 " + formatDate(draft.getExpiresAt()) + " 前有效。";
        });
    }

    /**
     * 确认预约草稿。
     *
     * <p>模型只能在用户明确说“确认提交”后调用；前端按钮是更推荐的确认入口。</p>
     */
    @Tool(name = "confirmReservation",
            description = "确认并提交一个已经展示给用户的预约草稿。只有用户明确确认后才能调用。")
    public String confirmReservation(
            @ToolParam(description = "预约草稿ID") Long draftId)
    {
        return execute("confirmReservation", "draftId=" + draftId, () -> {
            AgentContext.Context context = AgentContext.require();
            ReservationDraftView draft = reservationDraftService.confirmDraft(draftId, context.getUserId());
            context.setDraft(draft);
            return "预约已提交，预约编号 " + draft.getReservationNo()
                    + "，当前状态 " + draft.getReservationStatus() + "。";
        });
    }

    /**
     * 工具执行模板：统一处理成功、失败和审计日志。
     */
    private String execute(String toolName, String arguments, Supplier<String> action)
    {
        long started = System.currentTimeMillis();
        try
        {
            String result = action.get();
            toolAuditService.record(toolName, arguments, result, true, System.currentTimeMillis() - started);
            return result;
        }
        catch (Exception e)
        {
            String result = "工具调用失败：" + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            toolAuditService.record(toolName, arguments, result, false, System.currentTimeMillis() - started);
            return result;
        }
    }

    /**
     * 解析模型传入的时间字符串，兼容带秒、不带秒和纯日期三种格式。
     */
    private Date parse(String text, boolean endOfDay)
    {
        if (text == null || text.trim().isEmpty())
        {
            throw new IllegalArgumentException("时间不能为空");
        }
        String value = text.trim();
        String[] patterns = { "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd" };
        for (String pattern : patterns)
        {
            try
            {
                SimpleDateFormat format = new SimpleDateFormat(pattern);
                format.setLenient(false);
                Date date = format.parse(value);
                if ("yyyy-MM-dd".equals(pattern) && endOfDay)
                {
                    date = new Date(date.getTime() + 86_400_000L - 1000L);
                }
                return date;
            }
            catch (Exception ignored)
            {
                // 尝试下一种格式
            }
        }
        throw new IllegalArgumentException("时间格式应为 yyyy-MM-dd HH:mm:ss：" + text);
    }

    /**
     * 工具返回给模型的简短日期格式，避免响应过长。
     */
    private String formatDate(Date date)
    {
        return date == null ? "暂无" : new SimpleDateFormat("yyyy-MM-dd HH:mm").format(date);
    }

    /**
     * 把数据库状态码翻译成自然语言，降低模型误解状态的概率。
     */
    private String statusLabel(String status)
    {
        if (ReservationStatus.PENDING.equals(status)) return "待审核";
        if (ReservationStatus.APPROVED.equals(status)) return "已通过";
        if (ReservationStatus.REJECTED.equals(status)) return "已驳回";
        if (ReservationStatus.CANCELED.equals(status)) return "已取消";
        if (ReservationStatus.COMPLETED.equals(status)) return "已完成";
        if (ReservationStatus.CHECKED_IN.equals(status)) return "已签到";
        if (ReservationStatus.NO_SHOW.equals(status)) return "已爽约";
        return "未知";
    }
}
