package com.ruoyi.module.safety.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 安全考试判分测试。
 *
 * @author itjialu
 */
class SafetyExamGradingTest
{
    @Test
    void singleAnswerMustMatchExactly()
    {
        assertTrue(SafetyExamGrading.matches(List.of("A"), List.of("a")));
        assertFalse(SafetyExamGrading.matches(List.of("A"), List.of("B")));
        assertFalse(SafetyExamGrading.matches(List.of("A"), List.of("A", "B")));
    }

    @Test
    void multipleAnswerIsOrderIndependent()
    {
        assertTrue(SafetyExamGrading.matches(List.of("A", "C"), List.of("c", "a")));
        assertFalse(SafetyExamGrading.matches(List.of("A", "C"), List.of("A")));
        assertFalse(SafetyExamGrading.matches(List.of("A", "C"), List.of("A", "B", "C")));
    }

    @Test
    void emptyAnswerIsIncorrect()
    {
        assertFalse(SafetyExamGrading.matches(List.of("A"), List.of()));
        assertFalse(SafetyExamGrading.matches(List.of("A"), null));
    }
}
