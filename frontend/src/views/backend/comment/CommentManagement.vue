<template>
  <div class="comment-management-container">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <h2>留言与评价管理</h2>
        </div>
      </template>

      <!-- 搜索栏 -->
      <div class="search-bar">
        <el-select
          v-model="searchForm.type"
          placeholder="类型"
          clearable
          style="width: 150px; margin-right: 10px;"
          @change="handleSearch"
        >
          <el-option label="全部" value="" />
          <el-option label="评价" :value="1" />
          <el-option label="留言" :value="2" />
        </el-select>

        <el-select
          v-model="searchForm.status"
          placeholder="状态"
          clearable
          style="width: 150px; margin-right: 10px;"
          @change="handleSearch"
        >
          <el-option label="全部" value="" />
          <el-option label="显示" :value="1" />
          <el-option label="隐藏" :value="0" />
        </el-select>

        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>

      <!-- 评论表格 -->
      <el-table :data="comments" stripe style="width: 100%; margin-top: 20px" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="类型" width="80">
          <template #default="scope">
            <el-tag :type="scope.row.type === 1 ? 'success' : 'info'" size="small">
              {{ scope.row.type === 1 ? '评价' : '留言' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="房源" min-width="150" show-overflow-tooltip>
          <template #default="scope">
            {{ scope.row.house?.title || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="用户" width="120">
          <template #default="scope">
            <div class="user-cell">
              <el-avatar :size="30" :src="formatImageUrl(scope.row.userAvatar)" />
              <span style="margin-left: 8px;">{{ scope.row.username || '匿名' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="评分" width="120">
          <template #default="scope">
            <el-rate v-if="scope.row.type === 1 && scope.row.score" v-model="scope.row.score" disabled size="small" />
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="内容" min-width="200" show-overflow-tooltip />
        <el-table-column label="房东回复" width="100">
          <template #default="scope">
            <span v-if="scope.row.replyContent" class="has-reply">已回复</span>
            <span v-else class="no-reply">未回复</span>
          </template>
        </el-table-column>
        <el-table-column label="管理员回复" width="100">
          <template #default="scope">
            <span v-if="scope.row.adminReplyContent" class="has-reply">已回复</span>
            <span v-else class="no-reply">未回复</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'" size="small">
              {{ scope.row.status === 1 ? '显示' : '隐藏' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="发布时间" width="180" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="scope">
            <el-button type="primary" link size="small" @click="viewComment(scope.row)">
              查看
            </el-button>
            <el-button
              type="success"
              link
              size="small"
              @click="handleReply(scope.row)"
            >
              {{ scope.row.adminReplyContent ? '修改管理员回复' : '管理员回复' }}
            </el-button>
            <el-button
              :type="scope.row.status === 1 ? 'warning' : 'success'"
              link
              size="small"
              @click="toggleStatus(scope.row)"
            >
              {{ scope.row.status === 1 ? '隐藏' : '显示' }}
            </el-button>
            <el-button
              type="danger"
              link
              size="small"
              @click="deleteComment(scope.row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-if="total > 0"
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next, sizes"
        :page-sizes="[10, 20, 50]"
        @current-change="fetchComments"
        @size-change="fetchComments"
        style="margin-top: 20px; justify-content: center"
      />
    </el-card>

    <!-- 查看详情对话框 -->
    <el-dialog
      v-model="detailVisible"
      title="评论详情"
      width="70%"
      top="5vh"
    >
      <div v-if="currentComment">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="类型">
            <el-tag :type="currentComment.type === 1 ? 'success' : 'info'">
              {{ currentComment.type === 1 ? '评价' : '留言' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="currentComment.status === 1 ? 'success' : 'danger'">
              {{ currentComment.status === 1 ? '显示' : '隐藏' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="房源" :span="2">
            {{ currentComment.house?.title || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="用户">
            {{ currentComment.username || '匿名' }}
          </el-descriptions-item>
          <el-descriptions-item label="评分">
            <el-rate v-if="currentComment.type === 1 && currentComment.score" v-model="currentComment.score" disabled />
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="发布时间" :span="2">
            {{ currentComment.createTime }}
          </el-descriptions-item>
          <el-descriptions-item label="内容" :span="2">
            <div class="comment-content">{{ currentComment.content }}</div>
          </el-descriptions-item>
          <el-descriptions-item label="房东回复" :span="2" v-if="currentComment.replyContent">
            <div class="reply-content landlord-reply">
              <div class="reply-header">
                <span class="reply-by">{{ currentComment.replyUserName || '房东' }}</span>
                <span class="reply-time">{{ currentComment.replyTime }}</span>
              </div>
              <div class="reply-text">{{ currentComment.replyContent }}</div>
            </div>
          </el-descriptions-item>
          <el-descriptions-item label="管理员回复" :span="2" v-if="currentComment.adminReplyContent">
            <div class="reply-content admin-reply">
              <div class="reply-header">
                <span class="reply-by">{{ currentComment.adminReplyUserName || '管理员' }}</span>
                <span class="reply-time">{{ currentComment.adminReplyTime }}</span>
              </div>
              <div class="reply-text">{{ currentComment.adminReplyContent }}</div>
            </div>
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>

    <!-- 回复对话框 -->
    <el-dialog
      v-model="replyVisible"
      :title="currentComment?.adminReplyContent ? '修改管理员回复' : '管理员回复'"
      width="50%"
    >
      <el-form :model="replyForm" label-width="100px">
        <el-form-item label="原内容">
          <div class="original-content">{{ currentComment?.content }}</div>
        </el-form-item>
        <el-form-item label="房东回复" v-if="currentComment?.replyContent">
          <div class="original-content">{{ currentComment?.replyContent }}</div>
        </el-form-item>
        <el-form-item label="管理员回复">
          <el-input
            v-model="replyForm.content"
            type="textarea"
            :rows="5"
            placeholder="请输入管理员回复内容..."
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="replyVisible = false">取消</el-button>
          <el-button type="primary" @click="submitReply" :loading="submittingReply">确定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const baseAPI = import.meta.env.VITE_BASE_API || '/api'

const loading = ref(false)
const comments = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const searchForm = reactive({
  type: '',
  status: ''
})

const detailVisible = ref(false)
const replyVisible = ref(false)
const currentComment = ref(null)
const submittingReply = ref(false)

const replyForm = reactive({
  content: ''
})

// 格式化图片URL
const formatImageUrl = (path) => {
  if (!path) return ''
  if (path.startsWith('http')) return path
  return baseAPI + path
}

// 获取评论列表
const fetchComments = async () => {
  loading.value = true
  try {
    const params = {
      currentPage: currentPage.value,
      size: pageSize.value,
      type: searchForm.type ? searchForm.type : undefined,
      status: searchForm.status !== '' && searchForm.status != null ? searchForm.status : undefined
    }

    const res = await request.get('/house-comments/admin', params)

    comments.value = res.records || []
    total.value = res.total || 0
  } catch (error) {
    console.error('获取评论列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  fetchComments()
}

// 重置搜索
const resetSearch = () => {
  searchForm.type = ''
  searchForm.status = ''
  handleSearch()
}

// 查看评论
const viewComment = (comment) => {
  currentComment.value = comment
  detailVisible.value = true
}

// 处理回复
const handleReply = (comment) => {
  currentComment.value = comment
  replyForm.content = comment.adminReplyContent || ''
  replyVisible.value = true
}

// 提交回复
const submitReply = async () => {
  if (!replyForm.content.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }

  submittingReply.value = true
  try {
    await request.put(`/house-comments/${currentComment.value.id}/reply`, null, {
      params: { replyContent: replyForm.content }
    })

    ElMessage.success('回复成功')
    replyVisible.value = false
    fetchComments()
  } catch (error) {
    console.error('回复失败:', error)
  } finally {
    submittingReply.value = false
  }
}

// 切换状态
const toggleStatus = async (comment) => {
  const newStatus = comment.status === 1 ? 0 : 1
  const action = newStatus === 1 ? '显示' : '隐藏'

  try {
    await ElMessageBox.confirm(`确定要${action}该评论吗?`, '提示', {
      type: 'warning'
    })

    await request.put(`/house-comments/${comment.id}/status`, null, {
      params: { status: newStatus }
    })

    ElMessage.success(`${action}成功`)
    fetchComments()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(`${action}失败:`, error)
    }
  }
}

// 删除评论
const deleteComment = async (comment) => {
  try {
    await ElMessageBox.confirm('确定要删除该评论吗?删除后将无法恢复!', '警告', {
      type: 'warning',
      confirmButtonText: '确定删除',
      cancelButtonText: '取消'
    })

    await request.delete(`/house-comments/${comment.id}`)
    ElMessage.success('删除成功')
    fetchComments()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

onMounted(() => {
  fetchComments()
})
</script>

<style lang="scss" scoped>
.comment-management-container {
  padding: 20px;
}

.card-header {
  h2 {
    margin: 0;
    font-size: 18px;
    font-weight: 600;
    color: #1d4ed8;
  }
}

.search-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.user-cell {
  display: flex;
  align-items: center;
}

.has-reply {
  color: #67c23a;
  font-weight: 500;
}

.no-reply {
  color: #909399;
}

.comment-content,
.reply-content,
.original-content {
  white-space: pre-line;
  line-height: 1.6;
  max-height: 400px;
  overflow-y: auto;
  padding: 10px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.original-content {
  color: #606266;
}

.reply-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-size: 13px;
}

.reply-by {
  font-weight: 600;
}

.reply-time {
  color: #909399;
}

.reply-text {
  line-height: 1.6;
}

.landlord-reply {
  border-left: 3px solid #67c23a;
}

.admin-reply {
  border-left: 3px solid #409eff;
}

// 响应式
@media (max-width: 768px) {
  .search-bar {
    .el-input,
    .el-select {
      width: 100% !important;
    }
  }
}
</style>
