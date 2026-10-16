package com.ruoyi.module.room.service.impl;

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
import com.ruoyi.module.room.mapper.RoomMapper;
import com.ruoyi.module.room.domain.Room;
import com.ruoyi.module.room.service.IRoomService;

import jakarta.validation.Validator;

/**
 * 实验室信息Service业务层处理
 *
 * @author itjialu
 * @date 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class RoomServiceImpl extends ServiceImpl<RoomMapper,Room> implements IRoomService {
    private final RoomMapper roomMapper;
    protected final Validator validator;

    @Override
    public String importRoom(
    List<Room> list, int titleNum, Boolean
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
            Room room =list.get(i);
            try {
                QueryWrapper<Room> queryWrapper = new QueryWrapper<>();
                List<Room> checkList = new ArrayList<>(); //roomMapper.selectList(queryWrapper);
                if (checkList.size() == 0) {
                    BeanValidators.validateWithException(validator, room);
                    insertRoom(room);
                    successNum++;
                    successMsg.append("<br/>" + successNum + "、记录" + (i + titleNum + 2) + "：" + room.getId() + " 导入成功")
                    ;
                } else if (isUpdateSupport) {
                    BeanValidators.validateWithException(validator, room);
                    room.setId(checkList.get(0).getId());
                    updateRoom(room);
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
    public IPage<Room> pageRoom(Page<Room> page, QueryWrapper<Room> queryWrapper) {
        return roomMapper.selectPage(page, queryWrapper);
    }

    /**
     * 查询实验室信息
     *
     * @param id 实验室信息主键
     * @return 实验室信息
     */
    @Override
    public Room selectRoomById(Long id) {
        return roomMapper.selectById(id);
    }

    /**
     * 新增实验室信息
     *
     * @param room 实验室信息
     * @return 结果
     */
    @Override
    public int insertRoom(Room room) {
                room.setCreateBy(SecurityUtils.getUsername());
                room.setCreateTime(DateUtils.getNowDate());
            return roomMapper.insert(room);
    }

    /**
     * 修改实验室信息
     *
     * @param room 实验室信息
     * @return 结果
     */
    @Override
    public int updateRoom(Room room) {
                room.setUpdateTime(DateUtils.getNowDate());
                room.setUpdateBy(SecurityUtils.getUsername());
        return roomMapper.updateById(room);
    }

    /**
     * 批量删除实验室信息
     *
     * @param ids 需要删除的实验室信息主键
     * @return 结果
     */
    @Override
    public int deleteRoomByIds(Long[] ids) {
        return roomMapper.deleteByIds(Arrays.asList(ids));
    }

    /**
     * 删除实验室信息信息
     *
     * @param id 实验室信息主键
     * @return 结果
     */
    @Override
    public int deleteRoomById(Long id) {
        return roomMapper.deleteById(id);
    }
}
