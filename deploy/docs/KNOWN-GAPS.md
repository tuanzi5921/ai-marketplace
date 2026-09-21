# 待修复清单 — 业务代码缺口（部署已就绪，功能未闭环）

> 环境搭建与部署已于 2026-09-21 完成并验收通过（见 `deploy/README.md`）。
> 本文记录的是**代码层面**的缺口：基础设施可用、进程可启动、数据库可读写，
> 但业务闭环（尤其是登录）尚未打通。以下每条都附有实测证据，按修复优先级排序。

---

## P0 — 登录链路：前端调用的接口后端不存在

两个前端（`frontend-workbench`、`frontend-admin`）调用的是同一组接口，后端一个都没实现。

| 前端调用 | 后端实际实现 | 问题 |
|---|---|---|
| `POST /api/auth/wecom/login?code=` | `GET /api/auth/login?code=` | 路径与方法都不符 |
| `GET  /api/auth/me` | 无 | 缺失 |
| `GET  /api/auth/wecom/redirect` | `GET /api/auth/oauth-url?redirectUri=` | 路径不符 |

代码位置：
- 后端 `backend/.../controller/AuthController.java`（只有 `/login` 与 `/oauth-url` 两个方法）
- 工作台 `frontend-workbench/src/api/auth.ts:12,19,24`
- 后台 `frontend-admin/src/api/auth.ts`

**后果：任何人都无法登录，系统不可用。**

### 附带问题 1：`/auth/oauth-url` 返回的是未拼装的模板串

`AuthController.java:36` 返回的字符串里 `appid=${corpid}`、`agentid=${agentid}` 是**字面量占位符**，没有用 `AppProperties` 里的真实值替换。实测响应：

```json
{"code":0,"data":"https://open.weixin.qq.com/connect/oauth2/authorize?appid=${corpid}&redirect_uri=http://192.168.1.132/login&response_type=code&scope=snsapi_base&agentid=${agentid}#wechat_redirect"}
```

### 附带问题 2：企微回调路由不存在

`application.yml` 的 `WECOM_AUTH_CALLBACK` 默认值是 `http://localhost:5173/oauth/callback`，
但 `frontend-workbench/src/router/index.ts` 里**没有 `/oauth/callback` 路由**，会命中末尾的
`/:pathMatch(.*)*` 重定向到 `/`，`code` 查询参数随之丢失。

真正读取 `code` 的是 `/login` 页面（`Login.vue` 的 `onMounted` 里取 `route.query.code`）。

> 部署时已把 `WECOM_AUTH_CALLBACK` 配成 `http://192.168.1.132/login` 以匹配前端现状。
> 若改回 `/oauth/callback`，需要同时在前端补这个路由。

### 附带问题 3：登录响应缺 `user` 字段

`AuthService.login()` 返回 `Map.of("token", token)`，但前端 `LoginResult` 期望 `{ token, user }`，
`auth.setUser(result.user)` 会拿到 `undefined`。

---

## P0 — 运营后台接口大面积缺失

后端只有 7 个 Controller，后台前端依赖的以下接口全部不存在：

| 前端调用（`frontend-admin/src/api/`） | 后端 | 说明 |
|---|---|---|
| `GET /admin/users` | 无 UserController | 用户列表 |
| `POST /admin/users/{id}/roles` | 无 | 授予角色 |
| `DELETE /admin/users/{id}/roles` | 无 | 撤销角色 |
| `POST /admin/users/{id}/enable` `/disable` | 无 | 启停用户 |
| `GET /admin/dashboard/stats` | 无 DashboardController | 数据看板 |
| `GET /judges/recommendations` | 无 JudgeController | 评委推荐列表 |
| `POST /judges/recommendations/{id}/approve` `/reject` | 无 | 评委备案 |
| `GET /submissions/admin` | 无 | 后台作品列表 |
| `POST /submissions/{id}/offline` | 无 | 作品下架 |
| `DELETE /submissions/{id}` | 无 | 作品删除 |
| `GET /comments/reported` | 无 | 被举报评论列表 |

README「v1 已完成范围」里写的 8 个 Controller，实际只有 7 个，且上述模块均未实现。

---

## P1 — 审核接口路径与参数形态不符

| 前端调用 | 后端实现 | 差异 |
|---|---|---|
| `GET /reviews/pending` | `GET /review/pending` | 单复数不一致 |
| `POST /reviews/{id}/approve?reason=` | `POST /review/action`（`ReviewActionDTO` 请求体） | 路径不同，且后端用 body 传参、前端用 query 传参 |
| `POST /reviews/{id}/reject?reason=` | 同上 | 同上 |

