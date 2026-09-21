# 企业 AI 应用市场 (Enterprise AI Marketplace)

> 员工上传 AI 生成的可复用解决方案（skill / 软件 / 平台 / 文档），支持下载、评分、排名，可发布到企微工作台或公司官网。AI 技能大赛作为启动活动，赛后转型为常驻资产库。

---

## 一、项目定位

| 维度 | 说明 |
|---|---|
| **长期定位** | 企业内部 AI 应用市场，常驻资产库 |
| **启动活动** | AI 技能大赛（流量入口 + 冷启动） |
| **部署形态** | 私有化部署，单机最小架构 |
| **集成入口** | 企微 PC 工作台 H5 应用 + 公司官网 |
| **架构特性** | 双模架构（大赛期 / 常驻期）+ AI 评审模块可插拔隔离 |

## 二、五角色权限矩阵

| 角色 | 上传/下载/评分/评论 | 审核作品 | 后台运营 | 大赛配置/权限授予 | 评分面板（权重高） | 部门看板 + 评委推荐 |
|---|---|---|---|---|---|---|
| 普通员工 | ✅ | — | — | — | — | — |
| 运营 | ✅ | ✅ | ✅ | — | — | — |
| 超管 | ✅ | ✅ | ✅ | ✅ | — | — |
| 评委 | ✅ | — | — | — | ✅（看全量作品） | — |
| 部门负责人 | ✅ | — | — | — | — | ✅（仅本部门） |

- 一个用户可叠加多角色（如部门负责人 + 评委）
- 角色授予：超管在后台手动授予
- 评委资格：部门推荐 + 运营备案，任期 1 年可轮换

## 三、四类作品形态

| 类型 | 存储方式 | 审核重点 |
|---|---|---|
| 源码 / 代码仓库链接 | 链接型不占存储；zip 走文件存储 | 代码审计、开源协议合规 |
| 可执行包 / 安装包 | 大文件存储 + 内部 CDN | 病毒扫描、依赖审计、签名 |
| 在线部署 SaaS / 平台 | 仅元数据，无文件 | 访问控制、数据合规、可用性 |
| 文档 / 方案 / 教程 / Prompt 库 | Markdown 结构化存储 | 内容合规、敏感信息识别 |

## 四、核心流程

### 4.1 先审后发（v1 标准三节点）
1. **提交节点**：作者提交作品 → 机器人提醒运营入队（个人应用消息）
2. **审核节点**：运营在后台审 → 通过则发布 + 推送上架通知；拒绝则退回作者 + 作者补修后重提
3. **下载评分节点**：用户下载后才能评分（防刷分约束）

- **审核 SLA**：3 个工作日
- **AI 初筛覆盖**（开启时）：文本敏感词、代码安全、二进制/镜像扫描、敏感数据 NER

### 4.2 评委评审机制
- 线上独立评分（无需线下集中会议）
- 评委在评分面板独立打分 → 系统按 **评委 3 + 员工 7** 权重汇总 → 输出名次

### 4.3 双模架构
- **大赛期**：开放自由上传、积分映射到入围 + 分赛道排名 + 评委评审、大赛节点倒计时展示
- **常驻期**：日常上传下载、积分兑换徽章与奖品、无大赛评定
- **节点切换 SOP**（全期宽松）：提交期 → 评审期（仅冻结新上传，仍允许下载/评分） → 颁奖期（恢复自由上传，无缝转常驻）

---

## 五、技术栈

| 层 | 技术选型 |
|---|---|
| 后端 | Spring Boot 3 + MyBatis-Plus + MySQL 8 + Redis + Hutool |
| 鉴权 | JWT + 企微 SSO OAuth2 |
| 文档 | Knife4j（/api/doc.html） |
| 工作台前端 | Vue 3 + TypeScript + Vite + Element Plus + Pinia |
| 运营后台前端 | Vue 3 + TypeScript + Vite + Element Plus + Pinia |
| 集成 | 企微 OpenAPI（access_token + 通讯录 + 个人应用消息） |

---

## 六、工程结构

