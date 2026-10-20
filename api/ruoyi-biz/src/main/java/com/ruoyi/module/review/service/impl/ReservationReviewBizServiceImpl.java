package com.ruoyi.module.review.service.impl;

import java.util.List;
import java.util.Date;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.module.common.service.ModuleDataScopeService;
import com.ruoyi.module.reservation.domain.Reservation;
import com.ruoyi.module.reservation.domain.ReservationCheckStatus;
import com.ruoyi.module.reservation.domain.ReservationStatus;
import com.ruoyi.module.reservation.domain.ReservationStatusAction;
import com.ruoyi.module.reservation.domain.ReservationStatusLog;
import com.ruoyi.module.reservation.domain.ReservationView;
import com.ruoyi.module.reservation.mapper.ReservationMapper;
import com.ruoyi.module.reservation.mapper.ReservationStatusLogMapper;
import com.ruoyi.module.reservation.service.IReservationRuleService;
import com.ruoyi.module.reservation.service.impl.ReservationViewAssembler;
import com.ruoyi.module.review.domain.ReservationReview;
import com.ruoyi.module.review.domain.ReservationReviewRequest;
import com.ruoyi.module.review.mapper.ReservationReviewMapper;
import com.ruoyi.module.review.service.IReservationReviewBizService;

import lombok.RequiredArgsConstructor;

/**
 * 预约审核流程Service业务层处理
 *
 * @author itjialu
 */
@Service
@RequiredArgsConstructor
public class ReservationReviewBizServiceImpl implements IReservationReviewBizService
{
    /** 审核结果：通过，与字典 module_review_result 一致 */
    public static final String RESULT_APPROVED = "1";

    /** 审核结果：驳回，与字典 module_review_result 一致 */
    public static final String RESULT_REJECTED = "2";

    private final ReservationMapper reservationMapper;

    private final ReservationReviewMapper reservationReviewMapper;

    private final ReservationStatusLogMapper reservationStatusLogMapper;

    private final IReservationRuleService reservationRuleService;

    private final ReservationViewAssembler reservationViewAssembler;

    private final ModuleDataScopeService moduleDataScopeService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void review(Long reservationId, ReservationReviewRequest request)
    {
        if (request == null || StringUtils.isEmpty(request.getResult()))
        {
            throw new ServiceException("请选择审核结果");
        }
        String action;
        if (RESULT_APPROVED.equals(request.getResult()))
        {
            action = ReservationStatusAction.APPROVE;
        }
        else if (RESULT_REJECTED.equals(request.getResult()))
        {
            action = ReservationStatusAction.REJECT;
        }
        else
        {
            throw new ServiceException("审核结果只能是通过或驳回");
        }
        updateStatus(reservationId, action, request.getComment());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long reservationId, String action, String comment)
    {
        if (reservationId == null)
        {
            throw new ServiceException("预约ID不能为空");
        }
        Reservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null)
        {
            throw new ServiceException("预约不存在");
        }
        moduleDataScopeService.checkStudentRecordAccess(reservation.getApplicantId());

        if (ReservationStatusAction.UNAUDIT.equals(action))
        {
            if (!ReservationStatus.APPROVED.equals(reservation.getStatus())
                    || !ReservationCheckStatus.HAD_CHECKED.equals(reservation.getCheckStatus()))
            {
                throw new ServiceException("只有已审核通过的预约可以弃审");
            }
            if (reservation.getStartTime() != null && !reservation.getStartTime().after(new Date()))
            {
                throw new ServiceException("预约已开始或已结束，不能弃审");
            }
            if (StringUtils.isEmpty(comment))
            {
                throw new ServiceException("弃审必须填写原因");
            }
            updateReservationStatus(reservationId, ReservationStatus.PENDING, ReservationCheckStatus.NO_CHECK,
                    null, null);
            logStatus(reservationId, ReservationStatus.APPROVED, ReservationStatus.PENDING,
                    ReservationStatusAction.UNAUDIT, comment);
            return;
        }

