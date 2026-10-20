package com.ruoyi.module.room.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.module.room.domain.Room;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 实验室档案Mapper接口
 *
 * @author itjialu
 * @date 2026-10-16
 */
public interface RoomMapper extends BaseMapper<Room> {

    /**
     * 预约写入前锁定实验室行，串行化同一实验室的冲突检查与插入。
     */
    @Select("SELECT * FROM room WHERE id = #{id} FOR UPDATE")
    Room selectByIdForUpdate(@Param("id") Long id);
}
