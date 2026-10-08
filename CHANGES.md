# AI Marketplace 改动清单

## 改动总览
本次修改解决了 4 个核心问题：
1. 后台创建用户后无法登录
2. 权限矩阵缺少普通用户角色
3. 合并前后台为统一界面（按角色显示不同页面）
4. 完整业务流程：提交 → 审核 → 评分 → 排行榜

---

## 后端改动 (backend/)

### 新增文件
- `src/main/java/com/company/ai/marketplace/integration/storage/StorageService.java`
  - 本地文件存储服务（SHA-256 哈希 + 日期目录结构）
- `src/main/java/com/company/ai/marketplace/integration/storage/StoredFile.java`
  - 存储文件实体类

### 修改文件
- `src/main/java/com/company/ai/marketplace/service/AuthService.java`
  - 修复 createUser() 中 wecomUserid 唯一约束冲突（设为 null）
  - 自动为新用户分配 USER 角色
  - 新增 requireAdmin() / requireAnyRole() 权限校验方法
  - 修复 BCrypt 密码校验逻辑

- `src/main/java/com/company/ai/marketplace/security/JwtService.java`
  - 修复角色解析未 trim() 的 bug
  - 空角色时默认添加 USER 角色

- `src/main/java/com/company/ai/marketplace/service/RatingService.java`
  - 新增 DEPT_HEAD（部门主管）加权评分
  - 评委/部门主管权重 30%，普通用户权重 70%
  - 打分前验证是否已下载作品

- `src/main/java/com/company/ai/marketplace/service/ReviewService.java`
  - 允许 JUDGE（评委）审核作品
  - 权限校验：ADMIN 或 JUDGE 可审核

- `src/main/java/com/company/ai/marketplace/service/SubmissionService.java`
  - 新增权限校验
  - 管理端列表筛选

- `src/main/java/com/company/ai/marketplace/service/DashboardService.java`
  - 新增管理员权限校验

- `src/main/java/com/company/ai/marketplace/controller/AdminUserController.java`
  - 新增管理员权限校验

- `src/main/java/com/company/ai/marketplace/controller/SubmissionController.java`
  - 新增管理端下架/重新上架接口
  - 完善权限控制

---

## 前端改动 (frontend-workbench/)

### 核心改动
- `src/router/index.ts` — 基于角色的路由守卫和动态路由
- `src/layouts/MainLayout.vue` — 按角色动态渲染侧边栏导航
- `src/stores/auth.ts` — 修复 localStorage 读取类型错误
- `src/api/request.ts` — 修复 token 读取类型错误

### 管理页面 (src/views/admin/)
- `Users.vue` — 用户管理（创建/授权角色/启停用）
- `SubmissionManagement.vue` — 作品管理
- `PendingReviews.vue` — 待审核作品
- `Judges.vue` — 评委评分管理
- `Competitions.vue` — 赛事管理
- `Comments.vue` — 评论管理
- `Dashboard.vue` — 数据看板

### 用户页面 (src/views/)
- `Home.vue` — 首页/作品广场
- `Submit.vue` — 提交作品
- `Ranking.vue` — 排行榜
- `SubmissionDetail.vue` — 作品详情/下载/评分
- `Me.vue` — 我的作品
- `Login.vue` — 登录页
- `ChangePassword.vue` — 修改密码

### API 层 (src/api/)
- `auth.ts` — 认证接口
- `user.ts` — 用户管理接口
- `submission.ts` — 作品提交/列表接口
- `admin-submission.ts` — 管理端作品接口
- `review.ts` — 审核接口
- `rating.ts` — 评分接口
- `ranking.ts` — 排行榜接口
- `judge.ts` — 评委接口
- `dashboard.ts` — 看板接口
- `comment.ts` — 评论接口
- `admin-comment.ts` — 管理端评论接口
- `competition.ts` — 赛事接口
- `admin-competition.ts` — 管理端赛事接口

---

## 角色权限矩阵

| 功能 | USER | JUDGE | DEPT_HEAD | OPERATOR | ADMIN |
|------|------|-------|-----------|----------|-------|
| 浏览作品 | ✓ | ✓ | ✓ | ✓ | ✓ |
| 提交作品 | ✓ | ✓ | ✓ | ✓ | ✓ |
| 下载作品 | ✓ | ✓ | ✓ | ✓ | ✓ |
| 用户评分 | ✓ | ✓ | ✓ | ✓ | ✓ |
| 审核作品 | | ✓ | | | ✓ |
| 评委打分 | | ✓ | ✓ | | ✓ |
| 用户管理 | | | | | ✓ |
| 作品管理 | | | | | ✓ |
| 评论管理 | | | | | ✓ |
| 数据看板 | | | | | ✓ |

---

## 业务流程

```
用户/管理员 上传作品
        ↓
   PENDING（待审核）
        ↓
管理员/评委 审核通过
        ↓
 PUBLISHED（已发布）
        ↓
评委/部门主管 专业评分 (权重30%)
  + 普通用户 大众评分 (权重70%)
        ↓
  加权总分 → 排行榜排名
```

---

## 使用说明

1. 网络恢复后，双击 `push-to-github.bat` 即可一键提交并推送到 GitHub
2. 后端启动：`cd backend && mvn spring-boot:run`
3. 前端启动：`cd frontend-workbench && npm run dev`
4. 默认管理员账号密码在 `application.yml` 中配置
