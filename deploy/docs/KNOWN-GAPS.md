# 待修复清单 — 业务代码缺口

> 环境与部署已验收通过，**登录链路已于 2026-09-23 打通并浏览器实测通过**（见文末「已修复」）。
> 本文记录仍然存在的**代码层面**缺口，每条附实测证据，按优先级排序。

---

## P0 — 运营后台接口大面积缺失

后台页面能打开、能登录，但**除「大赛配置」外每个菜单点开都会报「服务端异常」**，
因为后端只有 7 个 Controller，下列接口全部不存在：

| 前端调用（`frontend-admin/src/api/`） | 后端 | 影响的页面 |
|---|---|---|
| `GET /admin/dashboard/stats` | 无 DashboardController | 仪表盘（实测已报「服务端异常」，四个统计数字恒为 0） |
| `GET /admin/users` | 无 UserController | 用户管理 |
| `POST /admin/users/{id}/roles` | 无 | 用户管理 — 授予角色 |
| `DELETE /admin/users/{id}/roles` | 无 | 用户管理 — 撤销角色 |
| `POST /admin/users/{id}/enable` `/disable` | 无 | 用户管理 — 启停 |
| `GET /judges/recommendations` | 无 JudgeController | 评委推荐 |
| `POST /judges/recommendations/{id}/approve` `/reject` | 无 | 评委推荐 — 备案 |
| `GET /submissions/admin` | 无 | 作品管理 |
| `POST /submissions/{id}/offline` | 无 | 作品管理 — 下架 |
| `DELETE /submissions/{id}` | 无 | 作品管理 — 删除 |
| `GET /comments/reported` | 无 | 评论管理 |

> 排查提示：这些接口返回的是 **HTTP 200 + 业务码 1001「未登录或 Token 已失效」**，
> 看起来像登录问题，实际是接口不存在。原因见下方 P2。

README「v1 已完成范围」声称有 8 个 Controller，实际 7 个，且上述模块均未实现。

---

## P1 — 审核接口路径与参数形态不符

| 前端调用 | 后端实现 | 差异 |
|---|---|---|
| `GET /reviews/pending` | `GET /review/pending` | 单复数不一致 |
| `POST /reviews/{id}/approve?reason=` | `POST /review/action`（`ReviewActionDTO` 请求体） | 路径不同，且后端用 body 传参、前端用 query 传参 |
| `POST /reviews/{id}/reject?reason=` | 同上 | 同上 |

影响「待审作品」页面。二选一对齐即可，建议改后端以匹配前端的 RESTful 风格。

---

## P1 — 分页响应结构不匹配（接口补齐后仍会白屏）

后端直接返回 MyBatis-Plus 的 `Page` 对象，实测结构：

```json
{"records":[],"total":0,"size":5,"current":1,"pages":0}
```

而 `frontend-admin/src/api/request.ts` 定义的 `PageResult<T>` 是：

```ts
{ list: T[]; total: number; page: number; size: number }
```

`records` vs `list`、`current` vs `page` 字段名不一致 → 即使接口路径修对了，
列表页也会拿到 `undefined` 而渲染为空。

该文件注释里已预留「后端如不一致可在 normalizer 处适配」，建议在响应拦截器里统一转换，
不要逐个页面改。`frontend-workbench` 的作品列表已经能正常显示空状态，但也要一并核对。

---

## P1 — 作品下载方法不一致

- 前端 `frontend-workbench/src/api/submission.ts:113`：`POST /submissions/{id}/download`
- 后端 `SubmissionController.java`：`@GetMapping("/{id}/download")`

方法不符 → 405。另外前端用 `request.post<Blob>` 取二进制，
而响应拦截器对无 `code` 字段的响应会原样返回，这条链路需要一并验证。

---

## P1 — 企微 SSO 仍缺可信域名，暂不可用

后端接口与凭据都已就绪，实测 `/api/auth/wecom/redirect` 已能返回正确拼装的授权地址：

```
https://open.weixin.qq.com/connect/oauth2/authorize?appid=wwca6dedc33172f790
  &redirect_uri=http%3A%2F%2F192.168.1.132%2Flogin&response_type=code
  &scope=snsapi_base&agentid=1000017#wechat_redirect
```

但 `redirect_uri` 现在是内网 IP，企微会拒绝。要真正跑通还需（**均为企微后台与网络侧操作，不是代码问题**）：

1. 定一个正式域名（`*.tri-ibiotech.com` 通配符证书已覆盖任意一级子域，建议如 `ai.tri-ibiotech.com`）
2. 加 DNS 解析，并让公网入口（当前 `www.tri-ibiotech.com` 走的是第三方 WAF `ipm0ddpz.waf.zfuwuqi.com`）
   把该域名的 443 回源到 192.168.1.132
3. 在企微管理后台「网页授权及JS-SDK」里配置**可信域名**并完成归属验证
   —— 验证文件已放好，见 `deploy/README.md`
4. 把 `WECOM_AUTH_CALLBACK` 改成 `https://<正式域名>/login`
5. 确认「企业可信IP」含 `121.46.250.190`（实测 gettoken 已返回 errcode 0，说明当前已放行）

另外：授权地址里**没有带 `state` 参数**，因此不存在 OAuth 登录 CSRF 防护。
前端契约里没有回传 state 的环节，要补需要前后端一起改（后端下发 state 存 Redis、
回调时校验），属于加固项。

---

## P2 — 所有异常都返回 HTTP 200，前端 401 处理永不触发

`GlobalExceptionHandler` 把包括 `BizException` 在内的所有异常都包成 HTTP 200 + 业务码。实测：

```
GET /api/submissions  （不带 token） → HTTP 200  {"code":1001,"message":"未登录或 Token 已失效"}
GET /api/admin/users  （接口不存在）  → HTTP 200  {"code":1001,"message":"未登录或 Token 已失效"}
GET /api/auth/me      （不带 token）  → HTTP 200  {"code":1001,...}
```

后果：**前端 `error.response.status === 401` 的分支永远不会执行**
（`workbench/src/api/request.ts`、`admin/src/api/request.ts` 都有这段逻辑），
token 过期时用户只看到一个错误提示、不会被跳转回登录页，页面卡在半登录状态。

修法二选一：拦截器改判 `body.code === 1001`，或让 `BizException` 映射到真实 HTTP 状态码。

> 附带说明：`AuthInterceptor` 拦截 `/**` 且先于路由匹配执行，所以任何不在白名单里的
> **不存在路径**都会先被判为未登录（1001）；而 `/auth/wecom/redirect` 等白名单路径下的
> 不存在接口才会走到 no-handler 返回 5000。这个差异可用来区分「接口不存在」和「真的未登录」。

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
- `frontend-admin`：13 处，集中在 `api/request.ts`（`http<T>` 的返回类型与 axios 泛型不符）
  与 `views/` 下多个页面（`el-table` 作用域插槽的 `DefaultRow` 未窄化、`el-tag` 的
  `type` 传了空串、两处 import 未使用）

这些都不影响 `vite build` 产物，但建议清掉，否则 CI 里加类型门禁会一直红。
