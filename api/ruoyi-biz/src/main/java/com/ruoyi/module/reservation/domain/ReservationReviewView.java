package com.ruoyi.module.reservation.domain;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 预约审核历史。
 */
@Data
public class ReservationReviewView
{
    private Long id;

    private Long reviewerId;

    private String reviewerName;

    private String result;

    private String reviewComment;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date reviewTime;
}
