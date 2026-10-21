package com.ruoyi.module.agent.tool;

import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * 仅学生助手可调用的个人业务工具。
 *
 * @author itjialu
 */
@Component
@RequiredArgsConstructor
public class StudentAgentTools
{
    private final LabAgentTools delegate;

    @Tool(name = "checkSafetyExamStatus",
            description = "查询当前学生的安全考试状态，包括是否有效通过、最近成绩和有效期。")
    public String checkSafetyExamStatus()
    {
        return delegate.checkSafetyExamStatus();
    }

    @Tool(name = "queryMyCredit",
            description = "查询当前学生的信用分、预约门槛以及当前是否可以提交预约。")
    public String queryMyCredit()
    {
        return delegate.queryMyCredit();
    }

    @Tool(name = "checkMyReservationEligibility",
            description = "综合检查当前学生的预约资格。可传实验室ID；不传时返回通用规则和当前有权预约的实验室。")
    public String checkMyReservationEligibility(
            @ToolParam(required = false, description = "目标实验室ID，可为空") Long roomId)
    {
        return delegate.checkMyReservationEligibility(roomId);
    }

    @Tool(name = "queryMyReservations",
            description = "查询当前学生最近的预约记录，可按状态筛选：0待审核，1已通过，2已驳回，3已取消，4已完成，5已签到，6已爽约。")
    public String queryMyReservations(
            @ToolParam(required = false, description = "预约状态，可为空") String status)
    {
        return delegate.queryMyReservations(status);
    }

    @Tool(name = "queryReservationReview",
            description = "按预约编号查询当前学生自己的预约审核结果和驳回原因。")
    public String queryReservationReview(
            @ToolParam(description = "预约编号") String reservationNo)
    {
        return delegate.queryReservationReview(reservationNo);
    }

    @Tool(name = "createReservationDraft",
            description = "创建当前学生的实验室预约草稿。只是待确认草稿，不代表预约成功。")
    public String createReservationDraft(
            @ToolParam(description = "实验室ID") Long roomId,
            @ToolParam(description = "实验名称") String experimentName,
            @ToolParam(description = "开始时间，格式 yyyy-MM-dd HH:mm:ss") String startTime,
            @ToolParam(description = "结束时间，格式 yyyy-MM-dd HH:mm:ss") String endTime,
            @ToolParam(description = "参与人数") Integer participantCount,
            @ToolParam(required = false, description = "设备ID列表，可为空") List<Long> deviceIds,
            @ToolParam(required = false, description = "实验说明，可为空") String remark)
    {
        return delegate.createReservationDraft(roomId, experimentName, startTime, endTime, participantCount,
                deviceIds, remark);
    }

    @Tool(name = "confirmReservation",
            description = "确认并提交当前学生已经看到并明确确认的预约草稿。")
    public String confirmReservation(
            @ToolParam(description = "预约草稿ID") Long draftId)
    {
        return delegate.confirmReservation(draftId);
    }
}
