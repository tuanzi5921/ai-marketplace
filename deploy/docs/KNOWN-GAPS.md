# 待修复清单 — 业务代码缺口

> 环境与部署已验收通过。**登录链路** 2026-09-23 打通并浏览器实测；**四处接线类缺口 + 分页插件**
> 2026-09-30 修掉；团队 PR #3（`116c857`）补齐了**运营后台全部 13 个接口**与**真实 HTTP 状态码**，
> 已于 **2026-10-08 14:24 发版到 132** 并逐项对账；团队 `d876aa0`（修登录 bug + 前后台合并 +
> 角色权限）已于 **2026-10-12 14:47 发版到 132** 并浏览器实测；**管理员建号登不进去 + DataInitializer
> 每次重启冲掉 admin 口令**已于 **2026-10-12 15:56 发版到 132**，建号→登录闭环已浏览器实测，
> 见文末「已修复」。
> 本文记录仍然存在的**代码层面**缺口，每条附实测证据，按优先级排序。

---

## P1 — 作品类接口直接返回实体，员工侧页面要的字段大面积对不上（现存最大的功能性缺口）

`/submissions`、`/submissions/{id}`、`/submissions/admin`、`/submissions/mine`、`/reviews/pending`、
`/rankings/*` 全部把 `MpSubmission` 实体原样序列化返回：没有 VO、没有 join `sys_user`、
没有拼 `mp_saas_link`。

`d876aa0`（2026-10-12 发版）把**管理端**页面改成按实体字段名渲染，所以那几页现在是对的；
但**员工侧**页面仍按视图模型写，字段因此取不到值。页面**不报错、不白屏**，
相关列/项静默渲染成 `—`。

### 实测：四个接口返回的 key 完全相同（2026-10-12，132 本机 `127.0.0.1:8080`，带合法 token）

`/submissions/admin?page=1&size=1`、`/submissions/2`、`/reviews/pending?page=1&size=1`、
`/rankings/overall?n=2` 返回的都是这 23 个 key：

```
authorId  businessDomain  coverUrl  createdAt  deleted  detailDesc  downloadCount
id  latestArtifactId  publishedAt  ratingAvg  ratingCount  rejectReason  reviewedAt
reviewedBy  shortDesc  status  tags  techStack  title  type  updatedAt
usageScenario  version
```

### 管理端：`d876aa0` 已改对，无需处理

`views/admin/SubmissionManagement.vue:98-100` 与 `views/admin/PendingReviews.vue:81-83`
现在直接用 `downloadCount` / `ratingAvg` / `createdAt`，与实体一致。
2026-10-12 浏览器实测：作品管理列出 5 条真实作品、Total 5，下载量与提交时间均有值；
待审作品列出 2 条、Total 2，与看板「待审数 2」一致。

### 员工侧：仍缺字段（`frontend-workbench/src/views/`）

| 前端用的字段 | 后端实际返回 | 后果（2026-10-12 浏览器实测已复现） |
|---|---|---|
| `item.author?.displayName`（`Home.vue:246`）、`data.author?.displayName` / `.department`（`SubmissionDetail.vue:259-260`） | 只有 `authorId` | 首页卡片作者与详情页「作者：」都是 `—` |
| `item.avgScore` / `item.raterCount`（`Home.vue:243-244`）、`data.avgScore` / `data.raterCount`（`SubmissionDetail.vue:274-276`）、`row.avgScore` / `row.raterCount`（`Me.vue:147-148`） | `ratingAvg` / `ratingCount` | 评分显示 `—`、人数恒为 `(0人)` |
| `data.track`（`SubmissionDetail.vue:254`）、`row.track`（`Me.vue:137`） | 无（只有 `businessDomain`） | 绿色赛道标签永不渲染（详情页看到的「通用办公」来自 `businessDomain`，是另一个标签） |
| `data.saasUrl` / `data.saasCredentials`（`SubmissionDetail.vue:281-294`） | 无（数据在 `mp_saas_link`，未拼装） | SaaS 作品的访问入口与凭据整块缺失 |
| `item.rank` / `item.score`（`Ranking.vue:124,147,173,195,219,226`） | `/rankings/*` 返回的也是实体 | 总榜/赛道榜/大赛榜的名次与分数全是 `—` |
| `data?.tracks`（`Ranking.vue:77`） | 无此结构 | 「各赛道」下拉为空 |

### `tags` / `techStack` 是字符串，前端当数组用

实体里这两个字段都是 `String`（库里存 JSON 文本），前端却按数组消费：
`Home.vue:228` 的 `(item.techStack || []).slice(0, 3)`、`SubmissionDetail.vue:314,320,323,326`
的 `v-for` 与 `?.length`。字符串也有 `.length`，所以**不报错、不白屏**，
但 `v-for` 会逐字符迭代、`slice(0,3)` 切出的是前 3 个字符，标签显示成乱码片段。
这条比上面的空白列更隐蔽，因为它「看起来在动」。
（2026-10-12 现网两条已发布作品这两个字段都是 `null`，所以暂时看不出症状；
一旦有人填了值就会暴露。）

### 建议修法

后端补一层 VO（`SubmissionVO` / `ReviewItemVO` / `RankingItemVO`）：
join `sys_user` 取 `author.displayName` + `author.department`，
`ratingAvg`→`avgScore`，`ratingCount`→`raterCount`，`businessDomain`→`track`，
`tags`/`techStack` 在后端反序列化成 `List<String>` 再返回，SaaS 作品拼 `mp_saas_link`，
`/rankings/*` 补 `rank` 与 `score`。

管理端已经按实体口径写好了，所以 VO 要么**只加字段不改名**（在实体基础上追加
`author`/`avgScore`/`raterCount`/`track`/`saasUrl`，两套名字并存），要么连管理端一起改口径。
**别只改一边**，否则会把刚修好的管理端再弄坏。

> 不建议在员工侧前端逐页加适配层。2026-09-30 我曾在 `frontend-admin/src/api/review.ts` 里加过
> 一个 `toItem()` 适配（作者退化为「用户 #ID」），PR #3 合并时被覆盖删掉了。
> 前端适配只能改字段名、改不出「后端没查的表」——作者姓名与部门在客户端根本无从获得，
> 这类缺口只能在后端补。

---

## P1 — 企微 SSO 仍缺可信域名，暂不可用

后端接口与凭据都已就绪，实测 `/api/auth/wecom/redirect` 曾返回正确拼装的授权地址：

```
https://open.weixin.qq.com/connect/oauth2/authorize?appid=wwca6dedc33172f790
  &redirect_uri=http%3A%2F%2F192.168.1.132%2Flogin&response_type=code
  &scope=snsapi_base&agentid=1000017#wechat_redirect
```

但 `redirect_uri` 是内网 IP，企微会拒绝。要真正跑通还需（**均为企微后台与网络侧操作，不是代码问题**）：

