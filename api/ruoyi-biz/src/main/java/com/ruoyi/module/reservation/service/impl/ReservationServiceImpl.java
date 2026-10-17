package com.ruoyi.module.reservation.service.impl;

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.concurrent.ThreadLocalRandom;

import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.bean.BeanValidators;
import com.ruoyi.common.exception.ServiceException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
        import com.ruoyi.common.utils.DateUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.module.reservation.mapper.ReservationMapper;
import com.ruoyi.module.reservation.domain.Reservation;
import com.ruoyi.module.reservation.domain.ReservationStatus;
import com.ruoyi.module.reservation.domain.ReservationView;
import com.ruoyi.module.reservation.service.IReservationService;
import com.ruoyi.module.reservation.service.IReservationRuleService;
import com.ruoyi.module.room.domain.Room;

import jakarta.validation.Validator;

/**
 * 实验室预约Service业务层处理
 *
 * @author itjialu
 * @date 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class ReservationServiceImpl extends ServiceImpl<ReservationMapper,Reservation> implements IReservationService {
    private final ReservationMapper reservationMapper;
    private final IReservationRuleService reservationRuleService;
    private final ReservationViewAssembler reservationViewAssembler;
    protected final Validator validator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Reservation applyReservation(Reservation reservation)
    {
        Long userId = SecurityUtils.getUserId();
        Room room = reservationRuleService.checkApply(userId, reservation);
        boolean reviewRequired = reservationRuleService.isReviewRequired(room);

        Reservation apply = new Reservation();
        apply.setReservationNo(generateReservationNo(reservation.getStartTime()));
        apply.setApplicantId(userId);
        apply.setRoomId(room.getId());
        apply.setExperimentName(reservation.getExperimentName());
        apply.setStartTime(reservation.getStartTime());
        apply.setEndTime(reservation.getEndTime());
        apply.setParticipantCount(reservation.getParticipantCount());
        // 风险等级与是否需要审核由实验室安全等级决定，不接受前端传入
        apply.setRiskLevel(room.getSafetyLevel() == null ? 1L : room.getSafetyLevel());
        apply.setReviewRequired(reviewRequired ? "1" : "0");
        apply.setStatus(reviewRequired ? ReservationStatus.PENDING : ReservationStatus.APPROVED);
        apply.setRemark(reservation.getRemark());
        apply.setCreateBy(SecurityUtils.getUsername());
        apply.setCreateTime(DateUtils.getNowDate());
        reservationMapper.insert(apply);
        return apply;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cancelReservation(Long id)
    {
        Reservation reservation = reservationMapper.selectById(id);
        if (reservation == null)
        {
            throw new ServiceException("预约不存在");
        }
        reservationRuleService.checkCancel(reservation, SecurityUtils.getUserId());
        reservation.setStatus(ReservationStatus.CANCELED);
        reservation.setUpdateBy(SecurityUtils.getUsername());
        reservation.setUpdateTime(DateUtils.getNowDate());
        return reservationMapper.updateById(reservation);
    }

    @Override
    public IPage<ReservationView> pageMyReservation(Page<Reservation> page, String status, Date startTime, Date endTime)
    {
        QueryWrapper<Reservation> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("applicant_id", SecurityUtils.getUserId());
        if (StringUtils.isNotEmpty(status))
        {
            queryWrapper.eq("status", status);
        }
        if (startTime != null)
        {
            queryWrapper.ge("start_time", startTime);
        }
        if (endTime != null)
        {
            queryWrapper.le("end_time", endTime);
        }
        queryWrapper.orderByDesc("id");
        IPage<Reservation> pageData = reservationMapper.selectPage(page, queryWrapper);
        Page<ReservationView> viewPage = new Page<>(pageData.getCurrent(), pageData.getSize(), pageData.getTotal());
        viewPage.setRecords(reservationViewAssembler.assemble(pageData.getRecords()));
        return viewPage;
    }

    @Override
    public List<Room> selectAvailableRooms(Date startTime, Date endTime, Long participantCount)
    {
        return reservationRuleService.selectAvailableRooms(startTime, endTime, participantCount);
    }

    /**
     * 生成预约编号：RES + 开始日期 + 三位流水
     */
    private String generateReservationNo(Date startTime)
    {
        String day = new SimpleDateFormat("yyyyMMdd").format(startTime);
        for (int i = 0; i < 20; i++)
        {
            String reservationNo = "RES" + day + "-" + String.format("%03d", ThreadLocalRandom.current().nextInt(1000));
            QueryWrapper<Reservation> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("reservation_no", reservationNo);
            if (reservationMapper.selectList(queryWrapper).isEmpty())
            {
                return reservationNo;
            }
        }
        throw new ServiceException("预约编号生成失败，请重试");
    }

    @Override
    public String importReservation(
    List<Reservation> list, int titleNum, Boolean
    isUpdateSupport,
    String operName)

    {
        if (StringUtils.isNull(list) || list.size() == 0) {
            throw new ServiceException("导入数据不能为空！" );
        }
        int successNum = 0;
        int failureNum = 0;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder failureMsg = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            Reservation reservation =list.get(i);
            try {
                QueryWrapper<Reservation> queryWrapper = new QueryWrapper<>();
                List<Reservation> checkList = new ArrayList<>(); //reservationMapper.selectList(queryWrapper);
                if (checkList.size() == 0) {
                    BeanValidators.validateWithException(validator, reservation);
                    insertReservation(reservation);
                    successNum++;
                    successMsg.append("<br/>" + successNum + "、记录" + (i + titleNum + 2) + "：" + reservation.getId() + " 导入成功")
                    ;
                } else if (isUpdateSupport) {
                    BeanValidators.validateWithException(validator, reservation);
                    reservation.setId(checkList.get(0).getId());
                    updateReservation(reservation);
                    successNum++;
                    successMsg.append("<br/>" + successNum + "、记录" + (i + titleNum + 2) + " 更新成功")
                    ;
                } else {
                    failureNum++;
                    failureMsg.append("<br/>" + failureNum + "、记录" + (i + titleNum + 2) + " 已存在")
                    ;
                }
            } catch (Exception e) {
                failureNum++;
                String msg = "<br/>" + failureNum + "、记录" + (i + titleNum + 2) + " 导入失败：";
                failureMsg.append(msg + e.getMessage());
                log.error(msg, e);
            }
        }
        if (failureNum > 0) {
            failureMsg.insert(0, "很抱歉，导入失败！共 " + failureNum + " 条数据格式不正确，错误如下：" );
            throw new ServiceException(failureMsg.toString());
        } else {
            successMsg.insert(0, "恭喜您，数据已全部导入成功！共 " + successNum + " 条，数据如下：" );
        }
        return successMsg.toString();
    }

    @Override
    public IPage<Reservation> pageReservation(Page<Reservation> page, QueryWrapper<Reservation> queryWrapper) {
        return reservationMapper.selectPage(page, queryWrapper);
    }

    /**
     * 查询实验室预约
     *
     * @param id 实验室预约主键
     * @return 实验室预约
     */
    @Override
    public Reservation selectReservationById(Long id) {
        return reservationMapper.selectById(id);
    }

    /**
     * 新增实验室预约
     *
     * @param reservation 实验室预约
     * @return 结果
     */
    @Override
    public int insertReservation(Reservation reservation) {
                reservation.setCreateBy(SecurityUtils.getUsername());
                reservation.setCreateTime(DateUtils.getNowDate());
            return reservationMapper.insert(reservation);
    }

    /**
     * 修改实验室预约
     *
     * @param reservation 实验室预约
     * @return 结果
     */
    @Override
    public int updateReservation(Reservation reservation) {
                reservation.setUpdateTime(DateUtils.getNowDate());
                reservation.setUpdateBy(SecurityUtils.getUsername());
        return reservationMapper.updateById(reservation);
    }

    /**
     * 批量删除实验室预约
     *
     * @param ids 需要删除的实验室预约主键
     * @return 结果
     */
    @Override
    public int deleteReservationByIds(Long[] ids) {
        return reservationMapper.deleteByIds(Arrays.asList(ids));
    }

    /**
     * 删除实验室预约信息
     *
     * @param id 实验室预约主键
     * @return 结果
     */
    @Override
    public int deleteReservationById(Long id) {
        return reservationMapper.deleteById(id);
    }
}
