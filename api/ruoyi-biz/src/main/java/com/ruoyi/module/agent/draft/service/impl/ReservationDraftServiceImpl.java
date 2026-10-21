package com.ruoyi.module.agent.draft.service.impl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.module.agent.draft.domain.ReservationDraft;
import com.ruoyi.module.agent.draft.domain.ReservationDraftRequest;
import com.ruoyi.module.agent.draft.domain.ReservationDraftView;
import com.ruoyi.module.agent.draft.mapper.ReservationDraftMapper;
import com.ruoyi.module.agent.draft.service.IReservationDraftService;
import com.ruoyi.module.device.domain.Device;
import com.ruoyi.module.device.mapper.DeviceMapper;
import com.ruoyi.module.reservation.domain.Reservation;
import com.ruoyi.module.reservation.domain.ReservationType;
import com.ruoyi.module.reservation.service.IReservationRuleService;
import com.ruoyi.module.reservation.service.IReservationService;
import com.ruoyi.module.room.domain.Room;
import com.ruoyi.module.room.mapper.RoomMapper;

import lombok.RequiredArgsConstructor;

/**
 * 预约草稿服务实现。
 *
 * <p>创建草稿时先做预校验，让用户尽早知道条件不满足；确认时再次调用
 * ReservationService.applyReservation 做完整校验，防止草稿创建后状态发生变化。</p>
 *
 * @author itjialu
 */
@Service
@RequiredArgsConstructor
public class ReservationDraftServiceImpl implements IReservationDraftService
{
    private static final int EXPIRE_MINUTES = 15;

    private final ReservationDraftMapper draftMapper;

    private final IReservationRuleService reservationRuleService;

    private final IReservationService reservationService;

    private final RoomMapper roomMapper;

