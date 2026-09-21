<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, Star, ChatLineRound, Warning, Back } from '@element-plus/icons-vue'
import {
  detail,
  download,
  type SubmissionDetail
} from '@/api/submission'
import { rate } from '@/api/rating'
import {
  listBySubmission,
  create as createComment,
  report as reportComment,
  deleteOwn as deleteComment,
  type Comment
} from '@/api/comment'

const route = useRoute()
const submissionId = route.params.id as string

const data = ref<SubmissionDetail | null>(null)
const loading = ref(false)

const typeLabel: Record<string, string> = {
  CODE: '代码作品',
  EXECUTABLE: '可执行程序',
  SAAS: 'SaaS 服务',
  DOC: '文档方案'
}

// 打开 SaaS 服务地址
function openSaas(url?: string) {
  if (url) window.open(url, '_blank')
}

// 下载
async function handleDownload() {
  if (!data.value) return
  try {
    const blob = await download(submissionId)
    const url = window.URL.createObjectURL(blob as unknown as Blob)
    const a = document.createElement('a')
    a.href = url
    a.download = data.value.fileName || `${data.value.title}.${data.value.type.toLowerCase()}`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
    ElMessage.success('下载已开始')
    data.value.downloadCount = (data.value.downloadCount || 0) + 1
  } catch {
    // request 拦截器已提示
  }
}

// ============ 评分对话框 ============
const ratingVisible = ref(false)
const ratingLoading = ref(false)
const ratingForm = reactive({
  easeOfUse: 0,
  businessValue: 0,
  techQuality: 0,
  innovation: 0,
  commentText: ''
})
const ratingRules = {
  easeOfUse: [{ required: true, message: '请评分', trigger: 'change' }],
  businessValue: [{ required: true, message: '请评分', trigger: 'change' }],
  techQuality: [{ required: true, message: '请评分', trigger: 'change' }],
  innovation: [{ required: true, message: '请评分', trigger: 'change' }]
}
const ratingFormRef = ref()

function openRating() {
  ratingForm.easeOfUse = 0
  ratingForm.businessValue = 0
  ratingForm.techQuality = 0
  ratingForm.innovation = 0
  ratingForm.commentText = ''
  ratingVisible.value = true
}

async function submitRating() {
  await ratingFormRef.value?.validate()
  ratingLoading.value = true
  try {
    await rate({
      submissionId,
      easeOfUse: ratingForm.easeOfUse,
      businessValue: ratingForm.businessValue,
      techQuality: ratingForm.techQuality,
      innovation: ratingForm.innovation,
      commentText: ratingForm.commentText
    })
    ElMessage.success('评分成功，感谢您的评价')
    ratingVisible.value = false
    loadDetail()
    loadComments()
  } catch {
    // request 拦截器已提示
  } finally {
    ratingLoading.value = false
  }
}

// ============ 评论 ============
const comments = ref<Comment[]>([])
const commentLoading = ref(false)
const newComment = ref('')
const replyState = reactive<{ visible: boolean; parentId: string | number | null; content: string }>({
  visible: false,
  parentId: null,
  content: ''
})

async function loadComments() {
  commentLoading.value = true
  try {
    const res = await listBySubmission(submissionId)
    comments.value = res || []
  } catch {
    // request 拦截器已提示
  } finally {
    commentLoading.value = false
  }
}

async function submitComment() {
  if (!newComment.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }
  try {
    await createComment({
      submissionId,
      parentId: null,
      content: newComment.value.trim()
    })
    ElMessage.success('评论发表成功')
    newComment.value = ''
    loadComments()
  } catch {
    // request 拦截器已提示
  }
}

function openReply(parentId: string | number) {
  replyState.parentId = parentId
  replyState.content = ''
  replyState.visible = true
}

async function submitReply() {
  if (!replyState.content.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }
  try {
    await createComment({
      submissionId,
      parentId: replyState.parentId,
      content: replyState.content.trim()
    })
    ElMessage.success('回复成功')
    replyState.visible = false
    loadComments()
  } catch {
    // request 拦截器已提示
  }
}

async function handleReport(c: Comment) {
  const { value } = await ElMessageBox.prompt('请输入举报理由', '举报评论', {
    confirmButtonText: '提交举报',
    cancelButtonText: '取消',
    inputType: 'textarea',
    inputPlaceholder: '例如：包含广告、人身攻击、违法违规内容等'
  })
  if (!value) {
    ElMessage.warning('请填写举报理由')
    return
  }
  try {
    await reportComment({ commentId: c.id, reason: value })
    ElMessage.success('举报已提交，管理员将尽快处理')
  } catch {
    // request 拦截器已提示
  }
}

async function handleDelete(c: Comment) {
  await ElMessageBox.confirm('确定删除这条评论吗？', '删除评论', {
    type: 'warning'
  })
  try {
    await deleteComment(c.id)
    ElMessage.success('已删除')
    loadComments()
  } catch {
    // request 拦截器已提示
  }
}

