# 项目全局说明（AI 协作必读）

本文件是项目的全局约定。开始任何一次需求开发、缺陷修复、文档或配置变更之前，先读一遍本文件。

`api/AGENTS.md`（OpenSpec 流程）、`ui/CLAUDE.md` 等更深层目录中的专属规则同时生效；发生冲突时，以更深层目录的规则为准。

## 一、项目概况

| 项目 | 内容 |
| --- | --- |
| 题目 | 基于大语言模型 Agent 的校园实验室智能预约与安全管理平台设计与实现 |
| 类型 | 本科毕业设计，前后端分离的单体应用 |
| 核心目标 | 在传统实验室管理平台之上，用 Spring AI 的 Tool Calling 与 RAG 提供一个受控、可追溯的智能助手 |
| 完整方案 | 根目录 `基于大语言模型Agent的校园实验室智能预约与安全管理平台-计划书.md` |

实施节奏是“业务系统先行，AI 后接入”：先把实验室、设备、预约、审核、安全考试、信用分的业务闭环做通，再把已有后端能力封装成受控工具交给 Agent 调用。不要为了演示 AI 而跳过业务规则。

当前进度：第一期的 7 张业务表（`room`、`device`、`reservation`、`reservation_device`、`reservation_review`、`safety_exam_record`、`credit_record`）已生成前后端 CRUD 并跑通页面。预约冲突校验、审核流转、实验室准入、班级预约、签到/完成/爽约、信用账户与违规记录、安全考试题库与在线答题、角色感知的数据看板、30 台实验室演示设备和预约设备多选已实现；`springai` 分支已接入 Spring AI 会话、学生/教师/管理员三套角色助手、按角色隔离的工具白名单、综合预约资格与当前时段可约查询、工具调用审计、MySQL 关键词/TF-IDF 的 RAG 检索基线、受控预约草稿确认、管理员 Agent 审计页、40 篇/100 片段的实验室安全知识库、58 条 Agent 离线评测基线和 DeepSeek 连接自检，向量 embedding 与 LangChain4j 对比分支尚未实现。

## 二、技术栈与版本

| 层次 | 技术选型 |
| --- | --- |
| 前端 | Vue 3.5 + Vite 6 + Element Plus 2.10 + Axios + ECharts，状态管理 Pinia，包管理 npm |
| 后端 | Java 17+（本地用 JDK 21）、Spring Boot 3.5、Maven 多模块 |
| 数据访问 | MyBatis-Plus（业务模块）、MyBatis + PageHelper（若依原有部分） |
| 数据库 | MySQL 8（本地库名 `bishe_schema`） |
| 认证授权 | Spring Security + JWT + 若依菜单权限体系 |
| 缓存 | Redis（登录态、验证码） |
| 基础框架 | 若依 RuoYi-SpringBoot3-Pro（后端）、RuoYi-SpringBoot3-ElementPlus v3.9.1（前端） |
| 规划中的 AI 能力 | Spring AI + DeepSeek（或通义千问）、RAG + pgvector/Chroma |

## 三、目录结构

```text
D:\bishe_project
├── api        Spring Boot 3 后端（若依多模块工程）
├── ui         Vue 3 前端（若依 Element Plus 工程）
├── docs       需求、设计、开发与落地说明文档
├── sql        数据库脚本
├── 后端       旧版若依框架参考代码，仅作参考，不参与构建（已被 .gitignore 忽略）
└── 命名规范-最终版.md    命名口径的唯一权威来源
```

后端模块职责：

```text
api
├── ruoyi-admin       启动模块，配置文件在 src/main/resources
├── ruoyi-framework   框架层（安全、拦截器）：尽量不改
├── ruoyi-system      若依系统模块（用户、角色、菜单）：尽量不改
├── ruoyi-common      通用工具与基类：尽量不改
├── ruoyi-quartz      定时任务
├── ruoyi-generator   代码生成器（含模板，见第六节）
└── ruoyi-biz         业务模块，本项目所有实验室业务代码写在这里
```

业务包与页面的对应关系（第一期已落地）：

