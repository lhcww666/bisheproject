package com.ruoyi.module.agent.tool;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Supplier;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.module.agent.service.AgentContext;
import com.ruoyi.module.agent.service.ToolAuditService;
import com.ruoyi.module.common.service.ModuleDataScopeService;
import com.ruoyi.module.credit.domain.CreditSummary;
import com.ruoyi.module.credit.service.ICreditService;
import com.ruoyi.module.reservation.domain.Reservation;
import com.ruoyi.module.reservation.domain.ReservationStatus;
import com.ruoyi.module.reservation.domain.ReservationView;
import com.ruoyi.module.reservation.mapper.ReservationMapper;
import com.ruoyi.module.reservation.service.IReservationService;
import com.ruoyi.module.review.service.IReservationReviewBizService;
import com.ruoyi.module.safety.domain.SafetyExamStatusView;
import com.ruoyi.module.safety.service.ISafetyExamService;
import com.ruoyi.system.service.ISysUserService;

import lombok.RequiredArgsConstructor;

/**
 * 教师和管理员助手可调用的审核查询与班级统计工具。
 *
 * <p>所有查询都复用 {@link ModuleDataScopeService}，教师只能查看本人负责范围内的学生。
 * 审核通过、驳回和弃审仍必须由教师在页面确认，不向模型开放写操作。</p>
 *
 * @author itjialu
 */
@Component
@RequiredArgsConstructor
public class TeacherAgentTools
{
    private final IReservationReviewBizService reservationReviewBizService;

    private final IReservationService reservationService;

    private final ReservationMapper reservationMapper;

    private final ISysUserService sysUserService;

    private final ModuleDataScopeService moduleDataScopeService;

    private final ICreditService creditService;

    private final ISafetyExamService safetyExamService;

    private final ToolAuditService toolAuditService;

    @Tool(name = "queryPendingReservations",
            description = "查询当前教师负责范围内的待审核预约，最多返回10条，包含实验室、申请人、考试和信用分状态。")
    public String queryPendingReservations()
    {
        return execute("queryPendingReservations", "currentTeacher", () -> {
            AgentContext.require();
            IPage<ReservationView> data = reservationReviewBizService.pagePending(new Page<>(1, 10));
            if (data.getRecords().isEmpty())
            {
                return "当前负责范围内没有待审核预约。";
            }
            StringBuilder result = new StringBuilder("待审核预约：\n");
            for (ReservationView item : data.getRecords())
            {
                result.append("- ").append(item.getReservationNo())
                        .append("，申请人 ").append(item.getApplicantName())
                        .append("，").append(item.getRoomName())
                        .append("，").append(item.getExperimentName())
                        .append("，").append(formatDate(item.getStartTime()))
                        .append(" 至 ").append(formatDate(item.getEndTime()))
                        .append("，信用分 ").append(item.getCreditScore())
                        .append("，考试 ").append(Boolean.TRUE.equals(item.getExamValid()) ? "有效" : "无有效记录")
                        .append("\n");
            }
            return result.append("共 ").append(data.getTotal()).append(" 条待审核预约。").toString();
        });
    }

    @Tool(name = "queryClassStudentStatistics",
            description = "统计当前账号数据范围内的学生人数、平均信用分、低于预约门槛的学生和没有有效安全考试的学生。")
    public String queryClassStudentStatistics()
    {
        return execute("queryClassStudentStatistics", "currentScope", () -> {
            AgentContext.require();
            List<SysUser> students = moduleDataScopeService.selectScopedStudents();
            if (students.isEmpty())
            {
                return "当前数据范围内没有可统计的学生。";
            }

            long totalScore = 0L;
            int minScore = Integer.MAX_VALUE;
            List<String> belowThreshold = new ArrayList<>();
            List<String> withoutExam = new ArrayList<>();
            for (SysUser student : students)
            {
                CreditSummary credit = creditService.getSummary(student.getUserId());
                int score = credit.getScore() == null ? 0 : credit.getScore();
                int currentMinScore = credit.getMinScore() == null ? 60 : credit.getMinScore();
                totalScore += score;
                minScore = Math.min(minScore, currentMinScore);
                if (score < currentMinScore)
                {
                    belowThreshold.add(displayName(student) + "（" + student.getUserName() + "，" + score + "分）");
                }

                SafetyExamStatusView exam = safetyExamService.getStatus(student.getUserId());
                if (!Boolean.TRUE.equals(exam.getHasValidPass()))
                {
                    withoutExam.add(displayName(student) + "（" + student.getUserName() + "）");
                }
            }

            BigDecimal average = BigDecimal.valueOf(totalScore)
                    .divide(BigDecimal.valueOf(students.size()), 1, RoundingMode.HALF_UP);
            StringBuilder result = new StringBuilder("学生统计：\n")
                    .append("- 学生人数：").append(students.size()).append("\n")
                    .append("- 平均信用分：").append(average).append("，预约门槛 ").append(minScore).append("分\n")
                    .append("- 低于门槛：").append(belowThreshold.size()).append("人\n");
            appendNames(result, belowThreshold, 15);
            result.append("- 无有效安全考试：").append(withoutExam.size()).append("人\n");
            appendNames(result, withoutExam, 15);
            return result.toString().trim();
        });
    }