// ============ 加载详情 ============
async function loadDetail() {
  loading.value = true
  try {
    data.value = (await detail(submissionId)) as SubmissionDetail
  } catch {
    // request 拦截器已提示
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadDetail()
  loadComments()
})
</script>

<template>
  <div v-loading="loading" class="page-container detail">
    <div class="back" @click="$router.back()">
      <el-icon><Back /></el-icon> 返回
    </div>

    <el-card v-if="data" class="detail-card" shadow="never">
      <div class="detail-head">
        <div class="cover">
          <img v-if="data.coverUrl" :src="data.coverUrl" :alt="data.title" />
          <div v-else class="cover__placeholder">{{ data.title.charAt(0) }}</div>
        </div>
        <div class="head-info">
          <div class="head-info__tags">
            <el-tag effect="dark">{{ typeLabel[data.type] || data.type }}</el-tag>
            <el-tag v-if="data.track" type="success" effect="plain">{{ data.track }}</el-tag>
            <el-tag v-if="data.businessDomain" type="warning" effect="plain">{{ data.businessDomain }}</el-tag>
          </div>
          <h1 class="head-info__title">{{ data.title }}</h1>
          <p class="head-info__author">
            作者：{{ data.author?.displayName || '—' }}
            <span v-if="data.author?.department"> · {{ data.author.department }}</span>
          </p>
          <p class="head-info__desc">{{ data.shortDesc }}</p>
          <div class="head-info__meta">
            <div class="meta-block">
              <span class="meta-num">{{ data.version || '—' }}</span>
              <span class="meta-label">版本</span>
            </div>
            <div class="meta-block">
              <span class="meta-num">{{ data.downloadCount || 0 }}</span>
              <span class="meta-label">下载量</span>
            </div>
            <div class="meta-block">
              <span class="meta-num">
                {{ data.avgScore ? Number(data.avgScore).toFixed(1) : '—' }}
              </span>
              <span class="meta-label">评分 / {{ data.raterCount || 0 }}人</span>
            </div>
          </div>
          <div class="head-info__actions">
            <el-button
              v-if="data.type === 'SAAS' && data.saasUrl"
              type="primary"
              :icon="Star"
              @click="openSaas(data.saasUrl)"
            >
              访问 SaaS 服务
            </el-button>
            <el-button type="success" :icon="Download" @click="handleDownload">
              下载作品
            </el-button>
            <el-button :icon="Star" plain @click="openRating">我要评分</el-button>
          </div>
          <p v-if="data.type === 'SAAS' && data.saasCredentials" class="saas-cred">
            SaaS 访问凭据：{{ data.saasCredentials }}
          </p>
        </div>
      </div>

      <el-divider />

      <div class="detail-body">
        <section class="block">
          <h3 class="block__title">作品介绍</h3>
          <p class="block__text">{{ data.detailDesc || data.shortDesc || '暂无介绍' }}</p>
        </section>
        <section class="block">
          <h3 class="block__title">使用场景</h3>
          <p class="block__text">{{ data.usageScenario || '—' }}</p>
        </section>
        <section class="block">
          <h3 class="block__title">技术栈</h3>
          <div class="block__tags">
            <el-tag
              v-for="t in data.techStack || []"
              :key="t"
              effect="plain"
            >
              {{ t }}
            </el-tag>
            <span v-if="!data.techStack?.length">—</span>
          </div>
        </section>
        <section v-if="data.tags?.length" class="block">
          <h3 class="block__title">标签</h3>
          <div class="block__tags">
            <el-tag v-for="t in data.tags" :key="t" type="info" effect="plain">{{ t }}</el-tag>
          </div>
        </section>
      </div>
    </el-card>

    <!-- 评论区 -->
    <el-card class="comment-card" shadow="never">
      <template #header>
        <div class="comment-head">
          <el-icon><ChatLineRound /></el-icon>
          <span>评论 ({{ comments.length }})</span>
        </div>
      </template>

      <div class="comment-input">
        <el-input
          v-model="newComment"
          type="textarea"
          :rows="3"
          placeholder="说点什么吧……"
          maxlength="500"
          show-word-limit
        />
        <div class="comment-input__action">
          <el-button type="primary" @click="submitComment">发表评论</el-button>
        </div>
      </div>

      <div v-loading="commentLoading" class="comment-list">
        <div v-for="c in comments" :key="c.id" class="comment-item">
          <el-avatar :size="36" class="comment-item__avatar">
            {{ c.authorName?.charAt(0) || 'U' }}
          </el-avatar>
          <div class="comment-item__body">
            <div class="comment-item__head">
              <span class="comment-item__name">{{ c.authorName || '匿名用户' }}</span>
              <span class="comment-item__time">{{ c.createdAt || '' }}</span>
            </div>
            <div class="comment-item__content">{{ c.content }}</div>
            <div class="comment-item__actions">
              <el-button text size="small" @click="openReply(c.id)">回复</el-button>
              <el-button text size="small" @click="handleReport(c)">
                <el-icon><Warning /></el-icon> 举报
              </el-button>
              <el-button v-if="c.isOwn" text size="small" type="danger" @click="handleDelete(c)">
                删除
              </el-button>
            </div>
          </div>
        </div>
        <el-empty v-if="!commentLoading && !comments.length" description="还没有评论，来抢沙发" />
      </div>
    </el-card>

    <!-- 评分对话框 -->
    <el-dialog
      v-model="ratingVisible"
      title="为作品评分"
      width="460px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="ratingFormRef"
        :model="ratingForm"
        :rules="ratingRules"
        label-width="110px"
        label-position="top"
      >
        <el-form-item label="易用性（1-5）" prop="easeOfUse">
          <el-rate v-model="ratingForm.easeOfUse" :max="5" />
        </el-form-item>
        <el-form-item label="业务价值（1-5）" prop="businessValue">
          <el-rate v-model="ratingForm.businessValue" :max="5" />
        </el-form-item>
        <el-form-item label="技术质量（1-5）" prop="techQuality">
          <el-rate v-model="ratingForm.techQuality" :max="5" />
        </el-form-item>
        <el-form-item label="创新性（1-5）" prop="innovation">
          <el-rate v-model="ratingForm.innovation" :max="5" />
        </el-form-item>
        <el-form-item label="评论">
          <el-input
            v-model="ratingForm.commentText"
            type="textarea"
            :rows="3"
            placeholder="写下你的评价（可选）"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ratingVisible = false">取消</el-button>
        <el-button type="primary" :loading="ratingLoading" @click="submitRating">
          提交评分
        </el-button>
      </template>
    </el-dialog>

    <!-- 回复对话框 -->
    <el-dialog v-model="replyState.visible" title="回复评论" width="440px">
      <el-input
        v-model="replyState.content"
        type="textarea"
        :rows="4"
        placeholder="请输入回复内容"
        maxlength="500"
        show-word-limit
      />
      <template #footer>
        <el-button @click="replyState.visible = false">取消</el-button>
        <el-button type="primary" @click="submitReply">回复</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.detail {
  padding-top: 20px;
}
.back {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--brand-muted);
  cursor: pointer;
  margin-bottom: 14px;
  font-size: 14px;
}
.back:hover {
  color: var(--brand-primary);
}
.detail-card {
  margin-bottom: 20px;
}
.detail-head {
  display: flex;
  gap: 28px;
}
.cover {
  width: 280px;
  height: 200px;
  border-radius: 12px;
  overflow: hidden;
  flex-shrink: 0;
  background: linear-gradient(135deg, #eaf0fe, #f0fdfa);
}
.cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.cover__placeholder {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 72px;
  font-weight: 700;
  color: #3b5bdb;
  opacity: 0.5;
}
.head-info {
  flex: 1;
  min-width: 0;
}
.head-info__tags {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}
.head-info__title {
  margin: 0 0 8px;
  font-size: 26px;
  color: var(--brand-text);
}
.head-info__author {
  margin: 0 0 8px;
  font-size: 14px;
  color: var(--brand-muted);
}
.head-info__desc {
  margin: 0 0 18px;
  font-size: 14px;
  color: var(--brand-text);
  line-height: 1.7;
}
.head-info__meta {
  display: flex;
  gap: 36px;
  margin-bottom: 20px;
}
.meta-block {
  display: flex;
  flex-direction: column;
}
.meta-num {
  font-size: 24px;
  font-weight: 700;
  color: var(--brand-primary);
}
.meta-label {
  font-size: 12px;
  color: var(--brand-muted);
  margin-top: 2px;
}
.head-info__actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
.saas-cred {
  margin-top: 12px;
  font-size: 13px;
  color: var(--brand-muted);
  background: var(--el-color-primary-light-9);
  padding: 8px 12px;
  border-radius: 6px;
}

.detail-body {
  display: flex;
  flex-direction: column;
  gap: 22px;
}
.block__title {
  margin: 0 0 8px;
  font-size: 16px;
  color: var(--brand-text);
  position: relative;
  padding-left: 12px;
}
.block__title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 16px;
  border-radius: 4px;
  background: var(--brand-primary);
}
.block__text {
  margin: 0;
  color: var(--brand-text);
  line-height: 1.8;
  white-space: pre-wrap;
}
.block__tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.comment-card {
  margin-bottom: 20px;
}
.comment-head {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}
.comment-input {
  margin-bottom: 20px;
}
.comment-input__action {
  margin-top: 10px;
  text-align: right;
}
.comment-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.comment-item {
  display: flex;
  gap: 12px;
}
.comment-item__avatar {
  background: linear-gradient(135deg, #3b5bdb, #14b8a6);
  color: #fff;
  flex-shrink: 0;
}
.comment-item__body {
  flex: 1;
  min-width: 0;
}
.comment-item__head {
  display: flex;
  align-items: center;
  gap: 10px;
}
.comment-item__name {
  font-weight: 600;
  font-size: 14px;
}
.comment-item__time {
  font-size: 12px;
  color: var(--brand-muted);
}
.comment-item__content {
  margin: 6px 0;
  color: var(--brand-text);
  line-height: 1.6;
}
.comment-item__actions {
  display: flex;
  gap: 4px;
}
</style>
