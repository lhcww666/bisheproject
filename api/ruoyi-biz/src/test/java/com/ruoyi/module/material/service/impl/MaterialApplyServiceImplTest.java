package com.ruoyi.module.material.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.module.common.service.ModuleDataScopeService;
import com.ruoyi.module.material.domain.Material;
import com.ruoyi.module.material.domain.MaterialApply;
import com.ruoyi.module.material.domain.MaterialApplyStatus;
import com.ruoyi.module.material.domain.MaterialReviewRequest;
import com.ruoyi.module.material.domain.MaterialStockRecord;
import com.ruoyi.module.material.mapper.MaterialApplyMapper;
import com.ruoyi.module.material.mapper.MaterialMapper;
import com.ruoyi.module.material.mapper.MaterialStockRecordMapper;
import com.ruoyi.module.reservation.domain.Reservation;
import com.ruoyi.module.reservation.domain.ReservationStatus;
import com.ruoyi.module.reservation.mapper.ReservationMapper;
import com.ruoyi.module.room.mapper.RoomMapper;
import com.ruoyi.system.service.ISysUserService;

class MaterialApplyServiceImplTest
{
    @Test
    void approvedApplicationDeductsStockAndWritesOutboundRecord()
    {
        Fixture fixture = new Fixture();
        MaterialApply apply = fixture.pendingApply("2.00");
        Reservation reservation = fixture.reservation();
        Material material = fixture.material("10.00");
        when(fixture.applyMapper.selectByIdForUpdate(1L)).thenReturn(apply);
        when(fixture.applyMapper.selectReservationForUpdate(2L)).thenReturn(reservation);
        when(fixture.materialMapper.selectByIdForUpdate(3L)).thenReturn(material);
        when(fixture.materialMapper.applyStockDelta(any(), any(), anyString())).thenReturn(1);
        when(fixture.applyMapper.updateById(any(MaterialApply.class))).thenReturn(1);
        when(fixture.applyMapper.selectById(1L)).thenReturn(apply);

        MaterialApply result = fixture.withOperator(() -> fixture.service.review(1L,
                review(MaterialApplyStatus.APPROVED, "同意")));

        assertEquals(MaterialApplyStatus.APPROVED, result.getStatus());
        verify(fixture.materialMapper).applyStockDelta(argThat(id -> id.equals(3L)),
                argThat(delta -> delta.compareTo(new BigDecimal("-2.00")) == 0), anyString());
        verify(fixture.recordMapper).insert(argThat((MaterialStockRecord record) ->
                "APPLY_OUT".equals(record.getChangeType())
                        && record.getBeforeQuantity().compareTo(new BigDecimal("10.00")) == 0
                        && record.getAfterQuantity().compareTo(new BigDecimal("8.00")) == 0));
    }

    @Test
    void insufficientStockAutomaticallyRejectsWithoutStockRecord()
    {
        Fixture fixture = new Fixture();
        MaterialApply apply = fixture.pendingApply("12.00");
        when(fixture.applyMapper.selectByIdForUpdate(1L)).thenReturn(apply);
        when(fixture.applyMapper.selectReservationForUpdate(2L)).thenReturn(fixture.reservation());
        when(fixture.materialMapper.selectByIdForUpdate(3L)).thenReturn(fixture.material("10.00"));
        when(fixture.materialMapper.applyStockDelta(any(), any(), anyString())).thenReturn(0);
        when(fixture.applyMapper.updateById(any(MaterialApply.class))).thenReturn(1);
        when(fixture.applyMapper.selectById(1L)).thenReturn(apply);

        MaterialApply result = fixture.withOperator(() -> fixture.service.review(1L,
                review(MaterialApplyStatus.APPROVED, null)));

        assertEquals(MaterialApplyStatus.REJECTED, result.getStatus());
        assertEquals("库存不足，系统自动驳回", result.getReviewComment());
        verify(fixture.recordMapper, never()).insert(any(MaterialStockRecord.class));
    }

    @Test
    void duplicateReviewIsRejected()
    {
        Fixture fixture = new Fixture();
        MaterialApply apply = fixture.pendingApply("2.00");
        apply.setStatus(MaterialApplyStatus.APPROVED);
        when(fixture.applyMapper.selectByIdForUpdate(1L)).thenReturn(apply);

        assertThrows(ServiceException.class, () -> fixture.service.review(1L,
                review(MaterialApplyStatus.APPROVED, "重复审核")));
        verify(fixture.materialMapper, never()).applyStockDelta(any(), any(), anyString());
    }

