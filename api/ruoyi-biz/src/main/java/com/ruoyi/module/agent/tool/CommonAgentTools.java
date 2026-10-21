package com.ruoyi.module.agent.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * 学生、教师和管理员共用的实验室查询与安全知识工具。
 *
 * @author itjialu
 */
@Component
@RequiredArgsConstructor
public class CommonAgentTools
{
    private final LabAgentTools delegate;

    @Tool(name = "queryAvailableRooms",
            description = "查询指定时间段内当前用户可以预约的实验室，返回实验室编号、名称、容量、预约模式和安全等级。")
    public String queryAvailableRooms(
            @ToolParam(description = "开始时间，格式 yyyy-MM-dd HH:mm:ss") String startTime,
            @ToolParam(description = "结束时间，格式 yyyy-MM-dd HH:mm:ss") String endTime,
            @ToolParam(required = false, description = "参与人数，可为空") Integer participantCount)
    {
        return delegate.queryAvailableRooms(startTime, endTime, participantCount);
    }

    @Tool(name = "queryAvailableDevices",
            description = "查询指定实验室当前可预约的设备，返回设备编号、名称、数量和是否需要培训。")
    public String queryAvailableDevices(
            @ToolParam(description = "实验室ID") Long roomId)
    {
        return delegate.queryAvailableDevices(roomId);
    }

    @Tool(name = "searchSafetyKnowledge",
            description = "检索实验室安全知识库。回答化学品、消防、个人防护、设备安全等问题前应优先调用此工具。")
    public String searchSafetyKnowledge(
            @ToolParam(description = "要检索的安全问题") String question,
            @ToolParam(required = false, description = "返回条数，默认3") Integer topK)
    {
        return delegate.searchSafetyKnowledge(question, topK);
    }
}
