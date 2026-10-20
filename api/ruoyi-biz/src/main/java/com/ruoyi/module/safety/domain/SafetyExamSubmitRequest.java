package com.ruoyi.module.safety.domain;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 交卷请求。
 *
 * @author itjialu
 */
@Data
public class SafetyExamSubmitRequest
{
    @NotNull(message = "考试记录ID不能为空")
    private Long recordId;

    private List<AnswerItem> answers;

    @Data
    public static class AnswerItem
    {
        private Long questionId;

        private List<String> answer;
    }
}
