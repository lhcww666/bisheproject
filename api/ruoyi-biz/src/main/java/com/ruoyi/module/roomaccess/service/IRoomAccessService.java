package com.ruoyi.module.roomaccess.service;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.module.room.domain.Room;
import com.ruoyi.module.roomaccess.domain.RoomAccess;

/**
 * 实验室准入规则Service接口
 *
 * @author itjialu
 */
public interface IRoomAccessService
{
    IPage<RoomAccess> pageRoomAccess(Page<RoomAccess> page, QueryWrapper<RoomAccess> queryWrapper);

    RoomAccess selectRoomAccessById(Long id);

    int insertRoomAccess(RoomAccess roomAccess);

    int updateRoomAccess(RoomAccess roomAccess);

    int deleteRoomAccessByIds(Long[] ids);

    /**
     * 准入配置页面需要的实验室、部门树和学生下拉数据。
     */
    Map<String, Object> selectOptions();

    /**
     * 判断学生是否可以预约指定实验室。
     */
    boolean canBook(Long userId, Room room);

    /**
     * 校验预约准入，不满足时抛出业务异常。
     */
    void checkBookAccess(Long userId, Room room);

    /**
     * 判断某个班级是否对实验室有班级级准入资格。
     * 只认 CLASS/DEPT 规则，不把某个学生的 USER 白名单当作全班授权。
     */
    boolean hasClassAccess(Long classDeptId, Room room);

    /**
     * 从候选实验室中过滤出当前学生有权预约的实验室。
     */
    List<Room> filterBookableRooms(Long userId, List<Room> rooms);
}
