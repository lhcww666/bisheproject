package com.ruoyi.module.device.service;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.module.device.domain .Device;

/**
 * 设备信息Service接口
 *
 * @author itjialu
 * @date 2026-10-16
 */
public interface IDeviceService extends IService<Device> {
    public IPage<Device> pageDevice(Page<Device> page, QueryWrapper<Device> queryWrapper);

    /**
     * 查询设备信息
     *
     * @param id 设备信息主键
     * @return 设备信息
     */
    public Device selectDeviceById(Long id);

    /**
     * 新增设备信息
     *
     * @param device 设备信息
     * @return 结果
     */
    public int insertDevice(Device device);

    /**
     * 修改设备信息
     *
     * @param device 设备信息
     * @return 结果
     */
    public int updateDevice(Device device);

    /**
     * 批量删除设备信息
     *
     * @param ids 需要删除的设备信息主键集合
     * @return 结果
     */
    public int deleteDeviceByIds(Long[] ids);

    /**
     * 删除设备信息信息
     *
     * @param id 设备信息主键
     * @return 结果
     */
    public int deleteDeviceById(Long id);

    /**
     * 导入设备信息数据
     *
     * @param list            数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    public String importDevice(
    List<Device> list, int titleNum, Boolean
    isUpdateSupport,
    String operName);
}
