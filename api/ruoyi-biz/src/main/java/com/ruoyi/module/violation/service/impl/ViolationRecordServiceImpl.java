package com.ruoyi.module.violation.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.module.credit.service.ICreditService;
import com.ruoyi.module.reservation.domain.Reservation;
import com.ruoyi.module.reservation.mapper.ReservationMapper;
import com.ruoyi.module.violation.domain.ViolationRecord;
import com.ruoyi.module.violation.domain.ViolationStatus;
import com.ruoyi.module.violation.domain.ViolationType;
import com.ruoyi.module.violation.mapper.ViolationRecordMapper;
import com.ruoyi.module.violation.service.IViolationRecordService;
import com.ruoyi.system.service.ISysDeptService;
import com.ruoyi.system.service.ISysUserService;

import lombok.RequiredArgsConstructor;

/**
 * 违规记录Service实现。
 *
 * @author itjialu
 */
@Service
@RequiredArgsConstructor
public class ViolationRecordServiceImpl implements IViolationRecordService
{
    private final ViolationRecordMapper violationRecordMapper;

    private final ICreditService creditService;

    private final ISysUserService sysUserService;

    private final ISysDeptService sysDeptService;

    private final ReservationMapper reservationMapper;

    @Override
    public IPage<ViolationRecord> pageViolation(Page<ViolationRecord> page,
            QueryWrapper<ViolationRecord> queryWrapper)
    {
        IPage<ViolationRecord> pageData = violationRecordMapper.selectPage(page, queryWrapper);
        fillDisplayFields(pageData.getRecords());
        return pageData;
    }

    @Override
    public ViolationRecord selectViolationById(Long id)
    {
        ViolationRecord record = violationRecordMapper.selectById(id);
        if (record != null)
        {
            fillDisplayFields(List.of(record));
        }
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ViolationRecord createViolation(ViolationRecord record)
    {
        validate(record);
        record.setStatus(ViolationStatus.HANDLED);
        record.setHandlerId(SecurityUtils.getUserId());
        record.setHandleTime(DateUtils.getNowDate());
        record.setCreateBy(SecurityUtils.getUsername());
        record.setCreateTime(DateUtils.getNowDate());
        return insertAndApply(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeViolation(Long id, String reason)
    {
        if (StringUtils.isEmpty(reason))
        {
            throw new ServiceException("撤销原因不能为空");
        }
        ViolationRecord record = violationRecordMapper.selectById(id);
        if (record == null)
        {
            throw new ServiceException("违规记录不存在");
        }
        if (!ViolationStatus.HANDLED.equals(record.getStatus()))
        {
            throw new ServiceException("只有已处理的违规记录可以撤销");
        }
        record.setStatus(ViolationStatus.REVOKED);
        record.setUpdateBy(SecurityUtils.getUsername());
        record.setUpdateTime(DateUtils.getNowDate());
        record.setRemark(reason);
        violationRecordMapper.updateById(record);

        int reversal = record.getCreditChange() == null ? 0 : -record.getCreditChange();
        if (reversal != 0)
        {
            creditService.changeScore(record.getUserId(), reversal, "撤销违规：" + reason,
                    "VIOLATION_REVOKE", record.getId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordSystemViolation(Long userId, Long reservationId, String violationType, String description,
            int creditChange)
    {
        ViolationRecord record = new ViolationRecord();
        record.setUserId(userId);
        record.setReservationId(reservationId);
        record.setViolationType(violationType);
        record.setDescription(description);
        record.setCreditChange(creditChange);
        record.setStatus(ViolationStatus.HANDLED);
        record.setHandlerId(0L);
        record.setHandleTime(DateUtils.getNowDate());
        record.setCreateBy("system");
        record.setCreateTime(DateUtils.getNowDate());
        insertAndApply(record);
    }

    private ViolationRecord insertAndApply(ViolationRecord record)
    {
        violationRecordMapper.insert(record);
        if (record.getCreditChange() != null && record.getCreditChange() != 0)
        {
            int before = creditService.currentScore(record.getUserId());
            String reason = "违规：" + violationLabel(record.getViolationType())
                    + (StringUtils.isEmpty(record.getDescription()) ? "" : " - " + record.getDescription());
            int after = creditService.changeScore(record.getUserId(), record.getCreditChange(), reason,
                    "VIOLATION", record.getId());
            int actualChange = after - before;
            if (actualChange != record.getCreditChange())
            {
                // 信用分被 0/100 边界截断时，记录实际变动值，撤销时才能准确回补
                record.setCreditChange(actualChange);
                violationRecordMapper.updateById(record);
            }
        }
        return record;
    }

    private void validate(ViolationRecord record)
    {
        if (record == null)
        {
            throw new ServiceException("违规记录不能为空");
        }
        if (record.getUserId() == null)
        {
            throw new ServiceException("学生不能为空");
        }
        if (sysUserService.selectUserById(record.getUserId()) == null)
        {
            throw new ServiceException("学生不存在");
        }
        if (StringUtils.isEmpty(record.getViolationType()))
        {
            throw new ServiceException("违规类型不能为空");
        }
        if (record.getCreditChange() == null)
        {
            throw new ServiceException("信用变动不能为空");
        }
        if (record.getCreditChange() > 0)
        {
            throw new ServiceException("违规记录的信用变动不能为正数");
        }
        if (record.getReservationId() != null)
        {
            Reservation reservation = reservationMapper.selectById(record.getReservationId());
            if (reservation == null)
            {
                throw new ServiceException("关联预约不存在");
            }
            if (!record.getUserId().equals(reservation.getApplicantId()))
            {
                throw new ServiceException("关联预约不属于该学生");
            }
        }
    }

    private String violationLabel(String type)
    {
        if (ViolationType.NO_SHOW.equals(type))
        {
            return "爽约";
        }
        if (ViolationType.LATE_CANCEL.equals(type))
        {
            return "临近开始取消";
        }
        if (ViolationType.SAFETY_VIOLATION.equals(type))
        {
            return "安全违规";
        }
        if (ViolationType.EQUIPMENT_DAMAGE.equals(type))
        {
            return "设备损坏";
        }
        return "其他违规";
    }

    private void fillDisplayFields(List<ViolationRecord> records)
    {
        if (records == null || records.isEmpty())
        {
            return;
        }
        for (ViolationRecord record : records)
        {
            SysUser user = sysUserService.selectUserById(record.getUserId());
            if (user != null)
            {
                record.setNickName(StringUtils.isNotEmpty(user.getNickName()) ? user.getNickName()
                        : user.getUserName());
                record.setUserName(user.getUserName());
                SysDept dept = sysDeptService.selectDeptById(user.getDeptId());
                record.setDeptName(dept == null ? null : dept.getDeptName());
            }
            if (record.getReservationId() != null)
            {
                Reservation reservation = reservationMapper.selectById(record.getReservationId());
                if (reservation != null)
                {
                    record.setReservationNo(reservation.getReservationNo());
                }
            }
        }
    }
}
