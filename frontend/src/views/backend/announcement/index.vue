<template>
  <div class="announcement-container">
    <div class="page-header">
      <h1 class="page-title">公告管理</h1>
      <el-button type="primary" @click="handleAddAnnouncement">发布新公告</el-button>
    </div>

    <!-- 搜索区域 -->
    <div class="search-area">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="公告标题">
          <el-input v-model="searchForm.title" placeholder="请输入公告标题" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 数据表格 -->
    <div class="table-container">
      <el-table v-loading="loading" :data="announcementList" border stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="公告标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="content" label="公告内容" min-width="300" show-overflow-tooltip />
        <el-table-column label="发布人" width="120">
          <template #default="scope">
            {{ scope.row.admin?.name || '未知' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">
              {{ scope.row.status === 1 ? '已发布' : '已下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="发布时间" width="180" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="scope">
            <el-button type="primary" size="small" @click="handleViewAnnouncement(scope.row)">查看</el-button>
            <el-button type="success" size="small" @click="handleEditAnnouncement(scope.row)">编辑</el-button>
            <el-button 
              :type="scope.row.status === 1 ? 'warning' : 'success'" 
              size="small" 
              @click="handleToggleStatus(scope.row)"
            >
              {{ scope.row.status === 1 ? '下架' : '发布' }}
            </el-button>
            <el-button type="danger" size="small" @click="handleDeleteAnnouncement(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          :current-page="currentPage"
          :page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑公告' : '发布新公告'"
      width="50%"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="公告标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入公告标题" />
        </el-form-item>
        <el-form-item label="公告内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="10"
            placeholder="请输入公告内容"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">发布</el-radio>
            <el-radio :value="0">草稿</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm" :loading="submitLoading">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 查看对话框 -->
    <el-dialog
      v-model="viewDialogVisible"
      title="公告详情"
      width="50%"
    >
      <div class="announcement-detail">
        <h2 class="announcement-title">{{ viewForm.title }}</h2>
        <div class="announcement-meta">
          <span>发布人: {{ viewForm.admin?.name || '未知' }}</span>
          <span>发布时间: {{ viewForm.createTime }}</span>
          <span>状态: {{ viewForm.status === 1 ? '已发布' : '已下架' }}</span>
        </div>
        <div class="announcement-content">
          <p v-html="formatContent(viewForm.content)"></p>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

// 状态选项
const statusOptions = [
  { value: 1, label: '已发布' },
  { value: 0, label: '已下架' }
]

// 搜索表单
const searchForm = reactive({
  title: '',
  status: ''
})

// 表格数据
const loading = ref(false)
const announcementList = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 对话框控制
const dialogVisible = ref(false)
const viewDialogVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)

// 表单对象
const formRef = ref(null)
const form = reactive({
  id: '',
  title: '',
  content: '',
  status: 1
})

// 查看表单
const viewForm = reactive({
  id: '',
  title: '',
  content: '',
  status: 1,
  admin: null,
  createTime: ''
})

// 表单验证规则
const rules = {
  title: [
    { required: true, message: '请输入公告标题', trigger: 'blur' },
    { min: 2, max: 100, message: '长度在2到100个字符之间', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请输入公告内容', trigger: 'blur' }
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' }
  ]
}

// 初始化
onMounted(() => {
  fetchAnnouncementList()
})

// 获取公告列表
const fetchAnnouncementList = async () => {
  loading.value = true
  try {
    await request.get('/announcements', {
      title: searchForm.title,
      status: searchForm.status === '' ? null : searchForm.status,
      currentPage: currentPage.value,
      size: pageSize.value
    }, {
      onSuccess: (res) => {
        announcementList.value = res.records||[]
        total.value = res.total||0
      }
    })
  } catch (error) {
    console.error('获取公告列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  fetchAnnouncementList()
}

// 重置搜索
const resetSearch = () => {
  searchForm.title = ''
  searchForm.status = ''
  currentPage.value = 1
  fetchAnnouncementList()
}

// 分页大小变化
const handleSizeChange = (val) => {
  pageSize.value = val
  fetchAnnouncementList()
}

// 页码变化
const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchAnnouncementList()
}

// 添加公告
const handleAddAnnouncement = () => {
  isEdit.value = false
  dialogVisible.value = true
}

// 编辑公告
const handleEditAnnouncement = (row) => {
  isEdit.value = true
  Object.assign(form, row)
  dialogVisible.value = true
}

// 查看公告
const handleViewAnnouncement = (row) => {
  Object.assign(viewForm, row)
  viewDialogVisible.value = true
}

// 切换状态
const handleToggleStatus = (row) => {
  const newStatus = row.status === 1 ? 0 : 1
  const statusText = newStatus === 1 ? '发布' : '下架'
  
  ElMessageBox.confirm(
    `确定要${statusText}该公告吗？`,
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    request.put(`/admin/announcements/${row.id}/status`, null, {
      params: { status: newStatus },
      successMsg: `${statusText}成功`,
      onSuccess: () => {
        fetchAnnouncementList()
      }
    })
  }).catch(() => {})
}

// 删除公告
const handleDeleteAnnouncement = (row) => {
  ElMessageBox.confirm(
    '确定要删除该公告吗？删除后无法恢复',
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    request.delete(`/admin/announcements/${row.id}`, {
      successMsg: '删除成功',
      onSuccess: () => {
        fetchAnnouncementList()
      }
    })
  }).catch(() => {})
}

// 提交表单
const submitForm = () => {
  formRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        if (isEdit.value) {
          // 编辑
          await request.put(`/admin/announcements/${form.id}`, form, {
            successMsg: '更新成功',
            onSuccess: () => {
              dialogVisible.value = false
              fetchAnnouncementList()
            }
          })
        } else {
          // 新增
          await request.post('/admin/announcements', form, {
            successMsg: '发布成功',
            onSuccess: () => {
              dialogVisible.value = false
              fetchAnnouncementList()
            }
          })
        }
      } catch (error) {
        console.error('提交失败:', error)
      } finally {
        submitLoading.value = false
      }
    }
  })
}

// 重置表单
const resetForm = () => {
  if (formRef.value) {
    formRef.value.resetFields()
  }
  Object.assign(form, {
    id: '',
    title: '',
    content: '',
    status: 1
  })
}

// 格式化内容（将换行符转换为<br>）
const formatContent = (content) => {
  if (!content) return ''
  return content.replace(/\n/g, '<br>')
}
</script>

<style scoped>
.announcement-container {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  font-size: 24px;
  color: #303133;
  margin: 0;
}

.search-area {
  background-color: #fff;
  padding: 20px;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  margin-bottom: 20px;
}

.table-container {
  background-color: #fff;
  padding: 20px;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.pagination-container {
  margin-top: 20px;
  text-align: right;
}

.announcement-detail {
  padding: 0 20px;
}

.announcement-title {
  font-size: 20px;
  font-weight: bold;
  margin-bottom: 15px;
  text-align: center;
}

.announcement-meta {
  display: flex;
  justify-content: space-between;
  color: #909399;
  font-size: 14px;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid #EBEEF5;
}

.announcement-content {
  line-height: 1.8;
  font-size: 16px;
}
</style> 