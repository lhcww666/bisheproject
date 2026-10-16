package com.ruoyi.module.room.controller;

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
import com.ruoyi.module.room.domain.Room;
import com.ruoyi.module.room.service.IRoomService;
import com.ruoyi.module.room.mapper.RoomMapper;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 实验室档案Controller
 *
 * @author itjialu
 * @date 2026-10-16
 */
@RestController
@RequestMapping("/module/room")
@RequiredArgsConstructor
public class RoomController extends BaseController {
    private final IRoomService roomService;
    // private final RoomMapper roomMapper;

/**
 * 查询实验室档案列表
 */
@PreAuthorize("@ss.hasPermi('module:room:list')")
@GetMapping("/list")
    public TableDataInfo list(Room room) {
        QueryWrapper<Room> queryWrapper = getQueryWrapper(Room.class);
        IPage<Room> page = roomService.pageRoom(getPage(), queryWrapper);
        return getDataTableByPage(page);
    }

    /**
     * 导出实验室档案列表
     */
    @PreAuthorize("@ss.hasPermi('module:room:export')")
    @Log(title = "实验室档案", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Room room) {
        QueryWrapper<Room> queryWrapper = getQueryWrapper(Room.class);
        IPage<Room> page = roomService.pageRoom(getPage(), queryWrapper);
        List<Room> list = page.getRecords();
        ExcelUtil<Room> util = new ExcelUtil<Room>(Room. class);
        util.exportExcel(response, list, "实验室档案");
    }

    /**
     * 导入实验室档案数据
     */
    @PreAuthorize("@ss.hasPermi('module:room:import')")
    @Log(title = "实验室档案", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        int titleNum = 0;
        ExcelUtil<Room> util = new ExcelUtil<Room>(Room. class);
        List<Room> list = util.importExcel(file.getInputStream(), titleNum);
        String operName = getUsername();
        String message = roomService.importRoom(list, titleNum, updateSupport, operName);
        return success(message);
    }

    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response, Room room) {
        List<Room> list = new ArrayList<>();
        ExcelUtil<Room> util = new ExcelUtil<Room>(Room. class);
        util.exportExcel(response, list, "实验室档案");
    }

    /**
     * 获取实验室档案详细信息
     */
    @PreAuthorize("@ss.hasPermi('module:room:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(roomService.selectRoomById(id));
    }

    /**
     * 新增实验室档案
     */
    @PreAuthorize("@ss.hasPermi('module:room:add')")
    @Log(title = "实验室档案", businessType = BusinessType.INSERT)
    @PostMapping
    @RepeatSubmit
    public AjaxResult add(@Validated @RequestBody Room room) {
            roomService.insertRoom(room);
        JSONObject jsonObject = new JSONObject();
        jsonObject.putOpt("id", room.getId());
        return success(jsonObject);
    }

    /**
     * 修改实验室档案
     */
    @PreAuthorize("@ss.hasPermi('module:room:edit')")
    @Log(title = "实验室档案", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody Room room) {
        return toAjax(roomService.updateRoom(room));
    }

    /**
     * 删除实验室档案
     */
    @PreAuthorize("@ss.hasPermi('module:room:remove')")
    @Log(title = "实验室档案", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(roomService.deleteRoomByIds(ids));
    }
}
