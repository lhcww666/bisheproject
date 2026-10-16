package com.ruoyi.module.room.domain;

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
 * 实验室信息对象 room
 *
 * @author itjialu
 * @date 2026-10-16
 */
@Data
@TableName("room" )
public class Room extends BaseEntity
        {
private static final long serialVersionUID = 1L;

            /** 主键 */
            @TableId(type = IdType.AUTO)
            @OrderBy(asc = false, sort = 1)
        private Long id;

            /** 实验室编号 */
                @Excel(name = "实验室编号" )
        public static final String ROOMCODE = "room_code";
            @TableField(value = ROOMCODE)
            @NotBlank(message = "实验室编号不能为空")
            @Size(min = 0, max = 30, message = "实验室编号长度不能超过30个字符")
        private String roomCode;

            /** 实验室名称 */
                @Excel(name = "实验室名称" )
        public static final String ROOMNAME = "room_name";
            @TableField(value = ROOMNAME)
            @NotBlank(message = "实验室名称不能为空")
            @Size(min = 0, max = 100, message = "实验室名称长度不能超过100个字符")
        private String roomName;

            /** 实验室类型 */
                @Excel(name = "实验室类型" , dictType = "module_room_type" )
        public static final String ROOMTYPE = "room_type";
            @TableField(value = ROOMTYPE)
            @NotBlank(message = "实验室类型不能为空")
            @Size(min = 0, max = 30, message = "实验室类型长度不能超过30个字符")
        private String roomType;

            /** 位置 */
                @Excel(name = "位置" )
        public static final String LOCATION = "location";
            @TableField(value = LOCATION)
            @NotBlank(message = "位置不能为空")
            @Size(min = 0, max = 100, message = "位置长度不能超过100个字符")
        private String location;

            /** 最大人数 */
                @Excel(name = "最大人数" )
        public static final String CAPACITY = "capacity";
            @TableField(value = CAPACITY)
            @NotNull(message = "最大人数不能为空")
        private Long capacity;

            /** 安全等级 */
                @Excel(name = "安全等级" )
        public static final String SAFETYLEVEL = "safety_level";
            @TableField(value = SAFETYLEVEL)
            @NotNull(message = "安全等级不能为空")
        private Long safetyLevel;

            /** 开放开始时间 */
                @Excel(name = "开放开始时间" )
        public static final String OPENSTARTTIME = "open_start_time";
            @TableField(value = OPENSTARTTIME)
            @NotBlank(message = "开放开始时间不能为空")
        private String openStartTime;

            /** 开放结束时间 */
                @Excel(name = "开放结束时间" )
        public static final String OPENENDTIME = "open_end_time";
            @TableField(value = OPENENDTIME)
            @NotBlank(message = "开放结束时间不能为空")
        private String openEndTime;

            /** 状态 */
            // 逻辑删除替代物理删除，'0'：正常；'1'：删除
            // @TableLogic(value = "'0'" , delval = "'1'" )
            @NotBlank(message = "状态不能为空")
        private String status;

            /** 说明 */
                @Excel(name = "说明" )
        public static final String DESCRIPTION = "description";
            @TableField(value = DESCRIPTION)
            @Size(min = 0, max = 500, message = "说明长度不能超过500个字符")
        private String description;

}
