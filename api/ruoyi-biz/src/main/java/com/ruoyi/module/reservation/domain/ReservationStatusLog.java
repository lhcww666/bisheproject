package com.ruoyi.module.reservation.domain;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 预约状态变更日志。所有审核、签到、完成、爽约、取消动作都追加记录，不覆盖历史。
 *
 * @author itjialu
 */
@Data
@TableName("reservation_status_log")
public class ReservationStatusLog
{
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long reservationId;

    private String fromStatus;

    private String toStatus;

    private String action;

    private Long operatorId;

    private String operatorName;

    private String reason;

    @TableField("create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