```
E:\projects\ai-marketplace\
├── backend\                       # Spring Boot 3 后端
│   ├── pom.xml
│   └── src\main\
│       ├── java\com\company\ai\marketplace\
│       │   ├── AiMarketplaceApplication.java   # 启动类
│       │   ├── common\             # 通用异常 / 错误码 / Result
│       │   ├── config\             # AppProperties / 拦截器 / WebMvc
│       │   ├── security\           # JWT / LoginUser / ThreadLocalContext
│       │   ├── entity\             # 16 个实体（含 SysUser / MpSubmission / MpRating 等）
│       │   ├── mapper\             # 16 个 MyBatis-Plus Mapper
│       │   ├── dto\                # 请求 DTO（Competition / Rating / ReviewAction / SubmissionCreate）
│       │   ├── controller\         # 8 个 Controller
│       │   ├── service\            # 7 个核心 Service + review 子包
│       │   │   ├── AuthService            # 企微 SSO + JWT
│       │   │   ├── SubmissionService     # 作品提交 / 下载
│       │   │   ├── ReviewService         # 先审后发
│       │   │   ├── RatingService          # 评分（防刷 + 权重）
│       │   │   ├── CompetitionService    # 大赛配置 + 状态机
│       │   │   ├── CommentService         # 评论 + 举报
│       │   │   ├── RankingService         # 排行榜
│       │   │   ├── NotificationService   # 企微消息推送
│       │   │   ├── PointService           # 积分
│       │   │   ├── AuditService           # 操作审计
│       │   │   └── review\               # 审核步骤链
│       │   └── integration\
│       │       ├── wecom\          # 企微 OpenAPI 客户端
│       │       │   ├── WecomClient.java
│       │       │   └── dto\WecomApiDto.java
│       │       └── storage\        # 文件存储抽象 + 本地实现
│       │           ├── StorageService.java
│       │           └── LocalStorageService.java
│       └── resources\
│           ├── application.yml             # 主配置
│           ├── application-dev.yml         # 开发环境
│           ├── application-prod.yml        # 生产环境
│           └── sql\schema.sql              # 数据库初始化脚本
├── frontend-workbench\            # 员工工作台 H5（端口 5173）
│   ├── package.json
│   ├── vite.config.ts              # /api 代理到 8080
│   └── src\
│       ├── api\                    # 7 个 API 模块
│       ├── stores\auth.ts          # Pinia 鉴权
│       ├── layouts\MainLayout.vue  # 顶部导航
│       └── views\                  # 6 个页面
│           ├── Login.vue           # 企微 SSO
│           ├── Home.vue            # 作品列表 + 大赛倒计时
│           ├── SubmissionDetail.vue # 详情 + 评分 + 评论
│           ├── Submit.vue           # 上传作品
│           ├── Ranking.vue          # 排行榜
│           └── Me.vue              # 个人中心
├── frontend-admin\                 # 运营后台（端口 5174）
│   ├── package.json
│   ├── vite.config.ts              # /api 代理到 8080
│   └── src\
│       ├── api\                    # 9 个 API 模块
│       ├── stores\auth.ts          # 含 hasRole / canAccessAdmin
│       ├── layouts\AdminLayout.vue # 深色侧边栏后台布局
│       └── views\                  # 8 个页面
│           ├── Login.vue
│           ├── Dashboard.vue       # 数据看板
│           ├── PendingReviews.vue  # 待审作品
│           ├── SubmissionManagement.vue
│           ├── Competitions.vue    # 大赛配置 + 阶段切换
│           ├── Users.vue           # 用户管理
│           ├── Comments.vue        # 评论管理
│           └── Judges.vue          # 评委推荐备案
├── docs\adr\                       # 架构决策记录
│   ├── 0001-tech-stack-and-architecture.md
│   ├── 0002-dual-mode-architecture-competition-vs-permanent.md
│   └── 0003-ai-review-pluggable-isolation.md
└── CONTEXT.md                      # 项目术语表
```

---

## 七、环境要求

| 工具 | 版本 | 用途 |
|---|---|---|
| JDK | 17+ | 后端运行 |
| Maven | 3.8+ | 后端构建 |
| MySQL | 8.0+ | 数据存储 |
| Redis | 6.0+ | access_token 缓存 + 会话 |
| Node.js | 18+ | 前端构建 |
| npm | 9+ | 前端依赖管理 |

