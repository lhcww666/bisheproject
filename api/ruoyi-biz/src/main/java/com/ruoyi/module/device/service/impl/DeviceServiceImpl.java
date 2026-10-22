package com.ruoyi.module.device.service.impl;

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
import com.ruoyi.module.device.mapper.DeviceMapper;
import com.ruoyi.module.device.domain.Device;
import com.ruoyi.module.device.service.IDeviceService;
import com.ruoyi.module.room.domain.Room;
import com.ruoyi.module.room.mapper.RoomMapper;

import jakarta.validation.Validator;

/**
 * 设备档案Service业务层处理
 *
 * @author itjialu
 * @date 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class DeviceServiceImpl extends ServiceImpl<DeviceMapper,Device> implements IDeviceService {
    private final DeviceMapper deviceMapper;
    private final RoomMapper roomMapper;
    protected final Validator validator;

    @Override
    public String importDevice(
    List<Device> list, int titleNum, Boolean
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
            Device device =list.get(i);
            try {
                QueryWrapper<Device> queryWrapper = new QueryWrapper<>();
                List<Device> checkList = new ArrayList<>(); //deviceMapper.selectList(queryWrapper);
                if (checkList.size() == 0) {
                    BeanValidators.validateWithException(validator, device);
                    insertDevice(device);
                    successNum++;
                    successMsg.append("<br/>" + successNum + "、记录" + (i + titleNum + 2) + "：" + device.getId() + " 导入成功")
                    ;
                } else if (isUpdateSupport) {
                    BeanValidators.validateWithException(validator, device);
                    device.setId(checkList.get(0).getId());
                    updateDevice(device);
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
    public IPage<Device> pageDevice(Page<Device> page, QueryWrapper<Device> queryWrapper) {
        IPage<Device> pageData = deviceMapper.selectPage(page, queryWrapper);
        fillRoomDisplayFields(pageData.getRecords());
        return pageData;
    }

    @Override
    public List<Room> selectRoomOptions() {
        QueryWrapper<Room> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByAsc("room_code");
        return roomMapper.selectList(queryWrapper);
    }

    /**
     * 查询设备档案
     *
     * @param id 设备档案主键
     * @return 设备档案
     */
    @Override
    public Device selectDeviceById(Long id) {
        Device device = deviceMapper.selectById(id);
        if (device != null) {
            fillRoomDisplayFields(List.of(device));
        }
        return device;
    }

    /**
     * 新增设备档案
     *
     * @param device 设备档案
     * @return 结果
     */
    @Override
    public int insertDevice(Device device) {
                device.setCreateBy(SecurityUtils.getUsername());
                device.setCreateTime(DateUtils.getNowDate());
            return deviceMapper.insert(device);
    }

    /**
     * 修改设备档案
     *
     * @param device 设备档案
     * @return 结果
     */
    @Override
    public int updateDevice(Device device) {
                device.setUpdateTime(DateUtils.getNowDate());
                device.setUpdateBy(SecurityUtils.getUsername());
        return deviceMapper.updateById(device);
    }

    /**
     * 批量删除设备档案
     *
     * @param ids 需要删除的设备档案主键
     * @return 结果
     */
    @Override
    public int deleteDeviceByIds(Long[] ids) {
        return deviceMapper.deleteByIds(Arrays.asList(ids));
    }

    /**
     * 删除设备档案信息
     *
     * @param id 设备档案主键
     * @return 结果
     */
    @Override
    public int deleteDeviceById(Long id) {
        return deviceMapper.deleteById(id);
    }

    /**
     * 批量补充实验室编号和名称，避免前端只看到 roomId。
     */
    private void fillRoomDisplayFields(List<Device> devices) {
        if (devices == null || devices.isEmpty()) {
            return;
        }
        Set<Long> roomIds = devices.stream()
                .map(Device::getRoomId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (roomIds.isEmpty()) {
            return;
        }
        Map<Long, Room> roomMap = new HashMap<>();
        for (Room room : roomMapper.selectBatchIds(roomIds)) {
            roomMap.put(room.getId(), room);
        }
        for (Device device : devices) {
            Room room = roomMap.get(device.getRoomId());
            if (room != null) {
                device.setRoomCode(room.getRoomCode());
                device.setRoomName(room.getRoomName());
            }
        }
    }
}
