package com.ruoyi.module.material.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.module.material.domain.Material;
import com.ruoyi.module.material.domain.MaterialApply;
import com.ruoyi.module.material.domain.MaterialStatus;
import com.ruoyi.module.material.domain.MaterialStockAdjustRequest;
import com.ruoyi.module.material.domain.MaterialStockChangeType;
import com.ruoyi.module.material.domain.MaterialStockRecord;
import com.ruoyi.module.material.mapper.MaterialApplyMapper;
import com.ruoyi.module.material.mapper.MaterialMapper;
import com.ruoyi.module.material.mapper.MaterialStockRecordMapper;
import com.ruoyi.module.material.service.IMaterialService;
import com.ruoyi.module.room.domain.Room;
import com.ruoyi.module.room.mapper.RoomMapper;

import lombok.RequiredArgsConstructor;

/**
 * 耗材档案与库存服务实现。
 */
@Service
@RequiredArgsConstructor
public class MaterialServiceImpl implements IMaterialService
{
    private final MaterialMapper materialMapper;

    private final MaterialApplyMapper materialApplyMapper;

    private final MaterialStockRecordMapper stockRecordMapper;

    private final RoomMapper roomMapper;

    @Override
    public IPage<Material> pageMaterial(Page<Material> page, QueryWrapper<Material> queryWrapper)
    {
        IPage<Material> pageData = materialMapper.selectPage(page, queryWrapper);
        fillRoomNames(pageData.getRecords());
        return pageData;
    }

    @Override
    public Material selectMaterialById(Long id)
    {
        Material material = materialMapper.selectById(id);
        if (material != null)
        {
            fillRoomNames(List.of(material));
        }
        return material;
    }

    @Override
    public List<Room> selectRoomOptions()
    {
        QueryWrapper<Room> query = new QueryWrapper<>();
        query.eq("status", "0").orderByAsc("room_code");
        return roomMapper.selectList(query);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertMaterial(Material material)
    {
        normalizeMaterial(material, true);
        validateUniqueCode(material.getMaterialCode(), null);
        material.setCreateBy(currentOperator());
        material.setCreateTime(DateUtils.getNowDate());
        int rows = materialMapper.insert(material);
        if (material.getStockQuantity().compareTo(BigDecimal.ZERO) > 0)
        {
            writeRecord(material.getId(), MaterialStockChangeType.INITIAL, material.getStockQuantity(),
                    zero(), material.getStockQuantity(), null, null, "耗材初始化库存");
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateMaterial(Material material)
    {
        if (material.getId() == null)
        {
            throw new ServiceException("耗材ID不能为空");
        }
        Material existing = materialMapper.selectById(material.getId());
        if (existing == null)
        {
            throw new ServiceException("耗材不存在");
        }
        normalizeMaterial(material, false);
        validateUniqueCode(material.getMaterialCode(), material.getId());
        // 档案编辑不能绕过库存流水直接改库存
        material.setStockQuantity(existing.getStockQuantity());
        material.setUpdateBy(currentOperator());
        material.setUpdateTime(DateUtils.getNowDate());
        return materialMapper.updateById(material);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteMaterialByIds(Long[] ids)
    {
        if (ids == null || ids.length == 0)
        {
            throw new ServiceException("请选择要删除的耗材");
        }
        for (Long id : ids)
        {
            QueryWrapper<MaterialApply> applyQuery = new QueryWrapper<>();
            applyQuery.eq("material_id", id);
            if (materialApplyMapper.selectCount(applyQuery) > 0)
            {
                throw new ServiceException("耗材存在领用申请记录，不能删除；请改为停用");
            }
            QueryWrapper<MaterialStockRecord> recordQuery = new QueryWrapper<>();
            recordQuery.eq("material_id", id);
            if (stockRecordMapper.selectCount(recordQuery) > 0)
            {
                throw new ServiceException("耗材存在库存流水，不能删除；请改为停用");
            }
        }
        return materialMapper.deleteBatchIds(Arrays.asList(ids));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjustStock(Long id, MaterialStockAdjustRequest request)
    {
        if (request == null || !MaterialStockChangeType.isAdjustment(request.getChangeType()))
        {
            throw new ServiceException("库存调整方向不正确");
        }
        Material material = materialMapper.selectByIdForUpdate(id);
        if (material == null)
        {
            throw new ServiceException("耗材不存在");
        }
        BigDecimal quantity = normalizeQuantity(request.getQuantity());
        if (quantity.compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new ServiceException("调整数量必须大于0");
        }
        BigDecimal delta = MaterialStockChangeType.ADJUST_IN.equals(request.getChangeType())
                ? quantity : quantity.negate();
        BigDecimal before = normalizeQuantity(material.getStockQuantity());
        BigDecimal after = before.add(delta);
        if (after.compareTo(BigDecimal.ZERO) < 0)
        {
            throw new ServiceException("库存不足，调整后库存不能小于0");
        }
        int rows = materialMapper.applyStockDelta(id, delta, currentOperator());
        if (rows == 0)
        {
            throw new ServiceException("库存已发生变化，请刷新后重试");
        }
        writeRecord(id, request.getChangeType(), delta, before, after, null, null, request.getReason());
    }

    private void normalizeMaterial(Material material, boolean creating)
    {
        if (material == null)
        {
            throw new ServiceException("耗材信息不能为空");
        }
        material.setMaterialCode(StringUtils.trim(material.getMaterialCode()));
        material.setMaterialName(StringUtils.trim(material.getMaterialName()));
        material.setSpecification(StringUtils.trim(material.getSpecification()));
        material.setUnit(StringUtils.trim(material.getUnit()));
        if (!MaterialStatus.isValid(material.getStatus()))
        {
            throw new ServiceException("耗材状态不正确");
        }
        Room room = roomMapper.selectById(material.getRoomId());
        if (room == null)
        {
            throw new ServiceException("所属实验室不存在");
        }
        if (creating)
        {
            material.setStockQuantity(normalizeQuantity(
                    material.getStockQuantity() == null ? BigDecimal.ZERO : material.getStockQuantity()));
        }
        material.setMinimumStock(normalizeQuantity(
                material.getMinimumStock() == null ? BigDecimal.ZERO : material.getMinimumStock()));
    }

    private void validateUniqueCode(String materialCode, Long excludeId)
    {
        QueryWrapper<Material> query = new QueryWrapper<>();
        query.eq("material_code", materialCode);
        if (excludeId != null)
        {
            query.ne("id", excludeId);
        }
        if (materialMapper.selectCount(query) > 0)
        {
            throw new ServiceException("耗材编号已存在");
        }
    }

    private void fillRoomNames(List<Material> materials)
    {
        if (materials == null || materials.isEmpty())
        {
            return;
        }
        Set<Long> roomIds = new LinkedHashSet<>();
        for (Material material : materials)
        {
            if (material.getRoomId() != null)
            {
                roomIds.add(material.getRoomId());
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
        for (Material material : materials)
        {
            Room room = roomMap.get(material.getRoomId());
            if (room != null)
            {
                material.setRoomCode(room.getRoomCode());
                material.setRoomName(room.getRoomName());
            }
        }
    }

    private void writeRecord(Long materialId, String changeType, BigDecimal changeQuantity,
            BigDecimal before, BigDecimal after, Long applyId, Long reservationId, String reason)
    {
        MaterialStockRecord record = new MaterialStockRecord();
        record.setMaterialId(materialId);
        record.setChangeType(changeType);
        record.setChangeQuantity(normalizeQuantity(changeQuantity));
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

    private BigDecimal normalizeQuantity(BigDecimal value)
    {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal zero()
    {
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
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