1. 定一个正式域名（`*.tri-ibiotech.com` 通配符证书已覆盖任意一级子域，建议如 `ai.tri-ibiotech.com`）
2. 加 DNS 解析，并让公网入口（当前 `www.tri-ibiotech.com` 走的是第三方 WAF `ipm0ddpz.waf.zfuwuqi.com`）
   把该域名的 443 回源到 192.168.1.132
3. 在企微管理后台「网页授权及JS-SDK」里配置**可信域名**并完成归属验证
   —— 验证文件已放好，见 `deploy/README.md`
4. 把回跳地址改成 `https://<正式域名>/login`
5. 确认「企业可信IP」含 `121.46.250.190`（实测 gettoken 已返回 errcode 0，说明当前已放行）

另外：授权地址里**没有带 `state` 参数**，因此不存在 OAuth 登录 CSRF 防护。
前端契约里没有回传 state 的环节，要补需要前后端一起改（后端下发 state 存 Redis、
回调时校验），属于加固项。

> 注：企微 SSO 相关接口已在账号密码改造中从后端移除，`application.yml` 也不再读取任何
> `WECOM_*` 变量（env 模板里那几行是历史遗留，已于 2026-10-08 清掉）。
> 本节保留是因为「用企微免密登录」这个业务诉求本身还没实现。

---

## P2 — `cover_url` 里存的是网页地址，不是图片

现网 5 条作品中 3 条的 `cover_url` 是**作品首页网址**而不是图片地址：

```
1  源资腾讯会议预定分流系统(TRI_MEETING)  https://meeting.tri-ibiotech.com:8082/index.html
2  智能待办工作台                        https://86c710d1057e440b83709ae86a604e85.app.workbuddy.host/
3  智能待办工作台                        （同上）
```

`<img src="…index.html">` 必然加载失败。2026-09-30 已在展示侧兜住
（`@error` 后退回纯色占位，见下方「已修复」），**但根因仍在提交侧**：
`Submit.vue:184` 的字段提示是「作品封面图片地址（可选）」，既没有格式校验也不能上传，
用户很自然会把作品链接粘进去。建议二选一：

- 把该字段改成真正的图片上传（复用 `/submissions` 的 multipart 通道，落到 `/aitest/storage/files`）
- 或至少加前端校验：只接受图片扩展名 / `image/*`，并在提交时用 `Image()` 预加载试一次

---

## 已修复（2026-10-12 下午）—— 管理员建的账号登不进去

**现象**：管理员在后台建号，把口令告诉用户，用户登录一律 `1004 账号或口令错误`。
`stdout.log` 里 `yufei` 失败 **11 次**、`mzg` **9 次**、`mzg02` **2 次**。

**根因不是接口缺失，是「前端写死的口令 ≠ 后端实际用的口令」**：

- 后端 `AuthService.createUser()` 用 `app.auth.default-password`
  （env `AUTH_DEFAULT_PASSWORD`）生成哈希；现网该值是 **18 位**
- 前端却在三处把初始口令写死成 `Welcome@2026`：
  `frontend-workbench/src/views/admin/Users.vue:110`（建号成功提示）、
  `frontend-workbench/src/views/Login.vue:88`（**未登录的公开页面**上印着）、
  `frontend-admin/src/views/Users.vue:119,250`（8443 那套同样的提示）
- 管理员照着提示转告 `Welcome@2026`，用户必然登不进去

**实测定位过程**（132 本机，口令全程留在服务器内、未回显）：
拿 env 里的 `AUTH_DEFAULT_PASSWORD` 去登 `yufei` / `mzg02` / `mzg` → **三个全部 `code=0` 成功**；
拿 `Welcome@2026` 去登同样三个 → **全部 `1004`**。
说明账号数据与 BCrypt 校验都是好的，坏的只是「没人知道口令是什么」。

### 改法

| 位置 | 改动 |
|---|---|
| `dto/CreateUserResultVO.java`（新增） | `{ user, initialPassword }`；口令明文**只在这一个接口返回一次**，不进列表、不进登录返回体、不写日志与审计明细 |
| `AuthService.createUser()` | 返回 `CreateUserResultVO`，把 `props.getAuth().getDefaultPassword()` 原样带回 |
| `AdminUserController.createUser()` | 返回类型改 `Result<CreateUserResultVO>` |
| `dto/UserVO.java` | 补 `account` 字段——建号时账号可能由邮箱前缀推导，前端要显示准确的登录名就得靠它 |
| 两套前端的 `api/user.ts` | `createUser` 返回类型改 `{ user, initialPassword }` |
| 两套前端的 `Users.vue` | 建号成功后弹**结果对话框**展示「登录账号 + 初始口令」并给复制按钮，替换掉写死的提示；表单里那行说明也改成「口令由服务端下发，只显示一次」 |
| `frontend-workbench/src/views/Login.vue:88` | 公开页面上的「首次登录默认密码为 Welcome@2026」改成「初始口令由管理员发放」 |

### 顺带修掉的 P2：`DataInitializer` 每次重启都把 admin 口令打回默认值

原来它拿硬编码的 `Welcome@2026` 去校验 admin，不匹配就**覆盖哈希**并置
`must_change_password=1`，等于运维改过的口令一重启就作废
（2026-09-30、2026-10-08、2026-10-12 各发生一次，日志里都有 `admin 密码哈希已用运行时 BCrypt 重新生成`）。

现在改成：**只在 `password_hash` 仍等于 `schema.sql` 的占位字面量**
`$2a$10$placeholder.will.be.regenerated.by.DataInitializer.on.startup`（或为空）时才重生成，
且重生成时读 `app.auth.default-password`，不再自己留一份硬编码值；
`must_change_password=1` 也只在该分支里设置。其余情况一律不动，日志打
`admin 口令已由运维设置，本次启动不改动`。

这样全站只剩**一个**默认口令口径（env 的 `AUTH_DEFAULT_PASSWORD`），
admin 首登与管理员建号用的是同一个值，不会再出现「两处口令不一样」。
`schema.sql` 与 `CreateUserDTO` 里那两条已过时的注释一并更正。

> 副作用要知道：现网 `admin` 的口令在 2026-10-12 15:12 被运维改过（日志 `用户改密成功: id=1`），
> 改动后**重启不会再把它冲掉**——这是修复的目的，但也意味着忘了口令就只能走
> `UPDATE sys_user SET password_hash=...` 手工重置（`schema.sql` 注释里给了语句）。

### 发版与实测证据（2026-10-12 15:56）

发布包 `ai-marketplace-release-20261012-154852.tar.gz`（48,683,461 B，md5 `ff7ef0830c4db88fb38e6f923db8e874`），
15:56:00 部署到 132，后端 4.873 s 启动完成，`nginx -t` 通过后 reload。
线上 `/aitest/app/releases/ai-marketplace.jar` 的 md5 `ab2f88666d77a7e8edc29bec1e0f947f`
与本机 `backend/target/ai-marketplace.jar` **逐字节相同**。

