package com.ruoyi.module.device.service;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.module.device.domain .Device;
import com.ruoyi.module.room.domain.Room;

/**
 * 设备档案Service接口
 *
 * @author itjialu
 * @date 2026-10-16
 */
public interface IDeviceService extends IService<Device> {
    public IPage<Device> pageDevice(Page<Device> page, QueryWrapper<Device> queryWrapper);

    /**
     * 查询设备档案表单可选的实验室。
     *
     * @return 按实验室编号排序的实验室列表
     */
    public List<Room> selectRoomOptions();

    /**
     * 查询设备档案
     *
     * @param id 设备档案主键
     * @return 设备档案
     */
    public Device selectDeviceById(Long id);

    /**
     * 新增设备档案
     *
     * @param device 设备档案
     * @return 结果
     */
    public int insertDevice(Device device);

    /**
     * 修改设备档案
     *
     * @param device 设备档案
     * @return 结果
     */
    public int updateDevice(Device device);

    /**
     * 批量删除设备档案
     *
     * @param ids 需要删除的设备档案主键集合
     * @return 结果
     */
    public int deleteDeviceByIds(Long[] ids);

    /**
     * 删除设备档案信息
     *
     * @param id 设备档案主键
     * @return 结果
     */
    public int deleteDeviceById(Long id);

    /**
     * 导入设备档案数据
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
