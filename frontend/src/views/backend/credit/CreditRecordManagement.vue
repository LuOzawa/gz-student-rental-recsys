<template>
  <div class="credit-record-management">
    <!-- 搜索区域 -->
    <div class="search-container">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="用户名">
          <el-input v-model="searchForm.username" placeholder="请输入用户名" clearable />
        </el-form-item>
        <el-form-item label="变更类型">
          <el-select v-model="searchForm.changeType" placeholder="请选择类型" clearable>
            <el-option label="增加" value="增加" />
            <el-option label="扣除" value="扣除" />
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
      <el-button type="success" @click="handleAddCredit">增加信用分</el-button>
      <el-button type="warning" @click="handleDeductCredit">扣除信用分</el-button>
    </div>

    <!-- 表格 -->
    <el-table
      v-loading="loading"
      :data="tableData"
      border
    >
      <el-table-column prop="id" label="记录ID" width="100" />
      <el-table-column label="用户" width="150">
        <template #default="scope">
          <div v-if="scope.row.user">
            <div>{{ scope.row.user.name || scope.row.user.username }}</div>
            <div style="font-size: 12px; color: #999;">当前信用分: {{ scope.row.user.creditScore }}</div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="变更类型" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.changeType === '增加' ? 'success' : 'warning'">
            {{ scope.row.changeType }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="变更值" width="120">
        <template #default="scope">
          <span :style="{ color: scope.row.changeValue > 0 ? '#67C23A' : '#E6A23C', fontWeight: 'bold' }">
            {{ scope.row.changeValue > 0 ? '+' : '' }}{{ scope.row.changeValue }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="reason" label="变更原因" min-width="200" show-overflow-tooltip />
      <el-table-column prop="relatedId" label="关联ID" width="100" />
      <el-table-column label="操作人" width="120">
        <template #default="scope">
          <div v-if="scope.row.operator">{{ scope.row.operator.name || scope.row.operator.username }}</div>
          <div v-else>系统</div>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="scope">
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

    <!-- 增加/扣除信用分对话框 -->
    <el-dialog
      :title="dialogTitle"
      v-model="dialogVisible"
      width="500px"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item label="用户ID" prop="userId">
          <el-input v-model.number="form.userId" type="number" placeholder="请输入用户ID" />
        </el-form-item>
        <el-form-item label="变更值" prop="value">
          <el-input-number v-model="form.value" :min="1" :max="100" placeholder="请输入变更值" />
        </el-form-item>
        <el-form-item label="变更原因" prop="reason">
          <el-input
            v-model="form.reason"
            type="textarea"
            :rows="4"
            placeholder="请输入变更原因"
          />
        </el-form-item>
        <el-form-item label="关联ID" prop="relatedId">
          <el-input v-model.number="form.relatedId" type="number" placeholder="请输入关联ID(可选)" />
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
      title="信用记录详情"
      v-model="detailDialogVisible"
      width="600px"
    >
      <el-descriptions :column="2" border v-if="currentRecord">
        <el-descriptions-item label="记录ID">{{ currentRecord.id }}</el-descriptions-item>
        <el-descriptions-item label="变更类型">
          <el-tag :type="currentRecord.changeType === '增加' ? 'success' : 'warning'">
            {{ currentRecord.changeType }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="用户" v-if="currentRecord.user">
          {{ currentRecord.user.name || currentRecord.user.username }}
        </el-descriptions-item>
        <el-descriptions-item label="当前信用分" v-if="currentRecord.user">
          {{ currentRecord.user.creditScore }}
        </el-descriptions-item>
        <el-descriptions-item label="变更值">
          <span :style="{ color: currentRecord.changeValue > 0 ? '#67C23A' : '#E6A23C', fontWeight: 'bold' }">
            {{ currentRecord.changeValue > 0 ? '+' : '' }}{{ currentRecord.changeValue }}
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="关联ID">{{ currentRecord.relatedId || '无' }}</el-descriptions-item>
        <el-descriptions-item label="变更原因" :span="2">
          {{ currentRecord.reason }}
        </el-descriptions-item>
        <el-descriptions-item label="操作人" :span="2" v-if="currentRecord.operator">
          {{ currentRecord.operator.name || currentRecord.operator.username }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="2">{{ currentRecord.createTime }}</el-descriptions-item>
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
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
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
  changeType: ''
})

// 对话框
const dialogVisible = ref(false)
const dialogType = ref('add') // add: 增加, deduct: 扣除
const formRef = ref(null)
const form = reactive({
  userId: null,
  value: 1,
  reason: '',
  relatedId: null
})

const rules = {
  userId: [{ required: true, message: '请输入用户ID', trigger: 'blur' }],
  value: [{ required: true, message: '请输入变更值', trigger: 'blur' }],
  reason: [{ required: true, message: '请输入变更原因', trigger: 'blur' }]
}

const dialogTitle = computed(() => {
  return dialogType.value === 'add' ? '增加信用分' : '扣除信用分'
})

// 详情对话框
const detailDialogVisible = ref(false)
const currentRecord = ref(null)

// 初始化
onMounted(() => {
  fetchCreditRecords()
})

// 获取信用记录列表
const fetchCreditRecords = async () => {
  loading.value = true
  try {
    const params = {
      currentPage: currentPage.value,
      size: pageSize.value,
      changeType: searchForm.changeType || undefined,
      username: searchForm.username || undefined
    }

    const res = await request.get('/credit-records', params)

    tableData.value = res.records || []
    total.value = res.total || 0
  } catch (error) {
    console.error('获取信用记录列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  fetchCreditRecords()
}

// 重置搜索
const resetSearch = () => {
  searchForm.username = ''
  searchForm.changeType = ''
  handleSearch()
}

// 增加信用分
const handleAddCredit = () => {
  dialogType.value = 'add'
  resetForm()
  dialogVisible.value = true
}

// 扣除信用分
const handleDeductCredit = () => {
  dialogType.value = 'deduct'
  resetForm()
  dialogVisible.value = true
}

// 查看详情
const handleViewDetail = (row) => {
  currentRecord.value = row
  detailDialogVisible.value = true
}

// 重置表单
const resetForm = () => {
  form.userId = null
  form.value = 1
  form.reason = ''
  form.relatedId = null

  if (formRef.value) {
    formRef.value.resetFields()
  }
}

// 提交表单
const submitForm = () => {
  formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        const url = dialogType.value === 'add' ? '/credit-records/add' : '/credit-records/deduct'
        await request.post(url, {
          userId: form.userId,
          value: form.value,
          reason: form.reason,
          relatedId: form.relatedId
        })
        ElMessage.success('操作成功')
        dialogVisible.value = false
        fetchCreditRecords()
      } catch (error) {
        console.error('操作失败:', error)
      }
    }
  })
}

// 分页大小变化
const handleSizeChange = (val) => {
  pageSize.value = val
  fetchCreditRecords()
}

// 页码变化
const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchCreditRecords()
}
</script>

<style lang="scss" scoped>
.credit-record-management {
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
