package com.ruoyi.module.violation.domain;

/**
 * 违规类型，与字典 module_violation_type 保持一致。
 *
 * @author itjialu
 */
public final class ViolationType
{
    /** 预约后未签到 */
    public static final String NO_SHOW = "NO_SHOW";

    /** 临近开始才取消 */
    public static final String LATE_CANCEL = "LATE_CANCEL";

    /** 安全操作违规 */
    public static final String SAFETY_VIOLATION = "SAFETY_VIOLATION";

    /** 设备损坏 */
    public static final String EQUIPMENT_DAMAGE = "EQUIPMENT_DAMAGE";

    /** 其他违规 */
    public static final String OTHER = "OTHER";

    private ViolationType()
    {
    }
}
