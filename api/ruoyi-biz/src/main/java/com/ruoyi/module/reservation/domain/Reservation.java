package com.ruoyi.module.reservation.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
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
 * 实验室预约对象 reservation
 *
 * @author itjialu
 * @date 2026-10-16
 */
@Data
@TableName("reservation" )
public class Reservation extends BaseEntity
        {
private static final long serialVersionUID = 1L;

            /** 主键 */
            @TableId(type = IdType.AUTO)
            @OrderBy(asc = false, sort = 1)
        private Long id;

            /** 预约编号 */
                @Excel(name = "预约编号" )
        public static final String RESERVATIONNO = "reservation_no";
            @TableField(value = RESERVATIONNO)
            @NotBlank(message = "预约编号不能为空")
            @Size(min = 0, max = 32, message = "预约编号长度不能超过32个字符")
        private String reservationNo;

            /** 申请人ID（若依用户主键 sys_user.user_id） */
                @Excel(name = "申请人ID" )
        public static final String APPLICANTID = "applicant_id";
            @TableField(value = APPLICANTID)
            @NotNull(message = "申请人ID不能为空")
        private Long applicantId;

            /** 实验室 */
                @Excel(name = "实验室" )
        public static final String ROOMID = "room_id";
            @TableField(value = ROOMID)
            @NotNull(message = "实验室不能为空")
        private Long roomId;

            /** 实验名称 */
                @Excel(name = "实验名称" )
        public static final String EXPERIMENTNAME = "experiment_name";
            @TableField(value = EXPERIMENTNAME)
            @NotBlank(message = "实验名称不能为空")
            @Size(min = 0, max = 100, message = "实验名称长度不能超过100个字符")
        private String experimentName;

            /** 开始时间 */
        public static final String STARTTIME = "start_time";
            @TableField(value = STARTTIME)
            @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss" )
                @Excel(name = "开始时间" , width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss" )
            @NotNull(message = "开始时间不能为空")
        private Date startTime;

            /** 结束时间 */
        public static final String ENDTIME = "end_time";
            @TableField(value = ENDTIME)
            @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss" )
                @Excel(name = "结束时间" , width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss" )
            @NotNull(message = "结束时间不能为空")
        private Date endTime;

            /** 参与人数 */
                @Excel(name = "参与人数" )
        public static final String PARTICIPANTCOUNT = "participant_count";
            @TableField(value = PARTICIPANTCOUNT)
            @NotNull(message = "参与人数不能为空")
        private Long participantCount;

            /** 风险等级 */
                @Excel(name = "风险等级" )
        public static final String RISKLEVEL = "risk_level";
            @TableField(value = RISKLEVEL)
            @NotNull(message = "风险等级不能为空")
        private Long riskLevel;

            /** 预约状态 */
            // 逻辑删除替代物理删除，'0'：正常；'1'：删除
            // @TableLogic(value = "'0'" , delval = "'1'" )
            @NotBlank(message = "预约状态不能为空")
        private String status;

            /** 是否需要审核 */
                @Excel(name = "是否需要审核" , dictType = "module_yes_no" )
        public static final String REVIEWREQUIRED = "review_required";
            @TableField(value = REVIEWREQUIRED)
            @NotBlank(message = "是否需要审核不能为空")
        private String reviewRequired;

}
