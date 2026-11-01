package com.ruoyi.module.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.module.agent.config.AgentProperties;
import com.ruoyi.module.agent.domain.AgentReservationDraftCreateRequest;
import com.ruoyi.module.agent.domain.ReservationDraft;
import com.ruoyi.module.agent.domain.ReservationDraftView;
import com.ruoyi.module.agent.mapper.ReservationDraftMapper;
import com.ruoyi.module.agent.service.IReservationDraftService;
import com.ruoyi.module.device.domain.Device;
import com.ruoyi.module.reservation.service.IReservationService;
import com.ruoyi.module.room.domain.Room;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Agent 预约草稿服务实现。
 */
@Service
@RequiredArgsConstructor
public class ReservationDraftServiceImpl implements IReservationDraftService
{
    private static final String PENDING = "PENDING";

    private static final String CANCELED = "CANCELED";

    private static final String EXPIRED = "EXPIRED";

    private final ReservationDraftMapper draftMapper;

    private final IReservationService reservationService;

    private final AgentProperties properties;

    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReservationDraftView create(
            Long userId,
            String idempotencyKey,
            AgentReservationDraftCreateRequest request)
    {
        if (StringUtils.isEmpty(idempotencyKey))
        {
            throw new ServiceException("预约草稿幂等键不能为空");
        }
        ReservationDraft existing = findByKey(idempotencyKey);
        if (existing != null)
        {
            if (!userId.equals(existing.getUserId()))
            {
                throw new ServiceException("预约草稿幂等键已被占用");
            }
            expireIfNeeded(existing);
            return toView(existing);
        }

        Date startTime = parseDateTime(request.getStartTime());
        Date endTime = parseDateTime(request.getEndTime());
        validateAvailability(userId, request, startTime, endTime);

        ReservationDraft draft = new ReservationDraft();
        draft.setUserId(userId);
        draft.setSessionId(request.getSessionId());
        draft.setStatus(PENDING);
        draft.setExpiresAt(new Date(
                System.currentTimeMillis()
                        + TimeUnit.MINUTES.toMillis(Math.max(properties.getDraftTtlMinutes(), 1))));
        draft.setIdempotencyKey(idempotencyKey);
        draft.setCreateBy(SecurityUtils.getUsername());
        draft.setCreateTime(DateUtils.getNowDate());
        try
        {
            draft.setRequestJson(objectMapper.writeValueAsString(request));
            draftMapper.insert(draft);
        }
        catch (DuplicateKeyException exception)
        {
            ReservationDraft concurrent = findByKey(idempotencyKey);
            if (concurrent == null || !userId.equals(concurrent.getUserId()))
            {
                throw new ServiceException("预约草稿创建冲突，请重试");
            }
            return toView(concurrent);
        }
        catch (Exception exception)
        {
            throw new ServiceException("保存预约草稿失败");
        }
        return toView(draft);
    }

    @Override
    public ReservationDraftView get(Long draftId, Long userId)
    {
        return toView(requireOwned(draftId, userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReservationDraftView cancel(Long draftId, Long userId)
    {
        ReservationDraft draft = requireOwned(draftId, userId);
        if (!PENDING.equals(draft.getStatus()))
        {
            throw new ServiceException("只有待确认的预约草稿可以取消");
        }
        draft.setStatus(CANCELED);
        draft.setUpdateBy(SecurityUtils.getUsername());
        draft.setUpdateTime(DateUtils.getNowDate());
        draftMapper.updateById(draft);
        return toView(draft);
    }

    private ReservationDraft requireOwned(Long draftId, Long userId)
    {
        ReservationDraft draft = draftMapper.selectById(draftId);
        if (draft == null || !userId.equals(draft.getUserId()))
        {
            throw new ServiceException("预约草稿不存在或无权访问");
        }
        expireIfNeeded(draft);
        return draft;
    }

    private ReservationDraft findByKey(String idempotencyKey)
    {
        LambdaQueryWrapper<ReservationDraft> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReservationDraft::getIdempotencyKey, idempotencyKey);
        return draftMapper.selectOne(wrapper);
    }

    private void expireIfNeeded(ReservationDraft draft)
    {
        if (PENDING.equals(draft.getStatus())
                && draft.getExpiresAt() != null
                && draft.getExpiresAt().before(DateUtils.getNowDate()))
        {
            draft.setStatus(EXPIRED);
            draft.setUpdateTime(DateUtils.getNowDate());
            draftMapper.updateById(draft);
            throw new ServiceException("预约草稿已过期");
        }
    }

    private void validateAvailability(
            Long userId,
            AgentReservationDraftCreateRequest request,
            Date startTime,
            Date endTime)
    {
        if (!endTime.after(startTime))
        {
            throw new ServiceException("预约结束时间必须晚于开始时间");
        }
        Set<Long> availableRoomIds = new HashSet<>();
        for (Room room : reservationService.selectAvailableRooms(
                userId,
                startTime,
                endTime,
                request.getParticipantCount()))
        {
            availableRoomIds.add(room.getId());
        }
        if (!availableRoomIds.contains(request.getRoomId()))
        {
            throw new ServiceException("所选实验室在该时段不可预约");
        }
        if (request.getDeviceIds() == null || request.getDeviceIds().isEmpty())
        {
            return;
        }
        Set<Long> availableDeviceIds = new HashSet<>();
        for (Device device : reservationService.selectAvailableDevices(request.getRoomId()))
        {
            availableDeviceIds.add(device.getId());
        }
        for (Long deviceId : request.getDeviceIds())
        {
            if (deviceId == null || !availableDeviceIds.contains(deviceId))
            {
                throw new ServiceException("所选设备在该实验室不可用");
            }
        }
    }

    private ReservationDraftView toView(ReservationDraft draft)
    {
        try
        {
            return new ReservationDraftView(
                    draft.getId(),
                    draft.getSessionId(),
                    draft.getStatus(),
                    draft.getExpiresAt(),
                    draft.getConfirmedReservationId(),
                    objectMapper.readValue(
                            draft.getRequestJson(),
                            AgentReservationDraftCreateRequest.class));
        }
        catch (Exception exception)
        {
            throw new ServiceException("预约草稿数据损坏");
        }
    }

    private Date parseDateTime(String value)
    {
        String[] patterns = { "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd" };
        for (String pattern : patterns)
        {
            try
            {
                SimpleDateFormat formatter = new SimpleDateFormat(pattern);
                formatter.setLenient(false);
                return formatter.parse(value);
            }
            catch (ParseException ignored)
            {
            }
        }
        throw new ServiceException("时间格式不正确：" + value);
    }
}
