package com.ruoyi.module.material.domain;

/**
 * 耗材档案状态。
 */
public final class MaterialStatus
{
    public static final String DISABLED = "0";

    public static final String ACTIVE = "1";

    private MaterialStatus()
    {
    }

    public static boolean isValid(String status)
    {
        return ACTIVE.equals(status) || DISABLED.equals(status);
    }
}
