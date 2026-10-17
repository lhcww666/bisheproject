package com.ruoyi.module.reservation.domain;

import java.util.Date;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 预约展示对象：在预约记录上补充实验室名称、申请人、资格与审核意见等展示字段
 *
 * @author itjialu
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ReservationView extends Reservation
{
    private static final long serialVersionUID = 1L;

    /** 实验室编号 */
    private String roomCode;

    /** 实验室名称 */
    private String roomName;

    /** 申请人名称 */
    private String applicantName;

    /** 安全考试是否在有效期内 */
    private Boolean examValid;

    /** 当前信用分 */
    private Long creditScore;

    /** 最近一次审核结果 */
    private String reviewResult;

    /** 最近一次审核意见 */
    private String reviewComment;

    /** 最近一次审核时间 */
    private Date reviewTime;
}
