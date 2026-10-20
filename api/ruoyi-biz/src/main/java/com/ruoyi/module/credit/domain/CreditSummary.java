package com.ruoyi.module.credit.domain;

import lombok.Data;

/**
 * 当前用户信用分摘要。
 *
 * @author itjialu
 */
@Data
public class CreditSummary
{
    private Long userId;

    private Integer score;

    /** 允许预约的最低信用分 */
    private Integer minScore;

    /** 当前是否允许提交预约 */
    private Boolean canBook;
}
