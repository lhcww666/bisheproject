package com.ruoyi.module.safety.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 安全考试判分规则：多选题必须完全一致，单选/判断题只能有一个答案。
 *
 * @author itjialu
 */
public final class SafetyExamGrading
{
    private SafetyExamGrading()
    {
    }

    public static boolean matches(List<String> correctAnswers, List<String> submittedAnswers)
    {
        if (correctAnswers == null || correctAnswers.isEmpty())
        {
            return false;
        }
        Set<String> correct = normalize(correctAnswers);
        Set<String> submitted = normalize(submittedAnswers);
        return !submitted.isEmpty() && correct.equals(submitted);
    }

    public static Set<String> normalize(List<String> answers)
    {
        Set<String> result = new HashSet<>();
        if (answers == null)
        {
            return result;
        }
        for (String answer : answers)
        {
            if (answer != null && !answer.trim().isEmpty())
            {
                result.add(answer.trim().toUpperCase());
            }
        }
        return result;
    }
}
