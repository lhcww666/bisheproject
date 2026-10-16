package com.ruoyi.module.review.controller;

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
import com.ruoyi.module.review.domain.ReservationReview;
import com.ruoyi.module.review.service.IReservationReviewService;
import com.ruoyi.module.review.mapper.ReservationReviewMapper;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 预约审核Controller
 *
 * @author itjialu
 * @date 2026-10-16
 */
@RestController
@RequestMapping("/module/reservationReview")
@RequiredArgsConstructor
public class ReservationReviewController extends BaseController {
    private final IReservationReviewService reservationReviewService;
    // private final ReservationReviewMapper reservationReviewMapper;

/**
 * 查询预约审核列表
 */
@PreAuthorize("@ss.hasPermi('module:reservationReview:list')")
@GetMapping("/list")
    public TableDataInfo list(ReservationReview reservationReview) {
        QueryWrapper<ReservationReview> queryWrapper = getQueryWrapper(ReservationReview.class);
        IPage<ReservationReview> page = reservationReviewService.pageReservationReview(getPage(), queryWrapper);
        return getDataTableByPage(page);
    }

    /**
     * 导出预约审核列表
     */
    @PreAuthorize("@ss.hasPermi('module:reservationReview:export')")
    @Log(title = "预约审核", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ReservationReview reservationReview) {
        QueryWrapper<ReservationReview> queryWrapper = getQueryWrapper(ReservationReview.class);
        IPage<ReservationReview> page = reservationReviewService.pageReservationReview(getPage(), queryWrapper);
        List<ReservationReview> list = page.getRecords();
        ExcelUtil<ReservationReview> util = new ExcelUtil<ReservationReview>(ReservationReview. class);
        util.exportExcel(response, list, "预约审核");
    }

    /**
     * 导入预约审核数据
     */
    @PreAuthorize("@ss.hasPermi('module:reservationReview:import')")
    @Log(title = "预约审核", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        int titleNum = 0;
        ExcelUtil<ReservationReview> util = new ExcelUtil<ReservationReview>(ReservationReview. class);
        List<ReservationReview> list = util.importExcel(file.getInputStream(), titleNum);
        String operName = getUsername();
        String message = reservationReviewService.importReservationReview(list, titleNum, updateSupport, operName);
        return success(message);
    }

    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response, ReservationReview reservationReview) {
        List<ReservationReview> list = new ArrayList<>();
        ExcelUtil<ReservationReview> util = new ExcelUtil<ReservationReview>(ReservationReview. class);
        util.exportExcel(response, list, "预约审核");
    }

    /**
     * 获取预约审核详细信息
     */
    @PreAuthorize("@ss.hasPermi('module:reservationReview:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(reservationReviewService.selectReservationReviewById(id));
    }

    /**
     * 新增预约审核
     */
    @PreAuthorize("@ss.hasPermi('module:reservationReview:add')")
    @Log(title = "预约审核", businessType = BusinessType.INSERT)
    @PostMapping
    @RepeatSubmit
    public AjaxResult add(@Validated @RequestBody ReservationReview reservationReview) {
            reservationReviewService.insertReservationReview(reservationReview);
        JSONObject jsonObject = new JSONObject();
        jsonObject.putOpt("id", reservationReview.getId());
        return success(jsonObject);
    }

    /**
     * 修改预约审核
     */
    @PreAuthorize("@ss.hasPermi('module:reservationReview:edit')")
    @Log(title = "预约审核", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody ReservationReview reservationReview) {
        return toAjax(reservationReviewService.updateReservationReview(reservationReview));
    }

    /**
     * 删除预约审核
     */
    @PreAuthorize("@ss.hasPermi('module:reservationReview:remove')")
    @Log(title = "预约审核", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(reservationReviewService.deleteReservationReviewByIds(ids));
    }
}