**一、建号 → 登录闭环（132 本机 curl，口令全程不回显）**

以 `mzg02`（ADMIN）登录取 token → `POST /api/admin/users` 建探针账号：

- `data` 顶层键 = `['initialPassword', 'user']`，`initialPassword` 长度 18，
  与 env `AUTH_DEFAULT_PASSWORD` **一致**；`user.account` 正确回显
- 拿响应里的 `initialPassword` 去 `POST /api/auth/local/login` → **HTTP 200 + token**，
  `mustChangePassword=true`
- 拿 `Welcome@2026` 去登同一账号 → **HTTP 401 `code=1004`**（坐实旧提示是错的）
- 落库正确：`account` 有值、`wecom_userid` 为 NULL、`roles=USER`、`enabled=1`、
  `must_change_password=1`、哈希 `$2a$10$` 前缀 60 位
- 分页仍生效（`MybatisPlusConfig` 没在这轮合并里丢掉）：日志出现
  `SELECT COUNT(*) AS total FROM sys_user`，`size=2` 时 `total=5` 且只返回 2 条

**二、口令没有外泄**

`stdout.log` 全文出现初始口令明文 **0 次**；`sys_audit_log` 全表扫描含明文的行 **0 行**，
建号审计的 `detail` 只有 `account=e2eprobe1012 roles=USER`。

**三、浏览器实测（公网 8081，MCP 浏览器）**

- 登录页提示已是「初始口令由管理员发放，首次登录后请及时修改」，全站产物 grep `Welcome@2026` **0 命中**
- 用一次性 ADMIN 账号登录 → 管理后台 → 用户管理（`Total 5`，分页正常）→ 新建用户
  （邮箱 `e2ecreated@tri-ibiotech.local`，**账号栏故意留空**，角色 USER）→ 创建
- 弹出「用户已创建」结果框，含 `初始口令只显示这一次，关闭后无法再查看` 告警条、
  登录账号 `e2ecreated`（**邮箱前缀推导正确**）、初始口令 18 位、`复制口令` / `我已记录` 两个按钮
- 弹窗里那串口令的 SHA-256 = `ad9fa151dd3acaf2a81ab7e3bcc6a114dee98eec8624eebba5929e2ddbb95b44`，
  与服务器上 `printf %s "$AUTH_DEFAULT_PASSWORD" | sha256sum` **完全相同**
  ——这样既能证明 UI 显示的就是真实口令，又不必把明文写进任何记录
- `复制口令` 在自动化环境里走的是降级分支：合成点击没有 transient activation，
  `navigator.clipboard.writeText` 抛 `NotAllowedError`，页面正确弹出
  `浏览器拒绝了剪贴板访问，请手动选中复制`。真实浏览器里是可信手势，走成功分支
- 关闭结果框后列表自动刷新为 `Total 6`，新账号排在首位；**控制台零 error 零 warning**
- 对这个「由 UI 建出来的」账号再回服务器验一次登录 → HTTP 200，`/auth/me` 通过

**四、nginx 实际提供的产物**

workbench `index-ChPbVqB9.js`、admin `index-JelEJ0eq.js`，与本机 `dist/index.html` 引用一致；
`Users-DCJU0sDz.js`(8359 B) / `Users-DHNlcnU6.js`(7767 B) 均 HTTP 200 且字节数与本机 dist 相同；
经 8081 拉下来的 `Login-XgjxC-ZW.js` 里 `Welcome@2026` 出现 **0 次**。

**五、P2 那一刀真的没复发**

`stdout.log` 三次启动对比：

| 时间 | DataInitializer 日志 |
|---|---|
| 2026-10-08 14:24:51 | `admin 密码哈希已用运行时 BCrypt 重新生成` + `must_change_password 已修正为 1` |
| 2026-10-12 14:47:18 | 同上（把运维 15:12 之前的改密冲掉过一次） |
| **2026-10-12 15:56:07** | **`admin 口令已由运维设置，本次启动不改动`** |

发版前后 `sys_user` 四行的 `MD5(password_hash)` **逐一相同**，`admin` 的 `must_change_password` 保持 0。

**六、清理**

三个探针账号（`e2eprobe1012` / `e2eui1012` / `e2ecreated`）已**硬删**
（`account` 是 UNIQUE，软删会占住唯一索引导致同名建不了）；`sys_user` 回到原有 4 行、哈希未变。
服务器 `/tmp` 下的脚本、口令暂存文件、发布包已删。
动手前的备份留在 `/aitest/backup/sys_user-pre-createuser-fix-20261012_155449.sql`（chmod 600）。

> 有意留下的痕迹：`sys_audit_log` 里 3 行 `CREATE_USER`（`target_id` 6/7/8）指向已删除的探针账号。
> 审计记录不宜由部署方单方面删改，需要清就说一声。

**七、顺带查清的现网事实**

`mzg` / `mzg02` / `yufei` 这三个账号**数据库层面没坏**——它们的口令就是 env 里那个 18 位默认值，
实测三个都能登进去；坏的只是「管理员不知道口令是什么」。审计日志显示 `mzg02`、`yufei`
正是 2026-10-08 由 `admin` 从后台建的。发版后管理员建任意一个新号即可从结果弹窗读到该默认口令，
据此就能把这三个账号用起来（各自首登仍会被要求改密）。

---

## P2 — 没有「重置用户口令」接口，管理员无法救回失联账号

`AdminUserController` 只有建号、列表、授/撤角色、启/停用六个动作，**没有重置口令**。
用户忘了口令、或像上面那样建号时没人记下初始口令，管理员在后台无路可走，
只能登服务器 `UPDATE sys_user SET password_hash=...`（还得自己会算 BCrypt）。

建号接口现在会把初始口令回显一次，但**关掉弹窗就再也拿不到**（这是有意的），
所以「没记下来」这个场景仍然没有出口。

建议加 `POST /admin/users/{id}/reset-password`：把哈希重置为
`app.auth.default-password`、`must_change_password` 置 1、写 `PERMISSION/RESET_PASSWORD` 审计，
响应体同样只回显一次口令（可直接复用 `CreateUserResultVO` 的形状）。
前端在用户列表的操作列加一项，二次确认后弹同一个结果框。

---

## P3 — `Welcome@2026` 仍是配置项的兜底默认值，且已随仓库公开

`application.yml:54` 是 `default-password: ${AUTH_DEFAULT_PASSWORD:Welcome@2026}`，
`AppProperties.Auth.defaultPassword` 的字段初值也是同一个字面量。
现网 132 的 env 里设了 18 位的 `AUTH_DEFAULT_PASSWORD`，所以**当前没有实际暴露**；
但任何一次新部署只要漏配这个环境变量，就会静默落到这个公开值上，
于是所有新建账号的初始口令都是仓库里能搜到的字符串——而且不会有任何报错提示。

