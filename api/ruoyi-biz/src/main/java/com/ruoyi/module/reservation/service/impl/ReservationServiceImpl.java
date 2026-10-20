package com.ruoyi.module.reservation.service.impl;

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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
import com.ruoyi.module.reservation.mapper.ReservationDeviceMapper;
import com.ruoyi.module.reservation.mapper.ReservationStatusLogMapper;
import com.ruoyi.module.review.mapper.ReservationReviewMapper;
import com.ruoyi.module.review.domain.ReservationReview;
import com.ruoyi.module.reservation.domain.Reservation;
import com.ruoyi.module.reservation.domain.ReservationDevice;
import com.ruoyi.module.reservation.domain.ReservationStatus;
import com.ruoyi.module.reservation.domain.ReservationStatusAction;
import com.ruoyi.module.reservation.domain.ReservationCheckStatus;
import com.ruoyi.module.reservation.domain.ReservationType;
import com.ruoyi.module.reservation.domain.ReservationView;
import com.ruoyi.module.reservation.domain.ReservationStatusLog;
import com.ruoyi.module.reservation.service.IReservationService;
import com.ruoyi.module.reservation.service.IReservationRuleService;
import com.ruoyi.module.device.domain.Device;
import com.ruoyi.module.device.mapper.DeviceMapper;
import com.ruoyi.module.room.domain.Room;
import com.ruoyi.module.room.mapper.RoomMapper;
import com.ruoyi.module.violation.domain.ViolationType;
import com.ruoyi.module.violation.domain.ViolationRecord;
import com.ruoyi.module.violation.mapper.ViolationRecordMapper;
import com.ruoyi.module.violation.service.IViolationRecordService;

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
    private final ReservationDeviceMapper reservationDeviceMapper;
    private final ReservationReviewMapper reservationReviewMapper;
    private final ReservationStatusLogMapper reservationStatusLogMapper;
    private final DeviceMapper deviceMapper;
    private final RoomMapper roomMapper;
    private final IReservationRuleService reservationRuleService;
    private final ReservationViewAssembler reservationViewAssembler;
    private final IViolationRecordService violationRecordService;
    private final ViolationRecordMapper violationRecordMapper;
    protected final Validator validator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Reservation applyReservation(Reservation reservation)
    {
        Long userId = SecurityUtils.getUserId();
        String reservationType = StringUtils.isEmpty(reservation.getReservationType())
                ? ReservationType.INDIVIDUAL : reservation.getReservationType().trim().toUpperCase();
        if (!ReservationType.isValid(reservationType))
        {
            throw new ServiceException("预约类型不正确");
        }
        reservation.setReservationType(reservationType);
        if (ReservationType.CLASS.equals(reservationType))
        {
            // 班级ID由登录用户所属班级决定，不信任前端传入值
            reservation.setClassDeptId(SecurityUtils.getDeptId());
        }
        else
        {
            reservation.setClassDeptId(null);
        }
        // 同一实验室的冲突检查和插入串行执行，避免两个请求同时通过校验后写出重叠预约
        roomMapper.selectByIdForUpdate(reservation.getRoomId());
        Room room = reservationRuleService.checkApply(userId, reservation);
        Reservation apply = new Reservation();
        apply.setReservationNo(generateReservationNo(reservation.getStartTime()));
        apply.setApplicantId(userId);
        apply.setRoomId(room.getId());
        apply.setExperimentName(reservation.getExperimentName());
        apply.setStartTime(reservation.getStartTime());
        apply.setEndTime(reservation.getEndTime());
        apply.setParticipantCount(reservation.getParticipantCount());
        apply.setReservationType(reservationType);
        apply.setClassDeptId(reservation.getClassDeptId());
        // 风险等级由实验室安全等级决定，不接受前端传入
        apply.setRiskLevel(room.getSafetyLevel() == null ? 1L : room.getSafetyLevel());
        boolean reviewRequired = reservationRuleService.isReviewRequired(room);
        apply.setStatus(reviewRequired ? ReservationStatus.PENDING : ReservationStatus.APPROVED);
        apply.setReviewRequired(reviewRequired ? "1" : "0");
        apply.setCheckStatus(reviewRequired ? ReservationCheckStatus.NO_CHECK : ReservationCheckStatus.HAD_CHECKED);
        apply.setCheckName(reviewRequired ? null : "系统自动通过");
        apply.setCheckDate(reviewRequired ? null : DateUtils.getNowDate());
        apply.setRemark(reservation.getRemark());
        apply.setCreateBy(SecurityUtils.getUsername());
        apply.setCreateTime(DateUtils.getNowDate());
        reservationMapper.insert(apply);
        saveReservationDevices(apply.getId(), room, userId, reservation.getDeviceIds());
        logStatus(apply.getId(), null, apply.getStatus(),
                reviewRequired ? ReservationStatusAction.CREATE : ReservationStatusAction.AUTO_APPROVE,
                reviewRequired ? "提交预约申请" : "低风险预约自动通过");
        return apply;
    }

    /**
     * 保存预约与设备的关联。设备必须属于预约实验室、状态正常；
     * 需要培训的设备还要求申请人持有有效期内的安全考试记录。
     */
    private void saveReservationDevices(Long reservationId, Room room, Long userId, List<Long> deviceIds)
    {
        if (deviceIds == null || deviceIds.isEmpty())
        {
            return;
        }
        Set<Long> uniqueDeviceIds = new LinkedHashSet<>(deviceIds);
        for (Long deviceId : uniqueDeviceIds)
        {
            if (deviceId == null)
            {
                throw new ServiceException("设备不能为空");
            }
            Device device = deviceMapper.selectById(deviceId);
            if (device == null)
            {
                throw new ServiceException("选择的设备不存在");
            }
            if (!room.getId().equals(device.getRoomId()))
            {
                throw new ServiceException("设备“" + device.getDeviceName() + "”不属于所选实验室");
            }
            if (!"0".equals(device.getStatus()))
            {
                throw new ServiceException("设备“" + device.getDeviceName() + "”当前不可用");
            }
            if (device.getQuantity() == null || device.getQuantity() < 1L)
            {
                throw new ServiceException("设备“" + device.getDeviceName() + "”没有可用数量");
            }
            if ("1".equals(device.getRequiresTraining()) && !reservationRuleService.isExamValid(userId))
            {
                throw new ServiceException("设备“" + device.getDeviceName() + "”需要培训，请先通过安全考试");
            }

            ReservationDevice reservationDevice = new ReservationDevice();
            reservationDevice.setReservationId(reservationId);
            reservationDevice.setDeviceId(deviceId);
            reservationDevice.setQuantity(1L);
            reservationDevice.setCreateBy(SecurityUtils.getUsername());
            reservationDevice.setCreateTime(DateUtils.getNowDate());
            reservationDeviceMapper.insert(reservationDevice);
        }
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
        Date now = DateUtils.getNowDate();
        boolean lateCancel = ReservationStatus.APPROVED.equals(reservation.getStatus())
                && reservation.getStartTime() != null
                && reservation.getStartTime().getTime() - now.getTime() <= 120 * 60_000L;
        String fromStatus = reservation.getStatus();
        reservation.setStatus(ReservationStatus.CANCELED);
        reservation.setCancelTime(now);
        reservation.setCancelReason(lateCancel ? "开始前 2 小时内取消" : "学生取消预约");
        reservation.setUpdateBy(SecurityUtils.getUsername());
        reservation.setUpdateTime(now);
        int rows = reservationMapper.updateById(reservation);
        logStatus(reservation.getId(), fromStatus, ReservationStatus.CANCELED, ReservationStatusAction.CANCEL,
                reservation.getCancelReason());
        if (lateCancel)
        {
            violationRecordService.recordSystemViolation(reservation.getApplicantId(), reservation.getId(),
                    ViolationType.LATE_CANCEL, "预约开始前 2 小时内取消", -2);
        }
        return rows;
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
    public List<Room> selectAvailableRooms(Long userId, Date startTime, Date endTime, Long participantCount)
    {
        return reservationRuleService.selectAvailableRooms(userId, startTime, endTime, participantCount);
    }

    @Override
    public List<Device> selectAvailableDevices(Long roomId)
    {
        if (roomId == null)
        {
            throw new ServiceException("请选择实验室");
        }
        QueryWrapper<Device> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("room_id", roomId);
        queryWrapper.eq("status", "0");
        queryWrapper.orderByAsc("device_code");
        return deviceMapper.selectList(queryWrapper);
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
    public IPage<ReservationView> pageReservation(Page<Reservation> page, QueryWrapper<Reservation> queryWrapper) {
        IPage<Reservation> pageData = reservationMapper.selectPage(page, queryWrapper);
        Page<ReservationView> viewPage = new Page<>(pageData.getCurrent(), pageData.getSize(), pageData.getTotal());
        viewPage.setRecords(reservationViewAssembler.assemble(pageData.getRecords()));
        return viewPage;
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

    @Override
    public ReservationView selectReservationDetail(Long id) {
        Reservation reservation = reservationMapper.selectById(id);
        if (reservation == null) {
            return null;
        }
        return reservationViewAssembler.assembleDetail(reservation);
    }

    /**
     * 新增实验室预约
     *
     * @param reservation 实验室预约
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertReservation(Reservation reservation) {
        normalizeReservationType(reservation, true);
        roomMapper.selectByIdForUpdate(reservation.getRoomId());
        reservationRuleService.checkAdminReservation(reservation);
        Room room = roomMapper.selectById(reservation.getRoomId());
        boolean reviewRequired = reservationRuleService.isReviewRequired(room);
        reservation.setRiskLevel(room.getSafetyLevel() == null ? 1L : room.getSafetyLevel());
        reservation.setStatus(reviewRequired ? ReservationStatus.PENDING : ReservationStatus.APPROVED);
        reservation.setReviewRequired(reviewRequired ? "1" : "0");
        reservation.setCheckStatus(reviewRequired ? ReservationCheckStatus.NO_CHECK : ReservationCheckStatus.HAD_CHECKED);
        reservation.setCheckName(reviewRequired ? null : "系统自动通过");
        reservation.setCheckDate(reviewRequired ? null : DateUtils.getNowDate());
        reservation.setCreateBy(SecurityUtils.getUsername());
        reservation.setCreateTime(DateUtils.getNowDate());
        int rows = reservationMapper.insert(reservation);
        saveReservationDevices(reservation.getId(), room, reservation.getApplicantId(), reservation.getDeviceIds());
        logStatus(reservation.getId(), null, reservation.getStatus(),
                reviewRequired ? ReservationStatusAction.CREATE : ReservationStatusAction.AUTO_APPROVE,
                "管理员新增预约");
        return rows;
    }

    /**
     * 修改实验室预约
     *
     * @param reservation 实验室预约
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateReservation(Reservation reservation) {
        Reservation existing = reservationMapper.selectById(reservation.getId());
        if (existing == null) {
            throw new ServiceException("预约不存在");
        }
        if (!ReservationStatus.PENDING.equals(existing.getStatus())
                && !ReservationStatus.REJECTED.equals(existing.getStatus())) {
            throw new ServiceException("只有待审核或已驳回的预约可以修改");
        }
        normalizeReservationType(reservation, false);
        roomMapper.selectByIdForUpdate(reservation.getRoomId());
        reservationRuleService.checkAdminReservation(reservation);
        Room room = roomMapper.selectById(reservation.getRoomId());
        boolean reviewRequired = reservationRuleService.isReviewRequired(room);
        reservation.setRiskLevel(room.getSafetyLevel() == null ? 1L : room.getSafetyLevel());
        reservation.setStatus(reviewRequired ? ReservationStatus.PENDING : ReservationStatus.APPROVED);
        reservation.setReviewRequired(reviewRequired ? "1" : "0");
        reservation.setCheckStatus(reviewRequired ? ReservationCheckStatus.NO_CHECK : ReservationCheckStatus.HAD_CHECKED);
        reservation.setCheckName(reviewRequired ? null : "系统自动通过");
        reservation.setCheckDate(reviewRequired ? null : DateUtils.getNowDate());
        reservation.setUpdateTime(DateUtils.getNowDate());
        reservation.setUpdateBy(SecurityUtils.getUsername());
        if (reservation.getDeviceIds() != null) {
            QueryWrapper<ReservationDevice> deviceQuery = new QueryWrapper<>();
            deviceQuery.eq("reservation_id", reservation.getId());
            reservationDeviceMapper.delete(deviceQuery);
            saveReservationDevices(reservation.getId(), room, reservation.getApplicantId(),
                    reservation.getDeviceIds());
        }
        int rows = reservationMapper.updateById(reservation);
        if (ReservationStatus.APPROVED.equals(reservation.getStatus()))
        {
            ReservationReview review = new ReservationReview();
            review.setReservationId(reservation.getId());
            review.setReviewerId(SecurityUtils.getUserId());
            review.setResult("1");
            review.setReviewComment("修改后系统自动通过");
            review.setReviewTime(DateUtils.getNowDate());
            review.setCreateBy(SecurityUtils.getUsername());
            review.setCreateTime(DateUtils.getNowDate());
            reservationReviewMapper.insert(review);
        }
        logStatus(reservation.getId(), existing.getStatus(), reservation.getStatus(),
                ReservationStatusAction.EDIT, "管理员修改预约，重新进入审核流程");
        return rows;
    }

    /**
     * 批量删除实验室预约
     *
     * @param ids 需要删除的实验室预约主键
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteReservationByIds(Long[] ids) {
        for (Long id : ids) {
            Reservation existing = reservationMapper.selectById(id);
            if (existing != null && (ReservationStatus.APPROVED.equals(existing.getStatus())
                    || ReservationStatus.CHECKED_IN.equals(existing.getStatus()))) {
                throw new ServiceException("已通过或已签到的预约不能直接删除，请先取消或弃审");
            }
            QueryWrapper<ViolationRecord> violationQuery = new QueryWrapper<>();
            violationQuery.eq("reservation_id", id);
            if (violationRecordMapper.selectCount(violationQuery) > 0) {
                throw new ServiceException("该预约存在违规记录，不能删除，请保留审计数据");
            }
        }
        List<Long> idList = Arrays.asList(ids);
        QueryWrapper<ReservationStatusLog> logQuery = new QueryWrapper<>();
        logQuery.in("reservation_id", idList);
        reservationStatusLogMapper.delete(logQuery);
        QueryWrapper<ReservationDevice> deviceQuery = new QueryWrapper<>();
        deviceQuery.in("reservation_id", idList);
        reservationDeviceMapper.delete(deviceQuery);
        QueryWrapper<ReservationReview> reviewQuery = new QueryWrapper<>();
        reviewQuery.in("reservation_id", idList);
        reservationReviewMapper.delete(reviewQuery);
        return reservationMapper.deleteByIds(idList);
    }

    /**
     * 删除实验室预约信息
     *
     * @param id 实验室预约主键
     * @return 结果
     */
    @Override
    public int deleteReservationById(Long id) {
        return deleteReservationByIds(new Long[] { id });
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

    private void normalizeReservationType(Reservation reservation, boolean applyDefault)
    {
        if (StringUtils.isEmpty(reservation.getReservationType()))
        {
            if (applyDefault)
            {
                reservation.setReservationType(ReservationType.INDIVIDUAL);
            }
            return;
        }
        if (!ReservationType.isValid(reservation.getReservationType()))
        {
            throw new ServiceException("预约类型不正确");
        }
    }
}