| 表 | 后端包 | 前端页面 | 权限前缀 |
| --- | --- | --- | --- |
| `room` | `com.ruoyi.module.room` | `ui/src/views/module/room` | `module:room:*` |
| `device` | `com.ruoyi.module.device` | `ui/src/views/module/device` | `module:device:*` |
| `reservation` | `com.ruoyi.module.reservation` | `ui/src/views/module/reservation` | `module:reservation:*` |
| `reservation_device` | `com.ruoyi.module.reservation` | `ui/src/views/module/reservationDevice` | `module:reservationDevice:*` |
| `reservation_review` | `com.ruoyi.module.review` | `ui/src/views/module/reservationReview` | `module:reservationReview:*` |
| `safety_exam_record` | `com.ruoyi.module.safety` | `ui/src/views/module/safetyExamRecord` | `module:safetyExamRecord:*` |
| `credit_record` | `com.ruoyi.module.credit` | `ui/src/views/module/creditRecord` | `module:creditRecord:*` |

注意：`reservation_device` 的后端包名是 `reservation`，但前端目录是 `reservationDevice`，这是既有事实，新增代码时保持一致，不要顺手改名。

智能助手模块统一放在 `com.ruoyi.module.agent`，前端目录为 `ui/src/views/module/agent`，接口文件为 `ui/src/api/module/agent.js`，权限统一使用 `module:agent:*`。

## 四、命名规范

以根目录 `命名规范-最终版.md` 为准，不再使用 `lab_` 前缀与 `LabRoom`、`com.ruoyi.lab` 等旧写法。

```text
后端包：com.ruoyi.module.<业务模块>     例：com.ruoyi.module.room
后端类：大驼峰                          例：Room、ReservationDevice
前端接口：ui/src/api/module/<小驼峰>.js
前端页面：ui/src/views/module/<小驼峰>/index.vue
数据库表：小写下划线                    例：room、reservation_device、safety_exam_record
数据库字段：room_code、start_time 之类，不重复添加 lab_ 前缀
```

`docs/数据库设计说明-核心版.md` 里仍写作 `lab_room`、`lab_reservation`，属于历史版本；正式建表以 `sql/module_core.sql` 和《命名规范-最终版》为准，不要把新旧两套表混用。

## 五、开发环境与启动方式

| 项 | 值 |
| --- | --- |
| JDK | 21（`D:\JDK\jdk-21.0.9`） |
| Node.js | 20+（本地 Node 22） |
| MySQL | 8，库名 `bishe_schema`，JDBC 必须带 `allowPublicKeyRetrieval=true` |
| Redis | 6379 |
| 后端地址 | http://localhost:8087 |
| 前端地址 | http://127.0.0.1:8090/admin/ |

后端激活的 profile 是 `devmy`，配置文件为 `api/ruoyi-admin/src/main/resources/application-devmy.yml`。

DeepSeek Key 的推荐填写位置是已被 Git 忽略的 `api/ruoyi-admin/application-local.yml`，填写 `bishe.ai.deepseek-api-key` 后重启后端。也可以在 IDEA 启动配置中设置环境变量 `DEEPSEEK_API_KEY`，环境变量优先于本地文件；真实 Key 不得写入源码或提交记录。

启动命令：

```powershell
# 后端
cd D:\bishe_project\api
mvn spring-boot:run -pl ruoyi-admin

# 前端
cd D:\bishe_project\ui
npm run dev
```

前端通过 `ui/vite.config.js` 把 `/dev-api` 代理到 `http://localhost:8087`。

登录与调试的两个固有条件（容易踩坑）：密码需要先用 `ui/src/utils/jsencrypt.js` 的公钥做 RSA 加密再提交；验证码答案存放在 Redis 的 `captcha_codes:<uuid>`（db1）。本地开发账号与重置细节见 `docs/04-本地开发指导书.md`，不要把密码写进代码或提交记录。

### 资源文件与 IDEA/JRebel 启动约束

`src/main/resources` 是源文件，任何情况下都不允许删除、清空或覆盖其中的配置、Mapper XML、i18n 等资源。`target/classes` 只是构建产物，IDEA/JRebel 的 Make 可能只编译 Java 而不拷贝 `resources`，表现为 `application.yml`、`mybatis/mybatis-config.xml`、`mapper/**/*Mapper.xml` 在运行类路径中突然消失；这并不代表源文件被删除。

