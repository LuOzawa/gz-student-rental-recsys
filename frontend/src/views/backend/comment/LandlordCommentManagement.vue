<template>
  <div class="landlord-comment-management-container">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <h2>我的房源留言与评价</h2>
        </div>
      </template>

      <!-- 搜索栏 -->
      <div class="search-bar">
        <el-select
          v-model="searchForm.houseId"
          placeholder="选择房源"
          clearable
          filterable
          style="width: 200px; margin-right: 10px;"
          @change="handleSearch"
        >
          <el-option
            v-for="house in myHouses"
            :key="house.id"
            :label="house.title"
            :value="house.id"
          />
        </el-select>

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

        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>

      <!-- 统计信息 -->
      <el-row :gutter="20" class="stats-container">
        <el-col :span="6">
          <el-card>
            <div class="stat-item">
              <div class="stat-label">我的信用分</div>
              <div class="stat-value credit" :class="getCreditScoreClass(landlordCreditScore)">
                {{ landlordCreditScore || 100 }}
              </div>
              <div class="stat-sublabel">{{ getCreditLevel(landlordCreditScore || 100) }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card>
            <div class="stat-item">
              <div class="stat-label">总评论</div>
              <div class="stat-value total">{{ stats.total }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card>
            <div class="stat-item">
              <div class="stat-label">未回复</div>
              <div class="stat-value pending">{{ stats.unreplied }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card>
            <div class="stat-item">
              <div class="stat-label">已回复</div>
              <div class="stat-value success">{{ stats.replied }}</div>
            </div>
          </el-card>
        </el-col>
      </el-row>

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
        <el-table-column label="我的回复" width="100">
          <template #default="scope">
            <el-tag v-if="scope.row.replyContent" type="success" size="small">已回复</el-tag>
            <el-tag v-else type="warning" size="small">未回复</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="管理员回复" width="100">
          <template #default="scope">
            <el-tag v-if="scope.row.adminReplyContent" type="info" size="small">已回复</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="发布时间" width="180" />
        <el-table-column label="操作" width="180" fixed="right">
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
              {{ scope.row.replyContent ? '修改回复' : '回复' }}
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
          <el-descriptions-item label="我的回复" :span="2" v-if="currentComment.replyContent">
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
      :title="currentComment?.replyContent ? '修改我的回复' : '回复留言/评价'"
      width="50%"
    >
      <el-form :model="replyForm" label-width="100px">
        <el-form-item label="房源">
          <div class="form-text">{{ currentComment?.house?.title }}</div>
        </el-form-item>
        <el-form-item label="用户">
          <div class="form-text">{{ currentComment?.username }}</div>
        </el-form-item>
        <el-form-item label="原内容">
          <div class="original-content">{{ currentComment?.content }}</div>
        </el-form-item>
        <el-form-item label="管理员回复" v-if="currentComment?.adminReplyContent">
          <div class="original-content admin-reply">{{ currentComment?.adminReplyContent }}</div>
        </el-form-item>
        <el-form-item label="我的回复">
          <el-input
            v-model="replyForm.content"
            type="textarea"
            :rows="5"
            placeholder="请输入您的回复内容..."
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
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const baseAPI = import.meta.env.VITE_BASE_API || '/api'

const loading = ref(false)
const comments = ref([])
const allComments = ref([]) // 存储所有评论用于统计
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const myHouses = ref([])

const searchForm = reactive({
  houseId: null,
  type: ''
})

const stats = reactive({
  total: 0,
  unreplied: 0,
  replied: 0
})

const detailVisible = ref(false)
const replyVisible = ref(false)
const currentComment = ref(null)
const submittingReply = ref(false)

const replyForm = reactive({
  content: ''
})

// 房东信用分
const landlordCreditScore = ref(100)

// 获取信用分等级
const getCreditLevel = (score) => {
  if (score >= 90) return '优秀'
  if (score >= 80) return '良好'
  if (score >= 70) return '中等'
  if (score >= 60) return '及格'
  return '较差'
}

// 获取信用分样式类
const getCreditScoreClass = (score) => {
  if (score >= 90) return 'excellent'
  if (score >= 80) return 'good'
  if (score >= 70) return 'medium'
  if (score >= 60) return 'pass'
  return 'poor'
}

// 格式化图片URL
const formatImageUrl = (path) => {
  if (!path) return ''
  if (path.startsWith('http')) return path
  return baseAPI + path
}

// 获取房东的房源列表
const fetchMyHouses = async () => {
  try {
    const res = await request.get('/houses/landlord', {
      currentPage: 1,
      size: 1000 // 获取所有房源
    })
    myHouses.value = res.records || []
  } catch (error) {
    console.error('获取房源列表失败:', error)
  }
}

// 获取评论列表
const fetchComments = async () => {
  loading.value = true
  try {
    const params = {
      currentPage: currentPage.value,
      size: pageSize.value,
      houseId: searchForm.houseId || undefined,
      type: searchForm.type ? searchForm.type : undefined
    }

    const res = await request.get('/house-comments/landlord', params)

    comments.value = res.records || []
    total.value = res.total || 0

    // 保存所有评论用于统计
    allComments.value = res.records || []

    // 计算统计数据
    calculateStats()
  } catch (error) {
    console.error('获取评论列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 计算统计数据
const calculateStats = () => {
  stats.total = allComments.value.length
  stats.unreplied = allComments.value.filter(c => !c.replyContent).length
  stats.replied = allComments.value.filter(c => c.replyContent).length
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  fetchComments()
}

// 重置搜索
const resetSearch = () => {
  searchForm.houseId = null
  searchForm.type = ''
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
  replyForm.content = comment.replyContent || ''
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

// 获取当前用户信用分
const fetchLandlordCreditScore = async () => {
  try {
    // 从用户store获取当前用户ID
    const userId = userStore.user?.id
    if (userId) {
      const res = await request.get(`/users/${userId}`)
      if (res && res.creditScore !== null && res.creditScore !== undefined) {
        landlordCreditScore.value = res.creditScore
      }
    }
  } catch (error) {
    console.error('获取信用分失败:', error)
  }
}

onMounted(async () => {
  await fetchMyHouses()
  fetchComments()
  fetchLandlordCreditScore()
})
</script>

<style lang="scss" scoped>
.landlord-comment-management-container {
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
  margin-bottom: 20px;
}

.stats-container {
  margin-bottom: 20px;

  .stat-item {
    text-align: center;
    padding: 10px 0;

    .stat-label {
      font-size: 14px;
      color: #999;
      margin-bottom: 10px;
    }

    .stat-value {
      font-size: 28px;
      font-weight: bold;

      &.total {
        color: #409EFF;
      }

      &.pending {
        color: #E6A23C;
      }

      &.success {
        color: #67C23A;
      }

      &.credit {
        &.excellent {
          color: #10b981;
        }

        &.good {
          color: #3b82f6;
        }

        &.medium {
          color: #f59e0b;
        }

        &.pass {
          color: #f97316;
        }

        &.poor {
          color: #ef4444;
        }
      }
    }

    .stat-sublabel {
      font-size: 12px;
      color: #666;
      margin-top: 5px;
      font-weight: 500;
    }
  }
}

.user-cell {
  display: flex;
  align-items: center;
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

.original-content,
.form-text {
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
