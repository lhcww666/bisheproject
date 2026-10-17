package com.ruoyi.module.review.service.impl;

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
import com.ruoyi.module.common.service.ModuleDataScopeService;
import com.ruoyi.module.reservation.domain.Reservation;
import com.ruoyi.module.reservation.domain.ReservationStatus;
import com.ruoyi.module.reservation.domain.ReservationView;
import com.ruoyi.module.reservation.mapper.ReservationMapper;
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

    private final IReservationRuleService reservationRuleService;

    private final ReservationViewAssembler reservationViewAssembler;

    private final ModuleDataScopeService moduleDataScopeService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void review(Long reservationId, ReservationReviewRequest request)
    {
        Reservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null)
        {
            throw new ServiceException("预约不存在");
        }
        if (!ReservationStatus.PENDING.equals(reservation.getStatus()))
        {
            throw new ServiceException("该预约不是待审核状态，不能重复审核");
        }
        moduleDataScopeService.checkStudentRecordAccess(reservation.getApplicantId());
        if (request == null || StringUtils.isEmpty(request.getResult()))
        {
            throw new ServiceException("请选择审核结果");
        }
        boolean approved = RESULT_APPROVED.equals(request.getResult());
        boolean rejected = RESULT_REJECTED.equals(request.getResult());
        if (!approved && !rejected)
        {
            throw new ServiceException("审核结果只能是通过或驳回");
        }
        if (rejected && StringUtils.isEmpty(request.getComment()))
        {
            throw new ServiceException("驳回预约时必须填写审核意见");
        }
        if (reservation.getApplicantId() != null && reservation.getApplicantId().equals(SecurityUtils.getUserId()))
        {
            throw new ServiceException("不能审核本人提交的预约，请由其他教师或管理员处理");
        }
        if (approved)
        {
            // 等待审核期间时段可能已被其它已通过的预约占用，通过前必须再校验一次
            reservationRuleService.checkApprovedConflict(reservation.getRoomId(), reservation.getStartTime(),
                    reservation.getEndTime(), reservation.getId());
        }

        ReservationReview review = new ReservationReview();
        review.setReservationId(reservationId);
        review.setReviewerId(SecurityUtils.getUserId());
        review.setResult(request.getResult());
        review.setReviewComment(request.getComment());
        review.setReviewTime(DateUtils.getNowDate());
        reservationReviewMapper.insert(review);

        reservation.setStatus(approved ? ReservationStatus.APPROVED : ReservationStatus.REJECTED);
        reservation.setUpdateBy(SecurityUtils.getUsername());
        reservation.setUpdateTime(DateUtils.getNowDate());
        reservationMapper.updateById(reservation);
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
}