PowerShell 验证命令：
```powershell
java -version; mvn -v; mysql --version; redis-server --version; node -v; npm -v
```

---

## 八、启动顺序

### 步骤 1：初始化数据库

```sql
-- 用 root 登录 MySQL
CREATE DATABASE ai_marketplace DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'ai_mp'@'%' IDENTIFIED BY 'your_password';
GRANT ALL ON ai_marketplace.* TO 'ai_mp'@'%';
FLUSH PRIVILEGES;
```

执行初始化脚本：
```powershell
mysql -u ai_mp -p ai_marketplace < E:\projects\ai-marketplace\backend\src\main\resources\sql\schema.sql
```

### 步骤 2：启动 Redis

```powershell
redis-server --port 6379
```

### 步骤 3：配置后端环境变量

在 `E:\projects\ai-marketplace\backend\` 下创建 `.env` 或直接设置环境变量：

```powershell
# 数据库
$env:DB_HOST="localhost"
$env:DB_PORT="3306"
$env:DB_NAME="ai_marketplace"
$env:DB_USER="ai_mp"
$env:DB_PASSWORD="your_password"

# Redis
$env:REDIS_HOST="localhost"
$env:REDIS_PORT="6379"

# JWT 密钥（生产请用 32 位以上随机串）
$env:JWT_SECRET="change-me-in-production-please-use-strong-secret"

# 文件存储根目录
$env:STORAGE_BASE_PATH="E:/projects/ai-marketplace/storage"

# 企微配置（v1 默认关闭推送，开启时再注入）
$env:WECOM_CORP_ID=""
$env:WECOM_AGENT_ID=""
$env:WECOM_APP_SECRET=""
$env:WECOM_AUTH_CALLBACK="http://localhost:5173/oauth/callback"
$env:WECOM_GROUP_WEBHOOK=""
$env:WECOM_MESSAGE_ENABLED="false"
```

### 步骤 4：启动后端

```powershell
cd E:\projects\ai-marketplace\backend
mvn clean spring-boot:run
```

启动成功标志：
- 控制台显示 `Started AiMarketplaceApplication in xxx seconds`
- 接口文档可访问：`http://localhost:8080/api/doc.html`
- 健康检查：`http://localhost:8080/api/actuator/health`

### 步骤 5：启动前端工作台 H5

```powershell
cd E:\projects\ai-marketplace\frontend-workbench
npm install
npm run dev
```

访问 `http://localhost:5173`，未登录会自动跳转到 `/login`。

### 步骤 6：启动运营后台

```powershell
cd E:\projects\ai-marketplace\frontend-admin
npm install
npm run dev
```

访问 `http://localhost:5174`。

---

## 九、配置项说明

### 9.1 后端 application.yml 核心配置

```yaml
app:
  wecom:
    corp-id: ${WECOM_CORP_ID:}              # 企微企业 ID
    agent-id: ${WECOM_AGENT_ID:}            # 应用 AgentID
    app-secret: ${WECOM_APP_SECRET:}        # 应用 Secret
    auth-callback: ${WECOM_AUTH_CALLBACK:}  # OAuth 回调地址
    group-webhook: ${WECOM_GROUP_WEBHOOK:}  # 群机器人 Webhook（v1.1+）
  storage:
    base-path: ${STORAGE_BASE_PATH:./storage}  # 文件存储根目录
  jwt:
    secret: ${JWT_SECRET:change-me}            # JWT 签名密钥
    ttl-minutes: 7200                          # JWT 有效期（分钟）

wecom:
  message:
    enabled: ${WECOM_MESSAGE_ENABLED:false}    # 推送开关，v1 默认关闭
```

### 9.2 前端环境变量

| 变量 | 工作台默认 | 运营后台默认 | 说明 |
|---|---|---|---|
| `VITE_API_BASE` | `http://localhost:8080` | `http://localhost:8080` | 后端 API 地址（dev） |
| `VITE_API_BASE` (prod) | （空，相对路径） | （空，相对路径） | 生产同源部署 |

### 9.3 企微推送开关

