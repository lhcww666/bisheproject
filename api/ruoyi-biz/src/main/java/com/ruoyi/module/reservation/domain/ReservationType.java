package com.ruoyi.module.reservation.domain;

/**
 * 预约类型，与字典 module_reservation_type 保持一致。
 *
 * @author itjialu
 */
public final class ReservationType
{
    /** 个人预约 */
    public static final String INDIVIDUAL = "INDIVIDUAL";

    /** 班级预约，由班级负责人代表本班提交 */
    public static final String CLASS = "CLASS";

    private ReservationType()
    {
    }

    public static boolean isValid(String type)
    {
        return INDIVIDUAL.equals(type) || CLASS.equals(type);
    }
}
