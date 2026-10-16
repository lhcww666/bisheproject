package com.ruoyi.module.device.domain;

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
 * 设备档案对象 device
 *
 * @author itjialu
 * @date 2026-10-16
 */
@Data
@TableName("device" )
public class Device extends BaseEntity
        {
private static final long serialVersionUID = 1L;

            /** 主键 */
            @TableId(type = IdType.AUTO)
            @OrderBy(asc = false, sort = 1)
        private Long id;

            /** 所属实验室 */
                @Excel(name = "所属实验室" )
        public static final String ROOMID = "room_id";
            @TableField(value = ROOMID)
            @NotNull(message = "所属实验室不能为空")
        private Long roomId;

            /** 设备编号 */
                @Excel(name = "设备编号" )
        public static final String DEVICECODE = "device_code";
            @TableField(value = DEVICECODE)
            @NotBlank(message = "设备编号不能为空")
            @Size(min = 0, max = 30, message = "设备编号长度不能超过30个字符")
        private String deviceCode;

            /** 设备名称 */
                @Excel(name = "设备名称" )
        public static final String DEVICENAME = "device_name";
            @TableField(value = DEVICENAME)
            @NotBlank(message = "设备名称不能为空")
            @Size(min = 0, max = 100, message = "设备名称长度不能超过100个字符")
        private String deviceName;

            /** 设备类型 */
                @Excel(name = "设备类型" , dictType = "module_device_type" )
        public static final String DEVICETYPE = "device_type";
            @TableField(value = DEVICETYPE)
            @NotBlank(message = "设备类型不能为空")
            @Size(min = 0, max = 50, message = "设备类型长度不能超过50个字符")
        private String deviceType;

            /** 数量 */
                @Excel(name = "数量" )
        public static final String QUANTITY = "quantity";
            @TableField(value = QUANTITY)
            @NotNull(message = "数量不能为空")
        private Long quantity;

            /** 状态 */
            // 逻辑删除替代物理删除，'0'：正常；'1'：删除
            // @TableLogic(value = "'0'" , delval = "'1'" )
            @NotBlank(message = "状态不能为空")
        private String status;

            /** 是否需要培训 */
                @Excel(name = "是否需要培训" , dictType = "module_yes_no" )
        public static final String REQUIRESTRAINING = "requires_training";
            @TableField(value = REQUIRESTRAINING)
            @NotBlank(message = "是否需要培训不能为空")
        private String requiresTraining;

}
