# Git 推送说明

本项目同时关联 Gitee 和 GitHub 两个远程仓库，两边内容保持一致。本文说明日常怎么提交、怎么推送，以及推送失败时怎么排查。

## 1. 两个远程仓库

| 远程名 | 平台 | 地址 | 说明 |
|---|---|---|---|
| `origin` | Gitee | `https://gitee.com/itjialu/bishe_project.git` | 原有远程，日常主用 |
| `github` | GitHub | `https://github.com/lhcww666/bisheproject.git` | 2026-10-16 新增，公开仓库 |

查看当前配置：

```powershell
git remote -v
```

注意 GitHub 账号名是 `lhcww666`（顺序是 l-h-c-w-w），不是 `lchww666`。写错会得到 `Repository not found`，看起来像仓库不存在，其实只是地址错了。

## 2. 分支约定

- `dev`：日常开发和问题修复分支，所有日常改动都提交到这里。
- `main`：稳定版本分支，`dev` 验证通过后再合并过去。

本地 `dev` 跟踪 `origin/dev`（Gitee）。推送到 GitHub 时不需要改跟踪关系，直接指定远程名即可。

## 3. 日常推送流程

```powershell
cd D:\bishe_project

# 1. 确认当前分支和改动
git branch --show-current
git status

# 2. 暂存并提交，提交信息按 AGENTS.md 的类型前缀写
git add docs/05-Git推送说明.md
git commit -m "docs: 新增 Git 双远程推送说明"

# 3. 分别推送到两个远程
git push origin dev
git push github dev
```

一条命令推两边（已配置别名 `pushall`）：

```powershell
git pushall
```

这个别名等价于 `git push origin HEAD && git push github HEAD`，推的是当前分支，所以在 `dev` 上执行就推 `dev`，在 `main` 上执行就推 `main`。

第一次推送某个新分支时加 `-u` 建立跟踪关系，例如 `git push -u github dev`。之后直接 `git push github dev` 即可。

只想推一边时写明远程名就好：`git push origin dev` 只推 Gitee，`git push github dev` 只推 GitHub。

## 4. 认证方式

两个远程都用 HTTPS，凭据由 Git Credential Manager 保管（本仓库配置了 `credential.helper=manager`）。第一次推送会弹出浏览器授权窗口，登录对应平台账号后即可，之后不会再问。

如果 GitHub 提示需要密码而不是弹窗，要填的是 Personal Access Token，不是账号密码：GitHub → Settings → Developer settings → Personal access tokens，生成时勾选 `repo` 权限，用户名填 GitHub 用户名，密码填这个 token。

凭据失效或想换账号时，打开「Windows 凭据管理器 → 普通凭据」，删除 `git:https://github.com` 这一条，再重新推送就会重新走授权流程。

## 5. 提交信息规范

类型前缀和示例见根目录 `AGENTS.md`。常用的几个：

```text
feat: 新增实验室预约冲突检测
fix: 修复预约列表分页后筛选条件丢失
docs: 补充本地开发与部署说明
chore: 升级前端构建依赖
```

一个逻辑完整的改动提交一次，不要把互不相关的改动混在同一个提交里。

## 6. 哪些内容不会进仓库

规则写在根目录 `.gitignore`，主要忽略以下几类：

- 依赖和构建产物：`ui/node_modules/`、`ui/dist/`、`api/**/target/`、`api/**/out/`
- IDE 和本地工具：`.idea/`、`.vscode/`、`*.iml`
- 运行时文件和本地配置：`*.log`、`api/**/logs/`、`api/**/uploadPath/`、`application-local.yml`、`*.local`
- 框架克隆残留的 Git 元数据：`ui/.git/`、`api/.git/`、`后端/`

其中 `后端/` 里目前只有一个 `RuoYi-SpringBoot3-Pro\.idea` 空壳（约 3.6 KB），是当初下载框架时的残留，不属于项目代码，因此被忽略。项目的后端代码在 `api/`，前端在 `ui/`。

## 7. 提交身份

本仓库当前的提交身份是：

```text
user.name  = itjialu
user.email = 16857353+itjialu@user.noreply.gitee.com
```

邮箱是 Gitee 的 noreply 地址，所以推送到 GitHub 的提交不会关联到 GitHub 账号（贡献图里不显示）。想让 GitHub 也识别这些提交，可以把邮箱换成 GitHub 的 noreply 地址（GitHub → Settings → Emails 里可查），设置只对之后的提交生效。

## 8. 常见问题排查

**`remote: Repository not found.`**

三种可能：一是账号名拼写错了，本项目的账号是 `lhcww666`；二是仓库是私有的但当前凭据未认证，需要先登录；三是当前账号没有该仓库的权限。先在浏览器里打开远程地址确认能访问，再检查拼写。

**`Authentication failed` 或一直弹授权窗口**

多半是凭据过期。按第 4 节删掉 Windows 凭据管理器里的 `git:https://github.com` 记录后重试。

**`! [rejected] ... (non-fast-forward)`**

远程有本地没有的提交（比如在另一台机器上推过）。先拉取再推：

```powershell
git pull --rebase origin dev
git pushall
```

GitHub 那边如果提示同样的问题，把 `origin` 换成 `github` 处理一次。

**想修改远程地址或删掉远程**

```powershell
git remote set-url github https://github.com/lhcww666/bisheproject.git
git remote remove github
```

**推送很慢或者超时**

先确认能打开对应平台网页。需要走代理时可以临时指定：

```powershell
git config --global http.proxy http://127.0.0.1:7890
git config --global --unset http.proxy
```

**在 Codex 桌面端里执行 `git commit`、`git push` 会弹审批**

这是正常现象：Codex 的沙箱会保护 `.git` 目录，让它对沙箱进程只读，所以写 `.git` 的操作需要在沙箱外执行，Codex 会先征求同意。直接同意即可，不影响推送结果。

## 9. 安全提醒

GitHub 仓库是公开的，任何推上去的内容都可以被搜索到。提交前检查：

- 不要提交数据库密码、API Key、Token、`.env` 里的密钥；
- `application-local.yml`、`*.local`、日志目录已在忽略列表里，不要把敏感配置挪到会被跟踪的文件中；
- `ui/.env.development`、`ui/.env.production`、`ui/.env.staging` 目前是前端基础配置，不含密钥（2026-10-16 核对），后续新增变量时注意这一点；
- 一旦误提交了密钥，光删除文件不够，必须立即更换密钥，并清理 Git 历史。

## 10. 配置变更记录

2026-10-16：

- 新增 `github` 远程，指向 `https://github.com/lhcww666/bisheproject.git`；
- 修正远程用户名（`lhcww666`，此前误用 `lchww666` 导致推送被拒）；
- 新增 `pushall` 别名，用于一次推送到两个远程；
- `dev`、`main` 两个分支已推送到 GitHub；
- 排查并修复了 Codex 沙箱无法启动终端的问题：`D:\bishe_project\.git` 的所有者异常变成沙箱账号、缺少更改权限，用管理员权限执行 `takeown` 和 `icacls` 恢复后正常。
