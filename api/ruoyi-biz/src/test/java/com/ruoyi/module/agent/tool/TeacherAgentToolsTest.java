package com.ruoyi.module.agent.tool;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.module.agent.service.AgentContext;
import com.ruoyi.module.agent.service.ToolAuditService;
import com.ruoyi.module.common.service.ModuleDataScopeService;
import com.ruoyi.module.credit.domain.CreditSummary;
import com.ruoyi.module.credit.service.ICreditService;
import com.ruoyi.module.reservation.domain.ReservationView;
import com.ruoyi.module.reservation.mapper.ReservationMapper;
import com.ruoyi.module.reservation.service.IReservationService;
import com.ruoyi.module.review.service.IReservationReviewBizService;
import com.ruoyi.module.safety.domain.SafetyExamStatusView;
import com.ruoyi.module.safety.service.ISafetyExamService;
import com.ruoyi.system.service.ISysUserService;

/**
 * 教师助手工具测试。
 *
 * @author itjialu
 */
class TeacherAgentToolsTest
{
    @Test
    void pendingReservationsUseScopedReviewService()
    {
        IReservationReviewBizService reviewService = mock(IReservationReviewBizService.class);
        ToolAuditService auditService = mock(ToolAuditService.class);
        ReservationView item = new ReservationView();
        item.setReservationNo("RES20261021-001");
        item.setApplicantName("学生甲");
        item.setRoomName("化学实验室");
        item.setExperimentName("酸碱滴定");
        item.setStartTime(new Date(System.currentTimeMillis() + 3_600_000L));
        item.setEndTime(new Date(System.currentTimeMillis() + 7_200_000L));
        item.setCreditScore(90L);
        item.setExamValid(true);
        Page<ReservationView> page = new Page<>(1, 10, 1);
        page.setRecords(List.of(item));
        when(reviewService.pagePending(any())).thenReturn(page);

        TeacherAgentTools tools = new TeacherAgentTools(reviewService, mock(IReservationService.class),
                mock(ReservationMapper.class), mock(ISysUserService.class), mock(ModuleDataScopeService.class),
                mock(ICreditService.class), mock(ISafetyExamService.class), auditService);
        AgentContext.open(100L, 2L);
        try
        {
            String result = tools.queryPendingReservations();
            assertTrue(result.contains("RES20261021-001"));
            assertTrue(result.contains("学生甲"));
            assertTrue(result.contains("共 1 条待审核预约"));
        }
        finally
        {
            AgentContext.close();
        }
    }

    @Test
    void classStatisticsSummarizeScopedStudents()
    {
        ModuleDataScopeService dataScopeService = mock(ModuleDataScopeService.class);
        ICreditService creditService = mock(ICreditService.class);
        ISafetyExamService safetyExamService = mock(ISafetyExamService.class);
        when(dataScopeService.selectScopedStudents()).thenReturn(List.of(
                student(11L, "student01", "学生甲"),
                student(12L, "student02", "学生乙"),
                student(13L, "student03", "学生丙")));
        when(creditService.getSummary(11L)).thenReturn(credit(90));
        when(creditService.getSummary(12L)).thenReturn(credit(55));
        when(creditService.getSummary(13L)).thenReturn(credit(80));
        when(safetyExamService.getStatus(11L)).thenReturn(exam(true));
        when(safetyExamService.getStatus(12L)).thenReturn(exam(true));
        when(safetyExamService.getStatus(13L)).thenReturn(exam(false));

        TeacherAgentTools tools = new TeacherAgentTools(mock(IReservationReviewBizService.class),
                mock(IReservationService.class), mock(ReservationMapper.class), mock(ISysUserService.class),
                dataScopeService, creditService, safetyExamService, mock(ToolAuditService.class));
        AgentContext.open(100L, 2L);
        try
        {
            String result = tools.queryClassStudentStatistics();
            assertTrue(result.contains("学生人数：3"));
            assertTrue(result.contains("平均信用分：75.0"));
            assertTrue(result.contains("低于门槛：1人"));
            assertTrue(result.contains("学生乙（student02，55分）"));
            assertTrue(result.contains("无有效安全考试：1人"));
            assertTrue(result.contains("学生丙（student03）"));
        }
        finally
        {
            AgentContext.close();
        }
    }

    private SysUser student(Long userId, String userName, String nickName)
    {
        SysUser user = new SysUser(userId);
        user.setUserName(userName);
        user.setNickName(nickName);
        return user;
    }

    private CreditSummary credit(int score)
    {
        CreditSummary summary = new CreditSummary();
        summary.setScore(score);
        summary.setMinScore(60);
        summary.setCanBook(score >= 60);
        return summary;
    }

    private SafetyExamStatusView exam(boolean valid)
    {
        SafetyExamStatusView status = new SafetyExamStatusView();
        status.setHasValidPass(valid);
        return status;
    }
}
