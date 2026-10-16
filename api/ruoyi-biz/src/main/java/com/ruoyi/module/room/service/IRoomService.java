package com.ruoyi.module.room.service;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.module.room.domain .Room;

/**
 * 实验室档案Service接口
 *
 * @author itjialu
 * @date 2026-10-16
 */
public interface IRoomService extends IService<Room> {
    public IPage<Room> pageRoom(Page<Room> page, QueryWrapper<Room> queryWrapper);

    /**
     * 查询实验室档案
     *
     * @param id 实验室档案主键
     * @return 实验室档案
     */
    public Room selectRoomById(Long id);

    /**
     * 新增实验室档案
     *
     * @param room 实验室档案
     * @return 结果
     */
    public int insertRoom(Room room);

    /**
     * 修改实验室档案
     *
     * @param room 实验室档案
     * @return 结果
     */
    public int updateRoom(Room room);

    /**
     * 批量删除实验室档案
     *
     * @param ids 需要删除的实验室档案主键集合
     * @return 结果
     */
    public int deleteRoomByIds(Long[] ids);

    /**
     * 删除实验室档案信息
     *
     * @param id 实验室档案主键
     * @return 结果
     */
    public int deleteRoomById(Long id);

    /**
     * 导入实验室档案数据
     *
     * @param list            数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    public String importRoom(
    List<Room> list, int titleNum, Boolean
    isUpdateSupport,
    String operName);
}
