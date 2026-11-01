package com.ruoyi.module.agent.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

/**
 * 预约草稿展示对象。
 */
@Data
@AllArgsConstructor
public class ReservationDraftView
{
    private Long id;

    private Long sessionId;

    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date expiresAt;

    private Long confirmedReservationId;

    private AgentReservationDraftCreateRequest request;
}
