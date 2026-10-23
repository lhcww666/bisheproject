package com.ruoyi.module.agent.role;

import java.util.List;

/**
 * 智能助手角色。角色来自登录用户的真实角色，不接受用户文本声明。
 *
 * @author itjialu
 */
public enum AgentRole
{
    STUDENT("student", "学生", "学生预约助手",
            "查询个人预约资格、安全考试、信用分和可用实验室",
            List.of("现在还能预约哪些实验室？", "我现在能不能预约实验室？", "查看我最近的预约", "我的预约为什么被驳回？")),

    TEACHER("teacher", "教师", "教师审核助手",
            "查看本人负责范围内的待审核预约和学生资质",
            List.of("查看本班待审核预约", "统计本班平均信用分", "哪些学生没有有效安全考试？", "查询学生 student01 的考试和信用情况")),

    ADMIN("admin", "管理员", "平台管理助手",
            "查看全校预约审核概况、实验室规则和平台运行状态",
            List.of("查看当前待审核预约", "统计全校平均信用分", "哪些学生信用分低于60？", "汇总当前预约审核情况")),

    UNKNOWN("unknown", "未知", "实验室智能助手",
            "当前账号没有可用的实验室业务助手角色",
            List.of("实验室安全注意事项"));

    private final String code;

    private final String label;

    private final String title;

    private final String description;

    private final List<String> examples;

    AgentRole(String code, String label, String title, String description, List<String> examples)
    {
        this.code = code;
        this.label = label;
        this.title = title;
        this.description = description;
        this.examples = examples;
    }

    public String getCode()
    {
        return code;
    }

    public String getLabel()
    {
        return label;
    }

    public String getTitle()
    {
        return title;
    }

    public String getDescription()
    {
        return description;
    }

    public List<String> getExamples()
    {
        return examples;
    }
}
