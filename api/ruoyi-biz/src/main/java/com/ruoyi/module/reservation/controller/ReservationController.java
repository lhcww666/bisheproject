package com.ruoyi.module.reservation.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.ArrayList;

import org.springframework.validation.annotation.Validated;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ruoyi.common.annotation.RepeatSubmit;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.module.reservation.domain.Reservation;
import com.ruoyi.module.reservation.service.IReservationService;
import com.ruoyi.module.reservation.mapper.ReservationMapper;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 实验室预约Controller
 *
 * @author itjialu
 * @date 2026-10-16
 */
@RestController
@RequestMapping("/module/reservation")
@RequiredArgsConstructor
public class ReservationController extends BaseController {
    private final IReservationService reservationService;
    // private final ReservationMapper reservationMapper;

/**
 * 查询实验室预约列表
 */
@PreAuthorize("@ss.hasPermi('module:reservation:list')")
@GetMapping("/list")
    public TableDataInfo list(Reservation reservation) {
        QueryWrapper<Reservation> queryWrapper = getQueryWrapper(Reservation.class);
        IPage<Reservation> page = reservationService.pageReservation(getPage(), queryWrapper);
        return getDataTableByPage(page);
    }

    /**
     * 导出实验室预约列表
     */
    @PreAuthorize("@ss.hasPermi('module:reservation:export')")
    @Log(title = "实验室预约", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Reservation reservation) {
        QueryWrapper<Reservation> queryWrapper = getQueryWrapper(Reservation.class);
        IPage<Reservation> page = reservationService.pageReservation(getPage(), queryWrapper);
        List<Reservation> list = page.getRecords();
        ExcelUtil<Reservation> util = new ExcelUtil<Reservation>(Reservation. class);
        util.exportExcel(response, list, "实验室预约");
    }

    /**
     * 导入实验室预约数据
     */
    @PreAuthorize("@ss.hasPermi('module:reservation:import')")
    @Log(title = "实验室预约", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        int titleNum = 0;
        ExcelUtil<Reservation> util = new ExcelUtil<Reservation>(Reservation. class);
        List<Reservation> list = util.importExcel(file.getInputStream(), titleNum);
        String operName = getUsername();
        String message = reservationService.importReservation(list, titleNum, updateSupport, operName);
        return success(message);
    }

    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response, Reservation reservation) {
        List<Reservation> list = new ArrayList<>();
        ExcelUtil<Reservation> util = new ExcelUtil<Reservation>(Reservation. class);
        util.exportExcel(response, list, "实验室预约");
    }

    /**
     * 获取实验室预约详细信息
     */
    @PreAuthorize("@ss.hasPermi('module:reservation:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(reservationService.selectReservationById(id));
    }

    /**
     * 新增实验室预约
     */
    @PreAuthorize("@ss.hasPermi('module:reservation:add')")
    @Log(title = "实验室预约", businessType = BusinessType.INSERT)
    @PostMapping
    @RepeatSubmit
    public AjaxResult add(@Validated @RequestBody Reservation reservation) {
            reservationService.insertReservation(reservation);
        JSONObject jsonObject = new JSONObject();
        jsonObject.putOpt("id", reservation.getId());
        return success(jsonObject);
    }

    /**
     * 修改实验室预约
     */
    @PreAuthorize("@ss.hasPermi('module:reservation:edit')")
    @Log(title = "实验室预约", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody Reservation reservation) {
        return toAjax(reservationService.updateReservation(reservation));
    }

    /**
     * 删除实验室预约
     */
    @PreAuthorize("@ss.hasPermi('module:reservation:remove')")
    @Log(title = "实验室预约", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(reservationService.deleteReservationByIds(ids));
    }
}
