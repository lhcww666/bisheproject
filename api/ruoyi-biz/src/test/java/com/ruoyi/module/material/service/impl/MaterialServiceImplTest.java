package com.ruoyi.module.material.service.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.module.material.domain.Material;
import com.ruoyi.module.material.domain.MaterialStockAdjustRequest;
import com.ruoyi.module.material.domain.MaterialStockRecord;
import com.ruoyi.module.material.mapper.MaterialApplyMapper;
import com.ruoyi.module.material.mapper.MaterialMapper;
import com.ruoyi.module.material.mapper.MaterialStockRecordMapper;
import com.ruoyi.module.room.mapper.RoomMapper;

class MaterialServiceImplTest
{
    @Test
    void outboundAdjustmentCannotReduceStockBelowZero()
    {
        MaterialMapper materialMapper = mock(MaterialMapper.class);
        MaterialStockRecordMapper recordMapper = mock(MaterialStockRecordMapper.class);
        Material material = material(1L, "3.00");
        when(materialMapper.selectByIdForUpdate(1L)).thenReturn(material);
        MaterialServiceImpl service = new MaterialServiceImpl(materialMapper, mock(MaterialApplyMapper.class),
                recordMapper, mock(RoomMapper.class));
        MaterialStockAdjustRequest request = adjustment("ADJUST_OUT", "5.00");

        assertThrows(ServiceException.class, () -> service.adjustStock(1L, request));
        verify(materialMapper, never()).applyStockDelta(any(), any(), anyString());
        verify(recordMapper, never()).insert(any(MaterialStockRecord.class));
    }

    @Test
    void successfulAdjustmentWritesBeforeAndAfterStock()
    {
        MaterialMapper materialMapper = mock(MaterialMapper.class);
        MaterialStockRecordMapper recordMapper = mock(MaterialStockRecordMapper.class);
        when(materialMapper.selectByIdForUpdate(1L)).thenReturn(material(1L, "3.00"));
        when(materialMapper.applyStockDelta(any(), any(), anyString())).thenReturn(1);
        MaterialServiceImpl service = new MaterialServiceImpl(materialMapper, mock(MaterialApplyMapper.class),
                recordMapper, mock(RoomMapper.class));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class))
        {
            security.when(SecurityUtils::getUserId).thenReturn(9L);
            security.when(SecurityUtils::getUsername).thenReturn("admin");
            service.adjustStock(1L, adjustment("ADJUST_IN", "2.00"));
        }

        verify(materialMapper).applyStockDelta(argThat(id -> id.equals(1L)),
                argThat(delta -> delta.compareTo(new BigDecimal("2.00")) == 0), anyString());
        verify(recordMapper).insert(argThat((MaterialStockRecord record) ->
                "ADJUST_IN".equals(record.getChangeType())
                        && record.getBeforeQuantity().compareTo(new BigDecimal("3.00")) == 0
                        && record.getAfterQuantity().compareTo(new BigDecimal("5.00")) == 0));
    }

    private Material material(Long id, String stock)
    {
        Material material = new Material();
        material.setId(id);
        material.setStockQuantity(new BigDecimal(stock));
        return material;
    }

    private MaterialStockAdjustRequest adjustment(String type, String quantity)
    {
        MaterialStockAdjustRequest request = new MaterialStockAdjustRequest();
        request.setChangeType(type);
        request.setQuantity(new BigDecimal(quantity));
        request.setReason("测试库存调整");
        return request;
    }
}
