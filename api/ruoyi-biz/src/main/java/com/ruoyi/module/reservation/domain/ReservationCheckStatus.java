package com.ruoyi.module.reservation.domain;

/**
 * 预约审核状态，与 aoliya 的 check_status 口径保持一致。
 *
 * @author itjialu
 */
public final class ReservationCheckStatus
{
    /** 未审核 */
    public static final String NO_CHECK = "no_check";

    /** 已审核（通过或驳回都算完成审核） */
    public static final String HAD_CHECKED = "had_checked";

    private ReservationCheckStatus()
    {
    }

    public static boolean isValid(String status)
    {
        return NO_CHECK.equals(status) || HAD_CHECKED.equals(status);
    }
}
