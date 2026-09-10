<template>
  <div class="home">
    <!-- 左侧栏 -->
    <div class="sidebar">
      <div class="sidebar-header">
        <h3>知识库</h3>
        <el-button type="primary" size="small" @click="openCreate">新建</el-button>
      </div>

      <div class="kb-list">
        <div v-if="knowledgeList.length === 0" class="empty">暂无知识库</div>
        <div
          v-for="kb in knowledgeList"
          :key="kb.id"
          class="kb-item"
          :class="{ active: kb.id === currentKbId }"
          @click="selectKb(kb)"
        >
          <el-icon><Folder /></el-icon>
          <span class="kb-name">{{ kb.name }}</span>
          <el-button
            class="kb-del"
            link
            type="danger"
            size="small"
            @click.stop="doDeleteKb(kb)"
          >删除</el-button>
        </div>
      </div>

      <!-- 文档区（选中知识库后显示） -->
      <div v-if="currentKbId" class="doc-section">
        <div class="doc-header">
          <span>文档</span>
          <el-upload :show-file-list="false" :before-upload="handleUpload" accept=".pdf,.txt">
            <el-button size="small" type="success">上传</el-button>
          </el-upload>
        </div>
        <div class="doc-list">
          <div v-if="documents.length === 0" class="empty">暂无文档</div>
          <div v-for="doc in documents" :key="doc.id" class="doc-item">
            <el-icon><Document /></el-icon>
            <span class="doc-name" :title="doc.fileName">{{ doc.fileName }}</span>
            <span class="doc-status" :class="'s' + doc.status">{{ statusText(doc.status) }}</span>
            <el-button
              v-if="doc.status === 0"
              link
              type="primary"
              size="small"
              @click="doParse(doc)"
            >解析</el-button>
            <el-button
              class="doc-del"
              link
              type="danger"
              size="small"
              @click.stop="doDelete(doc)"
            >删除</el-button>
          </div>
        </div>
      </div>

      <div class="sidebar-footer">
        <el-button text @click="logout">退出登录</el-button>
      </div>
    </div>

    <!-- 右侧对话区 -->
    <div class="chat-area">
      <div class="chat-header">
        <h3>{{ currentKbName || '请选择左侧知识库开始对话' }}</h3>
        <el-button size="small" @click="newChat">新对话</el-button>
      </div>

      <div class="chat-messages" ref="msgContainer">
        <div v-if="messages.length === 0" class="chat-empty">
          <p>在下方输入问题，开始与知识库对话</p>
        </div>
        <div v-for="(msg, i) in messages" :key="i" class="message" :class="msg.role">
          <div class="avatar">{{ msg.role === 'user' ? '我' : 'AI' }}</div>
          <div class="bubble">
            <div class="text">{{ msg.content }}</div>
            <div v-if="msg.references && msg.references.length" class="refs">
              <div class="refs-title">参考来源：</div>
              <div v-for="(ref, j) in msg.references" :key="j" class="ref-item">
                <el-icon><Link /></el-icon>
                <span>{{ ref.documentName }} · 第{{ ref.pageNumber }}页 · 相似度{{ (ref.score * 100).toFixed(1) }}%</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="chat-input">
        <el-input
          v-model="question"
          placeholder="输入问题，回车发送（未选知识库将检索我的全部知识库）"
          @keyup.enter="send"
        />
        <el-button type="primary" :loading="sending" @click="send">发送</el-button>
      </div>
    </div>

    <!-- 新建知识库弹窗 -->
    <el-dialog v-model="showCreate" title="新建知识库" width="420px">
      <el-form :model="kbForm" label-width="60px">
        <el-form-item label="名称">
          <el-input v-model="kbForm.name" placeholder="知识库名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="kbForm.description" type="textarea" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreate = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="createKb">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listKnowledge, createKnowledge, listDocuments, uploadDocument, parseDocument, deleteDocument, deleteKnowledge, chat } from '../api'

const router = useRouter()

const knowledgeList = ref([])
const currentKbId = ref(null)
const currentKbName = ref('')
const documents = ref([])
const messages = ref([])
const question = ref('')
const sending = ref(false)
const sessionId = ref('')
const msgContainer = ref()

const showCreate = ref(false)
const creating = ref(false)
const kbForm = reactive({ name: '', description: '' })

const statusMap = { 0: '待解析', 1: '解析中', 2: '已完成', 3: '失败' }
const statusText = (s) => statusMap[s] || '未知'

// 生成会话 id
const genSessionId = () => {
  if (crypto.randomUUID) return crypto.randomUUID()
  return 's-' + Date.now() + '-' + Math.random().toString(36).slice(2)
}

const loadKnowledge = async () => {
  const res = await listKnowledge()
  knowledgeList.value = res.data.records || []
}

const selectKb = async (kb) => {
  currentKbId.value = kb.id
  currentKbName.value = kb.name
  await loadDocuments()
}

const loadDocuments = async () => {
  if (!currentKbId.value) return
  const res = await listDocuments(currentKbId.value)
  documents.value = res.data || []
}

const openCreate = () => {
  kbForm.name = ''
  kbForm.description = ''
  showCreate.value = true
}

const createKb = async () => {
  if (!kbForm.name) {
    ElMessage.warning('请输入知识库名称')
    return
  }
  creating.value = true
  try {
    await createKnowledge({ name: kbForm.name, description: kbForm.description })
    ElMessage.success('创建成功')
    showCreate.value = false
    await loadKnowledge()
  } finally {
    creating.value = false
  }
}

// 上传文档
const handleUpload = async (file) => {
  await uploadDocument(currentKbId.value, file)
  ElMessage.success('上传成功，请点击"解析"开始向量化')
  await loadDocuments()
  return false // 阻止 el-upload 默认上传
}