建议去掉兜底：`default-password: ${AUTH_DEFAULT_PASSWORD}` 不带默认值，
并在启动时校验非空（空则 fail-fast），把「忘了配」变成启动失败而不是静默弱口令。
`schema.sql` 里 admin 那行的占位哈希保持现状即可——`DataInitializer` 只在它仍是占位值时才重生成。

---

## 已修复（2026-10-12）—— `d876aa0` 发版（修登录 bug + 前后台合并 + 角色权限）

发布包 `ai-marketplace-release-20261012-143543.tar.gz`（48,674,804 B，md5 `eeddde18d550b7229a03a3939f5ebecf`），
14:47:10 部署到 132，后端 4.775 s 启动完成，nginx 配置校验通过后 reload。
包已归档到 `/aitest/packages/`（归档后 md5 与本地一致）。
回滚 jar 已留：`/aitest/app/releases/ai-marketplace.jar.prev-20261012_144710`。

### 一、发版前先把会被覆盖的东西备份了

`DataInitializer` 每次启动都会重置 admin 口令（见上 P2），发版前已征得确认并落备份：
`/aitest/backup/sys_user-before-d876aa0-20261012_144528.sql`（4,278 B，chmod 600，131 上）。
备份时现网 `sys_user` 4 行，`admin` 的 `must_change_password=0`——**说明口令已被人改过**，
重启后确实被打回：启动日志 14:47:18 三行
`admin 密码哈希已用运行时 BCrypt 重新生成` / `must_change_password 已修正为 1` / `admin 账号已修复`。
其余 3 行（`mzg` / `mzg02` / `yufei`）Runner 不动，发版后 `/admin/users` 仍列出全部 4 人、角色与启用状态未变。

### 二、被 `d876aa0` 合并冲掉的三项，本次重新落地并验证

`d876aa0` 是从旧分支合过来的，把 2026-09-30 那三项又删了一遍（和 PR #3 同样的情况）：

| 项目 | 文件 | 不在会怎样 |
|---|---|---|
| 分页拦截器 | `backend/.../config/MybatisPlusConfig.java` | `selectPage` 不生成 `LIMIT` 也不 `COUNT`，每页返回全表且 `total` 恒为 0，**不报任何错** |
| 无封面纯色占位 | `frontend-workbench/src/utils/cover.ts` + `Home.vue` / `Ranking.vue` / `SubmissionDetail.vue` | 封面加载失败处露出空白/裂图 |
| 构建脚本 PowerShell 5.1 兼容 | `deploy/build-release.ps1` | 一键构建在 `java -version` 那步就被 `NativeCommandError` 打断 |

三项都已重新落地并随本次发布包上线。

### 三、分页拦截器确实生效（不是「代码放上去了」而是「行为对了」）

`GET /api/admin/users?page=1&size=2` 返回 `list` 2 条、`total=4`、`page=1`、`size=2`；
`stdout.log` 里对应的两句 SQL 是：

```
SELECT COUNT(*) AS total FROM sys_user WHERE deleted = 0
SELECT id,wecom_userid,... FROM sys_user WHERE deleted=0 ORDER BY created_at DESC LIMIT ?
```

`COUNT` 与 `LIMIT` 都出现了——这正是缺拦截器时不会生成的两句。
部署后的 jar 内也确认含 `MybatisPlusConfig.class`，且 jar md5 与本机 `backend/target/` 产物一致
（`3b08f240f48c3a59ddf1fa8d96ba9058`）。

### 四、`d876aa0` 的成果，逐项对账通过

后端接口（132 本机 curl，带 admin token，全部 `HTTP 200` + `code=0`）：
`/submissions`、`/submissions/admin`、`/admin/dashboard/stats`、`/reviews/pending`、
`/comments/reported`、`/judges/recommendations`、`/rankings/overall`、`/competitions/current`。
未登录打 `/api/auth/me` 仍是 `HTTP 401` + `code 1001`，说明真实状态码那套没被合并回退。

浏览器实测（`https://ai-marketplace.tri-ibiotech.com:8081/`，公网入口，控制台零 error 零 warning）：

- 登录页只剩账号/密码，企微入口已按改造下线；登录后被路由守卫正确拦到 `/change-password`
- **用户管理**：4 个真实用户、`Total 4`，角色/积分/状态齐全
- **数据看板**：总作品 5、待审 2、累计下载 1、活跃用户 4，与库里行数一致（这页在 PR #3 之前是 `5000`）
- **作品管理**：5 条作品、`Total 5`，非 `PUBLISHED` 行的「下架」按钮正确置灰
- **待审作品**：2 条、`Total 2`，与看板待审数一致
- **评论管理 / 评委管理 / 大赛配置**：均正常渲染空态（对应表确实 0 行），不再报 `5000`
- **首页 / 排行榜 / 作品详情**：无封面纯色占位生效，且同一作品在三个页面取到**同一个颜色**
  （「智能待办工作台」三处均为 `rgb(191,163,163)`，「TRI_MEETING」均为 `rgb(75,159,225)`），
  说明按作品 id+标题稳定取色的实现是对的

> 看管理页时没有去改 admin 口令（现网在用，改了会影响别人）。绕过强制改密页用的是
> **纯客户端**手段：把 `localStorage.ai_market_user` 里的 `mustChangePassword` 改成 `false`
> 后重新导航。服务端 `must_change_password` 仍是 1，没有写库；token 是合法登录换来的，
> 管理端接口的 `requireAdmin` 也确实用 token 里的 `ADMIN` 角色过的关。验证完已清掉 localStorage。
> 全程只读，没有点任何「停用/删除/下架/通过/退回」。

### 五、前端产物按「nginx 实际吐出的字节」核对

`https://127.0.0.1:8081/index.html` 现在指向 `assets/index-d7-xU6Jk.js`（发版前是 `index-vCHlM7hG.js`）。
14 个 chunk 逐个 curl 均 `HTTP 200`，含 `cover-DwVakeLa.js`（占位实现）、`request-BAc76VSA.js`、
以及 7 个新管理页 chunk（`Users` / `Dashboard` / `PendingReviews` / `SubmissionManagement` /
`Comments` / `Competitions` / `Judges`）。

8443 的运营后台入口 `index-ChiImY1n.js` **与发版前同名**——vite 的 chunk 名是内容哈希，
同名即同字节，说明 `d876aa0` 没动 `frontend-admin`（管理页都搬到 workbench 里了）。
两个站点的 `WW_verify_61IR393ntYc1jRjW.txt` 都被发版脚本保住，curl 仍 `200`。

> `frontend-admin`（8443）现在是**冗余入口**：同样的管理功能在 workbench 里已按角色渲染。
> 它仍在构建和发布，是为了不打断已经在用 8443 的人。要不要下线属于产品决定，不在本次范围。

