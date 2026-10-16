package com.ruoyi.module.reservation.service.impl;

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
import com.ruoyi.module.reservation.mapper.ReservationMapper;
import com.ruoyi.module.reservation.domain.Reservation;
import com.ruoyi.module.reservation.service.IReservationService;

import jakarta.validation.Validator;

/**
 * 实验室预约Service业务层处理
 *
 * @author itjialu
 * @date 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class ReservationServiceImpl extends ServiceImpl<ReservationMapper,Reservation> implements IReservationService {
    private final ReservationMapper reservationMapper;
    protected final Validator validator;

    @Override
    public String importReservation(
    List<Reservation> list, int titleNum, Boolean
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
            Reservation reservation =list.get(i);
            try {
                QueryWrapper<Reservation> queryWrapper = new QueryWrapper<>();
                List<Reservation> checkList = new ArrayList<>(); //reservationMapper.selectList(queryWrapper);
                if (checkList.size() == 0) {
                    BeanValidators.validateWithException(validator, reservation);
                    insertReservation(reservation);
                    successNum++;
                    successMsg.append("<br/>" + successNum + "、记录" + (i + titleNum + 2) + "：" + reservation.getId() + " 导入成功")
                    ;
                } else if (isUpdateSupport) {
                    BeanValidators.validateWithException(validator, reservation);
                    reservation.setId(checkList.get(0).getId());
                    updateReservation(reservation);
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
    public IPage<Reservation> pageReservation(Page<Reservation> page, QueryWrapper<Reservation> queryWrapper) {
        return reservationMapper.selectPage(page, queryWrapper);
    }

    /**
     * 查询实验室预约
     *
     * @param id 实验室预约主键
     * @return 实验室预约
     */
    @Override
    public Reservation selectReservationById(Long id) {
        return reservationMapper.selectById(id);
    }

    /**
     * 新增实验室预约
     *
     * @param reservation 实验室预约
     * @return 结果
     */
    @Override
    public int insertReservation(Reservation reservation) {
                reservation.setCreateBy(SecurityUtils.getUsername());
                reservation.setCreateTime(DateUtils.getNowDate());
            return reservationMapper.insert(reservation);
    }

    /**
     * 修改实验室预约
     *
     * @param reservation 实验室预约
     * @return 结果
     */
    @Override
    public int updateReservation(Reservation reservation) {
                reservation.setUpdateTime(DateUtils.getNowDate());
                reservation.setUpdateBy(SecurityUtils.getUsername());
        return reservationMapper.updateById(reservation);
    }

    /**
     * 批量删除实验室预约
     *
     * @param ids 需要删除的实验室预约主键
     * @return 结果
     */
    @Override
    public int deleteReservationByIds(Long[] ids) {
        return reservationMapper.deleteByIds(Arrays.asList(ids));
    }

    /**
     * 删除实验室预约信息
     *
     * @param id 实验室预约主键
     * @return 结果
     */
    @Override
    public int deleteReservationById(Long id) {
        return reservationMapper.deleteById(id);
    }
}
