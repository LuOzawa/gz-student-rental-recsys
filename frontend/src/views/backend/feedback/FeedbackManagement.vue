<template>
  <div class="feedback-management">
    <!-- 搜索区域 -->
    <div class="search-container">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="用户名">
          <el-input v-model="searchForm.username" placeholder="请输入用户名" clearable />
        </el-form-item>
        <el-form-item label="反馈类型">
          <el-select v-model="searchForm.type" placeholder="请选择类型" clearable>
            <el-option label="功能建议" value="功能建议" />
            <el-option label="投诉" value="投诉" />
            <el-option label="Bug反馈" value="Bug反馈" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
            <el-option label="待处理" :value="0" />
            <el-option label="处理中" :value="1" />
            <el-option label="已处理" :value="2" />
            <el-option label="已关闭" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 统计信息 -->
    <el-row :gutter="20" class="stats-container">
      <el-col :span="6">
        <el-card>
          <div class="stat-item">
            <div class="stat-label">待处理</div>
            <div class="stat-value pending">{{ pendingCount }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div class="stat-item">
            <div class="stat-label">处理中</div>
            <div class="stat-value processing">{{ processingCount }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div class="stat-item">
            <div class="stat-label">已处理</div>
            <div class="stat-value success">{{ processedCount }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div class="stat-item">
            <div class="stat-label">已关闭</div>
            <div class="stat-value closed">{{ closedCount }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 表格 -->
    <el-table
      v-loading="loading"
      :data="tableData"
      border
    >
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column label="反馈用户" width="120">
        <template #default="scope">
          <div v-if="scope.row.user">{{ scope.row.user.name || scope.row.user.username }}</div>
        </template>
      </el-table-column>
      <el-table-column prop="type" label="反馈类型" width="120" />
      <el-table-column prop="title" label="反馈标题" min-width="180" show-overflow-tooltip />
      <el-table-column prop="content" label="反馈内容" min-width="200" show-overflow-tooltip />
      <el-table-column prop="contact" label="联系方式" width="120" />
      <el-table-column label="状态" width="100">
        <template #default="scope">
          <el-tag :type="getStatusType(scope.row.status)">
            {{ getStatusText(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="250" fixed="right">
        <template #default="scope">
          <el-button type="primary" link @click="handleReply(scope.row)">回复</el-button>
          <el-button
            type="success"
            link
            @click="handleMarkProcessed(scope.row)"
            :disabled="scope.row.status === 2 || scope.row.status === 3"
          >
            标记已处理
          </el-button>
          <el-button
            type="warning"
            link
            @click="handleClose(scope.row)"
            :disabled="scope.row.status === 3"
          >
            关闭
          </el-button>
          <el-button type="primary" link @click="handleViewDetail(scope.row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-container">
      <el-pagination
        :current-page="currentPage"
        :page-size="pageSize"
        :page-sizes="[10, 20, 50]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <!-- 回复对话框 -->
    <el-dialog
      title="回复反馈"
      v-model="replyDialogVisible"
      width="600px"
    >
      <el-form
        ref="replyFormRef"
        :model="replyForm"
        :rules="replyRules"
        label-width="80px"
      >
        <el-form-item label="回复内容" prop="reply">
          <el-input
            v-model="replyForm.reply"
            type="textarea"
            :rows="6"
            placeholder="请输入回复内容"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="replyDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitReply">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog
      title="反馈详情"
      v-model="detailDialogVisible"
      width="700px"
    >
      <el-descriptions :column="2" border v-if="currentFeedback">
        <el-descriptions-item label="反馈ID">{{ currentFeedback.id }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentFeedback.status)">
            {{ getStatusText(currentFeedback.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="反馈用户" v-if="currentFeedback.user">
          {{ currentFeedback.user.name || currentFeedback.user.username }}
        </el-descriptions-item>
        <el-descriptions-item label="反馈类型">{{ currentFeedback.type }}</el-descriptions-item>
        <el-descriptions-item label="反馈标题" :span="2">{{ currentFeedback.title }}</el-descriptions-item>
        <el-descriptions-item label="反馈内容" :span="2">
          <div style="white-space: pre-line;">{{ currentFeedback.content }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="联系方式">{{ currentFeedback.contact || '未提供' }}</el-descriptions-item>
        <el-descriptions-item label="处理时间">{{ currentFeedback.handleTime || '未处理' }}</el-descriptions-item>
        <el-descriptions-item label="管理员回复" :span="2">
          <div style="white-space: pre-line;">{{ currentFeedback.reply || '暂无回复' }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="2">{{ currentFeedback.createTime }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="detailDialogVisible = false">关闭</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

// 表格数据
const loading = ref(false)
const tableData = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 统计数据
const pendingCount = ref(0)
const processingCount = ref(0)
const processedCount = ref(0)
const closedCount = ref(0)

// 搜索表单
const searchForm = reactive({
  username: '',
  type: '',
  status: null
})

// 回复对话框
const replyDialogVisible = ref(false)
const replyFormRef = ref(null)
const replyForm = reactive({
  feedbackId: null,
  reply: ''
})
const replyRules = {
  reply: [
    { required: true, message: '请输入回复内容', trigger: 'blur' },
    { min: 5, message: '回复内容至少5个字符', trigger: 'blur' }
  ]
}

// 详情对话框
const detailDialogVisible = ref(false)
const currentFeedback = ref(null)

// 初始化
onMounted(() => {
  fetchFeedbacks()
  fetchStatistics()
})

// 获取反馈列表
const fetchFeedbacks = async () => {
  loading.value = true
  try {
    const params = {
      currentPage: currentPage.value,
      size: pageSize.value,
      status: searchForm.status !== null && searchForm.status !== undefined && searchForm.status !== '' ? searchForm.status : undefined,
      type: searchForm.type || undefined,
      username: searchForm.username || undefined
    }

    const res = await request.get('/feedbacks', params)

    tableData.value = res.records || []
    total.value = res.total || 0
  } catch (error) {
    console.error('获取反馈列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 获取统计数据
const fetchStatistics = async () => {
  try {
    const stats = await request.get('/feedbacks/statistics')
    pendingCount.value = stats.pendingCount || 0
    processingCount.value = stats.processingCount || 0
    processedCount.value = stats.processedCount || 0
    closedCount.value = stats.closedCount || 0
  } catch (error) {
    console.warn('获取统计数据失败:', error)
  }
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  fetchFeedbacks()
}

// 重置搜索
const resetSearch = () => {
  searchForm.username = ''
  searchForm.type = ''
  searchForm.status = null
  handleSearch()
}

// 回复
const handleReply = (row) => {
  replyForm.feedbackId = row.id
  replyForm.reply = row.reply || ''
  replyDialogVisible.value = true
}

// 提交回复
const submitReply = () => {
  replyFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        await request.put(`/feedbacks/${replyForm.feedbackId}/reply`, null, {
          params: { reply: replyForm.reply }
        })
        ElMessage.success('回复成功')
        replyDialogVisible.value = false
        fetchFeedbacks()
        fetchStatistics()
      } catch (error) {
        console.error('回复失败:', error)
      }
    }
  })
}

// 标记已处理
const handleMarkProcessed = async (row) => {
  try {
    await ElMessageBox.confirm('确定将该反馈标记为已处理吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'success'
    })

    await request.put(`/feedbacks/${row.id}/process`)
    ElMessage.success('操作成功')
    fetchFeedbacks()
    fetchStatistics()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('操作失败:', error)
    }
  }
}

// 关闭
const handleClose = async (row) => {
  try {
    await ElMessageBox.confirm('确定关闭该反馈吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await request.put(`/feedbacks/${row.id}/close`)
    ElMessage.success('操作成功')
    fetchFeedbacks()
    fetchStatistics()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('操作失败:', error)
    }
  }
}

// 查看详情
const handleViewDetail = (row) => {
  currentFeedback.value = row
  detailDialogVisible.value = true
}

// 获取状态类型
const getStatusType = (status) => {
  const typeMap = {
    0: 'warning',
    1: 'primary',
    2: 'success',
    3: 'info'
  }
  return typeMap[status] || 'info'
}

// 获取状态文本
const getStatusText = (status) => {
  const textMap = {
    0: '待处理',
    1: '处理中',
    2: '已处理',
    3: '已关闭'
  }
  return textMap[status] || '未知'
}

// 分页大小变化
const handleSizeChange = (val) => {
  pageSize.value = val
  fetchFeedbacks()
}

// 页码变化
const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchFeedbacks()
}
</script>

<style lang="scss" scoped>
.feedback-management {
  .search-container {
    margin-bottom: 20px;
    padding: 18px;
    background-color: #fff;
    border-radius: 4px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
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

        &.pending {
          color: #E6A23C;
        }

        &.processing {
          color: #409EFF;
        }

        &.success {
          color: #67C23A;
        }

        &.closed {
          color: #909399;
        }
      }
    }
  }

  .pagination-container {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