二选一对齐即可，建议改后端以匹配前端 RESTful 风格。

---

## P1 — 分页响应结构不匹配（接口打通后仍会白屏）

后端直接返回 MyBatis-Plus 的 `Page` 对象，实测结构：

```json
{"records":[],"total":0,"size":5,"current":1,"pages":0}
```

而 `frontend-admin/src/api/request.ts` 定义的 `PageResult<T>` 是：

```ts
{ list: T[]; total: number; page: number; size: number }
```

`records` vs `list`、`current` vs `page` 字段名不一致 → 即使接口路径修对了，列表页也会拿到 `undefined` 而渲染为空。

该文件注释里已预留「后端如不一致可在 normalizer 处适配」，建议在响应拦截器里统一转换，
避免逐个页面改。`frontend-workbench` 同样需要核对。

---

## P1 — 作品下载方法不一致

- 前端 `frontend-workbench/src/api/submission.ts:113`：`POST /submissions/{id}/download`
- 后端 `SubmissionController.java:58`：`@GetMapping("/{id}/download")`

方法不符 → 405。另外前端用 `request.post<Blob>` 取二进制，而响应拦截器对无 `code` 字段的响应会原样返回，这条链路需要一并验证。

---

## P2 — 所有异常都返回 HTTP 200，前端 401 处理永不触发

`GlobalExceptionHandler` 把包括 `BizException` 在内的所有异常都包成 HTTP 200 + 业务码。实测：

```
GET /api/submissions  （不带 token） → HTTP 200  {"code":1001,"message":"未登录或 Token 已失效"}
GET /api/admin/users  （接口不存在）  → HTTP 200  {"code":1001,"message":"未登录或 Token 已失效"}
GET /api/auth/me      （接口不存在）  → HTTP 200  {"code":5000,"message":"服务端异常"}
```

两个后果：

1. **前端 `error.response.status === 401` 的分支永远不会执行**（`workbench/src/api/request.ts`、
   `admin/src/api/request.ts` 都有这段逻辑），token 过期时用户只看到一个错误提示，
   不会被跳转回登录页，页面会卡在半登录状态。
   修法：拦截器里改为判断 `body.code === 1001`，或让 `BizException` 映射到真实 HTTP 状态码。

2. **接口不存在时返回「未登录」，极具误导性**。因为 `AuthInterceptor` 拦截 `/**` 且先于路由匹配执行，
   任何未在白名单里的不存在路径都会先被判为未登录。排查后台 404 时容易被带偏到登录问题上。
   注意 `/auth/**` 在白名单内，所以同样不存在的 `/api/auth/me` 返回的是 5000 而非 1001——
   这个差异可以用来区分「接口不存在」和「真的未登录」。

---

## 已在部署过程中修复的问题（仅供追溯，无需再处理）

后端原本**无法通过编译**，共 6 处错误、涉及 5 个文件，已修复并产出可运行 jar：

| 文件 | 问题 | 修法 |
|---|---|---|
| `common/Result.java:34` | 跨类直接访问 `ErrorCode` 的 private 字段 `code`/`message` | 改用 Lombok 生成的 `getCode()`/`getMessage()` |
| `security/JwtService.java:55` | Hutool 5.8.32 的 `JWT` 类没有 `getExpiresAt()` | 改用 `JWTValidator.of(jwt).validateDate()`，与 `sign()` 里 `setExpiresAt` 的存取约定一致，避免手写秒/毫秒换算 |
| `service/AuthService.java:45` | 引用不存在的 `WecomClient.WecomUserInfo` | 改为 `getUserDetail()` 的真实返回类型 `WecomApiDto.UserDetailResp` |
| `service/ReviewService.java:67,90` | 局部变量 `log`（`MpReviewLog`）遮蔽了 `@Slf4j` 的日志字段 | 重命名为 `reviewLog` |
| `integration/storage/StorageService.java` | 缺少 `SubmissionController` 调用的 `resolve(String)` | 补抽象方法，并在 `LocalStorageService` 实现 |
| `integration/storage/LocalStorageService.java:29` | `@ConditionalOnMissingBean` 用在 `@Component` 上，扫描期把自身算作已存在的 `StorageService` 而跳过注册，导致容器内无任何 `StorageService`，`SubmissionService` 注入失败、应用无法启动 | 移除该注解；日后替换实现时让新实现标 `@Primary` |

> 最后一条尤其隐蔽：编译能过、进程能起、日志里只有 Spring 的 `UnsatisfiedDependencyException`。
> `@ConditionalOnMissingBean` 只对 auto-configuration 的 `@Bean` 方法可靠，不要用在 `@Component` 上。
