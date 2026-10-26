# Change: 增加实验室耗材领用库存闭环

## Why

当前平台已经覆盖实验室、设备、预约、审核和准入，但实验室耗材仍缺少可追溯的申请、审核、库存扣减和取消归还能力。耗材库存必须在并发领用下保持非负，并通过库存流水保留每一次变化。

## What Changes

- 新增耗材档案、耗材领用申请和库存流水三张业务表，统一归属 `material` 模块。
- 管理员维护耗材档案和最低库存，并通过专用库存调整动作写入流水。
- 学生可针对本人待审核或已通过、且属于对应实验室的预约提交领用申请。
- 管理员审核领用申请；库存不足、重复审核和越权或失效申请不得扣减库存。
- 审核通过时使用事务和条件更新扣减库存，并写入流水。
- 预约取消时关闭未审核申请，归还已领取耗材并追加归还流水。
- 新增耗材库存、耗材领用和库存流水页面及角色菜单权限。

## Impact

- Affected specs: `material-usage`
- Affected code: `api/ruoyi-biz/src/main/java/com/ruoyi/module/material`、`api/ruoyi-biz/src/main/resources/mapper/module`、`ui/src/views/module/material`、`ui/src/api/module`、`ui/src/router`、`sql/module/42-material.sql`
- Data migration: 新增可重复执行的 `42-material.sql`，创建表、字典、菜单、角色授权和演示耗材数据
- Existing behavior: 预约取消在同一事务内触发耗材归还；存在耗材申请记录的预约不能直接删除
