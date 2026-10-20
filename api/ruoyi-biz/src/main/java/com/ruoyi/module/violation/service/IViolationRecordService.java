package com.ruoyi.module.violation.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.module.violation.domain.ViolationRecord;

/**
 * 违规记录Service。
 *
 * @author itjialu
 */
public interface IViolationRecordService
{
    IPage<ViolationRecord> pageViolation(Page<ViolationRecord> page, QueryWrapper<ViolationRecord> queryWrapper);

    ViolationRecord selectViolationById(Long id);

    /**
     * 管理员登记违规并立即扣分。
     */
    ViolationRecord createViolation(ViolationRecord record);

    /**
     * 撤销违规并回补信用分。
     */
    void revokeViolation(Long id, String reason);

    /**
     * 系统自动登记违规（爽约、超时取消等），不依赖登录上下文。
     */
    void recordSystemViolation(Long userId, Long reservationId, String violationType, String description,
            int creditChange);
}
