package com.ruoyi.module.review.domain;

import lombok.Data;

/**
 * 预约审核请求对象
 *
 * @author itjialu
 */
@Data
public class ReservationReviewRequest
{
    /** 审核结果：1通过 2驳回，与字典 module_review_result 一致 */
    private String result;

    /** 审核意见，驳回时必填 */
    private String comment;
}
