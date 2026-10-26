package com.ruoyi.module.material.mapper;

import java.math.BigDecimal;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.module.material.domain.Material;

/**
 * 耗材档案 Mapper。
 */
public interface MaterialMapper extends BaseMapper<Material>
{
    @Select("SELECT * FROM material WHERE id = #{id} FOR UPDATE")
    Material selectByIdForUpdate(@Param("id") Long id);

    /**
     * 条件更新库存。delta 为负时由 stock_quantity + delta >= 0 兜底防负库存。
     */
    @Update("UPDATE material SET stock_quantity = stock_quantity + #{delta}, "
            + "update_by = #{operator}, update_time = NOW() "
            + "WHERE id = #{id} AND stock_quantity + #{delta} >= 0")
    int applyStockDelta(@Param("id") Long id, @Param("delta") BigDecimal delta,
            @Param("operator") String operator);
}
