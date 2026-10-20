package com.ruoyi.module.credit.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DuplicateKeyException;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.module.credit.domain.CreditAccount;
import com.ruoyi.module.credit.domain.CreditRecord;
import com.ruoyi.module.credit.domain.CreditSummary;
import com.ruoyi.module.credit.mapper.CreditAccountMapper;
import com.ruoyi.module.credit.mapper.CreditRecordMapper;
import com.ruoyi.module.credit.service.ICreditService;
import com.ruoyi.system.service.ISysUserService;

import lombok.RequiredArgsConstructor;

/**
 * 信用分账户服务实现。
 *
 * @author itjialu
 */
@Service
@RequiredArgsConstructor
public class CreditServiceImpl implements ICreditService
{
    /** 允许预约的最低信用分 */
    public static final int MIN_BOOK_SCORE = 60;

    /** 没有信用记录时的初始分 */
    public static final int DEFAULT_SCORE = 100;

    private static final int MAX_SCORE = 100;

    private final CreditAccountMapper creditAccountMapper;

    private final CreditRecordMapper creditRecordMapper;

    private final ISysUserService sysUserService;

    @Override
    public int currentScore(Long userId)
    {
        CreditAccount account = creditAccountMapper.selectById(userId);
        if (account == null)
        {
            account = ensureAccount(userId);
        }
        return account.getScore() == null ? DEFAULT_SCORE : account.getScore();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int changeScore(Long userId, int delta, String reason, String bizType, Long bizId)
    {
        if (userId == null)
        {
            throw new ServiceException("用户ID不能为空");
        }
        if (delta == 0)
        {
            return currentScore(userId);
        }
        if (StringUtils.isEmpty(reason))
        {
            throw new ServiceException("信用分变动原因不能为空");
        }
        if (sysUserService.selectUserById(userId) == null)
        {
            throw new ServiceException("用户不存在");
        }

        CreditAccount account = creditAccountMapper.selectByUserIdForUpdate(userId);
        if (account == null)
        {
            account = ensureAccount(userId);
        }
        int oldScore = account.getScore() == null ? DEFAULT_SCORE : account.getScore();
        int newScore = Math.max(0, Math.min(MAX_SCORE, oldScore + delta));
        int actualDelta = newScore - oldScore;
        if (actualDelta == 0)
        {
            return oldScore;
        }

        account.setScore(newScore);
        account.setUpdateTime(DateUtils.getNowDate());
        creditAccountMapper.updateById(account);

        CreditRecord record = new CreditRecord();
        record.setUserId(userId);
        record.setChangeScore((long) actualDelta);
        record.setBalanceScore((long) newScore);
        record.setReason(reason);
        record.setBizType(bizType);
        record.setBizId(bizId);
        record.setCreateBy(currentOperator());
        record.setCreateTime(DateUtils.getNowDate());
        creditRecordMapper.insert(record);
        return newScore;
    }

    @Override
    public CreditSummary getSummary(Long userId)
    {
        CreditSummary summary = new CreditSummary();
        summary.setUserId(userId);
        summary.setScore(currentScore(userId));
        summary.setMinScore(MIN_BOOK_SCORE);
        summary.setCanBook(summary.getScore() >= MIN_BOOK_SCORE);
        return summary;
    }

    private CreditAccount ensureAccount(Long userId)
    {
        CreditAccount existing = creditAccountMapper.selectById(userId);
        if (existing != null)
        {
            return existing;
        }
        CreditAccount account = new CreditAccount();
        account.setUserId(userId);
        account.setScore(initialScoreFromLedger(userId));
        account.setUpdateTime(DateUtils.getNowDate());
        try
        {
            creditAccountMapper.insert(account);
            return account;
        }
        catch (DuplicateKeyException e)
        {
            // 并发首次初始化时另一个事务已经插入，直接复用已有账户
            CreditAccount concurrent = creditAccountMapper.selectById(userId);
            if (concurrent == null)
            {
                throw e;
            }
            return concurrent;
        }
    }

    private int initialScoreFromLedger(Long userId)
    {
        QueryWrapper<CreditRecord> query = new QueryWrapper<>();
        query.eq("user_id", userId).orderByDesc("id").last("limit 1");
        List<CreditRecord> records = creditRecordMapper.selectList(query);
        if (records.isEmpty() || records.get(0).getBalanceScore() == null)
        {
            return DEFAULT_SCORE;
        }
        return records.get(0).getBalanceScore().intValue();
    }

    private String currentOperator()
    {
        try
        {
            String username = SecurityUtils.getUsername();
            return StringUtils.isEmpty(username) ? "system" : username;
        }
        catch (Exception e)
        {
            return "system";
        }
    }
}
