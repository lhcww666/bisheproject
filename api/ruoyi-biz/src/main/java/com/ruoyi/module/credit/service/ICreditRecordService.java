package com.ruoyi.module.credit.service;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.module.credit.domain .CreditRecord;

/**
 * 信用分流水Service接口
 *
 * @author itjialu
 * @date 2026-10-16
 */
public interface ICreditRecordService extends IService<CreditRecord> {
    public IPage<CreditRecord> pageCreditRecord(Page<CreditRecord> page, QueryWrapper<CreditRecord> queryWrapper);

    /**
     * 查询信用分流水
     *
     * @param id 信用分流水主键
     * @return 信用分流水
     */
    public CreditRecord selectCreditRecordById(Long id);

    /**
     * 新增信用分流水
     *
     * @param creditRecord 信用分流水
     * @return 结果
     */
    public int insertCreditRecord(CreditRecord creditRecord);

    /**
     * 修改信用分流水
     *
     * @param creditRecord 信用分流水
     * @return 结果
     */
    public int updateCreditRecord(CreditRecord creditRecord);

    /**
     * 批量删除信用分流水
     *
     * @param ids 需要删除的信用分流水主键集合
     * @return 结果
     */
    public int deleteCreditRecordByIds(Long[] ids);

    /**
     * 删除信用分流水信息
     *
     * @param id 信用分流水主键
     * @return 结果
     */
    public int deleteCreditRecordById(Long id);

    /**
     * 导入信用分流水数据
     *
     * @param list            数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    public String importCreditRecord(
    List<CreditRecord> list, int titleNum, Boolean
    isUpdateSupport,
    String operName);
}
