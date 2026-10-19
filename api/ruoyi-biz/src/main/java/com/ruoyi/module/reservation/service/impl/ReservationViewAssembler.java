package com.ruoyi.module.reservation.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.module.reservation.domain.Reservation;
import com.ruoyi.module.reservation.domain.ReservationDevice;
import com.ruoyi.module.reservation.domain.ReservationView;
import com.ruoyi.module.reservation.mapper.ReservationDeviceMapper;
import com.ruoyi.module.reservation.service.IReservationRuleService;
import com.ruoyi.module.review.domain.ReservationReview;
import com.ruoyi.module.review.mapper.ReservationReviewMapper;
import com.ruoyi.module.device.domain.Device;
import com.ruoyi.module.device.mapper.DeviceMapper;
import com.ruoyi.module.room.domain.Room;
import com.ruoyi.module.room.mapper.RoomMapper;
import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.system.service.ISysDeptService;
import com.ruoyi.system.service.ISysUserService;

import lombok.RequiredArgsConstructor;

/**
 * 预约展示对象装配器：把预约记录补全为页面需要的展示数据
 *
 * @author itjialu
 */
@Component
@RequiredArgsConstructor
public class ReservationViewAssembler
{
    private final RoomMapper roomMapper;

    private final ReservationReviewMapper reservationReviewMapper;

    private final ReservationDeviceMapper reservationDeviceMapper;

    private final DeviceMapper deviceMapper;

    private final IReservationRuleService reservationRuleService;

    private final ISysUserService sysUserService;

    private final ISysDeptService sysDeptService;

    public List<ReservationView> assemble(List<Reservation> reservations)
    {
        if (reservations == null || reservations.isEmpty())
        {
            return Collections.emptyList();
        }
        Set<Long> roomIds = new LinkedHashSet<>();
        Set<Long> applicantIds = new LinkedHashSet<>();
        Set<Long> reservationIds = new LinkedHashSet<>();
        Set<Long> classDeptIds = new LinkedHashSet<>();
        for (Reservation reservation : reservations)
        {
            reservationIds.add(reservation.getId());
            if (reservation.getRoomId() != null)
            {
                roomIds.add(reservation.getRoomId());
            }
            if (reservation.getApplicantId() != null)
            {
                applicantIds.add(reservation.getApplicantId());
            }
            if (reservation.getClassDeptId() != null)
            {
                classDeptIds.add(reservation.getClassDeptId());
            }
        }

        Map<Long, Room> roomMap = new HashMap<>();
        if (!roomIds.isEmpty())
        {
            for (Room room : roomMapper.selectBatchIds(roomIds))
            {
                roomMap.put(room.getId(), room);
            }
        }

        Map<Long, String> applicantMap = new HashMap<>();
        for (Long applicantId : applicantIds)
        {
            SysUser user = sysUserService.selectUserById(applicantId);
            if (user != null)
            {
                applicantMap.put(applicantId, StringUtils.isNotEmpty(user.getNickName()) ? user.getNickName()
                        : user.getUserName());
            }
        }

        Map<Long, String> classDeptNameMap = new HashMap<>();
        for (Long classDeptId : classDeptIds)
        {
            SysDept dept = sysDeptService.selectDeptById(classDeptId);
            if (dept != null)
            {
                classDeptNameMap.put(classDeptId, dept.getDeptName());
            }
        }

        Map<Long, ReservationReview> reviewMap = new HashMap<>();
        QueryWrapper<ReservationReview> reviewQuery = new QueryWrapper<>();
        reviewQuery.in("reservation_id", reservationIds);
        reviewQuery.orderByDesc("id");
        for (ReservationReview review : reservationReviewMapper.selectList(reviewQuery))
        {
            // 同一预约保留最近一次审核记录
            reviewMap.putIfAbsent(review.getReservationId(), review);
        }

        Map<Long, List<String>> deviceNameMap = new HashMap<>();
        QueryWrapper<ReservationDevice> deviceQuery = new QueryWrapper<>();
        deviceQuery.in("reservation_id", reservationIds);
        List<ReservationDevice> reservationDevices = reservationDeviceMapper.selectList(deviceQuery);
        if (!reservationDevices.isEmpty())
        {
            Set<Long> deviceIds = new LinkedHashSet<>();
            for (ReservationDevice reservationDevice : reservationDevices)
            {
                deviceIds.add(reservationDevice.getDeviceId());
            }
            Map<Long, String> deviceMap = new HashMap<>();
            for (Device device : deviceMapper.selectBatchIds(deviceIds))
            {
                deviceMap.put(device.getId(), device.getDeviceName());
            }
            for (ReservationDevice reservationDevice : reservationDevices)
            {
                String deviceName = deviceMap.get(reservationDevice.getDeviceId());
                if (StringUtils.isNotEmpty(deviceName))
                {
                    deviceNameMap.computeIfAbsent(reservationDevice.getReservationId(), key -> new ArrayList<>())
                            .add(deviceName);
                }
            }
        }

        List<ReservationView> views = new ArrayList<>(reservations.size());
        for (Reservation reservation : reservations)
        {
            ReservationView view = new ReservationView();
            view.setId(reservation.getId());
            view.setReservationNo(reservation.getReservationNo());
            view.setApplicantId(reservation.getApplicantId());
            view.setRoomId(reservation.getRoomId());
            view.setExperimentName(reservation.getExperimentName());
            view.setStartTime(reservation.getStartTime());
            view.setEndTime(reservation.getEndTime());
            view.setParticipantCount(reservation.getParticipantCount());
            view.setReservationType(reservation.getReservationType());
            view.setClassDeptId(reservation.getClassDeptId());
            view.setClassDeptName(classDeptNameMap.get(reservation.getClassDeptId()));
            view.setRiskLevel(reservation.getRiskLevel());
            view.setStatus(reservation.getStatus());
            view.setCheckStatus(reservation.getCheckStatus());
            view.setCheckName(reservation.getCheckName());
            view.setCheckDate(reservation.getCheckDate());
            view.setReviewRequired(reservation.getReviewRequired());
            view.setRemark(reservation.getRemark());
            view.setCreateBy(reservation.getCreateBy());
            view.setCreateTime(reservation.getCreateTime());

            Room room = roomMap.get(reservation.getRoomId());
            if (room != null)
            {
                view.setRoomCode(room.getRoomCode());
                view.setRoomName(room.getRoomName());
            }
            view.setApplicantName(applicantMap.get(reservation.getApplicantId()));
            if (reservation.getApplicantId() != null)
            {
                view.setExamValid(reservationRuleService.isExamValid(reservation.getApplicantId()));
                view.setCreditScore(reservationRuleService.currentCreditScore(reservation.getApplicantId()));
            }
            ReservationReview review = reviewMap.get(reservation.getId());
            if (review != null)
            {
                view.setReviewResult(review.getResult());
                view.setReviewComment(review.getReviewComment());
                view.setReviewTime(review.getReviewTime());
            }
            view.setDeviceNames(deviceNameMap.getOrDefault(reservation.getId(), Collections.emptyList()));
            views.add(view);
        }
        return views;
    }
}
