<template>
  <div class="violation-management">
    <!-- 搜索区域 -->
    <div class="search-container">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="用户名">
          <el-input v-model="searchForm.username" placeholder="请输入用户名" clearable />
        </el-form-item>
        <el-form-item label="违规类型">
          <el-input v-model="searchForm.type" placeholder="请输入违规类型" clearable />
        </el-form-item>
        <el-form-item label="处理状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
            <el-option label="待处理" :value="0" />
            <el-option label="已处理" :value="1" />
            <el-option label="已忽略" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 工具栏 -->
    <div class="toolbar">
      <el-button type="primary" @click="handleAdd">添加违规记录</el-button>
    </div>

    <!-- 表格 -->
    <el-table
      v-loading="loading"
      :data="tableData"
      border
    >
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column label="违规用户" width="120">
        <template #default="scope">
          <div v-if="scope.row.user">{{ scope.row.user.name || scope.row.user.username }}</div>
        </template>
      </el-table-column>
      <el-table-column prop="type" label="违规类型" width="150" />
      <el-table-column prop="description" label="违规描述" min-width="200" show-overflow-tooltip />
      <el-table-column prop="relatedType" label="关联类型" width="100" />
      <el-table-column prop="penalty" label="处罚措施" min-width="150" show-overflow-tooltip />
      <el-table-column prop="creditDeduction" label="扣除信用分" width="120" />
      <el-table-column label="状态" width="100">
        <template #default="scope">
          <el-tag :type="getStatusType(scope.row.status)">
            {{ getStatusText(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="scope">
          <el-button
            type="success"
            link
            @click="handleProcess(scope.row)"
            :disabled="scope.row.status !== 0"
          >
            处理
          </el-button>
          <el-button
            type="warning"
            link
            @click="handleIgnore(scope.row)"
            :disabled="scope.row.status !== 0"
          >
            忽略
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

    <!-- 添加/编辑对话框 -->
    <el-dialog
      :title="dialogType === 'add' ? '添加违规记录' : '处理违规记录'"
      v-model="dialogVisible"
      width="600px"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
      >
        <el-form-item label="违规用户ID" prop="userId" v-if="dialogType === 'add'">
          <el-input v-model.number="form.userId" type="number" placeholder="请输入用户ID" />
        </el-form-item>
        <el-form-item label="违规类型" prop="type" v-if="dialogType === 'add'">
          <el-input v-model="form.type" placeholder="请输入违规类型" />
        </el-form-item>
        <el-form-item label="违规描述" prop="description" v-if="dialogType === 'add'">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            placeholder="请输入违规描述"
          />
        </el-form-item>
        <el-form-item label="关联ID" prop="relatedId" v-if="dialogType === 'add'">
          <el-input v-model.number="form.relatedId" type="number" placeholder="请输入关联ID(可选)" />
        </el-form-item>
        <el-form-item label="关联类型" prop="relatedType" v-if="dialogType === 'add'">
          <el-select v-model="form.relatedType" placeholder="请选择关联类型" clearable>
            <el-option label="房源" value="house" />
            <el-option label="帖子" value="post" />
            <el-option label="预约" value="appointment" />
          </el-select>
        </el-form-item>
        <el-form-item label="处罚措施" prop="penalty">
          <el-input
            v-model="form.penalty"
            type="textarea"
            :rows="3"
            placeholder="请输入处罚措施"
          />
        </el-form-item>
        <el-form-item label="扣除信用分" prop="creditDeduction">
          <el-input-number v-model="form.creditDeduction" :min="0" :max="100" />
        </el-form-item>
        <el-form-item label="处理备注" prop="handleRemark" v-if="dialogType === 'process'">
          <el-input
            v-model="form.handleRemark"
            type="textarea"
            :rows="3"
            placeholder="请输入处理备注"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog
      title="违规记录详情"
      v-model="detailDialogVisible"
      width="700px"
    >
      <el-descriptions :column="2" border v-if="currentViolation">
        <el-descriptions-item label="记录ID">{{ currentViolation.id }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentViolation.status)">
            {{ getStatusText(currentViolation.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="违规用户" v-if="currentViolation.user">
          {{ currentViolation.user.name || currentViolation.user.username }}
        </el-descriptions-item>
        <el-descriptions-item label="违规类型">{{ currentViolation.type }}</el-descriptions-item>
        <el-descriptions-item label="违规描述" :span="2">
          {{ currentViolation.description }}
        </el-descriptions-item>
        <el-descriptions-item label="关联类型">{{ currentViolation.relatedType || '无' }}</el-descriptions-item>
        <el-descriptions-item label="关联ID">{{ currentViolation.relatedId || '无' }}</el-descriptions-item>
        <el-descriptions-item label="处罚措施" :span="2">
          {{ currentViolation.penalty || '无' }}
        </el-descriptions-item>
        <el-descriptions-item label="扣除信用分">{{ currentViolation.creditDeduction }}</el-descriptions-item>
        <el-descriptions-item label="处理时间">{{ currentViolation.handleTime || '未处理' }}</el-descriptions-item>
        <el-descriptions-item label="处理备注" :span="2">
          {{ currentViolation.handleRemark || '无' }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="2">{{ currentViolation.createTime }}</el-descriptions-item>
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

// 搜索表单
const searchForm = reactive({
  username: '',
  type: '',
  status: null
})

// 对话框
const dialogVisible = ref(false)
const dialogType = ref('add')
const formRef = ref(null)
const form = reactive({
  userId: null,
  type: '',
  description: '',
  relatedId: null,
  relatedType: '',
  penalty: '',
  creditDeduction: 0,
  handleRemark: ''
})

const rules = {
  userId: [{ required: true, message: '请输入用户ID', trigger: 'blur' }],
  type: [{ required: true, message: '请输入违规类型', trigger: 'blur' }],
  description: [{ required: true, message: '请输入违规描述', trigger: 'blur' }],
  penalty: [{ required: true, message: '请输入处罚措施', trigger: 'blur' }],
  creditDeduction: [{ required: true, message: '请输入扣除信用分', trigger: 'blur' }]
}

// 详情对话框
const detailDialogVisible = ref(false)
const currentViolation = ref(null)

// 初始化
onMounted(() => {
  fetchViolations()
})

// 获取违规记录列表
const fetchViolations = async () => {
  loading.value = true
  try {
    const params = {
      currentPage: currentPage.value,
      size: pageSize.value,
      status: searchForm.status !== null && searchForm.status !== undefined && searchForm.status !== '' ? searchForm.status : undefined,
      username: searchForm.username || undefined,
      type: searchForm.type || undefined
    }

    const res = await request.get('/violations', params)

    tableData.value = res.records || []
    total.value = res.total || 0
  } catch (error) {
    console.error('获取违规记录列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  fetchViolations()
}

// 重置搜索
const resetSearch = () => {
  searchForm.username = ''
  searchForm.type = ''
  searchForm.status = null
  handleSearch()
}

// 添加
const handleAdd = () => {
  dialogType.value = 'add'
  resetForm()
  dialogVisible.value = true
}

// 处理
const handleProcess = (row) => {
  dialogType.value = 'process'
  resetForm()
  form.id = row.id
  form.penalty = row.penalty || ''
  form.creditDeduction = row.creditDeduction || 0
  dialogVisible.value = true
}

// 忽略
const handleIgnore = async (row) => {
  try {
    await ElMessageBox.confirm('确定忽略该违规记录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await request.put(`/violations/${row.id}/ignore`)
    ElMessage.success('操作成功')
    fetchViolations()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('忽略失败:', error)
    }
  }
}

// 查看详情
const handleViewDetail = (row) => {
  currentViolation.value = row
  detailDialogVisible.value = true
}

// 重置表单
const resetForm = () => {
  form.id = null
  form.userId = null
  form.type = ''
  form.description = ''
  form.relatedId = null
  form.relatedType = ''
  form.penalty = ''
  form.creditDeduction = 0
  form.handleRemark = ''

  if (formRef.value) {
    formRef.value.resetFields()
  }
}

// 提交表单
const submitForm = () => {
  formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        if (dialogType.value === 'add') {
          await request.post('/violations', form)
          ElMessage.success('添加成功')
        } else {
          await request.put(`/violations/${form.id}/handle`, null, {
            params: {
              penalty: form.penalty,
              creditDeduction: form.creditDeduction,
              handleRemark: form.handleRemark
            }
          })
          ElMessage.success('处理成功')
        }
        dialogVisible.value = false
        fetchViolations()
      } catch (error) {
        console.error('操作失败:', error)
      }
    }
  })
}

// 获取状态类型
const getStatusType = (status) => {
  const typeMap = {
    0: 'warning',
    1: 'success',
    2: 'info'
  }
  return typeMap[status] || 'info'
}

// 获取状态文本
const getStatusText = (status) => {
  const textMap = {
    0: '待处理',
    1: '已处理',
    2: '已忽略'
  }
  return textMap[status] || '未知'
}

// 分页大小变化
const handleSizeChange = (val) => {
  pageSize.value = val
  fetchViolations()
}

// 页码变化
const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchViolations()
}
</script>

<style lang="scss" scoped>
.violation-management {
  .search-container {
    margin-bottom: 20px;
    padding: 18px;
    background-color: #fff;
    border-radius: 4px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
  }

  .toolbar {
    margin-bottom: 20px;
  }

  .pagination-container {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