        if (!ReservationStatusAction.APPROVE.equals(action) && !ReservationStatusAction.REJECT.equals(action))
        {
            throw new ServiceException("审核动作不正确");
        }
        if (!ReservationStatus.PENDING.equals(reservation.getStatus())
                || ReservationCheckStatus.HAD_CHECKED.equals(reservation.getCheckStatus()))
        {
            throw new ServiceException("该预约不是待审核状态，不能重复审核");
        }
        if (reservation.getApplicantId() != null && reservation.getApplicantId().equals(SecurityUtils.getUserId()))
        {
            throw new ServiceException("不能审核本人提交的预约，请由其他教师或管理员处理");
        }
        if (ReservationStatusAction.REJECT.equals(action) && StringUtils.isEmpty(comment))
        {
            throw new ServiceException("驳回预约时必须填写审核意见");
        }
        if (ReservationStatusAction.APPROVE.equals(action))
        {
            if (reservation.getStartTime() != null && !reservation.getStartTime().after(new Date()))
            {
                throw new ServiceException("预约已开始或已结束，不能审核通过");
            }
            // 等待审核期间时段可能已被其它已通过的预约占用，通过前必须再校验一次
            reservationRuleService.checkApprovedConflict(reservation.getRoomId(), reservation.getStartTime(),
                    reservation.getEndTime(), reservation.getId());
        }

        ReservationReview review = new ReservationReview();
        review.setReservationId(reservationId);
        review.setReviewerId(SecurityUtils.getUserId());
        review.setResult(ReservationStatusAction.APPROVE.equals(action) ? RESULT_APPROVED : RESULT_REJECTED);
        review.setReviewComment(comment);
        review.setReviewTime(DateUtils.getNowDate());
        review.setCreateBy(SecurityUtils.getUsername());
        review.setCreateTime(DateUtils.getNowDate());
        reservationReviewMapper.insert(review);

        String checkName = SecurityUtils.getLoginUser().getUser().getNickName();
        if (StringUtils.isEmpty(checkName))
        {
            checkName = SecurityUtils.getUsername();
        }
        String newStatus = ReservationStatusAction.APPROVE.equals(action)
                ? ReservationStatus.APPROVED : ReservationStatus.REJECTED;
        updateReservationStatus(reservationId, newStatus, ReservationCheckStatus.HAD_CHECKED, checkName,
                DateUtils.getNowDate());
        logStatus(reservationId, ReservationStatus.PENDING, newStatus, action, comment);
    }

    private void updateReservationStatus(Long reservationId, String status, String checkStatus, String checkName,
            Date checkDate)
    {
        LambdaUpdateWrapper<Reservation> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Reservation::getId, reservationId)
                .set(Reservation::getStatus, status)
                .set(Reservation::getCheckStatus, checkStatus)
                .set(Reservation::getCheckName, checkName)
                .set(Reservation::getCheckDate, checkDate)
                .set(Reservation::getUpdateBy, SecurityUtils.getUsername())
                .set(Reservation::getUpdateTime, DateUtils.getNowDate());
        reservationMapper.update(null, updateWrapper);
    }

    @Override
    public IPage<ReservationView> pagePending(Page<Reservation> page)
    {
        QueryWrapper<Reservation> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", ReservationStatus.PENDING);
        moduleDataScopeService.applyStudentRecordScope(queryWrapper, "applicant_id");
        queryWrapper.orderByAsc("start_time");
        IPage<Reservation> pageData = reservationMapper.selectPage(page, queryWrapper);
        Page<ReservationView> viewPage = new Page<>(pageData.getCurrent(), pageData.getSize(), pageData.getTotal());
        viewPage.setRecords(reservationViewAssembler.assemble(pageData.getRecords()));
        return viewPage;
    }

    private void logStatus(Long reservationId, String fromStatus, String toStatus, String action, String reason)
    {
        ReservationStatusLog log = new ReservationStatusLog();
        log.setReservationId(reservationId);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setAction(action);
        log.setOperatorId(SecurityUtils.getUserId());
        log.setOperatorName(SecurityUtils.getUsername());
        log.setReason(reason);
        log.setCreateTime(DateUtils.getNowDate());
        reservationStatusLogMapper.insert(log);
    }
}
