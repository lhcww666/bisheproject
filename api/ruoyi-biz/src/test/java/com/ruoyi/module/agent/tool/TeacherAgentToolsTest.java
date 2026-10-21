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
import com.ruoyi.module.agent.service.AgentContext;
import com.ruoyi.module.agent.service.ToolAuditService;
import com.ruoyi.module.common.service.ModuleDataScopeService;
import com.ruoyi.module.credit.service.ICreditService;
import com.ruoyi.module.reservation.domain.ReservationView;
import com.ruoyi.module.reservation.mapper.ReservationMapper;
import com.ruoyi.module.reservation.service.IReservationService;
import com.ruoyi.module.review.service.IReservationReviewBizService;
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
}
