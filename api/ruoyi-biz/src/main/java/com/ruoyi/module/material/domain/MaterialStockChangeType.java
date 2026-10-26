package com.ruoyi.module.material.domain;

/**
 * 库存流水类型。
 */
public final class MaterialStockChangeType
{
    public static final String INITIAL = "INITIAL";

    public static final String ADJUST_IN = "ADJUST_IN";

    public static final String ADJUST_OUT = "ADJUST_OUT";

    public static final String APPLY_OUT = "APPLY_OUT";

    public static final String CANCEL_RETURN = "CANCEL_RETURN";

    private MaterialStockChangeType()
    {
    }

    public static boolean isAdjustment(String type)
    {
        return ADJUST_IN.equals(type) || ADJUST_OUT.equals(type);
    }
}
