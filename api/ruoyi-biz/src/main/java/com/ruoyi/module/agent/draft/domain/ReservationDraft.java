package com.ruoyi.module.agent.draft.domain;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * Agent 预约草稿。
 *
 * <p>草稿只是待用户确认的意向，不等于预约成功；只有 confirm 后才会写入 reservation。</p>
 *
 * @author itjialu
 */
@Data
@TableName("reservation_draft")
public class ReservationDraft
{
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long sessionId;

    private Long roomId;

    private String experimentName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    private Integer participantCount;

    /** 设备ID JSON 数组，确认时还原为 List<Long>。 */
    private String deviceIds;

    private String remark;

    private String status;

    private Long reservationId;

    private String reservationNo;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date expiresAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
