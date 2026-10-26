package com.ruoyi.module.material.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.module.common.service.ModuleDataScopeService;
import com.ruoyi.module.material.domain.Material;
import com.ruoyi.module.material.domain.MaterialApply;
import com.ruoyi.module.material.domain.MaterialApplyStatus;
import com.ruoyi.module.material.domain.MaterialReservationOption;
import com.ruoyi.module.material.domain.MaterialReviewRequest;
import com.ruoyi.module.material.domain.MaterialStatus;
import com.ruoyi.module.material.domain.MaterialStockChangeType;
import com.ruoyi.module.material.domain.MaterialStockRecord;
import com.ruoyi.module.material.mapper.MaterialApplyMapper;
import com.ruoyi.module.material.mapper.MaterialMapper;
import com.ruoyi.module.material.mapper.MaterialStockRecordMapper;
import com.ruoyi.module.material.service.IMaterialApplyService;
import com.ruoyi.module.reservation.domain.Reservation;
import com.ruoyi.module.reservation.domain.ReservationStatus;
import com.ruoyi.module.reservation.mapper.ReservationMapper;
import com.ruoyi.module.room.domain.Room;
import com.ruoyi.module.room.mapper.RoomMapper;
import com.ruoyi.system.service.ISysUserService;

import lombok.RequiredArgsConstructor;

/**
 * 耗材领用申请服务实现。
 */
@Service
@RequiredArgsConstructor
public class MaterialApplyServiceImpl implements IMaterialApplyService
{
    private final MaterialApplyMapper materialApplyMapper;

    private final MaterialMapper materialMapper;

    private final MaterialStockRecordMapper stockRecordMapper;

    private final ReservationMapper reservationMapper;

    private final RoomMapper roomMapper;

    private final ISysUserService sysUserService;

    private final ModuleDataScopeService moduleDataScopeService;

    @Override
    public IPage<MaterialApply> pageApply(Page<MaterialApply> page,
            QueryWrapper<MaterialApply> queryWrapper)
    {
        queryWrapper.orderByDesc("id");
        IPage<MaterialApply> pageData = materialApplyMapper.selectPage(page, queryWrapper);
        fillDisplayFields(pageData.getRecords());
        return pageData;
    }

    @Override
    public MaterialApply selectApplyById(Long id)
    {
        MaterialApply apply = materialApplyMapper.selectById(id);
        if (apply != null)
        {
            fillDisplayFields(List.of(apply));
        }
        return apply;
    }

    @Override
    public List<MaterialReservationOption> selectReservationOptions()
    {
        requireStudent();
        QueryWrapper<Reservation> query = new QueryWrapper<>();
        query.eq("applicant_id", SecurityUtils.getUserId())
                .in("status", ReservationStatus.PENDING, ReservationStatus.APPROVED)
                .orderByAsc("start_time");
        List<Reservation> reservations = reservationMapper.selectList(query);
        if (reservations.isEmpty())
        {
            return Collections.emptyList();
        }
        Set<Long> roomIds = new LinkedHashSet<>();
        for (Reservation reservation : reservations)
        {
            roomIds.add(reservation.getRoomId());
        }
        Map<Long, Room> roomMap = new HashMap<>();
        for (Room room : roomMapper.selectBatchIds(roomIds))
        {
            roomMap.put(room.getId(), room);
        }
        List<MaterialReservationOption> options = new ArrayList<>(reservations.size());
        for (Reservation reservation : reservations)
        {
            MaterialReservationOption option = new MaterialReservationOption();
            option.setId(reservation.getId());
            option.setReservationNo(reservation.getReservationNo());
            option.setExperimentName(reservation.getExperimentName());
            option.setRoomId(reservation.getRoomId());
            option.setStatus(reservation.getStatus());
            option.setStartTime(reservation.getStartTime());
            option.setEndTime(reservation.getEndTime());
            Room room = roomMap.get(reservation.getRoomId());
            if (room != null)
            {
                option.setRoomCode(room.getRoomCode());
                option.setRoomName(room.getRoomName());
            }
            options.add(option);
        }
        return options;
    }

