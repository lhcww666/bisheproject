package com.ruoyi.module.agent.service;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 每次模型调用都注入的实时上下文。
 *
 * @author itjialu
 */
public final class AgentPromptContext
{
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final Map<DayOfWeek, String> WEEKDAY_LABELS = Map.of(
            DayOfWeek.MONDAY, "星期一",
            DayOfWeek.TUESDAY, "星期二",
            DayOfWeek.WEDNESDAY, "星期三",
            DayOfWeek.THURSDAY, "星期四",
            DayOfWeek.FRIDAY, "星期五",
            DayOfWeek.SATURDAY, "星期六",
            DayOfWeek.SUNDAY, "星期日");

    private AgentPromptContext()
    {
    }

    /**
     * 告诉模型服务器当前日期，避免把“明天”“下周三”换算成错误年份。
     */
    public static String currentTimeInstruction()
    {
        ZonedDateTime now = ZonedDateTime.now(ZONE);
        return "当前服务器时间：" + FORMATTER.format(now)
                + "（" + WEEKDAY_LABELS.get(now.getDayOfWeek()) + "，Asia/Shanghai）。"
                + "用户说今天、明天、下周几时，必须以此时间为基准换算。"
                + "如果用户只给上午或下午而未给具体时段，应先按常识选择明确区间，再调用实验室查询工具；"
                + "若业务规则拒绝，需要说明实际查询区间和失败原因。";
    }
}
