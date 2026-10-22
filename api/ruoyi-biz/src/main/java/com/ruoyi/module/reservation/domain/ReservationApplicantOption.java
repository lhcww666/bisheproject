package com.ruoyi.module.reservation.domain;

import lombok.Data;

/**
 * 预约申请人下拉选项，仅暴露表单展示所需字段。
 *
 * @author itjialu
 */
@Data
public class ReservationApplicantOption
{
    /** 用户主键 */
    private Long userId;

    /** 登录账号 */
    private String userName;

    /** 用户姓名 */
    private String nickName;

    /** 所属班级名称 */
    private String deptName;
}