`WECOM_MESSAGE_ENABLED` 三态行为：
- `false`（默认）：仅记日志，不真实推送，不抛异常
- `true` 且 corp-id/app-secret 已注入：调用 WecomClient 真实推送文本卡片消息
- `true` 但配置缺失：跳过推送 + 警告日志

---

## 十、企微接入指引

### 10.1 创建自建应用
1. 登录企微管理后台 → 应用管理 → 自建 → 创建应用
2. 记录 `AgentID` 和 `Secret`
3. 可信域名填 `http://localhost:5173`（生产改为正式域名）
4. 工作台应用入口配置 H5 链接：`http://localhost:5173/login`

### 10.2 OAuth2 SSO 流程
1. 用户访问 `/login` → 点击「企微 SSO 登录」
2. 前端跳转 `${VITE_API_BASE}/api/auth/wecom/redirect`
3. 后端 302 到企微授权页：`https://open.weixin.qq.com/connect/oauth2/authorize?appid={corpId}&redirect_uri={callback}&response_type=code&scope=snsapi_base#wechat_redirect`
4. 用户授权后企微回调 `auth-callback?code=xxx`
5. 前端取 code 调 `POST /api/auth/wecom/login?code=xxx` 换 JWT
6. JWT 存 localStorage，后续请求 header 带 `Authorization: Bearer <token>`

### 10.3 个人应用消息（v1）
- 触发场景：审核结果、退回补修、评分提醒
- 消息形态：文本卡片（含作品详情页跳转 URL）
- 失败策略：try-catch 包住，不阻塞主流程

### 10.4 群机器人（v1.1+，未启用）
- 上架通知、频道热门作品、大赛节点提醒
- 通过 `group-webhook` 推送

---

## 十一、本地开发指南

### 11.1 后端开发
- 启动类：`AiMarketplaceApplication.java`
- 包路径：`com.company.ai.marketplace`
- 代码风格：Lombok + MyBatis-Plus + Service-Controller 分层
- 接口测试：Knife4j 文档 `http://localhost:8080/api/doc.html`
- 数据库变更：修改 `schema.sql` 后重新执行

### 11.2 前端开发
- 工作台：`frontend-workbench`，端口 5173
- 运营后台：`frontend-admin`，端口 5174
- `/api` 经 Vite 代理转发到后端 8080，无 CORS 问题
- 类型声明：首次 `npm run dev` 会自动生成 `auto-imports.d.ts` 和 `components.d.ts`

### 11.3 新增模块示例
后端新增 Service：
```
entity/XxxEntity.java         ← MyBatis-Plus 实体
mapper/XxxMapper.java         ← 继承 BaseMapper
service/XxxService.java       ← 业务逻辑
controller/XxxController.java ← REST 接口
```

前端新增页面：
```
views/Xxx.vue                 ← 在 router/index.ts 注册路由
api/xxx.ts                    ← 在 src/api/ 下新增 API 模块
```

---

## 十二、生产部署提示

### 12.1 后端打包
```powershell
cd E:\projects\ai-marketplace\backend
mvn clean package -DskipTests
# 产物：target/ai-marketplace-backend-1.0.0.jar
java -jar target/ai-marketplace-backend-1.0.0.jar --spring.profiles.active=prod
```

### 12.2 前端打包
```powershell
# 工作台
cd E:\projects\ai-marketplace\frontend-workbench
npm run build   # 产物：dist/，部署到 Nginx 根目录

# 运营后台
cd E:\projects\ai-marketplace\frontend-admin
npm run build   # 产物：dist/，部署到 Nginx /admin 子路径
```

