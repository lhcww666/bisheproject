package com.ruoyi.module.review.domain;

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
 * 预约审核对象 reservation_review
 *
 * @author itjialu
 * @date 2026-10-16
 */
@Data
@TableName("reservation_review" )
public class ReservationReview extends BaseEntity
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

            /** 审核人ID（若依用户主键 sys_user.user_id） */
                @Excel(name = "审核人ID" )
        public static final String REVIEWERID = "reviewer_id";
            @TableField(value = REVIEWERID)
            @NotNull(message = "审核人ID不能为空")
        private Long reviewerId;

            /** 审核结果 */
                @Excel(name = "审核结果" , dictType = "module_review_result" )
        public static final String RESULT = "result";
            @TableField(value = RESULT)
            @NotBlank(message = "审核结果不能为空")
        private String result;

            /** 审核意见 */
                @Excel(name = "审核意见" )
        public static final String REVIEWCOMMENT = "review_comment";
            @TableField(value = REVIEWCOMMENT)
            @Size(min = 0, max = 500, message = "审核意见长度不能超过500个字符")
        private String reviewComment;

            /** 审核时间 */
        public static final String REVIEWTIME = "review_time";
            @TableField(value = REVIEWTIME)
            @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss" )
                @Excel(name = "审核时间" , width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss" )
        private Date reviewTime;

}
