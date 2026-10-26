<template>
  <div class="house-type-container">
    <div class="page-header">
      <h2>房屋类型管理</h2>
      <el-button type="primary" @click="handleAddClick" :icon="Plus">添加房屋类型</el-button>
    </div>

    <!-- 搜索区域 -->
    <div class="search-area">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="类型名称">
          <el-input v-model="searchForm.name" placeholder="请输入类型名称" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch" :icon="Search">搜索</el-button>
          <el-button @click="resetSearch" :icon="Refresh">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 表格区域 -->
    <div class="table-area">
      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        style="width: 100%"
        :header-cell-style="{ backgroundColor: '#EFF6FF', color: '#1D4ED8' }"
      >
        <el-table-column type="index" label="序号" width="80" align="center" />
        <el-table-column prop="name" label="类型名称" width="200" />
        <el-table-column prop="description" label="类型描述" min-width="300" />
        <el-table-column label="创建时间" width="180" align="center">
          <template #default="scope">
            {{ formatDateTime(scope.row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="180" align="center">
          <template #default="scope">
            {{ formatDateTime(scope.row.updateTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center">
          <template #default="scope">
            <el-button
              type="primary"
              size="small"
              text
              @click="handleEdit(scope.row)"
              :icon="Edit"
            >
              编辑
            </el-button>
            <el-button
              type="danger"
              size="small"
              text
              @click="handleDelete(scope.row)"
              :icon="Delete"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页组件 -->
      <div class="pagination-container">
        <el-pagination
          :current-page="currentPage"
          :page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          @update:current-page="val => currentPage = val"
          @update:page-size="val => pageSize = val"
          background
        />
      </div>
    </div>

    <!-- 添加/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogType === 'add' ? '添加房屋类型' : '编辑房屋类型'"
      width="500px"
    >
      <el-form
        :model="form"
        :rules="rules"
        ref="formRef"
        label-width="100px"
        status-icon
      >
        <el-form-item label="类型名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入类型名称" />
        </el-form-item>
        <el-form-item label="类型描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            placeholder="请输入类型描述"
            rows="4"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitLoading">
          确认
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Plus, Edit, Delete, Search, Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import DateUtils from '@/utils/dateUtils'

// 表格数据
const tableData = ref([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

// 将 DateUtils 暴露给模板
const formatDateTime = (date) => {
  return DateUtils.formatDateTime(date);
}

// 搜索表单
const searchForm = reactive({
  name: ''
})

// 添加/编辑表单
const dialogVisible = ref(false)
const dialogType = ref('add') // add 或 edit
const submitLoading = ref(false)
const form = reactive({
  id: null,
  name: '',
  description: ''
})
const formRef = ref(null)
const rules = {
  name: [
    { required: true, message: '请输入类型名称', trigger: 'blur' },
    { min: 2, max: 20, message: '长度在 2 到 20 个字符', trigger: 'blur' }
  ],
  description: [
    { required: true, message: '请输入类型描述', trigger: 'blur' },
    { max: 200, message: '长度不能超过 200 个字符', trigger: 'blur' }
  ]
}

// 生命周期钩子
onMounted(() => {
  fetchData()
})

// 获取表格数据
const fetchData = async () => {
  loading.value = true
  try {
    await request.get('/house-types/page', {
      name: searchForm.name,
      currentPage: currentPage.value,
      size: pageSize.value
    }, {
      onSuccess: (res) => {
        tableData.value = res.records||[]
        total.value = res.total||0
      }
    })
  } catch (error) {
    console.error('获取房屋类型列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  fetchData()
}

// 重置搜索
const resetSearch = () => {
  searchForm.name = ''
  currentPage.value = 1
  fetchData()
}

// 每页数量变化
const handleSizeChange = (val) => {
  pageSize.value = val
  fetchData()
}

// 当前页变化
const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchData()
}

// 添加按钮点击
const handleAddClick = () => {
  dialogType.value = 'add'
  resetForm()
  dialogVisible.value = true
}

// 编辑按钮点击
const handleEdit = (row) => {
  dialogType.value = 'edit'
  resetForm()
  form.id = row.id
  form.name = row.name
  form.description = row.description
  dialogVisible.value = true
}

// 删除按钮点击
const handleDelete = (row) => {
  ElMessageBox.confirm('确认删除该房屋类型吗？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await request.delete(`/house-types/${row.id}`, {
        successMsg: '删除成功',
        errorMsg: '删除失败',
        onSuccess: () => {
          fetchData()
        }
      })
    } catch (error) {
      console.error('删除房屋类型失败:', error)
    }
  }).catch(() => {
    // 取消操作，不做任何处理
  })
}

// 重置表单
const resetForm = () => {
  form.id = null
  form.name = ''
  form.description = ''
  if (formRef.value) {
    formRef.value.resetFields()
  }
}

// 提交表单
const submitForm = () => {
  formRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        if (dialogType.value === 'add') {
          // 添加
          await request.post('/house-types', form, {
            successMsg: '添加成功',
            errorMsg: '添加失败',
            onSuccess: () => {
              dialogVisible.value = false
              fetchData()
            }
          })
        } else {
          // 编辑
          await request.put(`/house-types/${form.id}`, form, {
            successMsg: '更新成功',
            errorMsg: '更新失败',
            onSuccess: () => {
              dialogVisible.value = false
              fetchData()
            }
          })
        }
      } catch (error) {
        console.error(`${dialogType.value === 'add' ? '添加' : '更新'}房屋类型失败:`, error)
      } finally {
        submitLoading.value = false
      }
    }
  })
}
</script>

<style lang="scss" scoped>
.house-type-container {
  padding: 20px;
  
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    
    h2 {
      font-size: 24px;
      color: #1D4ED8;
      margin: 0;
    }
  }
  
  .search-area {
    background: #FFFFFF;
    padding: 20px;
    border-radius: 8px;
    box-shadow: 0 2px 12px rgba(59, 130, 246, 0.08);
    margin-bottom: 20px;
  }
  
  .table-area {
    background: #FFFFFF;
    padding: 20px;
    border-radius: 8px;
    box-shadow: 0 2px 12px rgba(59, 130, 246, 0.08);
  }
  
  .pagination-container {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}
</style> 