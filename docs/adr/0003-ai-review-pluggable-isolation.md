# ADR-0003: AI 评审模块的可插拔隔离设计

- **状态**：Accepted
- **日期**：2026-08-27
- **决策来源**：Round 4 / Round 5 访谈结论

## 上下文 (Context)

平台是私有化部署（数据不出公司），但用户在 Round 4 提出"AI 评审可通过开关控制，默认关闭，若开启走全云端 API"，Round 5 澄清为：

> "AI 评审功能隔离，v1 默认关闭，若开启采用全云端方案。当前仅考虑本地部署并且人工评审。"

这意味着：
1. v1 上线时：仅人工审核，无 AI 评审
2. 未来可能开启 AI 评审：走云端 API（已知合规风险，需法务报备）
3. 开启/关闭由开关控制
4. AI 评审模块必须与人工审核流程解耦，便于独立切换

设计选择会影响审核流程的接口与数据模型：
- 若 v1 把 AI 评审硬编码进流程，未来切换/关闭需重构
- 若 v1 完全不考虑 AI 评审，未来加入时需要侵入式修改审核流程
- 需要在 v1 阶段就以"可插拔"方式预留接入位置

## 决策 (Decision)

采用 **可插拔隔离设计**：审核流程抽象出 `ReviewStep` 接口，AI 评审作为可选 Step，通过配置开关启用/禁用。

### 审核流程抽象

```
SubmissionPipeline {
  steps: List<ReviewStep>
}

interface ReviewStep {
  execute(submission): ReviewResult
  is_enabled(): boolean  // 配置开关
}
```

### v1 默认 Steps

```
1. ManualReviewStep  (人工终审，必经)
```

### v1.1+ 可选 Steps（默认关闭）

```
2. AIReviewStep_TextScan        (敏感词扫描)
3. AIReviewStep_CodeScan        (代码安全扫描)
4. AIReviewStep_BinaryScan      (二进制/镜像扫描)
5. AIReviewStep_DataScan        (敏感数据 NER)
```

### 配置开关

```
review.ai.enabled = false  (默认)
review.ai.endpoint = ""    (开启时填云端 API URL)
review.ai.api_key = ""     (开启时填 API Key)
review.ai.steps = [...]    (开启时选择启用的子步骤)
```

### 数据模型

```
SubmissionReviewLog {
  submission_id
  step_name       (manual / ai_text / ai_code / ai_binary / ai_data)
  step_status     (skipped / passed / rejected / pending)
  result_detail   (jsonb，AI 给出的具体原因/坐标)
  reviewer_id     (人工时填，AI 时为空)
  reviewed_at
}
```

- v1 所有 submission 的 step_name 只有 `manual`
- 未来开启 AI 时，新 submission 会按配置插入 AI steps；已发布作品不补审

### 接入位置

- 后端：`ReviewStep` 接口 + Spring `@ConditionalOnProperty` 控制 Bean 注入
- 配置：运营在后台"AI 评审配置"页填 API endpoint/key，开关默认 off
- 前端：审核详情页预留"AI 扫描结果"区块（v1 不展示，配置开启后展示）

## 备选方案 (Alternatives Considered)

### A. v1 不预留 AI 评审位置，未来重构
- 优点：v1 实现最简
- 缺点：未来开启 AI 评审时需重构 SubmissionPipeline、ReviewLog、前端审核详情页；改动面大、回归测试成本高
- **否决**：未来变更成本高于 v1 多写一层抽象的成本

### B. v1 内置 AI 评审但默认关闭
- 优点：未来开启只需翻开关
- 缺点：v1 必须实现 AI 评审的调用代码，引入云端 API SDK 依赖；与"v1 仅人工评审"的承诺冲突；可能拖慢上线
- **否决**：违背 Round 5 "v1 默认关闭、仅人工评审"的明确决策

### C. AI 评审做成完全独立服务，平台通过 HTTP 调用
- 优点：彻底解耦，AI 服务可独立演进
- 缺点：v1 阶段尚不需要独立服务；过早微服务化增加部署与运维成本
- **否决**：v1 阶段过度设计；当 AI 评审启用且调用量大时再拆分

## 后果 (Consequences)

### 正面
- v1 上线快（只实现 ManualReviewStep）
- 未来开启 AI 评审无需侵入审核流程
- AI 评审子维度可独立开关（如只开启文本扫描，不开启代码扫描）
- 历史作品不受新 AI 评审开关影响（避免已发布作品被批量复审）
- 前端审核详情页"AI 扫描结果"区块预留位置，开启后直接展示

### 负面
- v1 多写一层 `ReviewStep` 接口与 SubmissionPipeline 抽象
- ReviewLog 表数据量在 AI 启用后会增长（每个 submission 多条记录），需考虑分表/归档策略
- 前端审核详情页有"空区块"（v1 AI 扫描结果区块不展示）

### 风险与缓解
- **风险**：AI 评审走云端与私有化部署的合规冲突
- **缓解**：默认关闭；启用前必须由法务/合规签字；运营在配置页看到显著警示
- **风险**：开启 AI 评审后历史作品如何处理
- **缓解**：决策——不补审；只有新提交作品走 AI 步骤；在作品详情页加注"未经过 AI 评审"提示（v1 已发布作品）

## 相关 ADR
- ADR-0001（技术栈）：Spring Boot 的 `@ConditionalOnProperty` 直接支持本设计
- ADR-0002（双模架构）：AI 评审开启/关闭与大赛/常驻模式独立，互不耦合