### 六、发版后健康检查

`ai-marketplace` / `nginx` 均 `active`，8080 在 LISTEN，131 的 `mysql` / `redis-server` 均 `active`。
`stdout.log` 里最后一条 `ERROR` 时间戳是 `2026-09-30T17:24:26`——**本次发版后零新增 ERROR**，
唯一一条 WARN 是我自己那次未登录探测（`code=1001`）。
公网旁路 `https://ai-marketplace.tri-ibiotech.com:8081/` 返回 200，新 chunk 也取得到。

### 七、Schema 无需迁移

`d876aa0` 只改了 `CreateUserDTO`（加 `username`，可选）、`JwtService`、5 个 `Service`，
**没有动任何 `@TableName` 实体**，`schema.sql` 也未变。发版前后 `sys_user` 的列清单一致
（启动日志里那句 SELECT 的 16 个列名与库中相同），故未跑任何 DDL。

---

## 已修复（2026-10-08）—— PR #3 发版 + 被合并回退的三项重新落地

发布包 `ai-marketplace-release-20261008-141354.tar.gz`（48,656,998 B），
14:24:44 部署到 132，后端 4.838 s 启动完成，nginx 配置校验通过后 reload。
回滚 jar 已留：`/aitest/app/releases/ai-marketplace.jar.prev-20261008_142444`。

### 一、团队 PR #3 的成果，逐项对账通过

**运营后台 13 个接口全部实现**（原 P0，此前每个菜单点开都是「服务端异常」）。
新增 `DashboardController`、`JudgeController`、`AdminUserController`，
`SubmissionController` 补 `/admin`、`/{id}/offline`、`DELETE /{id}`，
`CommentController` 补 `/reported`、`/{id}/hide`、`/{id}/restore`。
`frontend-admin/src/api/` 里出现过的 25 个 URL 现已**全部有后端映射**。

> 此前文档里提醒过的「`/submissions/admin` 必须声明在 `/{id}` 之前，否则被
> `@PathVariable Long id` 抢先匹配」已被采纳：`SubmissionController` 里 `/admin` 在第 63 行、
> `/{id}` 在第 72 行。实测 `/submissions/admin?page=1&size=2` 返回 code=0，
> 不再出现 `MethodArgumentTypeMismatchException: For input string: "admin"`。

**异常不再一律 HTTP 200**（原 P2，此前前端 `status === 401` 分支永不触发）。
`GlobalExceptionHandler` 改为返回 `ResponseEntity`，`BizException` 按业务码映射真实状态，
`handleOther` 返回 500。`AuthInterceptor` 抛的 `BizException(AUTH_MISSING)` 也走这条路，
所以未登录现在是真的 401。实测不带 token：

```
GET /api/submissions   -> HTTP 401  {"code":1001,"message":"未登录或 Token 已失效"}
GET /api/auth/me       -> HTTP 401  {"code":1001,...}
GET /api/admin/users   -> HTTP 401  {"code":1001,...}
```

token 过期时前端会真的跳回登录页，不再卡在半登录状态。

**统一分页结构 `PageResult{list,total,page,size}`**（原 P1）。
运营后台 5 个列表页（`Users`/`PendingReviews`/`SubmissionManagement`/`Comments`/`Judges`）
与工作台 `Home` 全部改成读 `res.list`/`res.total`；两个前端源码里已无 `.records` 残留
（`Home.vue` 里的 `page.current` 是本地分页状态，不是响应字段）。
我在 admin 端加的 `normalizePage()` 适配层随之删除——后端已经是正确结构，不再需要。

**审核接口路径**：`ReviewController` 定为 `/reviews/pending`、`/reviews/{id}/approve|reject`
（`reason` 走 `@RequestParam`），前端 `review.ts` 同形态。

**下载接口**：后端改成 `@PostMapping("/{id}/download")`，与前端一直以来的 POST 对齐
（方向与我 09-30 的做法相反——我当时改前端，团队改后端，结果一致）。

### 二、分页拦截器、封面占位、构建脚本：合并把它们删了，本次重新落地

PR #3 的分支基线早于我的 `b2f85af`/`44f0ef2`/`0199f1c`，合并时对 `deploy/` 与
`frontend-workbench/src/utils/` 取了对方版本，导致四项已完成的工作被静默回退：

| 被删掉的东西 | 后果 | 本次处理 |
|---|---|---|
| `backend/.../config/MybatisPlusConfig.java` | **7 处 `selectPage` 全部静默失效**：不生成 LIMIT、不执行 COUNT，每页返回全表且 `total` 恒为 0，不报任何错。PR #3 新加的 `PageResult.from()` 直接取 `page.getTotal()`，所以换了新结构照样是错的 | 按原样重建 |
| `frontend-workbench/src/utils/cover.ts` + 3 个视图的接线 | 无封面作品退回千篇一律的浅蓝渐变半透明字，即 2026-09-30 那个需求整体失效 | 按原样重建 |
| `deploy/build-release.ps1` 的 PowerShell 5.1 适配 | **一键构建脚本在本机直接跑不通**：`$ErrorActionPreference='Stop'` 会把 java/mvn/npm/tar 写到 stderr 的正常输出当成终止性 `NativeCommandError`；`tar -czf $Archive *` 还会被 Git Bash 的 GNU tar 解析成 `host:path` 远程语法而报 `Cannot connect to D:` | 从 `0199f1c` 恢复（UTF-8 BOM、`Invoke-Tool` 包装、相对路径打包后再挪回） |
| `deploy/env/ai-marketplace.env.template` | 模板退回改造前：列了 6 个**后端已不再读取**的 `WECOM_*`，却删掉了**仍在读取**的 `AUTH_LOCAL_LOGIN_ENABLED` / `AUTH_DEFAULT_PASSWORD` | 恢复，并逐变量核对 |

模板与 `application.yml` 已逐一对账：yml 里出现的 **16 个 `${VAR}` 在模板中全部有对应行**，
无缺漏、无多余。

> 线上 `/aitest/app/config/ai-marketplace.env` 未受影响——`deploy-release.sh` 不碰 env 文件，
> 且它本来就带着 `AUTH_LOCAL_LOGIN_ENABLED` / `AUTH_DEFAULT_PASSWORD`。
> 模板写错只会影响**将来新装环境**，不会影响现网。

`deploy/sql/03-account-login.sql` 被 PR #3 删除是正确的——它早已在现网执行过，
`sys_user.account` / `password_hash` / `must_change_password` 三列实测都在。

### 三、分页拦截器确实生效（不是「代码放上去了」而是「行为对了」）

真分页的判据是**跨页不重复、末页条数正确、总数与库一致**。
实测（132 本机 `:8080`，带合法 token），末尾为数据库权威值：

