package com.ruoyi.module.material.service;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.module.material.domain.Material;
import com.ruoyi.module.material.domain.MaterialApply;
import com.ruoyi.module.material.domain.MaterialReservationOption;
import com.ruoyi.module.material.domain.MaterialReviewRequest;

/**
 * 耗材领用申请服务。
 */
public interface IMaterialApplyService
{
    IPage<MaterialApply> pageApply(Page<MaterialApply> page, QueryWrapper<MaterialApply> queryWrapper);

    MaterialApply selectApplyById(Long id);

    List<MaterialReservationOption> selectReservationOptions();

    List<Material> selectMaterialOptions(Long reservationId);

    MaterialApply applyMaterial(MaterialApply request);

    MaterialApply review(Long id, MaterialReviewRequest request);

    void releaseForCanceledReservation(Long reservationId);

    boolean hasRecordsForReservation(Long reservationId);
}