每次在 IDEA 里启动后端之前（尤其是刚改过 Java/XML、切换分支、执行过 clean 或 IDEA 重新 Make 后），先执行：

```powershell
cd D:\bishe_project\api
mvn -q -pl ruoyi-admin -am process-resources
```

并确认以下文件存在，全部为 `True` 再启动：

```powershell
Test-Path ruoyi-admin\target\classes\application.yml
Test-Path ruoyi-admin\target\classes\mybatis\mybatis-config.xml
Test-Path ruoyi-admin\target\classes\i18n\messages.properties
Test-Path ruoyi-biz\target\classes\mapper\module\RoomMapper.xml
```

更稳妥的做法是在 IDEA 的 Spring Boot 启动配置里增加 Before launch → Run Maven Goal：`process-resources`，工作目录为 `D:\bishe_project\api`。`RuoYiApplication` 里的资源回退只作为最后兜底，不能替代正常的资源拷贝流程。

## 六、数据库与代码生成流程

业务表到可运行页面经过固定几步，脚本执行顺序不能颠倒：

```text
1. sql/ruoyi 若依基础脚本
2. sql/module_core.sql                      业务表建表 + 实验室种子数据
3. 代码生成 -> 导入表                        把 7 张表导入 gen_table
4. sql/module/02-gen-table-config.sql       模块名/包路径/业务名/类名/字段类型口径
5. sql/module/01-module-dict.sql            枚举字段字典 + 字段字典绑定
6. 代码生成 -> 生成代码（批量）              产出 Java / Mapper / Vue / 菜单 SQL
7. sql/module/*Menu.sql                     各模块菜单与按钮权限
8. sql/module/00-module-menu-directory.sql  统一挂到“实验室业务”一级目录
```

- `02` 必须在“导入表”之后执行，`01` 必须在“生成代码”之前执行，否则包路径、字典绑定会回到默认值。
- 每次重新导入表后，`01` 和 `02` 都要重跑；菜单 SQL 生成后要再执行一次 `00-module-menu-directory.sql`，否则菜单会落回“系统工具”目录。
- 生成器配置口径：模块名 `module`、包路径 `com.ruoyi.module`、模板类型 `element-plus`、作者 `itjialu`。
- 实验室准入、班级预约、审核状态、数据维护菜单与预约页面结构脚本在菜单脚本之后按顺序执行：`sql/module/10-room-access.sql`、`sql/module/11-class-representative.sql`、`sql/module/12-demo-class-data.sql`、`sql/module/13-reservation-audit.sql`、`sql/module/14-menu-data-maintenance.sql`、`sql/module/15-reservation-page-structure.sql`；预约生命周期、信用违规、安全考试、数据看板、智能助手、RAG、预约草稿、Agent 审计、离线评测、知识库扩充、资格工具、当前时段查询和设备扩充脚本继续执行 `sql/module/20-reservation-lifecycle.sql`、`sql/module/21-credit-violation.sql`、`sql/module/22-safety-exam.sql`、`sql/module/23-stats-dashboard.sql`、`sql/module/24-agent.sql`、`sql/module/25-agent-menu-rename.sql`、`sql/module/26-agent-rag.sql`、`sql/module/27-agent-reservation-draft.sql`、`sql/module/28-agent-audit.sql`、`sql/module/29-agent-eval.sql`、`sql/module/30-agent-knowledge-expansion.sql`、`sql/module/31-agent-eval-heldout.sql`、`sql/module/32-agent-eligibility-tool.sql`、`sql/module/33-agent-current-room.sql`、`sql/module/34-device-expansion.sql`。以上脚本均可重复执行。

## 七、编码约定

后端：

