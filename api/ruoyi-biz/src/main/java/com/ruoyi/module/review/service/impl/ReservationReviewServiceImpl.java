package com.ruoyi.module.review.service.impl;

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
import com.ruoyi.module.review.mapper.ReservationReviewMapper;
import com.ruoyi.module.review.domain.ReservationReview;
import com.ruoyi.module.review.service.IReservationReviewService;

import jakarta.validation.Validator;

/**
 * 预约审核Service业务层处理
 *
 * @author itjialu
 * @date 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class ReservationReviewServiceImpl extends ServiceImpl<ReservationReviewMapper,ReservationReview> implements IReservationReviewService {
    private final ReservationReviewMapper reservationReviewMapper;
    protected final Validator validator;

    @Override
    public String importReservationReview(
    List<ReservationReview> list, int titleNum, Boolean
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
            ReservationReview reservationReview =list.get(i);
            try {
                QueryWrapper<ReservationReview> queryWrapper = new QueryWrapper<>();
                List<ReservationReview> checkList = new ArrayList<>(); //reservationReviewMapper.selectList(queryWrapper);
                if (checkList.size() == 0) {
                    BeanValidators.validateWithException(validator, reservationReview);
                    insertReservationReview(reservationReview);
                    successNum++;
                    successMsg.append("<br/>" + successNum + "、记录" + (i + titleNum + 2) + "：" + reservationReview.getId() + " 导入成功")
                    ;
                } else if (isUpdateSupport) {
                    BeanValidators.validateWithException(validator, reservationReview);
                    reservationReview.setId(checkList.get(0).getId());
                    updateReservationReview(reservationReview);
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
    public IPage<ReservationReview> pageReservationReview(Page<ReservationReview> page, QueryWrapper<ReservationReview> queryWrapper) {
        return reservationReviewMapper.selectPage(page, queryWrapper);
    }

    /**
     * 查询预约审核
     *
     * @param id 预约审核主键
     * @return 预约审核
     */
    @Override
    public ReservationReview selectReservationReviewById(Long id) {
        return reservationReviewMapper.selectById(id);
    }

    /**
     * 新增预约审核
     *
     * @param reservationReview 预约审核
     * @return 结果
     */
    @Override
    public int insertReservationReview(ReservationReview reservationReview) {
            return reservationReviewMapper.insert(reservationReview);
    }

    /**
     * 修改预约审核
     *
     * @param reservationReview 预约审核
     * @return 结果
     */
    @Override
    public int updateReservationReview(ReservationReview reservationReview) {
        return reservationReviewMapper.updateById(reservationReview);
    }

    /**
     * 批量删除预约审核
     *
     * @param ids 需要删除的预约审核主键
     * @return 结果
     */
    @Override
    public int deleteReservationReviewByIds(Long[] ids) {
        return reservationReviewMapper.deleteByIds(Arrays.asList(ids));
    }

    /**
     * 删除预约审核信息
     *
     * @param id 预约审核主键
     * @return 结果
     */
    @Override
    public int deleteReservationReviewById(Long id) {
        return reservationReviewMapper.deleteById(id);
    }
}
