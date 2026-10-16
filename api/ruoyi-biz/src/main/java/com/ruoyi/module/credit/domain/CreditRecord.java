package com.ruoyi.module.credit.domain;

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
 * 信用分流水对象 credit_record
 *
 * @author itjialu
 * @date 2026-10-16
 */
@Data
@TableName("credit_record" )
public class CreditRecord extends BaseEntity
        {
private static final long serialVersionUID = 1L;

            /** 主键 */
            @TableId(type = IdType.AUTO)
            @OrderBy(asc = false, sort = 1)
        private Long id;

            /** 用户，对应若依 sys_user.user_id */
        public static final String USERID = "user_id";
            @TableField(value = USERID)
            @NotNull(message = "用户，对应若依 sys_user.user_id不能为空")
        private Long userId;

            /** 变动分值，可正可负 */
                @Excel(name = "变动分值，可正可负" )
        public static final String CHANGESCORE = "change_score";
            @TableField(value = CHANGESCORE)
            @NotNull(message = "变动分值，可正可负不能为空")
        private Long changeScore;

            /** 变动后分值 */
                @Excel(name = "变动后分值" )
        public static final String BALANCESCORE = "balance_score";
            @TableField(value = BALANCESCORE)
            @NotNull(message = "变动后分值不能为空")
        private Long balanceScore;

            /** 变动原因 */
                @Excel(name = "变动原因" )
        public static final String REASON = "reason";
            @TableField(value = REASON)
            @NotBlank(message = "变动原因不能为空")
            @Size(min = 0, max = 200, message = "变动原因长度不能超过200个字符")
        private String reason;

}
