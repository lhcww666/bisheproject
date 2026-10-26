package com.ruoyi.module.material.domain;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 学生提交耗材领用时的可选预约。
 */
@Data
public class MaterialReservationOption
{
    private Long id;

    private String reservationNo;

    private String experimentName;

    private Long roomId;

    private String roomCode;

    private String roomName;

    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;
}
