package com.ruoyi.module.agent.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

/**
 * 提供给 Agent 的本人预约摘要。
 */
@Data
@AllArgsConstructor
public class AgentReservationSummary
{
    private Long id;

    private String reservationNo;

    private String roomName;

    private String experimentName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    private String status;

    private String reviewComment;
}