### 12.3 Nginx 反代示例
```nginx
server {
    listen 80;
    server_name marketplace.company.com;

    # 工作台
    location / {
        root /var/www/workbench;
        try_files $uri $uri/ /index.html;
    }

    # 运营后台
    location /admin {
        alias /var/www/admin;
        try_files $uri $uri/ /admin/index.html;
    }

    # 后端 API
    location /api/ {
        proxy_pass http://127.0.0.1:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

### 12.4 文件存储
- v1：本地磁盘（`STORAGE_BASE_PATH`），生产建议替换为 NAS / 对象存储
- 替换方式：实现 `StorageService` 子类，注入覆盖 `LocalStorageService`（`@ConditionalOnMissingBean` 自动让位）

### 12.5 access_token 缓存
- Redis key：`wecom:access_token`
- TTL：7000 秒（提前 200 秒刷新）
- 过期错误码（42001 / 40014）自动清缓存让下次刷新

---

## 十三、v1 已完成范围

### 后端
- ✅ Spring Boot 3 工程骨架（pom + 启动类 + 配置）
- ✅ 16 个实体 + 16 个 Mapper
- ✅ 8 个 Controller + 7 个核心 Service + review 子包
- ✅ 鉴权：JWT + 企微 SSO OAuth2
- ✅ 业务闭环：作品提交 / 下载 / 先审后发 / 评分（防刷+权重）/ 评论 / 排行榜 / 大赛配置 / 积分 / 审计
- ✅ 集成层：WecomClient（access_token + SSO + 通讯录 + 个人应用消息）+ StorageService（本地实现）
- ✅ 双模架构：大赛期 ↔ 常驻期切换
- ✅ AI 评审模块可插拔隔离设计（默认关闭）

### 前端
- ✅ 工作台 H5（26 个文件）：6 个核心页面 + 7 个 API 模块 + Pinia 鉴权
- ✅ 运营后台（31 个文件）：8 个核心页面 + 9 个 API 模块 + 角色守卫

### 文档
- ✅ 3 个 ADR（技术栈 / 双模架构 / AI 评审隔离）
- ✅ CONTEXT 术语表
- ✅ 本 README

---

## 十四、后续路线（v1.1+）

| 优先级 | 模块 | 说明 |
|---|---|---|
| P0 | 企微真实接入测试 | 注入 corp-id / app-secret 后验证 SSO + 推送闭环 |
| P0 | 对象存储 | 替换 LocalStorageService 为 NAS / OSS / COS / Minio |
| P1 | 群机器人推送 | 上架通知、热门作品、大赛节点提醒 |
| P1 | AI 初筛模块 | 文本敏感词、代码安全、镜像扫描、敏感数据 NER |
| P1 | 评委评分面板 | 独立于普通员工评分入口，看全量作品 |
| P2 | 部门负责人看板 | 本部门作品统计 + 评委推荐入口 |
| P2 | 积分兑换 | 徽章、奖品兑换商城 |
| P2 | 大赛颁奖页 | 获奖作品展示 + 颁奖直播嵌入 |

---

## 十五、常见问题

### Q1：启动后端报 `access denied for user`
A：检查 `.env` 或环境变量中的 `DB_USER` / `DB_PASSWORD`，并确认 MySQL 已执行授权 SQL。

### Q2：前端登录后 401
A：检查 localStorage 是否有 `jwt`，后端 `JWT_SECRET` 是否一致，token 是否过期。

### Q3：企微 SSO 回调失败
A：检查 `WECOM_AUTH_CALLBACK` 是否与企微后台「可信域名」一致，回调地址需带上 `/oauth/callback` 路径。

### Q4：文件上传失败
A：检查 `STORAGE_BASE_PATH` 目录是否存在且有写权限。

### Q5：企微推送不工作
A：默认 `WECOM_MESSAGE_ENABLED=false`，仅记日志。开启需设为 `true` 并注入 `WECOM_CORP_ID` + `WECOM_APP_SECRET`。

---

## 十六、参考文档

- [CONTEXT.md](CONTEXT.md) — 项目术语表
- [docs/adr/0001-tech-stack-and-architecture.md](docs/adr/0001-tech-stack-and-architecture.md) — 技术栈决策
- [docs/adr/0002-dual-mode-architecture-competition-vs-permanent.md](docs/adr/0002-dual-mode-architecture-competition-vs-permanent.md) — 双模架构决策
- [docs/adr/0003-ai-review-pluggable-isolation.md](docs/adr/0003-ai-review-pluggable-isolation.md) — AI 评审隔离决策
- [企微 OAuth2 文档](https://developer.work.weixin.qq.com/document/path/91022)
- [企微个人应用消息推送](https://developer.work.weixin.qq.com/document/path/90236)

---

**版本**：v1.0.0
**最后更新**：2026-08-27