    @Override
    public List<Material> selectMaterialOptions(Long reservationId)
    {
        requireStudent();
        Reservation reservation = requireApplicableReservation(reservationId);
        Room room = roomMapper.selectById(reservation.getRoomId());
        if (room == null || !"0".equals(room.getStatus()))
        {
            throw new ServiceException("预约实验室当前不可用");
        }
        QueryWrapper<Material> query = new QueryWrapper<>();
        query.eq("room_id", reservation.getRoomId())
                .eq("status", MaterialStatus.ACTIVE)
                .orderByAsc("material_code");
        return materialMapper.selectList(query);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MaterialApply applyMaterial(MaterialApply request)
    {
        requireStudent();
        if (request == null || request.getReservationId() == null || request.getMaterialId() == null)
        {
            throw new ServiceException("预约和耗材不能为空");
        }
        Reservation reservation = materialApplyMapper.selectReservationForUpdate(request.getReservationId());
        if (reservation == null)
        {
            throw new ServiceException("预约不存在");
        }
        assertReservationApplicable(reservation);
        Room room = roomMapper.selectById(reservation.getRoomId());
        if (room == null || !"0".equals(room.getStatus()))
        {
            throw new ServiceException("预约实验室当前不可用");
        }
        Material material = materialMapper.selectById(request.getMaterialId());
        if (material == null)
        {
            throw new ServiceException("耗材不存在");
        }
        if (!MaterialStatus.ACTIVE.equals(material.getStatus()))
        {
            throw new ServiceException("耗材已停用，不能申请");
        }
        if (!reservation.getRoomId().equals(material.getRoomId()))
        {
            throw new ServiceException("所选耗材不属于预约实验室");
        }
        BigDecimal quantity = normalizeQuantity(request.getApplyQuantity());
        if (quantity.compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new ServiceException("申请数量必须大于0");
        }
        if (normalizeQuantity(material.getStockQuantity()).compareTo(quantity) < 0)
        {
            throw new ServiceException("当前库存不足，无法提交领用申请");
        }
        QueryWrapper<MaterialApply> duplicateQuery = new QueryWrapper<>();
        duplicateQuery.eq("reservation_id", reservation.getId())
                .eq("material_id", material.getId())
                .in("status", MaterialApplyStatus.PENDING, MaterialApplyStatus.APPROVED);
        if (materialApplyMapper.selectCount(duplicateQuery) > 0)
        {
            throw new ServiceException("该预约已对同一耗材提交过未结束的领用申请");
        }

        MaterialApply apply = new MaterialApply();
        apply.setApplyNo(generateApplyNo());
        apply.setReservationId(reservation.getId());
        apply.setApplicantId(reservation.getApplicantId());
        apply.setMaterialId(material.getId());
        apply.setApplyQuantity(quantity);
        apply.setStatus(MaterialApplyStatus.PENDING);
        apply.setCreateBy(currentOperator());
        apply.setCreateTime(DateUtils.getNowDate());
        materialApplyMapper.insert(apply);
        return selectApplyById(apply.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MaterialApply review(Long id, MaterialReviewRequest request)
    {
        if (!moduleDataScopeService.isAdmin())
        {
            throw new ServiceException("只有管理员可以审核耗材领用申请");
        }
        if (request == null)
        {
            throw new ServiceException("审核信息不能为空");
        }
        boolean approve = MaterialApplyStatus.APPROVED.equals(request.getStatus());
        boolean reject = MaterialApplyStatus.REJECTED.equals(request.getStatus());
        if (!approve && !reject)
        {
            throw new ServiceException("审核结果不正确");
        }
        if (reject && StringUtils.isEmpty(request.getReviewComment()))
        {
            throw new ServiceException("驳回时必须填写审核意见");
        }
        MaterialApply apply = materialApplyMapper.selectByIdForUpdate(id);
        if (apply == null)
        {
            throw new ServiceException("领用申请不存在");
        }
        if (!MaterialApplyStatus.PENDING.equals(apply.getStatus()))
        {
            throw new ServiceException("该申请已处理，不能重复审核");
        }
        if (reject)
        {
            updateReviewResult(apply, MaterialApplyStatus.REJECTED, request.getReviewComment());
            return selectApplyById(id);
        }

        Reservation reservation = materialApplyMapper.selectReservationForUpdate(apply.getReservationId());
        if (reservation == null || !ReservationStatus.isOccupied(reservation.getStatus()))
        {
            return autoReject(apply, "关联预约已失效，系统自动驳回");
        }
        if (!apply.getApplicantId().equals(reservation.getApplicantId()))
        {
            return autoReject(apply, "申请人与预约申请人不一致，系统自动驳回");
        }
        Material material = materialMapper.selectByIdForUpdate(apply.getMaterialId());
        if (material == null || !MaterialStatus.ACTIVE.equals(material.getStatus()))
        {
            return autoReject(apply, "耗材不存在或已停用，系统自动驳回");
        }
        if (!reservation.getRoomId().equals(material.getRoomId()))
        {
            return autoReject(apply, "耗材与预约实验室不一致，系统自动驳回");
        }
        BigDecimal quantity = normalizeQuantity(apply.getApplyQuantity());
        BigDecimal before = normalizeQuantity(material.getStockQuantity());
        BigDecimal delta = quantity.negate();
        int rows = materialMapper.applyStockDelta(material.getId(), delta, currentOperator());
        if (rows == 0)
        {
            return autoReject(apply, "库存不足，系统自动驳回");
        }
        BigDecimal after = before.add(delta);
        updateReviewResult(apply, MaterialApplyStatus.APPROVED, request.getReviewComment());
        apply.setIssueTime(DateUtils.getNowDate());
        materialApplyMapper.updateById(apply);
        writeRecord(material.getId(), MaterialStockChangeType.APPLY_OUT, delta, before, after,
                apply.getId(), apply.getReservationId(), "耗材领用审核通过");
        return selectApplyById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseForCanceledReservation(Long reservationId)
    {
        QueryWrapper<MaterialApply> query = new QueryWrapper<>();
        query.eq("reservation_id", reservationId)
                .in("status", MaterialApplyStatus.PENDING, MaterialApplyStatus.APPROVED)
                .orderByAsc("id");
        List<MaterialApply> applications = materialApplyMapper.selectList(query);
        for (MaterialApply listed : applications)
        {
            MaterialApply apply = materialApplyMapper.selectByIdForUpdate(listed.getId());
            if (apply == null)
            {
                continue;
            }
            if (MaterialApplyStatus.PENDING.equals(apply.getStatus()))
            {
                apply.setStatus(MaterialApplyStatus.CANCELED);
                apply.setReviewComment("预约已取消，系统关闭领用申请");
                apply.setReviewTime(DateUtils.getNowDate());
                apply.setUpdateBy(currentOperator());
                apply.setUpdateTime(DateUtils.getNowDate());
                materialApplyMapper.updateById(apply);
            }
            else if (MaterialApplyStatus.APPROVED.equals(apply.getStatus()))
            {
                Material material = materialMapper.selectByIdForUpdate(apply.getMaterialId());
                if (material == null)
                {
                    throw new ServiceException("归还耗材不存在，请检查库存数据");
                }
                BigDecimal quantity = normalizeQuantity(apply.getApplyQuantity());
                BigDecimal before = normalizeQuantity(material.getStockQuantity());
                BigDecimal after = before.add(quantity);
                int rows = materialMapper.applyStockDelta(material.getId(), quantity, currentOperator());
                if (rows == 0)
                {
                    throw new ServiceException("归还库存失败，请刷新后重试");
                }
                apply.setStatus(MaterialApplyStatus.RETURNED);
                apply.setReturnTime(DateUtils.getNowDate());
                apply.setUpdateBy(currentOperator());
                apply.setUpdateTime(DateUtils.getNowDate());
                materialApplyMapper.updateById(apply);
                writeRecord(material.getId(), MaterialStockChangeType.CANCEL_RETURN, quantity, before, after,
                        apply.getId(), apply.getReservationId(), "预约取消，归还已领用耗材");
            }
        }
    }

    @Override
    public boolean hasRecordsForReservation(Long reservationId)
    {
        QueryWrapper<MaterialApply> query = new QueryWrapper<>();
        query.eq("reservation_id", reservationId);
        return materialApplyMapper.selectCount(query) > 0;
    }

    private Reservation requireApplicableReservation(Long reservationId)
    {
        Reservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null)
        {
            throw new ServiceException("预约不存在");
        }
        assertReservationApplicable(reservation);
        return reservation;
    }

    private void assertReservationApplicable(Reservation reservation)
    {
        if (!SecurityUtils.getUserId().equals(reservation.getApplicantId()))
        {
            throw new ServiceException("只能对本人预约申请耗材");
        }
        if (!ReservationStatus.PENDING.equals(reservation.getStatus())
                && !ReservationStatus.APPROVED.equals(reservation.getStatus()))
        {
            throw new ServiceException("只有待审核或已通过的预约可以申请耗材");
        }
    }

    private void requireStudent()
    {
        if (!moduleDataScopeService.isStudent())
        {
            throw new ServiceException("只有学生可以提交耗材领用申请");
        }
    }

    private MaterialApply autoReject(MaterialApply apply, String comment)
    {
        updateReviewResult(apply, MaterialApplyStatus.REJECTED, comment);
        return selectApplyById(apply.getId());
    }

    private void updateReviewResult(MaterialApply apply, String status, String comment)
    {
        apply.setStatus(status);
        apply.setReviewerId(currentOperatorId());
        apply.setReviewComment(comment);
        apply.setReviewTime(DateUtils.getNowDate());
        apply.setUpdateBy(currentOperator());
        apply.setUpdateTime(DateUtils.getNowDate());
        materialApplyMapper.updateById(apply);
    }

    private void writeRecord(Long materialId, String type, BigDecimal change, BigDecimal before,
            BigDecimal after, Long applyId, Long reservationId, String reason)
    {
        MaterialStockRecord record = new MaterialStockRecord();
        record.setMaterialId(materialId);
        record.setChangeType(type);
        record.setChangeQuantity(normalizeQuantity(change));
        record.setBeforeQuantity(normalizeQuantity(before));
        record.setAfterQuantity(normalizeQuantity(after));
        record.setApplyId(applyId);
        record.setReservationId(reservationId);
        record.setOperatorId(currentOperatorId());
        record.setOperatorName(currentOperator());
        record.setReason(reason);
        record.setCreateTime(DateUtils.getNowDate());
        stockRecordMapper.insert(record);
    }

    private void fillDisplayFields(List<MaterialApply> applications)
    {
        if (applications == null || applications.isEmpty())
        {
            return;
        }
        Set<Long> reservationIds = new LinkedHashSet<>();
        Set<Long> materialIds = new LinkedHashSet<>();
        Set<Long> userIds = new LinkedHashSet<>();
        for (MaterialApply apply : applications)
        {
            reservationIds.add(apply.getReservationId());
            materialIds.add(apply.getMaterialId());
            userIds.add(apply.getApplicantId());
            if (apply.getReviewerId() != null)
            {
                userIds.add(apply.getReviewerId());
            }
        }
        Map<Long, Reservation> reservationMap = new HashMap<>();
        for (Reservation reservation : reservationMapper.selectBatchIds(reservationIds))
        {
            reservationMap.put(reservation.getId(), reservation);
        }
        Map<Long, Material> materialMap = new HashMap<>();
        Set<Long> roomIds = new LinkedHashSet<>();
        for (Material material : materialMapper.selectBatchIds(materialIds))
        {
            materialMap.put(material.getId(), material);
            roomIds.add(material.getRoomId());
        }
        for (Reservation reservation : reservationMap.values())
        {
            if (reservation.getRoomId() != null)
            {
                roomIds.add(reservation.getRoomId());
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
        Map<Long, String> userNameMap = new HashMap<>();
        for (Long userId : userIds)
        {
            SysUser user = sysUserService.selectUserById(userId);
            if (user != null)
            {
                userNameMap.put(userId, displayName(user));
            }
        }
        for (MaterialApply apply : applications)
        {
            Reservation reservation = reservationMap.get(apply.getReservationId());
            Material material = materialMap.get(apply.getMaterialId());
            if (reservation != null)
            {
                apply.setReservationNo(reservation.getReservationNo());
            }
            if (material != null)
            {
                apply.setMaterialCode(material.getMaterialCode());
                apply.setMaterialName(material.getMaterialName());
                apply.setSpecification(material.getSpecification());
                apply.setUnit(material.getUnit());
                apply.setRoomId(material.getRoomId());
                apply.setCurrentStock(material.getStockQuantity());
                Room room = roomMap.get(material.getRoomId());
                if (room != null)
                {
                    apply.setRoomCode(room.getRoomCode());
                    apply.setRoomName(room.getRoomName());
                }
            }
            apply.setApplicantName(userNameMap.get(apply.getApplicantId()));
            apply.setReviewerName(userNameMap.get(apply.getReviewerId()));
        }
    }

    private String displayName(SysUser user)
    {
        return StringUtils.isNotEmpty(user.getNickName()) ? user.getNickName() : user.getUserName();
    }

    private String generateApplyNo()
    {
        String day = new SimpleDateFormat("yyyyMMdd").format(new Date());
        for (int i = 0; i < 30; i++)
        {
            String applyNo = "MAT" + day + "-" + String.format("%05d",
                    ThreadLocalRandom.current().nextInt(100000));
            QueryWrapper<MaterialApply> query = new QueryWrapper<>();
            query.eq("apply_no", applyNo);
            if (materialApplyMapper.selectCount(query) == 0)
            {
                return applyNo;
            }
        }
        throw new ServiceException("领用申请编号生成失败，请稍后重试");
    }

    private BigDecimal normalizeQuantity(BigDecimal value)
    {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private Long currentOperatorId()
    {
        try
        {
            return SecurityUtils.getUserId();
        }
        catch (Exception ignored)
        {
            return null;
        }
    }

    private String currentOperator()
    {
        try
        {
            String username = SecurityUtils.getUsername();
            return StringUtils.isEmpty(username) ? "system" : username;
        }
        catch (Exception ignored)
        {
            return "system";
        }
    }
}
