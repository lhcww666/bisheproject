package com.ruoyi.module.credit.service;

import com.ruoyi.module.credit.domain.CreditSummary;

/**
 * 信用分账户服务。所有信用变动必须通过这里，保证账户余额与流水一致。
 *
 * @author itjialu
 */
public interface ICreditService
{
    /**
     * 查询用户当前信用分。
     */
    int currentScore(Long userId);

    /**
     * 变更信用分并写流水。
     *
     * @return 变更后的分值
     */
    int changeScore(Long userId, int delta, String reason, String bizType, Long bizId);

    /**
     * 当前用户信用分摘要。
     */
    CreditSummary getSummary(Long userId);
}
