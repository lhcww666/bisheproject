package com.ruoyi.module.room.domain;

/**
 * 实验室预约模式，与字典 module_reservation_mode 保持一致。
 *
 * @author itjialu
 */
public final class RoomReservationMode
{
    /** 开放预约：符合通用条件的学生均可预约 */
    public static final String OPEN = "OPEN";

    /** 限定范围：只有配置了准入规则的学生可以预约 */
    public static final String RESTRICTED = "RESTRICTED";

    /** 限定范围且必须教师审核 */
    public static final String APPROVAL_ONLY = "APPROVAL_ONLY";

    /** 停止学生预约 */
    public static final String CLOSED = "CLOSED";

    private RoomReservationMode()
    {
    }

    public static boolean isValid(String mode)
    {
        return OPEN.equals(mode) || RESTRICTED.equals(mode) || APPROVAL_ONLY.equals(mode) || CLOSED.equals(mode);
    }
}
