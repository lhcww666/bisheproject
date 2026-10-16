package com.ruoyi.module.safety.service.impl;

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
import org.springframework.stereotype.Service;
import com.ruoyi.module.safety.mapper.SafetyExamRecordMapper;
import com.ruoyi.module.safety.domain.SafetyExamRecord;
import com.ruoyi.module.safety.service.ISafetyExamRecordService;

import jakarta.validation.Validator;

/**
 * 安全考试记录Service业务层处理
 *
 * @author itjialu
 * @date 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class SafetyExamRecordServiceImpl extends ServiceImpl<SafetyExamRecordMapper,SafetyExamRecord> implements ISafetyExamRecordService {
    private final SafetyExamRecordMapper safetyExamRecordMapper;
    protected final Validator validator;

    @Override
    public String importSafetyExamRecord(
    List<SafetyExamRecord> list, int titleNum, Boolean
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
            SafetyExamRecord safetyExamRecord =list.get(i);
            try {
                QueryWrapper<SafetyExamRecord> queryWrapper = new QueryWrapper<>();
                List<SafetyExamRecord> checkList = new ArrayList<>(); //safetyExamRecordMapper.selectList(queryWrapper);
                if (checkList.size() == 0) {
                    BeanValidators.validateWithException(validator, safetyExamRecord);
                    insertSafetyExamRecord(safetyExamRecord);
                    successNum++;
                    successMsg.append("<br/>" + successNum + "、记录" + (i + titleNum + 2) + "：" + safetyExamRecord.getId() + " 导入成功")
                    ;
                } else if (isUpdateSupport) {
                    BeanValidators.validateWithException(validator, safetyExamRecord);
                    safetyExamRecord.setId(checkList.get(0).getId());
                    updateSafetyExamRecord(safetyExamRecord);
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
    public IPage<SafetyExamRecord> pageSafetyExamRecord(Page<SafetyExamRecord> page, QueryWrapper<SafetyExamRecord> queryWrapper) {
        return safetyExamRecordMapper.selectPage(page, queryWrapper);
    }

    /**
     * 查询安全考试记录
     *
     * @param id 安全考试记录主键
     * @return 安全考试记录
     */
    @Override
    public SafetyExamRecord selectSafetyExamRecordById(Long id) {
        return safetyExamRecordMapper.selectById(id);
    }

    /**
     * 新增安全考试记录
     *
     * @param safetyExamRecord 安全考试记录
     * @return 结果
     */
    @Override
    public int insertSafetyExamRecord(SafetyExamRecord safetyExamRecord) {
            return safetyExamRecordMapper.insert(safetyExamRecord);
    }

    /**
     * 修改安全考试记录
     *
     * @param safetyExamRecord 安全考试记录
     * @return 结果
     */
    @Override
    public int updateSafetyExamRecord(SafetyExamRecord safetyExamRecord) {
        return safetyExamRecordMapper.updateById(safetyExamRecord);
    }

    /**
     * 批量删除安全考试记录
     *
     * @param ids 需要删除的安全考试记录主键
     * @return 结果
     */
    @Override
    public int deleteSafetyExamRecordByIds(Long[] ids) {
        return safetyExamRecordMapper.deleteByIds(Arrays.asList(ids));
    }

    /**
     * 删除安全考试记录信息
     *
     * @param id 安全考试记录主键
     * @return 结果
     */
    @Override
    public int deleteSafetyExamRecordById(Long id) {
        return safetyExamRecordMapper.deleteById(id);
    }
}