- 业务代码只写在 `ruoyi-biz` 的 `com.ruoyi.module.*` 下，结构为 `controller` / `domain` / `mapper`（含 `resources/mapper` 下的 XML）/ `service` + `service/impl`。
- Controller 继承 `BaseController`，接口路径为 `/<模块>/<业务名>`（如 `/module/room`）；返回值用 `AjaxResult` 或 `TableDataInfo`；写操作加 `@Log(title = "...", businessType = BusinessType.XXX)`。
- 权限注解 `@PreAuthorize("@ss.hasPermi('module:xxx:list')")` 与菜单按钮权限标识保持一致，新增接口必须同时补菜单 SQL。
- 不要修改若依底层的安全、权限、日志框架代码；确有框架缺陷需要修时，优先改代码生成器模板这一“源头”，而不是手改每一份生成结果。
- 枚举字段用字典管理，不在业务代码里散落字符串常量；时间字段注意 MySQL `TIME` 类型在生成器里映射为 `String`（`open_start_time` / `open_end_time` 已如此处理）。
- 事务里必须保证业务规则成立（时间先后、容量、实验室状态、时段不重叠、安全考试有效期、信用分阈值），AI 工具调用同样要经过这些校验，不能信任模型传入的用户身份。

前端：

- 页面写在 `ui/src/views/module/<业务名>/index.vue`，接口写在 `ui/src/api/module/<业务名>.js`，风格对齐同目录已有模块，不要引入新的 UI 库或状态管理方案。
- 前端路由由后端菜单驱动，新页面需要在菜单里挂上才会出现。

## 八、改动完成后的验证

- 后端：在 `api` 目录执行 `mvn -q -pl ruoyi-biz -am compile` 确认编译通过；涉及启动配置或接口时用 `mvn spring-boot:run -pl ruoyi-admin` 实际跑一遍。
- 前端：`cd ui` 后 `npm run dev`，页面改动要在真实浏览器里点一遍（若依的登录、权限、字典渲染问题只有跑起来才看得出来），确认无控制台报错。
- 涉及生成器模板的改动：重新生成至少一个模块，确认生成的代码可编译、页面可渲染。
- 涉及 SQL 的改动：确认脚本在空库上按第六节顺序可重复执行。
- 后端启动前：确认 `target/classes` 中已有 `application.yml`、`mybatis/mybatis-config.xml` 和 `mapper/**/*Mapper.xml`；缺失时先执行 `mvn -q -pl ruoyi-admin -am process-resources`，不要直接启动或删除源码资源。
- 验证过程中产生的测试数据要清理干净。

## 九、硬性约束

- 默认不创建任何新的说明文档：不主动生成 README、设计文档、架构说明、使用说明。只有用户明确要求“写文档 / 生成 README / 写说明”时才允许创建或修改文档文件。
- 与用户交流使用中文。
- 不提交密码、令牌、`application-local.yml`、构建产物（`target/`、`dist/`、`node_modules/`）以及无关文件；`application-devdmy.yml` 中的数据库口令只在本地存在。
- 不直接修改 `main` 分支，不执行 `git reset --hard`、`git checkout --` 之类的破坏性命令。
- 不修改 `后端/` 目录（旧版框架参考代码）和 `api/.agent/`、`api/openspec/` 等工具目录，除非用户明确要求。
- 不删除、清空或覆盖 `api/**/src/main/resources` 下的资源文件；清理 `target/` 后必须重新执行 `process-resources` 或完整构建，确认资源恢复后再启动。

## 十、常见坑

| 现象 | 原因与处理 |
| --- | --- |
| `Public Key Retrieval is not allowed` | JDBC 地址缺 `allowPublicKeyRetrieval=true` |
| `Unable to connect to Redis` | Redis 未启动或不在 6379 |
| 登录接口 401/500 | 密码没做 RSA 加密，或验证码 uuid 与 Redis 中 `captcha_codes:<uuid>` 不匹配 |
| 登录/退出报 `No message found under code 'user.xxx'` | IDEA/JRebel 未复制 `i18n/messages.properties`；先执行 `process-resources`，项目已提供源码目录兜底但正常构建仍应复制资源 |
| 表单里某个字段整项不渲染 | 枚举列配了 select 但字典类型没绑定到 `gen_table_column`，需补 `01-module-dict.sql` |
| 保存报 `No validator could be found for constraint NotBlank` | 非字符串必填字段错用 `@NotBlank`，应为 `@NotNull` |
| 业务表编译报 `setUserId` / `setDeptId` 找不到 | 模板假定存在 `user_id` / `dept_id` 列，业务表没有时需要按列存在与否回填 |
| 新建模块菜单跑到“系统工具”下 | 补跑 `sql/module/00-module-menu-directory.sql` |
| `reservation_device` 主键相关编译错误 | 该表已改为自增代理主键 `id`，组合键降级为唯一索引 |
| 启动报 `Could not resolve placeholder 'xss.excludes'` | IDE Make/JRebel 没有把 `application.yml` 拷进 `target/classes`。先执行 `mvn -q -pl ruoyi-admin -am process-resources`，确认 `target/classes/application.yml` 存在后再启动 |
| 启动报 `mybatis/mybatis-config.xml cannot be opened` | 同一原因，`target/classes` 中的 MyBatis 配置或 Mapper XML 被 IDE 构建清空。先恢复资源，不要删除 `src/main/resources`；确认 `mybatis-config.xml` 和 `mapper/**/*Mapper.xml` 存在后再启动 |

