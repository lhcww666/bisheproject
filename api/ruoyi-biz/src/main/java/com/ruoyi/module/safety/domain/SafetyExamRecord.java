package com.ruoyi.module.safety.domain;

import java.math.BigDecimal;
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
 * 安全考试记录对象 safety_exam_record
 *
 * @author itjialu
 * @date 2026-10-16
 */
@Data
@TableName("safety_exam_record" )
public class SafetyExamRecord extends BaseEntity
        {
private static final long serialVersionUID = 1L;

            /** 主键 */
            @TableId(type = IdType.AUTO)
            @OrderBy(asc = false, sort = 1)
        private Long id;

            /** 学生ID（若依用户主键 sys_user.user_id） */
                @Excel(name = "学生ID" )
        public static final String STUDENTID = "student_id";
            @TableField(value = STUDENTID)
            @NotNull(message = "学生ID不能为空")
        private Long studentId;

            /** 考试成绩 */
                @Excel(name = "考试成绩" )
        public static final String SCORE = "score";
            @TableField(value = SCORE)
            @NotNull(message = "考试成绩不能为空")
        private BigDecimal score;

            /** 是否通过 */
                @Excel(name = "是否通过" , dictType = "module_yes_no" )
        public static final String PASSED = "passed";
            @TableField(value = PASSED)
            @NotBlank(message = "是否通过不能为空")
        private String passed;

            /** 考试时间 */
        public static final String EXAMTIME = "exam_time";
            @TableField(value = EXAMTIME)
            @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss" )
                @Excel(name = "考试时间" , width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss" )
            @NotNull(message = "考试时间不能为空")
        private Date examTime;

            /** 有效期至 */
        public static final String VALIDUNTIL = "valid_until";
            @TableField(value = VALIDUNTIL)
            @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss" )
                @Excel(name = "有效期至" , width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss" )
        private Date validUntil;

            /** 试卷配置ID */
            @TableField("config_id")
        private Long configId;

            /** 试卷编号 */
            @TableField("paper_no")
        private String paperNo;

            /** 交卷时间 */
            @TableField("submit_time")
            @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private Date submitTime;

            /** 0进行中，1已提交，2已过期 */
            @TableField("status")
        private String status;

            /** 答对题数 */
            @TableField("correct_count")
        private Integer correctCount;

            /** 学生姓名/账号/班级，仅用于列表展示 */
            @Excel(name = "学生姓名" )
            @TableField(exist = false)
        private String nickName;

            @Excel(name = "学生账号" )
            @TableField(exist = false)
        private String userName;

            @Excel(name = "班级" )
            @TableField(exist = false)
        private String deptName;

}
