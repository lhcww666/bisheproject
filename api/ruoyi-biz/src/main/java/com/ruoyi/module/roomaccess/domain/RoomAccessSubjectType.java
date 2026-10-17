package com.ruoyi.module.roomaccess.domain;

/**
 * 实验室准入授权对象类型，与字典 module_room_access_subject 保持一致。
 * COURSE 为后续课程授权预留，当前版本不参与匹配。
 *
 * @author itjialu
 */
public final class RoomAccessSubjectType
{
    public static final String DEPT = "DEPT";

    public static final String CLASS = "CLASS";

    public static final String USER = "USER";

    public static final String COURSE = "COURSE";

    private RoomAccessSubjectType()
    {
    }

    public static boolean isValidForCurrentVersion(String type)
    {
        return DEPT.equals(type) || CLASS.equals(type) || USER.equals(type);
    }
}
