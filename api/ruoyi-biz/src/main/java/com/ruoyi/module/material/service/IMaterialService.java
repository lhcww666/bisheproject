package com.ruoyi.module.material.service;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.module.material.domain.Material;
import com.ruoyi.module.material.domain.MaterialStockAdjustRequest;
import com.ruoyi.module.room.domain.Room;

/**
 * 耗材档案与库存服务。
 */
public interface IMaterialService
{
    IPage<Material> pageMaterial(Page<Material> page, QueryWrapper<Material> queryWrapper);

    Material selectMaterialById(Long id);

    List<Room> selectRoomOptions();

    int insertMaterial(Material material);

    int updateMaterial(Material material);

    int deleteMaterialByIds(Long[] ids);

    void adjustStock(Long id, MaterialStockAdjustRequest request);
}