// 触发解析
const doParse = async (doc) => {
  await parseDocument(doc.id)
  ElMessage.success('解析完成')
  await loadDocuments()
}

// 删除文档（二次确认 → 级联清向量/chunk）
const doDelete = async (doc) => {
  try {
    await ElMessageBox.confirm(
      `确定删除文档「${doc.fileName}」吗？其分块与向量会一并清除，不可恢复。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch (e) {
    return // 用户取消
  }
  await deleteDocument(doc.id)
  ElMessage.success('删除成功')
  await loadDocuments()
}

// 删除知识库（连带库内所有文档、chunk、向量）
const doDeleteKb = async (kb) => {
  try {
    await ElMessageBox.confirm(
      `确定删除知识库「${kb.name}」吗？库内所有文档及其分块、向量都会被清除，不可恢复。`,
      '删除知识库',
      { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
    )
  } catch (e) {
    return // 用户取消
  }
  await deleteKnowledge(kb.id)
  ElMessage.success('知识库已删除')
  // 若删的是当前选中的库，清空右侧对话与文档状态
  if (currentKbId.value === kb.id) {
    currentKbId.value = null
    currentKbName.value = ''
    documents.value = []
    messages.value = []
  }
  await loadKnowledge()
}

const scrollToBottom = async () => {
  await nextTick()
  if (msgContainer.value) {
    msgContainer.value.scrollTop = msgContainer.value.scrollHeight
  }
}

const send = async () => {
  const q = question.value.trim()
  if (!q) return
  question.value = ''
  messages.value.push({ role: 'user', content: q })
  sending.value = true
  try {
    const res = await chat({ question: q, knowledgeId: currentKbId.value, sessionId: sessionId.value })
    messages.value.push({ role: 'ai', content: res.data.answer, references: res.data.references })
  } catch (e) {
    // 错误已由拦截器提示
  } finally {
    sending.value = false
    scrollToBottom()
  }
}

const newChat = () => {
  sessionId.value = genSessionId()
  messages.value = []
}

const logout = () => {
  localStorage.removeItem('token')
  router.push('/login')
}

onMounted(() => {
  sessionId.value = genSessionId()
  loadKnowledge()
})
</script>

<style scoped>
.home {
  display: flex;
  height: 100vh;
  background: #f5f6f8;
}

/* 左侧栏 */
.sidebar {
  width: 280px;
  background: #fff;
  border-right: 1px solid #e5e6eb;
  display: flex;
  flex-direction: column;
}
.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #f0f1f3;
}
.sidebar-header h3 { margin: 0; font-size: 16px; }
.kb-list { flex: 1; overflow-y: auto; padding: 8px; }
.kb-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  margin-bottom: 4px;
}
.kb-item:hover { background: #f5f6f8; }
.kb-item.active { background: #e8f3ff; color: #409eff; }
.kb-name { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
/* 知识库删除按钮：hover 列表项才显现 */
.kb-del { opacity: 0; pointer-events: none; transition: opacity .15s; }
.kb-item:hover .kb-del { opacity: 1; pointer-events: auto; }
.empty { color: #999; text-align: center; padding: 20px; font-size: 13px; }

.doc-section { border-top: 1px solid #f0f1f3; padding: 12px 8px; max-height: 300px; overflow-y: auto; }
.doc-header { display: flex; justify-content: space-between; align-items: center; padding: 0 8px 8px; font-size: 14px; font-weight: 500; }
.doc-item { display: flex; align-items: center; gap: 6px; padding: 8px; border-radius: 6px; }
.doc-item:hover { background: #f5f6f8; }
.doc-name { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 13px; }
.doc-status { font-size: 12px; color: #999; white-space: nowrap; }
.doc-status.s2 { color: #67c23a; }
.doc-status.s1 { color: #e6a23c; }
.doc-status.s3 { color: #f56c6c; }
/* 删除按钮：hover 文档项才显现 */
.doc-del { opacity: 0; pointer-events: none; transition: opacity .15s; }
.doc-item:hover .doc-del { opacity: 1; pointer-events: auto; }

.sidebar-footer { padding: 12px; border-top: 1px solid #f0f1f3; text-align: center; }

/* 右侧对话区 */
.chat-area { flex: 1; display: flex; flex-direction: column; }
.chat-header {
  height: 56px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  background: #fff;
  border-bottom: 1px solid #e5e6eb;
}
.chat-header h3 { margin: 0; font-size: 16px; }
.chat-messages { flex: 1; overflow-y: auto; padding: 20px; }
.chat-empty { text-align: center; color: #999; margin-top: 80px; }

.message { display: flex; gap: 12px; margin-bottom: 20px; }
.message.user { flex-direction: row-reverse; }
.avatar {
  width: 36px; height: 36px; border-radius: 50%;
  background: #409eff; color: #fff;
  display: flex; align-items: center; justify-content: center;
  font-size: 14px; flex-shrink: 0;
}
.message.user .avatar { background: #67c23a; }
.bubble {
  max-width: 70%;
  background: #fff;
  padding: 12px 16px;
  border-radius: 8px;
  line-height: 1.6;
  font-size: 14px;
}
.message.user .bubble { background: #e8f3ff; }
.text { white-space: pre-wrap; word-break: break-word; }

.refs { margin-top: 10px; padding-top: 10px; border-top: 1px dashed #e5e6eb; }
.refs-title { font-size: 12px; color: #999; margin-bottom: 6px; }
.ref-item {
  display: flex; align-items: center; gap: 4px;
  font-size: 12px; color: #666;
  padding: 2px 0;
}

.chat-input {
  display: flex; gap: 10px;
  padding: 16px 20px;
  background: #fff;
  border-top: 1px solid #e5e6eb;
}
</style>
