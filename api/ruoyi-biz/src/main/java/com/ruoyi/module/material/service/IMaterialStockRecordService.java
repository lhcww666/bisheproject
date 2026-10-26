package com.ruoyi.module.material.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.module.material.domain.MaterialStockRecord;

/**
 * 耗材库存流水服务。
 */
public interface IMaterialStockRecordService
{
    IPage<MaterialStockRecord> pageRecord(Page<MaterialStockRecord> page,
            QueryWrapper<MaterialStockRecord> queryWrapper);

    MaterialStockRecord selectRecordById(Long id);
}
