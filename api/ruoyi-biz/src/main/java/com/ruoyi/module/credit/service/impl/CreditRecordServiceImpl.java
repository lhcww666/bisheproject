package com.ruoyi.module.credit.service.impl;

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.bean.BeanValidators;
import com.ruoyi.common.exception.ServiceException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
        import com.ruoyi.common.utils.DateUtils;
import org.springframework.stereotype.Service;
import com.ruoyi.module.credit.mapper.CreditRecordMapper;
import com.ruoyi.module.credit.domain.CreditRecord;
import com.ruoyi.module.credit.service.ICreditRecordService;

import jakarta.validation.Validator;

/**
 * 信用分流水Service业务层处理
 *
 * @author itjialu
 * @date 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class CreditRecordServiceImpl extends ServiceImpl<CreditRecordMapper,CreditRecord> implements ICreditRecordService {
    private final CreditRecordMapper creditRecordMapper;
    protected final Validator validator;

    @Override
    public String importCreditRecord(
    List<CreditRecord> list, int titleNum, Boolean
    isUpdateSupport,
    String operName)

    {
        if (StringUtils.isNull(list) || list.size() == 0) {
            throw new ServiceException("导入数据不能为空！" );
        }
        int successNum = 0;
        int failureNum = 0;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder failureMsg = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            CreditRecord creditRecord =list.get(i);
            try {
                QueryWrapper<CreditRecord> queryWrapper = new QueryWrapper<>();
                List<CreditRecord> checkList = new ArrayList<>(); //creditRecordMapper.selectList(queryWrapper);
                if (checkList.size() == 0) {
                    BeanValidators.validateWithException(validator, creditRecord);
                    insertCreditRecord(creditRecord);
                    successNum++;
                    successMsg.append("<br/>" + successNum + "、记录" + (i + titleNum + 2) + "：" + creditRecord.getId() + " 导入成功")
                    ;
                } else if (isUpdateSupport) {
                    BeanValidators.validateWithException(validator, creditRecord);
                    creditRecord.setId(checkList.get(0).getId());
                    updateCreditRecord(creditRecord);
                    successNum++;
                    successMsg.append("<br/>" + successNum + "、记录" + (i + titleNum + 2) + " 更新成功")
                    ;
                } else {
                    failureNum++;
                    failureMsg.append("<br/>" + failureNum + "、记录" + (i + titleNum + 2) + " 已存在")
                    ;
                }
            } catch (Exception e) {
                failureNum++;
                String msg = "<br/>" + failureNum + "、记录" + (i + titleNum + 2) + " 导入失败：";
                failureMsg.append(msg + e.getMessage());
                log.error(msg, e);
            }
        }
        if (failureNum > 0) {
            failureMsg.insert(0, "很抱歉，导入失败！共 " + failureNum + " 条数据格式不正确，错误如下：" );
            throw new ServiceException(failureMsg.toString());
        } else {
            successMsg.insert(0, "恭喜您，数据已全部导入成功！共 " + successNum + " 条，数据如下：" );
        }
        return successMsg.toString();
    }

    @Override
    public IPage<CreditRecord> pageCreditRecord(Page<CreditRecord> page, QueryWrapper<CreditRecord> queryWrapper) {
        return creditRecordMapper.selectPage(page, queryWrapper);
    }

    /**
     * 查询信用分流水
     *
     * @param id 信用分流水主键
     * @return 信用分流水
     */
    @Override
    public CreditRecord selectCreditRecordById(Long id) {
        return creditRecordMapper.selectById(id);
    }

    /**
     * 新增信用分流水
     *
     * @param creditRecord 信用分流水
     * @return 结果
     */
    @Override
    public int insertCreditRecord(CreditRecord creditRecord) {
                creditRecord.setUserId(SecurityUtils.getUserId());
                creditRecord.setCreateBy(SecurityUtils.getUsername());
                creditRecord.setCreateTime(DateUtils.getNowDate());
            return creditRecordMapper.insert(creditRecord);
    }

    /**
     * 修改信用分流水
     *
     * @param creditRecord 信用分流水
     * @return 结果
     */
    @Override
    public int updateCreditRecord(CreditRecord creditRecord) {
        return creditRecordMapper.updateById(creditRecord);
    }

    /**
     * 批量删除信用分流水
     *
     * @param ids 需要删除的信用分流水主键
     * @return 结果
     */
    @Override
    public int deleteCreditRecordByIds(Long[] ids) {
        return creditRecordMapper.deleteByIds(Arrays.asList(ids));
    }

    /**
     * 删除信用分流水信息
     *
     * @param id 信用分流水主键
     * @return 结果
     */
    @Override
    public int deleteCreditRecordById(Long id) {
        return creditRecordMapper.deleteById(id);
    }
}