    @Tool(name = "queryStudentQualification",
            description = "按学生账号查询本人负责范围内学生的信用分和安全考试状态。")
    public String queryStudentQualification(
            @ToolParam(description = "学生登录账号，例如 student01") String userName)
    {
        return execute("queryStudentQualification", "userName=" + userName, () -> {
            AgentContext.require();
            if (StringUtils.isEmpty(userName))
            {
                throw new IllegalArgumentException("学生账号不能为空");
            }
            SysUser user = sysUserService.selectUserByUserName(userName.trim());
            if (user == null)
            {
                return "未找到账号为 " + userName + " 的学生。";
            }
            moduleDataScopeService.checkStudentRecordAccess(user.getUserId());
            CreditSummary credit = creditService.getSummary(user.getUserId());
            SafetyExamStatusView exam = safetyExamService.getStatus(user.getUserId());
            return "学生资质：\n- 姓名：" + displayName(user)
                    + "\n- 账号：" + user.getUserName()
                    + "\n- 信用分：" + credit.getScore() + "，门槛 " + credit.getMinScore()
                    + "，" + (credit.getCanBook() ? "满足" : "不满足")
                    + "\n- 安全考试：" + (exam.getHasValidPass() ? "有效通过" : "无有效通过记录")
                    + "；最近成绩 " + safeValue(exam.getLatestScore())
                    + "；最近结果 " + (exam.getLatestPassed() == null ? "暂无" : (exam.getLatestPassed() ? "通过" : "未通过"))
                    + "；有效期至 " + formatDate(exam.getLatestValidUntil());
        });
    }

    @Tool(name = "queryReservationForReview",
            description = "按预约编号查询当前教师负责范围内的预约详情、当前状态和审核历史。")
    public String queryReservationForReview(
            @ToolParam(description = "预约编号，例如 RES20261021-001") String reservationNo)
    {
        return execute("queryReservationForReview", "reservationNo=" + reservationNo, () -> {
            AgentContext.require();
            if (StringUtils.isEmpty(reservationNo))
            {
                throw new IllegalArgumentException("预约编号不能为空");
            }
            QueryWrapper<Reservation> query = new QueryWrapper<>();
            query.eq("reservation_no", reservationNo.trim()).last("limit 1");
            Reservation reservation = reservationMapper.selectOne(query);
            if (reservation == null)
            {
                return "未找到预约编号 " + reservationNo + "。";
            }
            moduleDataScopeService.checkStudentRecordAccess(reservation.getApplicantId());
            ReservationView detail = reservationService.selectReservationDetail(reservation.getId());
            StringBuilder result = new StringBuilder("预约详情：\n- 编号：").append(detail.getReservationNo())
                    .append("\n- 申请人：").append(detail.getApplicantName())
                    .append("\n- 实验室：").append(detail.getRoomName())
                    .append("\n- 时间：").append(formatDate(detail.getStartTime())).append(" 至 ")
                    .append(formatDate(detail.getEndTime()))
                    .append("\n- 状态：").append(statusLabel(detail.getStatus()))
                    .append("；审核状态：").append(detail.getCheckStatus());
            if (detail.getReviewList() != null && !detail.getReviewList().isEmpty())
            {
                result.append("\n- 审核历史：");
                detail.getReviewList().forEach(review -> result.append("\n  - ")
                        .append(formatDate(review.getReviewTime())).append("：")
                        .append(review.getReviewComment() == null ? "无意见" : review.getReviewComment()));
            }
            return result.toString();
        });
    }

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

    private String displayName(SysUser user)
    {
        return StringUtils.isNotEmpty(user.getNickName()) ? user.getNickName() : user.getUserName();
    }

    private void appendNames(StringBuilder result, List<String> names, int limit)
    {
        if (names.isEmpty())
        {
            return;
        }
        int count = Math.min(names.size(), limit);
        for (int i = 0; i < count; i++)
        {
            result.append("  - ").append(names.get(i)).append("\n");
        }
        if (names.size() > limit)
        {
            result.append("  - 其余 ").append(names.size() - limit).append(" 人未展开\n");
        }
    }

    private String safeValue(Object value)
    {
        return value == null ? "暂无" : String.valueOf(value);
    }

    private String formatDate(Date date)
    {
        return date == null ? "暂无" : new SimpleDateFormat("yyyy-MM-dd HH:mm").format(date);
    }

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
