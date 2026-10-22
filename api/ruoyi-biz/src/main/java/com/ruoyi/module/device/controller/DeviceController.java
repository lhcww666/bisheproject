package com.ruoyi.module.device.controller;

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
import com.ruoyi.module.device.domain.Device;
import com.ruoyi.module.device.service.IDeviceService;
import com.ruoyi.module.device.mapper.DeviceMapper;
import com.ruoyi.module.room.domain.Room;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 设备档案Controller
 *
 * @author itjialu
 * @date 2026-10-16
 */
@RestController
@RequestMapping("/module/device")
@RequiredArgsConstructor
public class DeviceController extends BaseController {
    private final IDeviceService deviceService;
    // private final DeviceMapper deviceMapper;

/**
 * 查询设备档案列表
 */
@PreAuthorize("@ss.hasPermi('module:device:list')")
@GetMapping("/list")
    public TableDataInfo list(Device device) {
        QueryWrapper<Device> queryWrapper = getQueryWrapper(Device.class);
        IPage<Device> page = deviceService.pageDevice(getPage(), queryWrapper);
        return getDataTableByPage(page);
    }

    /**
     * 导出设备档案列表
     */
    @PreAuthorize("@ss.hasPermi('module:device:export')")
    @Log(title = "设备档案", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Device device) {
        QueryWrapper<Device> queryWrapper = getQueryWrapper(Device.class);
        IPage<Device> page = deviceService.pageDevice(getPage(), queryWrapper);
        List<Device> list = page.getRecords();
        ExcelUtil<Device> util = new ExcelUtil<Device>(Device. class);
        util.exportExcel(response, list, "设备档案");
    }

    /**
     * 导入设备档案数据
     */
    @PreAuthorize("@ss.hasPermi('module:device:import')")
    @Log(title = "设备档案", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        int titleNum = 0;
        ExcelUtil<Device> util = new ExcelUtil<Device>(Device. class);
        List<Device> list = util.importExcel(file.getInputStream(), titleNum);
        String operName = getUsername();
        String message = deviceService.importDevice(list, titleNum, updateSupport, operName);
        return success(message);
    }

    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response, Device device) {
        List<Device> list = new ArrayList<>();
        ExcelUtil<Device> util = new ExcelUtil<Device>(Device. class);
        util.exportExcel(response, list, "设备档案");
    }

    /**
     * 获取设备档案详细信息
     */
    @PreAuthorize("@ss.hasPermi('module:device:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(deviceService.selectDeviceById(id));
    }

    /**
     * 设备档案表单使用的实验室下拉选项。
     */
    @PreAuthorize("@ss.hasAnyPermi('module:device:list,module:device:add,module:device:edit')")
    @GetMapping("/roomOptions")
    public AjaxResult roomOptions() {
        List<Room> rooms = deviceService.selectRoomOptions();
        return success(rooms);
    }

    /**
     * 新增设备档案
     */
    @PreAuthorize("@ss.hasPermi('module:device:add')")
    @Log(title = "设备档案", businessType = BusinessType.INSERT)
    @PostMapping
    @RepeatSubmit
    public AjaxResult add(@Validated @RequestBody Device device) {
            deviceService.insertDevice(device);
        JSONObject jsonObject = new JSONObject();
        jsonObject.putOpt("id", device.getId());
        return success(jsonObject);
    }

    /**
     * 修改设备档案
     */
    @PreAuthorize("@ss.hasPermi('module:device:edit')")
    @Log(title = "设备档案", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody Device device) {
        return toAjax(deviceService.updateDevice(device));
    }

    /**
     * 删除设备档案
     */
    @PreAuthorize("@ss.hasPermi('module:device:remove')")
    @Log(title = "设备档案", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(deviceService.deleteDeviceByIds(ids));
    }
}
