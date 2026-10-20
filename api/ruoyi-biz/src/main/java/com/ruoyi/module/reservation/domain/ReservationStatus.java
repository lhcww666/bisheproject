package com.ruoyi.module.reservation.domain;

/**
 * 预约状态常量，与字典 module_reservation_status 保持一致
 *
 * @author itjialu
 */
public final class ReservationStatus
{
    /** 待审核 */
    public static final String PENDING = "0";

    /** 已通过 */
    public static final String APPROVED = "1";

    /** 已驳回 */
    public static final String REJECTED = "2";

    /** 已取消 */
    public static final String CANCELED = "3";

    /** 已完成 */
    public static final String COMPLETED = "4";

    /** 已签到 */
    public static final String CHECKED_IN = "5";

    /** 已爽约 */
    public static final String NO_SHOW = "6";

    private ReservationStatus()
    {
    }

    /**
     * 是否为占用实验室时段的有效预约（待审核、已通过、已签到）
     */
    public static boolean isOccupied(String status)
    {
        return PENDING.equals(status) || APPROVED.equals(status) || CHECKED_IN.equals(status);
    }
}
