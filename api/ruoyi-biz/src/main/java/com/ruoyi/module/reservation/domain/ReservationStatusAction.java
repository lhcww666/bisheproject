package com.ruoyi.module.reservation.domain;

/**
 * 预约审核动作。
 *
 * @author itjialu
 */
public final class ReservationStatusAction
{
    public static final String APPROVE = "APPROVE";

    public static final String REJECT = "REJECT";

    public static final String UNAUDIT = "UNAUDIT";

    public static final String CANCEL = "CANCEL";

    public static final String CHECK_IN = "CHECK_IN";

    public static final String FINISH = "FINISH";

    public static final String NO_SHOW = "NO_SHOW";

    public static final String CREATE = "CREATE";

    public static final String AUTO_APPROVE = "AUTO_APPROVE";

    public static final String EDIT = "EDIT";

    private ReservationStatusAction()
    {
    }
}
