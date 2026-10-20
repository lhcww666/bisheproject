package com.ruoyi.module.credit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.module.credit.domain.CreditAccount;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 信用分账户Mapper。
 *
 * @author itjialu
 */
public interface CreditAccountMapper extends BaseMapper<CreditAccount>
{
    @Select("SELECT * FROM credit_account WHERE user_id = #{userId} FOR UPDATE")
    CreditAccount selectByUserIdForUpdate(@Param("userId") Long userId);
}
