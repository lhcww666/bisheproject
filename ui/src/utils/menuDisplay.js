/**
 * 生成侧边栏展示菜单。
 *
 * 管理员保留“实验室业务”分组；学生和教师把业务子菜单展开为一级菜单。
 * 数据看板已经作为静态首页，过滤后端重复的 dashboard 子菜单。
 */
export function buildSidebarRoutes(routes, roles = []) {
  const roleKeys = Array.isArray(roles) ? roles : [];
  const flattenLabMenu = !roleKeys.includes("admin")
    && (roleKeys.includes("student") || roleKeys.includes("teacher"));
  return (routes || []).flatMap(route => {
    if (route.path !== "/module" || !Array.isArray(route.children)) {
      return [route];
    }
    const children = route.children.filter(child => child.path !== "dashboard");
    if (flattenLabMenu) {
      return children.map(child => ({
        ...child,
        path: joinMenuPath(route.path, child.path)
      }));
    }
    return [{ ...route, children }];
  });
}

export function joinMenuPath(basePath, childPath) {
  if (!childPath) return basePath;
  if (String(childPath).startsWith("/")) return childPath;
  return String(basePath).replace(/\/+$/, "") + "/" + String(childPath).replace(/^\/+/, "");
}
