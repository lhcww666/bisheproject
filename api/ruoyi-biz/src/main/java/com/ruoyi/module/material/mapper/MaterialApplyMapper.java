package com.ruoyi.module.material.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.module.material.domain.MaterialApply;
import com.ruoyi.module.reservation.domain.Reservation;

/**
 * 耗材领用申请 Mapper。
 */
public interface MaterialApplyMapper extends BaseMapper<MaterialApply>
{
    @Select("SELECT * FROM material_apply WHERE id = #{id} FOR UPDATE")
    MaterialApply selectByIdForUpdate(@Param("id") Long id);

    @Select("SELECT * FROM reservation WHERE id = #{id} FOR UPDATE")
    Reservation selectReservationForUpdate(@Param("id") Long id);
}
