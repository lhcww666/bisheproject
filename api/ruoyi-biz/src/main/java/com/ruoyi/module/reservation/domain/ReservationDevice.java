package com.ruoyi.module.reservation.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 预约设备关联对象 reservation_device
 *
 * @author itjialu
 * @date 2026-10-16
 */
@Data
@TableName("reservation_device" )
public class ReservationDevice extends BaseEntity
        {
private static final long serialVersionUID = 1L;

            /** 主键 */
            @TableId(type = IdType.AUTO)
            @OrderBy(asc = false, sort = 1)
        private Long id;

            /** 预约主键 */
                @Excel(name = "预约主键" )
        public static final String RESERVATIONID = "reservation_id";
            @TableField(value = RESERVATIONID)
            @NotNull(message = "预约主键不能为空")
        private Long reservationId;

            /** 设备主键 */
                @Excel(name = "设备主键" )
        public static final String DEVICEID = "device_id";
            @TableField(value = DEVICEID)
            @NotNull(message = "设备主键不能为空")
        private Long deviceId;

            /** 使用数量 */
                @Excel(name = "使用数量" )
        public static final String QUANTITY = "quantity";
            @TableField(value = QUANTITY)
            @NotNull(message = "使用数量不能为空")
        private Long quantity;

}
