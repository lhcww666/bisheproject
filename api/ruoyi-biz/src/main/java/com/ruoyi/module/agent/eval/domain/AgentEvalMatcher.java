package com.ruoyi.module.agent.eval.domain;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import com.ruoyi.common.utils.StringUtils;

/**
 * Agent 离线评测的意图与工具匹配规则。
 *
 * <p>部分业务问题存在语义等价的工具。例如“哪里能做有机实验”既可以用
 * {@code queryAvailableRooms} 查指定时段，也可以用 {@code queryAvailableRoomsNow}
 * 查当前时段。评测应判断工具是否完成同一类业务能力，而不是只做字符串相等。</p>
 *
 * @author itjialu
 */
public final class AgentEvalMatcher
{
    private static final String AVAILABLE_ROOMS = "queryAvailableRooms";

    private static final String AVAILABLE_ROOMS_NOW = "queryAvailableRoomsNow";

    private static final Set<String> ROOM_AVAILABILITY_INTENTS =
            Set.of(LocalIntentRouter.QUERY_ROOMS, LocalIntentRouter.ROOMS_NOW);

    private AgentEvalMatcher()
    {
    }

    /**
     * 判断任意一次实际工具调用是否命中期望意图。
     */
    public static boolean intentMatches(String expectedIntent, Collection<String> calledTools)
    {
        if (LocalIntentRouter.UNKNOWN.equals(expectedIntent))
        {
            return calledTools == null || calledTools.isEmpty();
        }
        if (calledTools == null)
        {
            return false;
        }
        return calledTools.stream()
                .map(LocalIntentRouter::intentForTool)
                .anyMatch(actualIntent -> equivalentIntent(expectedIntent, actualIntent));
    }

    /**
     * 判断成功工具调用中是否存在期望工具或其等价工具。
     */
    public static boolean toolMatches(String expectedTool, Collection<String> successfulTools)
    {
        if ("unknown".equals(expectedTool))
        {
            return successfulTools == null || successfulTools.isEmpty();
        }
        if (successfulTools == null)
        {
            return false;
        }
        return successfulTools.stream().anyMatch(actualTool -> equivalentTool(expectedTool, actualTool));
    }

    /**
     * 优先把与期望问题最接近的实际工具解释为实际意图，避免多工具调用时取错第一个工具。
     */
    public static String resolveActualIntent(String expectedIntent, String expectedTool, Collection<String> calledTools)
    {
        if (calledTools == null || calledTools.isEmpty())
        {
            return LocalIntentRouter.UNKNOWN;
        }
        for (String actualTool : calledTools)
        {
            if (equivalentTool(expectedTool, actualTool))
            {
                return expectedIntent;
            }
        }
        for (String actualTool : calledTools)
        {
            String actualIntent = LocalIntentRouter.intentForTool(actualTool);
            if (equivalentIntent(expectedIntent, actualIntent))
            {
                return expectedIntent;
            }
        }
        return LocalIntentRouter.intentForTool(calledTools.iterator().next());
    }

    private static boolean equivalentIntent(String expected, String actual)
    {
        return StringUtils.isNotEmpty(expected) && StringUtils.isNotEmpty(actual)
                && (expected.equals(actual)
                        || (ROOM_AVAILABILITY_INTENTS.contains(expected)
                                && ROOM_AVAILABILITY_INTENTS.contains(actual)));
    }

    private static boolean equivalentTool(String expected, String actual)
    {
        if (StringUtils.isEmpty(expected) || StringUtils.isEmpty(actual))
        {
            return false;
        }
        return expected.equals(actual)
                || (Set.of(AVAILABLE_ROOMS, AVAILABLE_ROOMS_NOW).contains(expected)
                        && Set.of(AVAILABLE_ROOMS, AVAILABLE_ROOMS_NOW).contains(actual));
    }
}