    @Test
    void cancelingReservationReturnsIssuedStock()
    {
        Fixture fixture = new Fixture();
        MaterialApply apply = fixture.pendingApply("2.00");
        apply.setStatus(MaterialApplyStatus.APPROVED);
        when(fixture.applyMapper.selectList(any())).thenReturn(List.of(apply));
        when(fixture.applyMapper.selectByIdForUpdate(1L)).thenReturn(apply);
        when(fixture.materialMapper.selectByIdForUpdate(3L)).thenReturn(fixture.material("8.00"));
        when(fixture.materialMapper.applyStockDelta(any(), any(), anyString())).thenReturn(1);
        when(fixture.applyMapper.updateById(any(MaterialApply.class))).thenReturn(1);

        fixture.withOperator(() -> {
            fixture.service.releaseForCanceledReservation(2L);
            return null;
        });

        assertEquals(MaterialApplyStatus.RETURNED, apply.getStatus());
        verify(fixture.materialMapper).applyStockDelta(argThat(id -> id.equals(3L)),
                argThat(delta -> delta.compareTo(new BigDecimal("2.00")) == 0), anyString());
        verify(fixture.recordMapper).insert(argThat((MaterialStockRecord record) ->
                "CANCEL_RETURN".equals(record.getChangeType())
                        && record.getAfterQuantity().compareTo(new BigDecimal("10.00")) == 0));
    }

    private MaterialReviewRequest review(String status, String comment)
    {
        MaterialReviewRequest request = new MaterialReviewRequest();
        request.setStatus(status);
        request.setReviewComment(comment);
        return request;
    }

    private static class Fixture
    {
        private final MaterialApplyMapper applyMapper = mock(MaterialApplyMapper.class);

        private final MaterialMapper materialMapper = mock(MaterialMapper.class);

        private final MaterialStockRecordMapper recordMapper = mock(MaterialStockRecordMapper.class);

        private final ReservationMapper reservationMapper = mock(ReservationMapper.class);

        private final RoomMapper roomMapper = mock(RoomMapper.class);

        private final ISysUserService userService = mock(ISysUserService.class);

        private final ModuleDataScopeService dataScope = mock(ModuleDataScopeService.class);

        private final MaterialApplyServiceImpl service;

        private Fixture()
        {
            when(dataScope.isAdmin()).thenReturn(true);
            when(reservationMapper.selectBatchIds(any())).thenReturn(Collections.emptyList());
            when(materialMapper.selectBatchIds(any())).thenReturn(Collections.emptyList());
            service = new MaterialApplyServiceImpl(applyMapper, materialMapper, recordMapper, reservationMapper,
                    roomMapper, userService, dataScope);
        }

        private <T> T withOperator(ThrowingSupplier<T> action)
        {
            try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class))
            {
                security.when(SecurityUtils::getUserId).thenReturn(9L);
                security.when(SecurityUtils::getUsername).thenReturn("admin");
                return action.get();
            }
        }

        private MaterialApply pendingApply(String quantity)
        {
            MaterialApply apply = new MaterialApply();
            apply.setId(1L);
            apply.setReservationId(2L);
            apply.setApplicantId(5L);
            apply.setMaterialId(3L);
            apply.setApplyQuantity(new BigDecimal(quantity));
            apply.setStatus(MaterialApplyStatus.PENDING);
            return apply;
        }

        private Reservation reservation()
        {
            Reservation reservation = new Reservation();
            reservation.setId(2L);
            reservation.setApplicantId(5L);
            reservation.setRoomId(4L);
            reservation.setStatus(ReservationStatus.APPROVED);
            return reservation;
        }

        private Material material(String stock)
        {
            Material material = new Material();
            material.setId(3L);
            material.setRoomId(4L);
            material.setMaterialCode("MAT-TEST-001");
            material.setMaterialName("测试耗材");
            material.setUnit("盒");
            material.setStatus("1");
            material.setStockQuantity(new BigDecimal(stock));
            return material;
        }
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T>
    {
        T get();
    }
}
