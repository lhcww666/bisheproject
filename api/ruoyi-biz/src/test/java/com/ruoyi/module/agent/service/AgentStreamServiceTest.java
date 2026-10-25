package com.ruoyi.module.agent.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * SSE 回答分块测试。
 *
 * @author itjialu
 */
class AgentStreamServiceTest
{
    @Test
    void splitChunksKeepsCompleteCodePoints()
    {
        List<String> chunks = AgentStreamService.splitChunks("实验室安全操作🧪请按规范执行");

        assertFalse(chunks.isEmpty());
        assertEquals("实验室安全操作🧪请按规范执行", String.join("", chunks));
        assertTrue(chunks.stream().allMatch(this::hasBalancedSurrogates));
    }

    @Test
    void emptyAnswerHasNoChunks()
    {
        assertEquals(List.of(), AgentStreamService.splitChunks(""));
    }

    private boolean hasBalancedSurrogates(String value)
    {
        for (int index = 0; index < value.length(); index++)
        {
            char current = value.charAt(index);
            if (Character.isHighSurrogate(current))
            {
                if (index + 1 >= value.length() || !Character.isLowSurrogate(value.charAt(index + 1)))
                {
                    return false;
                }
                index++;
            }
            else if (Character.isLowSurrogate(current))
            {
                return false;
            }
        }
        return true;
    }
}
