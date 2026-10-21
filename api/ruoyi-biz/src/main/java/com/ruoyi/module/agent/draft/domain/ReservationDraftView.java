package com.ruoyi.module.agent.draft.domain;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 预约草稿展示对象，供聊天卡片和确认接口返回。
 *
 * @author itjialu
 */
@Data
public class ReservationDraftView
{
    private Long id;

    private Long roomId;

    private String roomName;

    private String experimentName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    private Integer participantCount;

    private List<Long> deviceIds;

    private List<String> deviceNames;

    private String remark;

    private String status;

    private Long reservationId;

    private String reservationNo;

    /** 确认后预约的业务状态：0待审核、1已通过等。 */
    private String reservationStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date expiresAt;
}