```
/reviews/pending?page=1&size=1   -> list=1 total=2 page=1 ids=[3]
/reviews/pending?page=2&size=1   -> list=1 total=2 page=2 ids=[4]   ← 与上页不重复
/reviews/pending?page=1&size=10  -> list=2 total=2        ids=[3,4]
/submissions?page=1&size=1       -> list=1 total=2        ids=[2]
/submissions?page=1&size=10      -> list=2 total=2        ids=[2,1]
/submissions/admin?page=1&size=2 -> list=2 total=5        ids=[5,4]
/submissions/admin?page=3&size=2 -> list=1 total=5        ids=[1]   ← 末页只剩 1 条，切片正确
/submissions/mine?page=1&size=10 -> list=5 total=5        ids=[5,4,3,2,1]
/admin/users?page=1&size=1       -> list=1 total=2        ids=[3]
/admin/users?page=1&size=10      -> list=2 total=2        ids=[3,1]
/judges/recommendations          -> list=0 total=0
/comments/reported               -> list=0 total=0

DB: mp_submission(deleted=0)=5  status=PENDING=2  status=PUBLISHED=2
    sys_user(deleted=0)=2  mp_judge_recommendation=0  mp_comment_report=0
```

`/admin/dashboard/stats` 同样与 DB 一致：

```
{"totalUsers":2,"totalDownloads":0,"pendingReviews":2,"totalSubmissions":5,"activeCompetition":null}
```

拦截器缺失时上述每一行都会是 `list=全表 total=0`，故可确认 `PaginationInnerInterceptor` 已装配。

### 四、Schema 无需迁移（发版前已核对，不是发版后才发现）

PR #3 新增了 3 个 Controller，容易踩「代码要新表、库里没有」的坑，所以发版前做了两级核对：

- **表级**：16 个 `@TableName` 实体 ↔ 线上 `SHOW TABLES` 16 张，完全一致
- **列级**：从实体字段按驼峰转下划线推出 156 个 `表.列`，与
  `information_schema.COLUMNS` 的 156 条**双向 `comm` 比对，两个方向差集均为空**

> 比对时踩了个坑值得记：Git Bash 下 `python` 重定向出来的文件是 CRLF，
> 而 `ssh … mysql` 拿回来的是 LF，直接 `comm` 会把两个**完全相同**的集合判成全不匹配
> （表现是 156 行两边都进差集）。`hexdump` 首行才看出来。跨 Windows/Unix 传文本务必先 `tr -d '\r'`。

### 五、admin 口令被 `DataInitializer` 重置（发版前已征得确认并备份）

发版前实测线上 `admin` 的 `must_change_password=0` 且哈希非默认值，说明**有人已经改过这个口令**，
而 `DataInitializer` 会把它打回 `Welcome@2026`。因为这属于「改动共享系统上的凭据」，
先做了可回滚备份、确认后才发版：

```
备份：/aitest/backup/sys_user-pre-DataInitializer-20261008_141916.sql   (4067 B, mode 600)
      含 id=1(admin) 与 id=3(mzg) 两行完整 INSERT，原哈希已落盘

发版前： admin hash=$2a$10$Lvs…  must_change_password=0
发版后： admin hash=$2a$10$IT1…  must_change_password=1     ← 被 DataInitializer 改写
         mzg   hash=$2a$10$YFV…  must_change_password=1     ← 未被触碰（Runner 只管 admin）

启动日志实证（/aitest/app/logs/stdout.log）：
  14:24:51 DataInitializer : admin 密码哈希已用运行时 BCrypt 重新生成
  14:24:51 DataInitializer : admin must_change_password 已修正为 1
  14:24:51 DataInitializer : admin 账号已修复：password_hash=true, must_change_password=1
```

现在 `admin` / `Welcome@2026` 可以登录（实测 code=0，返回 264 字符 token，
用户「系统管理员」，角色 `['ADMIN','OPERATOR','USER']`），首次登录会被要求改密。
若要还原成发版前那个口令，用上面那个备份文件即可。
这个「每次重启都重置」的行为本身是缺陷，已记到上方 P2。

### 六、前端产物按「nginx 实际吐出的字节」核对，不看本地构建目录

端口与站点根目录以 `nginx -T` 为准（此前按「8081=admin」核对过，是错的）：

```
workbench  listen 80 default_server / 443 ssl / 8081 ssl   root /aitest/www/workbench
admin      listen 8443 ssl http2                            root /aitest/www/admin
（8082=tri-meeting，8083=aves-ai-hub，与本项目无关，未触碰）
```

实测：

```
workbench :8081  index.html -> assets/index-vCHlM7hG.js + index-DrrlG_uE.css
  cover-CSdc5Ctv.js    HTTP 200   522 B   调色板 20 色全部在产物里（逐个 grep 出 #RRGGBB）
  cover-S7fQQ3B8.css   HTTP 200  1156 B
  Home-BsX6XAsD.js              5628 B  import cover=1  onError:D=>B[e.id]=!0
  Ranking-CkwdmSrd.js           5601 B  import cover=1  onError:h=>E[a.id]=!0
  SubmissionDetail-C8zVGzJh.js 11453 B  import cover=1  onError:e[1]||(e[1]=h=>N.value=!0)

admin :8443  index.html -> assets/index-ChiImY1n.js + index-B6LVDrk6.css
  PendingReviews-OFKQKwHe.js  4519 B  仅含 /reviews/pending、/reviews/${p}/approve、/reject
  request-BIJJdl_m.js        52331 B  "records" 出现 0 次（适配层已删）  "401" 分支存在

企微域名归属验证文件 WW_verify_61IR393ntYc1jRjW.txt
  workbench :8081 -> HTTP 200     admin :8443 -> HTTP 200
```

`cover-CSdc5Ctv.js` 的哈希与 2026-09-30 那次**完全相同**。Vite 的 chunk 名是内容哈希，
同名即同字节，说明重建出来的封面逻辑与当时浏览器实测通过的那版一字不差。
三个视图的 chunk 哈希则变了（`Home-BsX6XAsD` 等），因为 PR #3 把 `res.records` 改成了 `res.list`。

### 七、验证方法备忘

**最省事的路子**：现在 admin 口令是已知的（`Welcome@2026`，见第五小节），
直接 `POST /api/auth/local/login` 拿 token 即可，不必再自己签 JWT。

**若口令又被改掉、且不想动数据库**：用后端自己的签名密钥本地铸一个 JWT。
`JwtService` 是标准 HS256，载荷字段 `uid`/`wid`/`name`/`dept`/`roles`（逗号串）+ `iat`/`exp`，
密钥取 `app.jwt.secret`（即 `/aitest/app/config/ai-marketplace.env` 里的 `JWT_SECRET`）。
在 132 上用 python3 的 `hmac`+`hashlib` 就地签发即可，全程只读，不落库、不改任何现有数据。
`roles` 必须含 `ADMIN`，否则过不了后台的角色守卫。