    private final DeviceMapper deviceMapper;

    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReservationDraftView createDraft(Long userId, Long sessionId, ReservationDraftRequest request)
    {
        if (request == null)
        {
            throw new ServiceException("预约草稿不能为空");
        }
        if (request.getRoomId() == null)
        {
            throw new ServiceException("请选择实验室");
        }
        if (StringUtils.isEmpty(request.getExperimentName()))
        {
            throw new ServiceException("请填写实验名称");
        }
        if (request.getParticipantCount() == null || request.getParticipantCount() < 1)
        {
            throw new ServiceException("参与人数必须大于 0");
        }
        Reservation reservation = toReservation(request);
        // 创建草稿时先做一次预校验，减少用户确认后才看到失败的情况。
        Room room = reservationRuleService.checkApply(userId, reservation);
        List<Long> deviceIds = validateDevices(userId, room.getId(), request.getDeviceIds());

        ReservationDraft draft = new ReservationDraft();
        draft.setUserId(userId);
        draft.setSessionId(sessionId);
        draft.setRoomId(room.getId());
        draft.setExperimentName(request.getExperimentName());
        draft.setStartTime(reservation.getStartTime());
        draft.setEndTime(reservation.getEndTime());
        draft.setParticipantCount(request.getParticipantCount());
        draft.setDeviceIds(writeIds(deviceIds));
        draft.setRemark(request.getRemark());
        draft.setStatus("DRAFT");
        Date now = new Date();
        draft.setCreateTime(now);
        draft.setUpdateTime(now);
        draft.setExpiresAt(new Date(now.getTime() + EXPIRE_MINUTES * 60_000L));
        draftMapper.insert(draft);
        return toView(draft);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReservationDraftView confirmDraft(Long draftId, Long userId)
    {
        ReservationDraft draft = requireDraft(draftId, userId);
        if (!"DRAFT".equals(draft.getStatus()))
        {
            throw new ServiceException("该预约草稿当前不能确认");
        }
        if (draft.getExpiresAt() != null && draft.getExpiresAt().before(new Date()))
        {
            draft.setStatus("EXPIRED");
            draft.setUpdateTime(new Date());
            draftMapper.updateById(draft);
            throw new ServiceException("预约草稿已过期，请让助手重新生成");
        }

        Reservation reservation = new Reservation();
        reservation.setRoomId(draft.getRoomId());
        reservation.setExperimentName(draft.getExperimentName());
        reservation.setStartTime(draft.getStartTime());
        reservation.setEndTime(draft.getEndTime());
        reservation.setParticipantCount(draft.getParticipantCount().longValue());
        reservation.setReservationType(ReservationType.INDIVIDUAL);
        reservation.setDeviceIds(readIds(draft.getDeviceIds()));
        reservation.setRemark(draft.getRemark());
        // 确认时再次走完整业务规则，不信任草稿创建时的校验结果。
        Reservation created = reservationService.applyReservation(reservation);
        draft.setStatus("CONFIRMED");
        draft.setReservationId(created.getId());
        draft.setReservationNo(created.getReservationNo());
        draft.setUpdateTime(new Date());
        draftMapper.updateById(draft);
        ReservationDraftView view = toView(draft);
        view.setReservationStatus(created.getStatus());
        return view;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReservationDraftView cancelDraft(Long draftId, Long userId)
    {
        ReservationDraft draft = requireDraft(draftId, userId);
        if (!"DRAFT".equals(draft.getStatus()))
        {
            throw new ServiceException("只有待确认的草稿可以取消");
        }
        draft.setStatus("CANCELED");
        draft.setUpdateTime(new Date());
        draftMapper.updateById(draft);
        return toView(draft);
    }

    @Override
    public ReservationDraftView getDraft(Long draftId, Long userId)
    {
        return toView(requireDraft(draftId, userId));
    }

    @Override
    public List<ReservationDraftView> listPendingDrafts(Long userId, Long sessionId)
    {
        QueryWrapper<ReservationDraft> query = new QueryWrapper<>();
        query.eq("user_id", userId).eq("status", "DRAFT").gt("expires_at", new Date());
        if (sessionId != null)
        {
            query.eq("session_id", sessionId);
        }
        query.orderByDesc("id");
        List<ReservationDraftView> result = new ArrayList<>();
        for (ReservationDraft draft : draftMapper.selectList(query))
        {
            result.add(toView(draft));
        }
        return result;
    }

    private ReservationDraft requireDraft(Long draftId, Long userId)
    {
        ReservationDraft draft = draftMapper.selectById(draftId);
        if (draft == null || !userId.equals(draft.getUserId()))
        {
            throw new ServiceException("预约草稿不存在或无权访问");
        }
        return draft;
    }

    private Reservation toReservation(ReservationDraftRequest request)
    {
        Reservation reservation = new Reservation();
        reservation.setRoomId(request.getRoomId());
        reservation.setExperimentName(request.getExperimentName());
        reservation.setStartTime(parse(request.getStartTime()));
        reservation.setEndTime(parse(request.getEndTime()));
        reservation.setParticipantCount(request.getParticipantCount().longValue());
        reservation.setReservationType(ReservationType.INDIVIDUAL);
        reservation.setRemark(request.getRemark());
        return reservation;
    }

    /**
     * 校验设备属于实验室、状态可用、需要培训时必须已有有效考试记录。
     */
    private List<Long> validateDevices(Long userId, Long roomId, List<Long> requested)
    {
        if (requested == null || requested.isEmpty())
        {
            return List.of();
        }
        List<Long> result = new ArrayList<>();
        for (Long deviceId : new LinkedHashSet<>(requested))
        {
            Device device = deviceMapper.selectById(deviceId);
            if (device == null)
            {
                throw new ServiceException("设备不存在：" + deviceId);
            }
            if (!roomId.equals(device.getRoomId()))
            {
                throw new ServiceException("设备“" + device.getDeviceName() + "”不属于所选实验室");
            }
            if (!"0".equals(device.getStatus()) || device.getQuantity() == null || device.getQuantity() < 1)
            {
                throw new ServiceException("设备“" + device.getDeviceName() + "”当前不可用");
            }
            if ("1".equals(device.getRequiresTraining()) && !reservationRuleService.isExamValid(userId))
            {
                throw new ServiceException("设备“" + device.getDeviceName() + "”需要培训，请先通过安全考试");
            }
            result.add(deviceId);
        }
        return result;
    }

    private ReservationDraftView toView(ReservationDraft draft)
    {
        ReservationDraftView view = new ReservationDraftView();
        view.setId(draft.getId());
        view.setRoomId(draft.getRoomId());
        Room room = roomMapper.selectById(draft.getRoomId());
        view.setRoomName(room == null ? null : room.getRoomName());
        view.setExperimentName(draft.getExperimentName());
        view.setStartTime(draft.getStartTime());
        view.setEndTime(draft.getEndTime());
        view.setParticipantCount(draft.getParticipantCount());
        List<Long> ids = readIds(draft.getDeviceIds());
        view.setDeviceIds(ids);
        List<String> names = new ArrayList<>();
        for (Long id : ids)
        {
            Device device = deviceMapper.selectById(id);
            if (device != null)
            {
                names.add(device.getDeviceName());
            }
        }
        view.setDeviceNames(names);
        view.setRemark(draft.getRemark());
        view.setStatus(draft.getStatus());
        view.setReservationId(draft.getReservationId());
        view.setReservationNo(draft.getReservationNo());
        view.setExpiresAt(draft.getExpiresAt());
        return view;
    }

    private String writeIds(List<Long> ids)
    {
        try
        {
            return objectMapper.writeValueAsString(ids == null ? List.of() : ids);
        }
        catch (Exception e)
        {
            throw new ServiceException("设备列表格式不正确");
        }
    }

    private List<Long> readIds(String json)
    {
        if (StringUtils.isEmpty(json))
        {
            return List.of();
        }
        try
        {
            return objectMapper.readValue(json, new TypeReference<List<Long>>()
            {
            });
        }
        catch (Exception e)
        {
            return List.of();
        }
    }

    private Date parse(String text)
    {
        String[] patterns = { "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd" };
        for (String pattern : patterns)
        {
            try
            {
                SimpleDateFormat format = new SimpleDateFormat(pattern);
                format.setLenient(false);
                return format.parse(text);
            }
            catch (Exception ignored)
            {
                // 尝试下一种格式
            }
        }
        throw new ServiceException("时间格式应为 yyyy-MM-dd HH:mm:ss：" + text);
    }
}
