package com.ruoyi.module.safety.service;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.module.safety.domain .SafetyExamRecord;

/**
 * 安全考试记录Service接口
 *
 * @author itjialu
 * @date 2026-10-16
 */
public interface ISafetyExamRecordService extends IService<SafetyExamRecord> {
    public IPage<SafetyExamRecord> pageSafetyExamRecord(Page<SafetyExamRecord> page, QueryWrapper<SafetyExamRecord> queryWrapper);

    /**
     * 查询安全考试记录
     *
     * @param id 安全考试记录主键
     * @return 安全考试记录
     */
    public SafetyExamRecord selectSafetyExamRecordById(Long id);

    /**
     * 新增安全考试记录
     *
     * @param safetyExamRecord 安全考试记录
     * @return 结果
     */
    public int insertSafetyExamRecord(SafetyExamRecord safetyExamRecord);

    /**
     * 修改安全考试记录
     *
     * @param safetyExamRecord 安全考试记录
     * @return 结果
     */
    public int updateSafetyExamRecord(SafetyExamRecord safetyExamRecord);

    /**
     * 批量删除安全考试记录
     *
     * @param ids 需要删除的安全考试记录主键集合
     * @return 结果
     */
    public int deleteSafetyExamRecordByIds(Long[] ids);

    /**
     * 删除安全考试记录信息
     *
     * @param id 安全考试记录主键
     * @return 结果
     */
    public int deleteSafetyExamRecordById(Long id);

    /**
     * 导入安全考试记录数据
     *
     * @param list            数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    public String importSafetyExamRecord(
    List<SafetyExamRecord> list, int titleNum, Boolean
    isUpdateSupport,
    String operName);
}
