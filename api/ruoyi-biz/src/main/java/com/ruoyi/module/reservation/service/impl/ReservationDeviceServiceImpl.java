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
import org.springframework.stereotype.Service;
import com.ruoyi.module.reservation.mapper.ReservationDeviceMapper;
import com.ruoyi.module.reservation.domain.ReservationDevice;
import com.ruoyi.module.reservation.service.IReservationDeviceService;

import jakarta.validation.Validator;

/**
 * 预约设备关联Service业务层处理
 *
 * @author itjialu
 * @date 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class ReservationDeviceServiceImpl extends ServiceImpl<ReservationDeviceMapper,ReservationDevice> implements IReservationDeviceService {
    private final ReservationDeviceMapper reservationDeviceMapper;
    protected final Validator validator;

    @Override
    public String importReservationDevice(
    List<ReservationDevice> list, int titleNum, Boolean
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
            ReservationDevice reservationDevice =list.get(i);
            try {
                QueryWrapper<ReservationDevice> queryWrapper = new QueryWrapper<>();
                List<ReservationDevice> checkList = new ArrayList<>(); //reservationDeviceMapper.selectList(queryWrapper);
                if (checkList.size() == 0) {
                    BeanValidators.validateWithException(validator, reservationDevice);
                    insertReservationDevice(reservationDevice);
                    successNum++;
                    successMsg.append("<br/>" + successNum + "、记录" + (i + titleNum + 2) + "：" + reservationDevice.getId() + " 导入成功")
                    ;
                } else if (isUpdateSupport) {
                    BeanValidators.validateWithException(validator, reservationDevice);
                    reservationDevice.setId(checkList.get(0).getId());
                    updateReservationDevice(reservationDevice);
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
    public IPage<ReservationDevice> pageReservationDevice(Page<ReservationDevice> page, QueryWrapper<ReservationDevice> queryWrapper) {
        return reservationDeviceMapper.selectPage(page, queryWrapper);
    }

    /**
     * 查询预约设备关联
     *
     * @param id 预约设备关联主键
     * @return 预约设备关联
     */
    @Override
    public ReservationDevice selectReservationDeviceById(Long id) {
        return reservationDeviceMapper.selectById(id);
    }

    /**
     * 新增预约设备关联
     *
     * @param reservationDevice 预约设备关联
     * @return 结果
     */
    @Override
    public int insertReservationDevice(ReservationDevice reservationDevice) {
            return reservationDeviceMapper.insert(reservationDevice);
    }

    /**
     * 修改预约设备关联
     *
     * @param reservationDevice 预约设备关联
     * @return 结果
     */
    @Override
    public int updateReservationDevice(ReservationDevice reservationDevice) {
        return reservationDeviceMapper.updateById(reservationDevice);
    }

    /**
     * 批量删除预约设备关联
     *
     * @param ids 需要删除的预约设备关联主键
     * @return 结果
     */
    @Override
    public int deleteReservationDeviceByIds(Long[] ids) {
        return reservationDeviceMapper.deleteByIds(Arrays.asList(ids));
    }

    /**
     * 删除预约设备关联信息
     *
     * @param id 预约设备关联主键
     * @return 结果
     */
    @Override
    public int deleteReservationDeviceById(Long id) {
        return reservationDeviceMapper.deleteById(id);
    }
}
