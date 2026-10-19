-- 按单据职责收敛页面后，删除不再使用的按钮权限及角色绑定
USE bishe_schema;

DELETE FROM sys_role_menu
WHERE menu_id IN (
  SELECT menu_id FROM (
    SELECT menu_id FROM sys_menu
    WHERE perms IN (
      'module:creditRecord:add',
      'module:creditRecord:edit',
      'module:creditRecord:remove',
      'module:safetyExamRecord:add',
      'module:safetyExamRecord:edit',
      'module:safetyExamRecord:remove',
      'module:reservationDevice:add',
      'module:reservationDevice:edit',
      'module:reservationDevice:remove',
      'module:reservationDevice:import',
      'module:reservationReview:add',
      'module:reservationReview:edit',
      'module:reservationReview:remove',
      'module:reservationReview:import'
    )
  ) menu_ids
);

DELETE FROM sys_menu
WHERE perms IN (
  'module:creditRecord:add',
  'module:creditRecord:edit',
  'module:creditRecord:remove',
  'module:safetyExamRecord:add',
  'module:safetyExamRecord:edit',
  'module:safetyExamRecord:remove',
  'module:reservationDevice:add',
  'module:reservationDevice:edit',
  'module:reservationDevice:remove',
  'module:reservationDevice:import',
  'module:reservationReview:add',
  'module:reservationReview:edit',
  'module:reservationReview:remove',
  'module:reservationReview:import'
);

SELECT menu_name, perms
FROM sys_menu
WHERE perms LIKE 'module:creditRecord:%'
   OR perms LIKE 'module:safetyExamRecord:%'
   OR perms LIKE 'module:reservationDevice:%'
   OR perms LIKE 'module:reservationReview:%'
ORDER BY perms;
