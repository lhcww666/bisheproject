package com.ruoyi.module.violation.domain;

/**
 * 违规记录状态。
 *
 * @author itjialu
 */
public final class ViolationStatus
{
    public static final String PENDING = "0";

    public static final String HANDLED = "1";

    public static final String REVOKED = "2";

    private ViolationStatus()
    {
    }
}
