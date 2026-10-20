package com.ruoyi.module.safety.service;

import com.ruoyi.module.safety.domain.SafetyExamResultView;
import com.ruoyi.module.safety.domain.SafetyExamStartView;
import com.ruoyi.module.safety.domain.SafetyExamStatusView;
import com.ruoyi.module.safety.domain.SafetyExamSubmitRequest;

/**
 * 安全考试Service。
 *
 * @author itjialu
 */
public interface ISafetyExamService
{
    SafetyExamStatusView getStatus(Long studentId);

    SafetyExamStartView startExam(Long studentId);

    SafetyExamResultView submitExam(Long studentId, SafetyExamSubmitRequest request);
}