## 十一、文档索引

| 文档 | 用途 |
| --- | --- |
| `基于大语言模型Agent的校园实验室智能预约与安全管理平台-计划书.md` | 整体方案、技术栈、实施计划、测试与评价方案 |
| `命名规范-最终版.md` | 命名口径唯一权威来源 |
| `docs/01-需求规格说明书.md` | 需求与角色 |
| `docs/02-系统概要设计说明书.md` | 架构与模块设计 |
| `docs/03-接口设计说明书.md` | 接口约定与业务规则 |
| `docs/04-本地开发指导书.md` | 环境、启动、登录调试 |
| `docs/05-代码生成与模块落地说明.md` | 从建表到页面的完整落地过程与已验证结论 |
| `docs/代码生成配置-最终版.md` | 各表生成器配置对照表 |
| `docs/数据库设计说明-核心版.md`、`docs/数据库设计说明.md` | 表结构设计（命名以《命名规范-最终版》为准） |
| `docs/页面原型说明.md` | 页面原型 |
| `api/openspec/AGENTS.md` | 需要写提案、规格或大改动时的 OpenSpec 流程 |

## 十二、Git 提交规范

### 分支约定

- `dev`：日常需求开发、问题修复和验证分支。
- `main`：稳定版本分支，完成验证后再合并。
- 每次开始新需求前先确认当前分支；默认在 `dev` 分支开发，不直接修改 `main`。

### 提交要求

- 每完成一个逻辑完整的改动就创建一次 Git 提交，不要把多个无关需求混在同一个提交中。
- 提交信息必须使用以下格式，并在冒号后写清楚本次提交做了什么：

  ```text
  <type>: <简洁、明确的改动说明>
  ```

- 提交说明应描述结果和影响，避免使用“修改一下”“更新代码”等无法判断内容的表述。
- 提交前检查 `git diff`、`git status`，确认没有把密码、令牌、构建产物、依赖目录或无关文件提交进去。
- 提交完成后推送当前分支：`git push`。

### 提交类型

- `feat`：新增功能。
- `fix`：修复 bug。
- `docs`：仅修改文档，例如 README、CHANGELOG 或项目说明。
- `test`：增加或修改测试用例，包括单元测试、集成测试等。
- `style`：只修改空行、缩进、引用排序等格式，不改变代码逻辑。
- `perf`：性能或体验优化，包括算法、响应速度和资源使用优化。
- `refactor`：代码重构，不新增功能，也不修复 bug。
- `chore`：构建流程、依赖库、开发工具或其他维护性变更。
- `revert`：回滚之前的提交，并在说明中写明回滚的提交内容或原因。
- `merge`：合并代码，并在说明中写明合并来源和目的。

### 示例

```text
feat: 新增实验室预约冲突检测
fix: 修复预约列表分页后筛选条件丢失
docs: 补充本地开发与部署说明
test: 增加预约服务的冲突校验测试
style: 统一前端组件缩进格式
perf: 优化实验室列表查询索引
refactor: 拆分预约校验服务
chore: 升级前端构建依赖
revert: 回滚实验室预约冲突检测
merge: 合并 dev 到 main 发布稳定版本
```

### 推荐流程

```text
确认分支 -> 修改代码 -> 运行相关检查/测试 -> 查看 diff -> 按类型提交 -> 推送当前分支
```
