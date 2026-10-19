-- 已生成模块统一改为 list.vue 作为菜单入口
USE bishe_schema;

UPDATE sys_menu SET component = 'module/room/list' WHERE perms = 'module:room:list';
UPDATE sys_menu SET component = 'module/device/list' WHERE perms = 'module:device:list';
UPDATE sys_menu SET component = 'module/creditRecord/list' WHERE perms = 'module:creditRecord:list';
UPDATE sys_menu SET component = 'module/safetyExamRecord/list' WHERE perms = 'module:safetyExamRecord:list';
UPDATE sys_menu SET component = 'module/reservationDevice/list' WHERE perms = 'module:reservationDevice:list';
UPDATE sys_menu SET component = 'module/reservationReview/list' WHERE perms = 'module:reservationReview:list';
UPDATE sys_menu SET component = 'module/roomAccess/list' WHERE perms = 'module:roomAccess:list';
UPDATE sys_menu SET component = 'module/classRepresentative/list' WHERE perms = 'module:classRepresentative:list';

SELECT menu_id, menu_name, path, component, perms
FROM sys_menu
WHERE perms IN (
  'module:room:list',
  'module:device:list',
  'module:creditRecord:list',
  'module:safetyExamRecord:list',
  'module:reservationDevice:list',
  'module:reservationReview:list',
  'module:roomAccess:list',
  'module:classRepresentative:list'
)
ORDER BY menu_id;
