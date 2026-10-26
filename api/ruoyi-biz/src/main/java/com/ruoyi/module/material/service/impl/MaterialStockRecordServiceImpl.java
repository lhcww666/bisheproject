package com.ruoyi.module.material.service.impl;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.module.material.domain.Material;
import com.ruoyi.module.material.domain.MaterialStockRecord;
import com.ruoyi.module.material.mapper.MaterialMapper;
import com.ruoyi.module.material.mapper.MaterialStockRecordMapper;
import com.ruoyi.module.material.service.IMaterialStockRecordService;
import com.ruoyi.module.room.domain.Room;
import com.ruoyi.module.room.mapper.RoomMapper;

import lombok.RequiredArgsConstructor;

/**
 * 耗材库存流水服务实现。
 */
@Service
@RequiredArgsConstructor
public class MaterialStockRecordServiceImpl implements IMaterialStockRecordService
{
    private final MaterialStockRecordMapper stockRecordMapper;

    private final MaterialMapper materialMapper;

    private final RoomMapper roomMapper;

    @Override
    public IPage<MaterialStockRecord> pageRecord(Page<MaterialStockRecord> page,
            QueryWrapper<MaterialStockRecord> queryWrapper)
    {
        queryWrapper.orderByDesc("id");
        IPage<MaterialStockRecord> pageData = stockRecordMapper.selectPage(page, queryWrapper);
        fillDisplayFields(pageData.getRecords());
        return pageData;
    }

    @Override
    public MaterialStockRecord selectRecordById(Long id)
    {
        MaterialStockRecord record = stockRecordMapper.selectById(id);
        if (record != null)
        {
            fillDisplayFields(List.of(record));
        }
        return record;
    }

    private void fillDisplayFields(List<MaterialStockRecord> records)
    {
        if (records == null || records.isEmpty())
        {
            return;
        }
        Set<Long> materialIds = new LinkedHashSet<>();
        for (MaterialStockRecord record : records)
        {
            materialIds.add(record.getMaterialId());
        }
        Map<Long, Material> materialMap = new HashMap<>();
        Set<Long> roomIds = new LinkedHashSet<>();
        for (Material material : materialMapper.selectBatchIds(materialIds))
        {
            materialMap.put(material.getId(), material);
            roomIds.add(material.getRoomId());
        }
        Map<Long, Room> roomMap = new HashMap<>();
        if (!roomIds.isEmpty())
        {
            for (Room room : roomMapper.selectBatchIds(roomIds))
            {
                roomMap.put(room.getId(), room);
            }
        }
        for (MaterialStockRecord record : records)
        {
            Material material = materialMap.get(record.getMaterialId());
            if (material == null)
            {
                continue;
            }
            record.setMaterialCode(material.getMaterialCode());
            record.setMaterialName(material.getMaterialName());
            record.setSpecification(material.getSpecification());
            record.setUnit(material.getUnit());
            Room room = roomMap.get(material.getRoomId());
            if (room != null)
            {
                record.setRoomCode(room.getRoomCode());
                record.setRoomName(room.getRoomName());
            }
        }
    }
}