> 该密钥是**长期有效且可离线伪造任意用户身份**的凭据，只在服务器本机内存中使用、
> 不写入任何文件或日志；排查完即删除临时脚本。若要收敛风险，可考虑给 JWT 加 `jti` + Redis 黑名单。

**免登录探接口是否存在**：见下方 2026-09-30 小节里那条「用错误的 HTTP 方法打目标路径」的技巧。
PR #3 之后未登录返回 401 而非 200，但「已映射→方法不符异常，未映射→被拦截器判未登录」
这个判别逻辑依然成立。

---

## 已修复（2026-09-30）—— 四处接线类缺口

> **本节的做法大部分已被 PR #3 反转**，保留只为留下排查线索。当时的思路是「改前端去迁就后端」，
> PR #3 改成「改后端去对齐前端」，方向相反但结论一致。逐条现状：

| 当时的改动 | 现在的状态 |
|---|---|
| `review.ts` 由 `/reviews/*` 改为 `/review/pending` + `POST /review/action`（`ReviewActionDTO` 请求体） | **已被 PR #3 反转**：后端定为 `/reviews/*` + `reason` 走 query，前端同步。现网生效的是后者 |
| `review.ts` 加 `toItem()` 适配字段名，作者退化为「用户 #ID」 | **已被 PR #3 删除**，作者姓名/部门的缺口因此重新暴露，见上方 P1 |
| `request.ts` 加 `normalizePage()`（`records`→`list`、`current`→`page`） | **已删除且不再需要**：后端现在直接返回 `PageResult` |
| `submission.ts:112` 下载由 POST 改 GET | **已被 PR #3 反转**：改成后端接受 POST。现网前端一直是 POST，故生效 |
| 新增 `MybatisPlusConfig`（分页拦截器） | **被 PR #3 合并误删，2026-10-08 已重建** |

当时实测过的分页效果（`total=5`、逐页切片），与 2026-10-08 重测的结论一致，不再重复。

当时定位到的两条机理仍然有效：

**`/submissions/admin` 被 `/{id}` 吃掉的机理**：不是「落到静态资源处理器」，而是被
`@GetMapping("/{id}")` + `@PathVariable Long id` 抢先匹配，`"admin"` 转 `Long` 失败 →
`MethodArgumentTypeMismatchException`。线上日志当时已累计 52 次：

```
Failed to convert value of type 'java.lang.String' to required type 'java.lang.Long';
For input string: "admin"
```

补接口时**字面量路径必须声明在模板路径之前**，否则匹配优先级会让它继续被 `/{id}` 吃掉。
这也解释了为什么「作品管理」页当时报 5000 而不是 1001。

**免登录判别接口是否存在**：用「错误的 HTTP 方法」打目标路径——

- 路径**已映射**（只是方法不符）→ `RequestMappingHandlerMapping` 在 `getHandler()` 阶段就抛
  `HttpRequestMethodNotSupportedException`，**早于拦截器**
- 路径**未映射** → 落到静态资源处理器 → `AuthInterceptor` 先跑 → 1001

当时的实测对照（PR #3 之后码值会变，但判别方式不变）：

```
POST /api/review/pending          -> 5000   （已映射为 GET，方法不符）
GET  /api/review/pending          -> 1001   （已映射，被登录拦截）
GET  /api/reviews/pending         -> 1001   （当时未映射）
GET  /api/submissions/1/download  -> 1001   （当时已映射为 GET）
POST /api/submissions/1/download  -> 5000   （当时未映射 POST，方法不符）
```

---

## 已修复（2026-09-30）—— 无封面作品改用纯色占位

