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
import com.ruoyi.module.reservation.domain.ReservationDevice;
import com.ruoyi.module.reservation.service.IReservationDeviceService;
import com.ruoyi.module.reservation.mapper.ReservationDeviceMapper;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 预约设备关联Controller
 *
 * @author itjialu
 * @date 2026-10-16
 */
@RestController
@RequestMapping("/module/reservationDevice")
@RequiredArgsConstructor
public class ReservationDeviceController extends BaseController {
    private final IReservationDeviceService reservationDeviceService;
    // private final ReservationDeviceMapper reservationDeviceMapper;

/**
 * 查询预约设备关联列表
 */
@PreAuthorize("@ss.hasPermi('module:reservationDevice:list')")
@GetMapping("/list")
    public TableDataInfo list(ReservationDevice reservationDevice) {
        QueryWrapper<ReservationDevice> queryWrapper = getQueryWrapper(ReservationDevice.class);
        IPage<ReservationDevice> page = reservationDeviceService.pageReservationDevice(getPage(), queryWrapper);
        return getDataTableByPage(page);
    }

    /**
     * 导出预约设备关联列表
     */
    @PreAuthorize("@ss.hasPermi('module:reservationDevice:export')")
    @Log(title = "预约设备关联", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ReservationDevice reservationDevice) {
        QueryWrapper<ReservationDevice> queryWrapper = getQueryWrapper(ReservationDevice.class);
        IPage<ReservationDevice> page = reservationDeviceService.pageReservationDevice(getPage(), queryWrapper);
        List<ReservationDevice> list = page.getRecords();
        ExcelUtil<ReservationDevice> util = new ExcelUtil<ReservationDevice>(ReservationDevice. class);
        util.exportExcel(response, list, "预约设备关联");
    }

    /**
     * 导入预约设备关联数据
     */
    @PreAuthorize("@ss.hasPermi('module:reservationDevice:import')")
    @Log(title = "预约设备关联", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        int titleNum = 0;
        ExcelUtil<ReservationDevice> util = new ExcelUtil<ReservationDevice>(ReservationDevice. class);
        List<ReservationDevice> list = util.importExcel(file.getInputStream(), titleNum);
        String operName = getUsername();
        String message = reservationDeviceService.importReservationDevice(list, titleNum, updateSupport, operName);
        return success(message);
    }

    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response, ReservationDevice reservationDevice) {
        List<ReservationDevice> list = new ArrayList<>();
        ExcelUtil<ReservationDevice> util = new ExcelUtil<ReservationDevice>(ReservationDevice. class);
        util.exportExcel(response, list, "预约设备关联");
    }

    /**
     * 获取预约设备关联详细信息
     */
    @PreAuthorize("@ss.hasPermi('module:reservationDevice:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(reservationDeviceService.selectReservationDeviceById(id));
    }

    /**
     * 新增预约设备关联
     */
    @PreAuthorize("@ss.hasPermi('module:reservationDevice:add')")
    @Log(title = "预约设备关联", businessType = BusinessType.INSERT)
    @PostMapping
    @RepeatSubmit
    public AjaxResult add(@Validated @RequestBody ReservationDevice reservationDevice) {
            reservationDeviceService.insertReservationDevice(reservationDevice);
        JSONObject jsonObject = new JSONObject();
        jsonObject.putOpt("id", reservationDevice.getId());
        return success(jsonObject);
    }

    /**
     * 修改预约设备关联
     */
    @PreAuthorize("@ss.hasPermi('module:reservationDevice:edit')")
    @Log(title = "预约设备关联", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody ReservationDevice reservationDevice) {
        return toAjax(reservationDeviceService.updateReservationDevice(reservationDevice));
    }

    /**
     * 删除预约设备关联
     */
    @PreAuthorize("@ss.hasPermi('module:reservationDevice:remove')")
    @Log(title = "预约设备关联", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(reservationDeviceService.deleteReservationDeviceByIds(ids));
    }
}
