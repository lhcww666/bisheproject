package com.ruoyi.module.reservation.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.module.reservation.domain.Reservation;
import com.ruoyi.module.reservation.domain.ReservationStatusRequest;
import com.ruoyi.module.reservation.domain.ReservationFinishRequest;
import com.ruoyi.module.reservation.domain.ReservationView;
import com.ruoyi.module.reservation.service.IReservationLifecycleService;
import com.ruoyi.module.reservation.service.IReservationService;
import com.ruoyi.module.reservation.mapper.ReservationMapper;
import com.ruoyi.module.common.service.ModuleDataScopeService;
import com.ruoyi.module.classrep.service.IClassRepresentativeService;
import com.ruoyi.module.review.domain.ReservationReviewRequest;
import com.ruoyi.module.review.service.IReservationReviewBizService;
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
    private final IReservationLifecycleService reservationLifecycleService;
    private final IReservationReviewBizService reservationReviewBizService;
    private final ModuleDataScopeService moduleDataScopeService;
    private final IClassRepresentativeService classRepresentativeService;
    // private final ReservationMapper reservationMapper;

/**
 * 查询实验室预约列表
 */
@PreAuthorize("@ss.hasPermi('module:reservation:list')")
@GetMapping("/list")
    public TableDataInfo list(Reservation reservation) {
        QueryWrapper<Reservation> queryWrapper = getQueryWrapper(Reservation.class);
        moduleDataScopeService.applyStudentRecordScope(queryWrapper, "applicant_id");
        IPage<ReservationView> page = reservationService.pageReservation(getPage(), queryWrapper);
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
        moduleDataScopeService.applyStudentRecordScope(queryWrapper, "applicant_id");
        IPage<ReservationView> page = reservationService.pageReservation(getPage(), queryWrapper);
        List<ReservationView> list = page.getRecords();
        ExcelUtil<ReservationView> util = new ExcelUtil<ReservationView>(ReservationView. class);
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
        Reservation reservation = reservationService.selectReservationById(id);
        if (reservation != null) {
            moduleDataScopeService.checkStudentRecordAccess(reservation.getApplicantId());
        }
        return success(reservation);
    }

    /**
     * 预约详情：包含设备明细和审核历史
     */
    @PreAuthorize("@ss.hasPermi('module:reservation:query')")
    @GetMapping("/{id}/detail")
    public AjaxResult getDetail(@PathVariable("id") Long id) {
        Reservation reservation = reservationService.selectReservationById(id);
        if (reservation != null) {
            moduleDataScopeService.checkStudentRecordAccess(reservation.getApplicantId());
        }
        return success(reservationService.selectReservationDetail(id));
    }

    /**
     * 预约筛选和新增/修改表单使用的申请人选项。
     */
    @PreAuthorize("@ss.hasPermi('module:reservation:list')")
    @GetMapping("/applicantOptions")
    public AjaxResult applicantOptions() {
        return success(reservationService.selectApplicantOptions());
    }

    /**
     * 预约筛选和新增/修改表单使用的实验室选项。
     */
    @PreAuthorize("@ss.hasPermi('module:reservation:list')")
    @GetMapping("/roomOptions")
    public AjaxResult roomOptions() {
        return success(reservationService.selectRoomOptions());
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
     * 提交预约申请（学生端入口，走完整业务规则校验）
     */
    @PreAuthorize("@ss.hasPermi('module:reservation:apply')")
    @Log(title = "实验室预约", businessType = BusinessType.INSERT)
    @PostMapping("/apply")
    @RepeatSubmit
    public AjaxResult apply(@RequestBody Reservation reservation) {
        Reservation created = reservationService.applyReservation(reservation);
        JSONObject jsonObject = new JSONObject();
        jsonObject.putOpt("id", created.getId());
        jsonObject.putOpt("reservationNo", created.getReservationNo());
        jsonObject.putOpt("status", created.getStatus());
        jsonObject.putOpt("reviewRequired", created.getReviewRequired());
        return success(jsonObject);
    }

    /**
     * 查询我的预约
     */
    @PreAuthorize("@ss.hasPermi('module:reservation:my')")
    @GetMapping("/my")
    public TableDataInfo my(@RequestParam(required = false) String status,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        IPage<ReservationView> page = reservationService.pageMyReservation(getPage(), status, parseDateTime(startTime),
                parseDateTime(endTime));
        return getDataTableByPage(page);
    }

    /**
     * 查询当前学生是否为本班班级负责人，用于决定是否显示“班级预约”
     */
    @PreAuthorize("@ss.hasPermi('module:reservation:apply')")
    @GetMapping("/myClassRepresentative")
    public AjaxResult myClassRepresentative() {
        return success(classRepresentativeService.getMyClassReservationInfo(SecurityUtils.getUserId()));
    }

    /**
     * 取消本人预约
     */
    @PreAuthorize("@ss.hasPermi('module:reservation:cancel')")
    @Log(title = "实验室预约", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/cancel")
    public AjaxResult cancel(@PathVariable("id") Long id) {
        return toAjax(reservationService.cancelReservation(id));
    }

    /**
     * 学生签到。后端校验预约状态和签到时间窗口。
     */
    @PreAuthorize("@ss.hasPermi('module:reservation:apply')")
    @Log(title = "实验室预约", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/check-in")
    public AjaxResult checkIn(@PathVariable("id") Long id) {
        reservationLifecycleService.checkIn(id);
        return success();
    }

    /**
     * 结束实验。只有已签到的预约可以结束。
     */
    @PreAuthorize("@ss.hasPermi('module:reservation:apply')")
    @Log(title = "实验室预约", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/finish")
    public AjaxResult finish(@PathVariable("id") Long id,
            @RequestBody(required = false) ReservationFinishRequest request) {
        Long actualCount = request == null ? null : request.getActualParticipantCount();
        String remark = request == null ? null : request.getRemark();
        reservationLifecycleService.finish(id, actualCount, remark);
        return success();
    }

    /**
     * 管理员手动标记爽约，定时任务也会自动处理。
     */
    @PreAuthorize("@ss.hasPermi('module:reservation:review')")
    @Log(title = "实验室预约", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/no-show")
    public AjaxResult noShow(@PathVariable("id") Long id) {
        reservationLifecycleService.markNoShow(id);
        return success();
    }

    /**
     * 预约状态变更历史。
     */
    @PreAuthorize("@ss.hasPermi('module:reservation:query')")
    @GetMapping("/{id}/status-log")
    public AjaxResult statusLog(@PathVariable("id") Long id) {
        return success(reservationLifecycleService.listStatusLogs(id));
    }

    /**
     * 查询指定时段可用的实验室
     */
    @PreAuthorize("@ss.hasPermi('module:reservation:available')")
    @GetMapping("/availableRooms")
    public AjaxResult availableRooms(@RequestParam String startTime, @RequestParam String endTime,
            @RequestParam(required = false) Long participantCount) {
        return success(reservationService.selectAvailableRooms(SecurityUtils.getUserId(), parseDateTime(startTime),
                parseDateTime(endTime), participantCount));
    }

    /**
     * 查询指定实验室中可预约的设备（学生预约时使用，不开放设备档案菜单）
     */
    @PreAuthorize("@ss.hasPermi('module:reservation:available')")
    @GetMapping("/availableDevices")
    public AjaxResult availableDevices(@RequestParam Long roomId) {
        return success(reservationService.selectAvailableDevices(roomId));
    }

    /**
     * 查询待审核预约列表（教师端）
     */
    @PreAuthorize("@ss.hasPermi('module:reservation:review')")
    @GetMapping("/review/pending")
    public TableDataInfo reviewPending() {
        IPage<ReservationView> page = reservationReviewBizService.pagePending(getPage());
        return getDataTableByPage(page);
    }

    /**
     * 审核预约：通过或驳回（教师端）
     */
    @PreAuthorize("@ss.hasPermi('module:reservation:review')")
    @Log(title = "实验室预约", businessType = BusinessType.UPDATE)
    @PostMapping("/review/{id}")
    public AjaxResult review(@PathVariable("id") Long id, @RequestBody ReservationReviewRequest request) {
        reservationReviewBizService.review(id, request);
        return success();
    }

    /**
     * 统一审核/弃审入口：APPROVE、REJECT、UNAUDIT
     */
    @PreAuthorize("@ss.hasPermi('module:reservation:review')")
    @Log(title = "实验室预约", businessType = BusinessType.UPDATE)
    @PostMapping("/updateStatus")
    public AjaxResult updateStatus(@Validated @RequestBody ReservationStatusRequest request) {
        reservationReviewBizService.updateStatus(request.getId(), request.getAction(), request.getComment());
        return success();
    }

    /**
     * 解析前端传入的时间参数，支持 yyyy-MM-dd HH:mm:ss、yyyy-MM-dd HH:mm、yyyy-MM-dd 三种写法
     */
    private Date parseDateTime(String text) {
        if (StringUtils.isEmpty(text)) {
            return null;
        }
        String[] patterns = { "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd" };
        for (String pattern : patterns) {
            try {
                return new SimpleDateFormat(pattern).parse(text);
            } catch (ParseException ignored) {
                // 换下一种格式继续尝试
            }
        }
        throw new ServiceException("时间格式不正确，应为 yyyy-MM-dd HH:mm:ss：" + text);
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