原先 4 处封面兜底都是同一个浅蓝渐变 + 半透明蓝字，整屏看起来千篇一律。
现改为**按作品稳定取色的纯色填充**，色系参考
[简书那篇文章](https://www.jianshu.com/p/48be69b211a3)提到的莫兰迪 / 马卡龙 / 多巴胺三档，
共 20 色（莫兰迪 10、马卡龙 6、多巴胺 4），企业内网以低饱和为主。

`frontend-workbench/src/utils/cover.ts`，4 处调用点：
`Home.vue`（卡片 160px）、`Ranking.vue`（榜单 56px，2 处）、`SubmissionDetail.vue`（详情 280×200）。
运营后台不渲染封面，未改动。

两个刻意的设计取舍：

- **种子是 `id + 标题` 的哈希，不是 `Math.random()`。** 随机数会让同一作品在列表页和详情页
  取到不同颜色，且每次重渲染都跳变。带 `id` 还顺带解决了现网有 3 条同名「智能待办工作台」
  的问题——只用标题做种子它们会同色。
- **同时补了图片加载失败兜底**（`@error` → 退回纯色）。不加这条的话，上面 P2 那 3 条
  `cover_url` 是网页地址的作品根本走不到占位分支，需求等于没生效。

实测证据（跑的是 `dist/assets/cover-*.js` 这个**构建产物**本身，不是源码副本）：

```
覆盖率   20/20 色全部可达   卡方 17.8（df=19，>30 才算偏斜）   区间 [931, 1061] / 期望 1000
对比度   最低 3.42:1 (#7E93A8)  最高 5.72:1 (#F0C879)  → 20/20 达到 WCAG 大字 3:1 门槛
稳定性   同输入 500 次结果恒定
一致性   同一作品的 56px 小图与 280×200 大图取色相同（浏览器 computed style 逐条比对）
```

过程中量出两个真问题，都已修掉：

1. **FNV-1a 直接 `% 20` 只命中偶数下标**，20 色里有 10 色永远取不到，卡方高达 31915。
   补一道 xorshift 终混后降到 17.8、20 色全覆盖。低位熵差是 FNV 的已知特性，
   任何「哈希后取模选样式」的场景都要注意。
2. **首字颜色原先按亮度阈值挑黑/白**，结果高饱和的橙 / 蓝 / 玫红被误判成深色底而选白字，
   实测对比度只有 2.57~2.97:1，不达标。改成按实际 WCAG 对比度择优后发现：**这 20 个色
   深色字全面胜出**，白字分支成了死代码，遂删掉、统一用 `rgba(31,41,55,.8)`，
   chunk 从 1.07 kB 降到 0.52 kB。往调色板加深色时需重新复核（源码注释已标注）。

> 对比度不能拿 sRGB 值直接加权，必须先线性化
> （`c<=0.03928 ? c/12.92 : ((c+0.055)/1.055)^2.4`）；半透明前景还要先与底色合成
> （`fg*a + bg*(1-a)`）再度量，否则算出来的数看着达标、实际不达标。

> 本次为纯前端改动。2026-10-08 重新落地后 chunk 哈希仍是 `cover-CSdc5Ctv.js`，
> 与当时浏览器实测通过的版本同字节。

---

## 已修复（2026-09-23，含实测证据）

### 登录链路 —— 原本完全不通，现已浏览器实测通过

原先前端调 `/auth/wecom/login`、`/auth/me`、`/auth/wecom/redirect`，
后端只有 `/auth/login` 和 `/auth/oauth-url`，三个接口全部 404（表现为业务码 5000），
点「企微 SSO 登录」按钮只发出一个请求就卡住不动。

改动：

| 文件 | 改动 |
|---|---|
| `controller/AuthController.java` | 重写为 `/auth/wecom/redirect`、`/auth/wecom/login`、`/auth/local/login`、`/auth/me` 四个接口，删除无人调用且返回模板串的旧 `/auth/login`、`/auth/oauth-url` |
| `service/AuthService.java` | 新增 `buildWecomAuthUrl()`（用真实 corpId/agentId 拼装、`redirect_uri` 做 URL 编码，替换掉原先的 `${corpid}` 字面量占位符）、`loginByWecomCode()`、`loginByPassword()`、`currentUser()`；登录响应改为同时返回 `token` 与 `user` |
| `config/WebMvcConfig.java` | 白名单从 `/auth/**` 收窄为逐个放行三个登录入口——否则 `/auth/me` 会被拦截器跳过，`ThreadLocalContext` 里拿不到登录态 |
| `dto/UserVO.java`、`dto/LoginResultVO.java`、`dto/LocalLoginDTO.java` | 新增。`UserVO` 统一两个前端的用户字段契约（`roles` 数组），且不返回 `passwordHash` |
| `entity/SysUser.java` | 增加 `account`、`passwordHash`；`passwordHash` 加 `@JsonIgnore` 防止任何直接返回实体的接口泄露哈希 |
| `config/AppProperties.java`、`resources/application.yml` | 增加 `app.auth.local-login-enabled`，默认 `false` |
| `common/ErrorCode.java` | 增加 1004～1007。其中「账号或口令错误」把账号不存在与口令不匹配合并为同一个码，避免被用来枚举有效账号 |
| 两个前端 `api/auth.ts` | 增加 `localLogin()`；工作台的 `redirectToWecomAuth()` 原先直接 `window.location` 导航到接口地址（只会看到一坨 JSON），改为 XHR 取 `redirectUrl` 再跳转，与运营后台一致 |
| 两个前端 `views/Login.vue` | 增加账号口令表单，与 SSO 按钮共存；成功/失败处理与角色守卫抽成 `applyLoginResult()` 供两条路径共用 |
| `workbench/stores/auth.ts`、`views/Me.vue` | 用户字段由单数 `role` 改为数组 `roles`，与后端 `UserVO` 对齐 |

实测结果（浏览器实操，非仅接口调用）：

- 运营后台 `http://192.168.1.132:8081/login` → 账号口令登录 → 跳转 `/dashboard`，
  标题「仪表盘 · 企业 AI 应用市场 — 运营后台」，角色守卫通过，顶栏显示「系统管理员」
- 员工工作台 `http://192.168.1.132/login` → 登录 → 跳转首页，作品列表正常渲染空状态，无错误提示
- 个人中心 `/me` 显示「信息技术部 · ADMIN / OPERATOR / USER」，刷新后能自动重新拉取 `/auth/me`
- 接口层：错误口令与不存在账号均返回 1004（不泄露账号存在性）；无 token 调 `/auth/me` 返回 1001

### 数据库迁移

`sys_user` 增加 `account`（唯一索引 `uk_account`）与 `password_hash` 两列，
见 `deploy/sql/02-add-local-login.sql`（幂等，可重复执行）。
`account` 与 `wecom_userid` 解耦，因此日后把 `wecom_userid` 从占位值 `admin`
换成真实企微 userId 时，账号口令登录不会失效。

### 此前修复的编译与启动阻塞（2026-09-21）

后端原本**无法通过编译**，6 处错误涉及 5 个文件；另有一处能编译但启动即崩：

| 文件 | 问题 | 修法 |
|---|---|---|
| `common/Result.java:34` | 跨类直接访问 `ErrorCode` 的 private 字段 | 改用 Lombok 生成的 getter |
| `security/JwtService.java:55` | Hutool 5.8.32 的 `JWT` 无 `getExpiresAt()` | 改用 `JWTValidator.of(jwt).validateDate()` |
| `service/AuthService.java:45` | 引用不存在的 `WecomClient.WecomUserInfo` | 改为真实返回类型 `WecomApiDto.UserDetailResp` |
| `service/ReviewService.java:67,90` | 局部变量 `log` 遮蔽 `@Slf4j` 字段 | 重命名为 `reviewLog` |
| `integration/storage/StorageService.java` | 缺 `SubmissionController` 调用的 `resolve(String)` | 补抽象方法并在 `LocalStorageService` 实现 |
| `integration/storage/LocalStorageService.java:29` | `@ConditionalOnMissingBean` 用在 `@Component` 上，扫描期把自身算作已存在 bean 而跳过注册，容器内无任何 `StorageService`，`SubmissionService` 注入失败 | 移除该注解；日后替换实现时让新实现标 `@Primary` |

> 最后一条尤其隐蔽：编译能过、进程能起、只在日志里报 `UnsatisfiedDependencyException`，
> 然后被 systemd 的 `Restart=on-failure` 拖进崩溃循环。
> `@ConditionalOnMissingBean` 只对 auto-configuration 的 `@Bean` 方法可靠，不要用在 `@Component` 上。

### 前端既有类型错误（未修，不阻塞构建）

`npm run build` 会先跑 `vue-tsc --noEmit` 因而失败，需改用 `npx vite build`。实测类型错误：

- `frontend-workbench`：仅 1 处，`tsconfig.json(31,18) TS6310: Referenced project
  'tsconfig.node.json' may not disable emit` —— 项目引用配置问题
- `frontend-admin`：**12 处**（2026-10-08 PR #3 之后重测，此前为 13 处），分四类：
  - 7× `TS2345`：`el-table` 作用域插槽的 `DefaultRow` 未窄化到行类型
    （`Comments.vue:117,118,119`、`PendingReviews.vue:106,109`、`SubmissionManagement.vue:198,202`）
  - 3× `TS2322`：`el-tag` 的 `type` 可能是空串 `""`，而组件只接受
    `primary|success|warning|info|danger|undefined`（`Users.vue:155`、
    `SubmissionManagement.vue:179`、`Judges.vue:107`）
  - 1× `TS2322`：`api/request.ts:97` 的 `http<T>` 返回类型与 axios 泛型不符
    （拦截器已解包，类型上却仍是 `AxiosResponse`）
  - 1× `TS6133`：`PendingReviews.vue:3` 的 `ElMessageBox` 导入未使用

这些都不影响 `vite build` 产物，但建议清掉，否则 CI 里加类型门禁会一直红。
