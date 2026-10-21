package com.ruoyi.module.agent.eval.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * 离线意图路由测试。
 *
 * @author itjialu
 */
class LocalIntentRouterTest
{
    @Test
    void reviewIntentTakesPriorityOverReservationQuery()
    {
        assertEquals(LocalIntentRouter.REVIEW, LocalIntentRouter.route("我的预约为什么被驳回？"));
        assertEquals("queryReservationReview", LocalIntentRouter.toolFor(LocalIntentRouter.REVIEW));
    }

    @Test
    void routesExamCreditAndSafetyQuestions()
    {
        assertEquals(LocalIntentRouter.EXAM_STATUS, LocalIntentRouter.route("我的安全考试有效期到什么时候？"));
        assertEquals(LocalIntentRouter.CREDIT, LocalIntentRouter.route("我的信用分是多少？"));
        assertEquals(LocalIntentRouter.SAFETY, LocalIntentRouter.route("酒精着火了应该怎么处理？"));
    }

    @Test
    void routesEquipmentAndReservationDraft()
    {
        assertEquals(LocalIntentRouter.DEVICES, LocalIntentRouter.route("离心机现在可以用吗？"));
        assertEquals(LocalIntentRouter.CREATE_DRAFT,
                LocalIntentRouter.route("帮我预约明天下午的有机实验室，先帮我生成草稿"));
    }

    @Test
    void routesNaturalRecentReservationQuestion()
    {
        assertEquals(LocalIntentRouter.MY_RESERVATIONS, LocalIntentRouter.route("查看我最近的预约"));
    }

    @Test
    void returnsUnknownForUnrelatedQuestion()
    {
        assertEquals(LocalIntentRouter.UNKNOWN, LocalIntentRouter.route("今天食堂有什么菜？"));
        assertEquals("unknown", LocalIntentRouter.toolFor(LocalIntentRouter.UNKNOWN));
    }
}
