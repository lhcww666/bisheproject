package com.ruoyi.module.roomaccess.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.module.roomaccess.domain.RoomAccess;
import com.ruoyi.module.roomaccess.service.IRoomAccessService;

import lombok.RequiredArgsConstructor;

/**
 * 实验室准入规则Controller
 *
 * @author itjialu
 */
@RestController
@RequestMapping("/module/roomAccess")
@RequiredArgsConstructor
public class RoomAccessController extends BaseController
{
    private final IRoomAccessService roomAccessService;

    @PreAuthorize("@ss.hasPermi('module:roomAccess:list')")
    @GetMapping("/list")
    public TableDataInfo list(RoomAccess roomAccess)
    {
        QueryWrapper<RoomAccess> queryWrapper = new QueryWrapper<>();
        if (roomAccess.getRoomId() != null)
        {
            queryWrapper.eq("room_id", roomAccess.getRoomId());
        }
        if (StringUtils.isNotEmpty(roomAccess.getSubjectType()))
        {
            queryWrapper.eq("subject_type", roomAccess.getSubjectType().trim().toUpperCase());
        }
        if (roomAccess.getSubjectId() != null)
        {
            queryWrapper.eq("subject_id", roomAccess.getSubjectId());
        }
        queryWrapper.orderByDesc("id");
        IPage<RoomAccess> page = roomAccessService.pageRoomAccess(getPage(), queryWrapper);
        return getDataTableByPage(page);
    }

    @PreAuthorize("@ss.hasPermi('module:roomAccess:list')")
    @GetMapping("/options")
    public AjaxResult options()
    {
        return success(roomAccessService.selectOptions());
    }

    @PreAuthorize("@ss.hasPermi('module:roomAccess:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(roomAccessService.selectRoomAccessById(id));
    }

    @PreAuthorize("@ss.hasPermi('module:roomAccess:add')")
    @Log(title = "实验室准入", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody RoomAccess roomAccess)
    {
        roomAccessService.insertRoomAccess(roomAccess);
        return success(roomAccess.getId());
    }

    @PreAuthorize("@ss.hasPermi('module:roomAccess:edit')")
    @Log(title = "实验室准入", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody RoomAccess roomAccess)
    {
        return toAjax(roomAccessService.updateRoomAccess(roomAccess));
    }

    @PreAuthorize("@ss.hasPermi('module:roomAccess:remove')")
    @Log(title = "实验室准入", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(roomAccessService.deleteRoomAccessByIds(ids));
    }
}
