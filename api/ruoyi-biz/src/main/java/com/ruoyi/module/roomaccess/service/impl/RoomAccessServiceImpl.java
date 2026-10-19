package com.ruoyi.module.roomaccess.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.module.room.domain.Room;
import com.ruoyi.module.room.domain.RoomReservationMode;
import com.ruoyi.module.room.mapper.RoomMapper;
import com.ruoyi.module.roomaccess.domain.RoomAccess;
import com.ruoyi.module.roomaccess.domain.RoomAccessSubjectType;
import com.ruoyi.module.roomaccess.mapper.RoomAccessMapper;
import com.ruoyi.module.roomaccess.service.IRoomAccessService;
import com.ruoyi.system.service.ISysDeptService;
import com.ruoyi.system.service.ISysRoleService;
import com.ruoyi.system.service.ISysUserService;

import lombok.RequiredArgsConstructor;

/**
 * 实验室准入规则Service业务层处理
 *
 * @author itjialu
 */
@Service
@RequiredArgsConstructor
public class RoomAccessServiceImpl extends ServiceImpl<RoomAccessMapper, RoomAccess>
        implements IRoomAccessService
{
    private final RoomAccessMapper roomAccessMapper;

    private final RoomMapper roomMapper;

    private final ISysUserService sysUserService;

    private final ISysDeptService sysDeptService;

    private final ISysRoleService sysRoleService;

    @Override
    public IPage<RoomAccess> pageRoomAccess(Page<RoomAccess> page, QueryWrapper<RoomAccess> queryWrapper)
    {
        IPage<RoomAccess> pageData = roomAccessMapper.selectPage(page, queryWrapper);
        fillDisplayNames(pageData.getRecords());
        return pageData;
    }

    @Override
    public RoomAccess selectRoomAccessById(Long id)
    {
        RoomAccess roomAccess = roomAccessMapper.selectById(id);
        if (roomAccess != null)
        {
            fillDisplayNames(List.of(roomAccess));
        }
        return roomAccess;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertRoomAccess(RoomAccess roomAccess)
    {
        validateRoomAccess(roomAccess, false);
        roomAccess.setCreateBy(SecurityUtils.getUsername());
        roomAccess.setCreateTime(DateUtils.getNowDate());
        return roomAccessMapper.insert(roomAccess);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateRoomAccess(RoomAccess roomAccess)
    {
        if (roomAccess.getId() == null)
        {
            throw new ServiceException("准入规则ID不能为空");
        }
        validateRoomAccess(roomAccess, true);
        roomAccess.setUpdateBy(SecurityUtils.getUsername());
        roomAccess.setUpdateTime(DateUtils.getNowDate());
        return roomAccessMapper.updateById(roomAccess);
    }

    @Override
    public int deleteRoomAccessByIds(Long[] ids)
    {
        return roomAccessMapper.deleteByIds(Arrays.asList(ids));
    }

    @Override
    public Map<String, Object> selectOptions()
    {
        Map<String, Object> options = new HashMap<>();
        QueryWrapper<Room> roomQuery = new QueryWrapper<>();
        roomQuery.orderByAsc("room_code");
        options.put("rooms", roomMapper.selectList(roomQuery));
        options.put("depts", sysDeptService.selectDeptTreeList(new SysDept()));

        List<Map<String, Object>> students = new ArrayList<>();
        SysRole roleQuery = new SysRole();
        roleQuery.setRoleKey("student");
        SysRole studentRole = sysRoleService.selectRoleList(roleQuery).stream()
                .filter(role -> "student".equals(role.getRoleKey()))
                .findFirst()
                .orElse(null);
        if (studentRole != null)
        {
            SysUser userQuery = new SysUser();
            userQuery.setRoleId(studentRole.getRoleId());
            for (SysUser user : sysUserService.selectAllocatedList(userQuery))
            {
                if (!"0".equals(user.getStatus()))
                {
                    continue;
                }
                Map<String, Object> student = new HashMap<>();
                student.put("userId", user.getUserId());
                student.put("userName", user.getUserName());
                student.put("nickName", user.getNickName());
                students.add(student);
            }
        }
        options.put("students", students);
        return options;
    }

    @Override
    public boolean canBook(Long userId, Room room)
    {
        if (room == null)
        {
            return false;
        }
        SysUser user = loadUser(userId);
        if (user == null)
        {
            return false;
        }
        List<RoomAccess> rules = selectActiveRules(List.of(room.getId()));
        return canBook(user, room, rules);
    }

    @Override
    public void checkBookAccess(Long userId, Room room)
    {
        if (room == null)
        {
            throw new ServiceException("实验室不存在");
        }
        SysUser user = loadUser(userId);
        if (user == null)
        {
            throw new ServiceException("当前用户不存在");
        }
        if (isAdmin(user))
        {
            return;
        }
        String mode = normalizeMode(room.getReservationMode());
        if (RoomReservationMode.CLOSED.equals(mode))
        {
            throw new ServiceException("该实验室当前已停止学生预约");
        }
        if (RoomReservationMode.OPEN.equals(mode))
        {
            return;
        }
        List<RoomAccess> rules = selectActiveRules(List.of(room.getId()));
        if (!matchesAny(user, rules))
        {
            throw new ServiceException("你不在该实验室的预约范围内，请联系实验室管理员开通准入权限");
        }
    }

    @Override
    public boolean hasClassAccess(Long classDeptId, Room room)
    {
        if (classDeptId == null || room == null)
        {
            return false;
        }
        String mode = normalizeMode(room.getReservationMode());
        if (RoomReservationMode.CLOSED.equals(mode))
        {
            return false;
        }
        if (RoomReservationMode.OPEN.equals(mode))
        {
            return true;
        }
        SysDept classDept = sysDeptService.selectDeptById(classDeptId);
        if (classDept == null)
        {
            return false;
        }
        for (RoomAccess rule : selectActiveRules(List.of(room.getId())))
        {
            if (RoomAccessSubjectType.CLASS.equals(rule.getSubjectType())
                    && Objects.equals(rule.getSubjectId(), classDeptId))
            {
                return true;
            }
            if (RoomAccessSubjectType.DEPT.equals(rule.getSubjectType()))
            {
                if (Objects.equals(rule.getSubjectId(), classDeptId))
                {
                    return true;
                }
                if (StringUtils.isNotEmpty(classDept.getAncestors())
                        && Arrays.asList(classDept.getAncestors().split(","))
                                .contains(String.valueOf(rule.getSubjectId())))
                {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public List<Room> filterBookableRooms(Long userId, List<Room> rooms)
    {
        if (rooms == null || rooms.isEmpty())
        {
            return Collections.emptyList();
        }
        SysUser user = loadUser(userId);
        if (user == null)
        {
            return Collections.emptyList();
        }
        Set<Long> roomIds = rooms.stream().map(Room::getId).filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, List<RoomAccess>> rulesByRoom = selectActiveRules(roomIds).stream()
                .collect(Collectors.groupingBy(RoomAccess::getRoomId));
        List<Room> result = new ArrayList<>();
        for (Room room : rooms)
        {
            if (canBook(user, room, rulesByRoom.getOrDefault(room.getId(), Collections.emptyList())))
            {
                result.add(room);
            }
        }
        return result;
    }

    private boolean canBook(SysUser user, Room room, List<RoomAccess> rules)
    {
        if (isAdmin(user))
        {
            return true;
        }
        String mode = normalizeMode(room.getReservationMode());
        if (RoomReservationMode.CLOSED.equals(mode))
        {
            return false;
        }
        if (RoomReservationMode.OPEN.equals(mode))
        {
            return true;
        }
        if (!RoomReservationMode.RESTRICTED.equals(mode) && !RoomReservationMode.APPROVAL_ONLY.equals(mode))
        {
            return false;
        }
        return matchesAny(user, rules);
    }

    private boolean matchesAny(SysUser user, List<RoomAccess> rules)
    {
        if (user.getDeptId() == null)
        {
            return false;
        }
        for (RoomAccess rule : rules)
        {
            if (matches(user, rule))
            {
                return true;
            }
        }
        return false;
    }

    private boolean matches(SysUser user, RoomAccess rule)
    {
        String type = rule.getSubjectType();
        if (RoomAccessSubjectType.USER.equals(type))
        {
            return Objects.equals(rule.getSubjectId(), user.getUserId());
        }
        if (RoomAccessSubjectType.CLASS.equals(type))
        {
            return Objects.equals(rule.getSubjectId(), user.getDeptId());
        }
        if (RoomAccessSubjectType.DEPT.equals(type))
        {
            if (Objects.equals(rule.getSubjectId(), user.getDeptId()))
            {
                return true;
            }
            SysDept dept = sysDeptService.selectDeptById(user.getDeptId());
            if (dept == null || StringUtils.isEmpty(dept.getAncestors()))
            {
                return false;
            }
            return Arrays.asList(dept.getAncestors().split(",")).contains(String.valueOf(rule.getSubjectId()));
        }
        // COURSE 为后续课程授权预留，当前版本不参与匹配
        return false;
    }

    private List<RoomAccess> selectActiveRules(Collection<Long> roomIds)
    {
        if (roomIds == null || roomIds.isEmpty())
        {
            return Collections.emptyList();
        }
        QueryWrapper<RoomAccess> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("room_id", roomIds);
        queryWrapper.and(wrapper -> wrapper.isNull("valid_from").or().le("valid_from", new Date()));
        queryWrapper.and(wrapper -> wrapper.isNull("valid_until").or().ge("valid_until", new Date()));
        return roomAccessMapper.selectList(queryWrapper);
    }

    private SysUser loadUser(Long userId)
    {
        if (userId == null)
        {
            return null;
        }
        return sysUserService.selectUserById(userId);
    }

    private boolean isAdmin(SysUser user)
    {
        if (user == null || user.getRoles() == null)
        {
            return false;
        }
        return user.getRoles().stream().anyMatch(role -> "admin".equals(role.getRoleKey())
                || "1".equals(role.getDataScope()));
    }

    private String normalizeMode(String mode)
    {
        if (StringUtils.isEmpty(mode))
        {
            return RoomReservationMode.OPEN;
        }
        return RoomReservationMode.isValid(mode) ? mode : RoomReservationMode.CLOSED;
    }

    private void validateRoomAccess(RoomAccess roomAccess, boolean update)
    {
        if (roomAccess == null)
        {
            throw new ServiceException("准入规则不能为空");
        }
        if (roomAccess.getRoomId() == null)
        {
            throw new ServiceException("请选择实验室");
        }
        if (roomMapper.selectById(roomAccess.getRoomId()) == null)
        {
            throw new ServiceException("实验室不存在");
        }
        String subjectType = StringUtils.isEmpty(roomAccess.getSubjectType()) ? null
                : roomAccess.getSubjectType().trim().toUpperCase();
        if (!RoomAccessSubjectType.isValidForCurrentVersion(subjectType))
        {
            throw new ServiceException("授权对象只能是院系、班级或学生");
        }
        roomAccess.setSubjectType(subjectType);
        if (roomAccess.getSubjectId() == null)
        {
            throw new ServiceException("请选择授权对象");
        }
        if (RoomAccessSubjectType.USER.equals(subjectType))
        {
            SysUser user = sysUserService.selectUserById(roomAccess.getSubjectId());
            if (user == null)
            {
                throw new ServiceException("学生不存在");
            }
            if (user.getRoles() == null || user.getRoles().stream()
                    .noneMatch(role -> "student".equals(role.getRoleKey())))
            {
                throw new ServiceException("学生白名单只能选择学生账号");
            }
        }
        else if (sysDeptService.selectDeptById(roomAccess.getSubjectId()) == null)
        {
            throw new ServiceException("院系或班级不存在");
        }
        if (roomAccess.getValidFrom() != null && roomAccess.getValidUntil() != null
                && roomAccess.getValidFrom().after(roomAccess.getValidUntil()))
        {
            throw new ServiceException("生效时间不能晚于失效时间");
        }
        QueryWrapper<RoomAccess> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("room_id", roomAccess.getRoomId());
        queryWrapper.eq("subject_type", roomAccess.getSubjectType());
        queryWrapper.eq("subject_id", roomAccess.getSubjectId());
        if (update)
        {
            queryWrapper.ne("id", roomAccess.getId());
        }
        if (roomAccessMapper.selectCount(queryWrapper) > 0)
        {
            throw new ServiceException("该实验室已经配置过相同的准入对象");
        }
    }

    private void fillDisplayNames(List<RoomAccess> records)
    {
        if (records == null || records.isEmpty())
        {
            return;
        }
        Set<Long> roomIds = records.stream().map(RoomAccess::getRoomId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> roomNameMap = new HashMap<>();
        if (!roomIds.isEmpty())
        {
            for (Room room : roomMapper.selectBatchIds(roomIds))
            {
                roomNameMap.put(room.getId(), room.getRoomName());
            }
        }
        Map<Long, String> deptNameMap = new HashMap<>();
        Map<Long, String> userNameMap = new HashMap<>();
        for (RoomAccess record : records)
        {
            record.setRoomName(roomNameMap.get(record.getRoomId()));
            if (RoomAccessSubjectType.USER.equals(record.getSubjectType()))
            {
                if (!userNameMap.containsKey(record.getSubjectId()))
                {
                    SysUser user = sysUserService.selectUserById(record.getSubjectId());
                    String name = null;
                    if (user != null)
                    {
                        name = StringUtils.isNotEmpty(user.getNickName()) ? user.getNickName() : user.getUserName();
                    }
                    userNameMap.put(record.getSubjectId(), name);
                }
                record.setSubjectName(userNameMap.get(record.getSubjectId()));
            }
            else
            {
                if (!deptNameMap.containsKey(record.getSubjectId()))
                {
                    SysDept dept = sysDeptService.selectDeptById(record.getSubjectId());
                    deptNameMap.put(record.getSubjectId(), dept == null ? null : dept.getDeptName());
                }
                record.setSubjectName(deptNameMap.get(record.getSubjectId()));
            }
        }
    }
}
