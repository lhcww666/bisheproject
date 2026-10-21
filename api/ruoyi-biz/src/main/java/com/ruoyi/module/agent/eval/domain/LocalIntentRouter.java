package com.ruoyi.module.agent.eval.domain;

/**
 * 离线评测使用的确定性意图路由器。
 *
 * <p>它不是模型，只是用于在没有 API Key 时建立可重复的回归基线；
 * 配置真实模型后，可以把评测模式扩展为 LLM 实际工具调用。</p>
 *
 * @author itjialu
 */
public final class LocalIntentRouter
{
    public static final String REVIEW = "REVIEW";
    public static final String ELIGIBILITY = "ELIGIBILITY";
    public static final String EXAM_STATUS = "EXAM_STATUS";
    public static final String CREDIT = "CREDIT";
    public static final String CREATE_DRAFT = "CREATE_DRAFT";
    public static final String SAFETY = "SAFETY";
    public static final String DEVICES = "DEVICES";
    public static final String MY_RESERVATIONS = "MY_RESERVATIONS";
    public static final String ROOMS_NOW = "ROOMS_NOW";
    public static final String QUERY_ROOMS = "QUERY_ROOMS";
    public static final String UNKNOWN = "UNKNOWN";

    private LocalIntentRouter()
    {
    }

    public static String route(String question)
    {
        String text = question == null ? "" : question;
        if (containsAny(text, "驳回", "审核意见", "没通过")
                || (text.contains("为什么") && containsAny(text, "预约", "审核")))
        {
            return REVIEW;
        }
        if (containsAny(text, "现在还能预约", "现在可以预约", "现在能预约", "当前时间",
                "现在哪个", "现在哪些", "当前还有", "现在还有", "现在可用"))
        {
            return ROOMS_NOW;
        }
        if (containsAny(text, "可以预约", "能预约", "能否预约", "能不能预约", "预约资格",
                "没通过安全考试", "没有通过安全考试", "没有安全考试", "安全考试不合格",
                "考试不合格", "考试没过"))
        {
            return ELIGIBILITY;
        }
        if (containsAny(text, "安全考试", "考试"))
        {
            return EXAM_STATUS;
        }
        if (containsAny(text, "信用分", "信用", "预约资格"))
        {
            return CREDIT;
        }
        if (containsAny(text, "草稿", "帮我预约", "帮我约", "先帮我"))
        {
            return CREATE_DRAFT;
        }
        if (containsAny(text, "着火", "稀释", "溅", "废液", "疏散", "灭菌锅", "穿什么", "安全注意",
                "沾到", "皮肤接触", "水银", "汞", "泄漏", "洒", "通风柜", "气瓶", "钢瓶",
                "加热", "割伤", "割破", "防冻", "超低温", "低温冰箱", "烘箱", "停电", "误食", "吸入", "刺鼻",
                "头晕", "菌液", "培养皿", "针头", "锐器", "刺伤", "扎到", "洗眼器", "灭火器"))
        {
            return SAFETY;
        }
        if (containsAny(text, "设备", "仪器", "离心机", "旋转蒸发仪"))
        {
            return DEVICES;
        }
        if (containsAny(text, "我的预约", "最近预约", "最近的预约", "预约记录", "已完成的预约"))
        {
            return MY_RESERVATIONS;
        }
        if (containsAny(text, "实验室", "空闲", "可用", "哪里"))
        {
            return QUERY_ROOMS;
        }
        return UNKNOWN;
    }

    public static String toolFor(String intent)
    {
        return switch (intent)
        {
            case REVIEW -> "queryReservationReview";
            case ELIGIBILITY -> "checkMyReservationEligibility";
            case EXAM_STATUS -> "checkSafetyExamStatus";
            case CREDIT -> "queryMyCredit";
            case CREATE_DRAFT -> "createReservationDraft";
            case SAFETY -> "searchSafetyKnowledge";
            case DEVICES -> "queryAvailableDevices";
            case MY_RESERVATIONS -> "queryMyReservations";
            case ROOMS_NOW -> "queryAvailableRoomsNow";
            case QUERY_ROOMS -> "queryAvailableRooms";
            default -> "unknown";
        };
    }

    private static boolean containsAny(String text, String... keywords)
    {
        for (String keyword : keywords)
        {
            if (text.contains(keyword))
            {
                return true;
            }
        }
        return false;
    }
}
