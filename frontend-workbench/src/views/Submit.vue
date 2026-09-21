<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { submit, type SubmissionType } from '@/api/submission'

const router = useRouter()

// 作品类型选项
const typeOptions: { label: string; value: SubmissionType }[] = [
  { label: '代码作品', value: 'CODE' },
  { label: '可执行程序', value: 'EXECUTABLE' },
  { label: 'SaaS 服务', value: 'SAAS' },
  { label: '文档方案', value: 'DOC' }
]

const domainOptions = [
  '通用办公',
  '研发效能',
  '市场营销',
  '客户服务',
  '数据分析',
  '人力资源',
  '财务管理'
]

// 表单数据
const form = reactive({
  title: '',
  shortDesc: '',
  detailDesc: '',
  type: 'CODE' as SubmissionType,
  businessDomain: '',
  usageScenario: '',
  techStack: '',
  tags: '',
  version: '1.0.0',
  coverUrl: '',
  saasUrl: '',
  saasCredentials: '',
  file: null as File | null
})

const formRef = ref<FormInstance>()

const rules: FormRules = {
  title: [
    { required: true, message: '请输入作品标题', trigger: 'blur' },
    { min: 2, max: 80, message: '长度在 2 到 80 个字符', trigger: 'blur' }
  ],
  shortDesc: [
    { required: true, message: '请输入一句话简介', trigger: 'blur' },
    { max: 200, message: '不超过 200 个字符', trigger: 'blur' }
  ],
  type: [{ required: true, message: '请选择作品类型', trigger: 'change' }],
  businessDomain: [{ required: true, message: '请选择业务领域', trigger: 'change' }],
  version: [{ required: true, message: '请填写版本号', trigger: 'blur' }],
  saasUrl: [
    {
      validator: (_r: unknown, value: string, cb: (e?: Error) => void) => {
        if (form.type === 'SAAS' && !value) cb(new Error('SaaS 类型需填写访问地址'))
        else cb()
      },
      trigger: 'blur'
    }
  ],
  file: [
    {
      validator: (_r: unknown, _value: unknown, cb: (e?: Error) => void) => {
        if (form.type !== 'SAAS' && !form.file) cb(new Error('请上传作品文件'))
        else cb()
      },
      trigger: 'change'
    }
  ]
}

// 文件选择
function handleFileChange(file: { raw: File } | undefined) {
  if (file?.raw) form.file = file.raw
}
function handleFileRemove() {
  form.file = null
}

// 是否需要上传文件（非 SaaS 类型）
const needFile = (t: SubmissionType) => t !== 'SAAS'

// 提交
const submitting = ref(false)
async function handleSubmit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    const result = await submit({ ...form })
    ElMessage.success('作品上传成功，等待审核')
    router.replace(`/submissions/${result.id}`)
  } catch {
    // request 拦截器已提示
  } finally {
    submitting.value = false
  }
}

function handleReset() {
  formRef.value?.resetFields()
}
</script>

<template>
  <div class="page-container submit">
    <div class="submit__head">
      <h2>上传作品</h2>
      <p>把你的 AI 应用分享给企业内每一位同事</p>
    </div>

    <el-card class="submit-card" shadow="never">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
        label-position="right"
      >
        <el-form-item label="作品标题" prop="title">
          <el-input v-model="form.title" placeholder="给作品起个响亮的名字" maxlength="80" show-word-limit />
        </el-form-item>

        <el-form-item label="一句话简介" prop="shortDesc">
          <el-input
            v-model="form.shortDesc"
            placeholder="一句话说明作品解决了什么问题"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="详细描述" prop="detailDesc">
          <el-input
            v-model="form.detailDesc"
            type="textarea"
            :rows="5"
            placeholder="详细介绍作品的功能、亮点与价值"
          />
        </el-form-item>

        <el-form-item label="作品类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio-button v-for="o in typeOptions" :key="o.value" :value="o.value">
              {{ o.label }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="业务领域" prop="businessDomain">
          <el-select v-model="form.businessDomain" placeholder="请选择" style="width: 240px">
            <el-option v-for="d in domainOptions" :key="d" :label="d" :value="d" />
          </el-select>
        </el-form-item>

        <el-form-item label="使用场景" prop="usageScenario">
          <el-input
            v-model="form.usageScenario"
            type="textarea"
            :rows="3"
            placeholder="例如：自动生成周报、客户工单分类等"
          />
        </el-form-item>

        <el-form-item label="技术栈" prop="techStack">
          <el-input v-model="form.techStack" placeholder="多个用逗号分隔，如：Python,LangChain,Vue3" />
        </el-form-item>

        <el-form-item label="标签" prop="tags">
          <el-input v-model="form.tags" placeholder="多个用逗号分隔" />
        </el-form-item>

        <el-form-item label="版本号" prop="version">
          <el-input v-model="form.version" placeholder="如 1.0.0" style="width: 200px" />
        </el-form-item>

        <el-form-item label="封面 URL" prop="coverUrl">
          <el-input v-model="form.coverUrl" placeholder="作品封面图片地址（可选）" />
        </el-form-item>

        <!-- SaaS 类型：访问地址 + 凭据 -->
        <template v-if="form.type === 'SAAS'">
          <el-form-item label="SaaS 地址" prop="saasUrl">
            <el-input v-model="form.saasUrl" placeholder="https://your-saas.example.com" />
          </el-form-item>
          <el-form-item label="访问凭据" prop="saasCredentials">
            <el-input
              v-model="form.saasCredentials"
              type="textarea"
              :rows="2"
              placeholder="账号 / 密码 / Token 等访问凭据（可选）"
            />
          </el-form-item>
        </template>

        <!-- 非 SaaS 类型：上传文件 -->
        <template v-else>
          <el-form-item label="作品文件" prop="file">
            <el-upload
              drag
              :auto-upload="false"
              :limit="1"
              :on-change="handleFileChange"
              :on-remove="handleFileRemove"
            >
              <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
              <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
              <template #tip>
                <div class="el-upload__tip">支持代码包、可执行程序、文档等</div>
              </template>
            </el-upload>
          </el-form-item>
        </template>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">
            提交作品
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.submit__head {
  margin-bottom: 20px;
}
.submit__head h2 {
  margin: 0 0 6px;
  font-size: 22px;
  color: var(--brand-text);
}
.submit__head p {
  margin: 0;
  color: var(--brand-muted);
  font-size: 14px;
}
.submit-card {
  max-width: 760px;
}
</style>
