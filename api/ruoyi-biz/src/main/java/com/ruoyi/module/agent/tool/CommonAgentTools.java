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

    @Tool(name = "queryAvailableRoomsNow",
            description = "使用服务器当前时间查询未来1小时的可预约实验室，适合用户说现在、当前时间或今天还能约什么。")
    public String queryAvailableRoomsNow(
            @ToolParam(required = false, description = "参与人数，可为空") Integer participantCount)
    {
        return delegate.queryAvailableRoomsNow(participantCount);
    }

    @Tool(name = "queryAvailableDevices",
            description = "按实验室名称、设备名称或实验室ID查询当前可预约设备。用户只给出“生物实验室”或“离心机”时可使用名称关键词，不要求提供实验室ID。")
    public String queryAvailableDevices(
            @ToolParam(required = false, description = "实验室ID，可为空") Long roomId,
            @ToolParam(required = false, description = "实验室编号或名称关键词，可为空") String roomKeyword,
            @ToolParam(required = false, description = "设备编号或名称关键词，可为空") String deviceKeyword)
    {
        return delegate.queryAvailableDevices(roomId, roomKeyword, deviceKeyword);
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
