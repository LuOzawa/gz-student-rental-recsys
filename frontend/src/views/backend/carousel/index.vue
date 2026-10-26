<template>
  <div class="carousel-container">
    <div class="page-header">
      <h2>轮播图管理</h2>
      <el-button type="primary" @click="handleAdd">添加轮播图</el-button>
    </div>
    
    <!-- 搜索区域 -->
    <div class="search-container">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="标题">
          <el-input v-model="searchForm.title" placeholder="请输入标题" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </div>
    
    <!-- 表格区域 -->
    <el-table
      v-loading="loading"
      :data="tableData"
      border
      style="width: 100%"
    >
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="title" label="标题" min-width="150" show-overflow-tooltip />
      <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
      <el-table-column label="图片" width="120">
        <template #default="scope">
          <el-image
            style="width: 100px; height: 60px"
            :src="formatImageUrl(scope.row.imageUrl)"
            :preview-src-list="[formatImageUrl(scope.row.imageUrl)]"
            :preview-teleported="true"
            fit="cover"
          >
            <template #error>
              <div class="image-error">
                <el-icon><Picture /></el-icon>
              </div>
            </template>
          </el-image>
        </template>
      </el-table-column>
      <el-table-column prop="linkUrl" label="链接地址" min-width="150" show-overflow-tooltip />
      <el-table-column label="状态" width="100">
        <template #default="scope">
          <el-switch
            v-model="scope.row.status"
            :active-value="1"
            :inactive-value="0"
            active-text="启用"
            inactive-text="禁用"
            inline-prompt
            @change="(val) => handleStatusChange(scope.row, val)"
          />
        </template>
      </el-table-column>
      <el-table-column prop="sort" label="排序" width="80" />
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="scope">
          <el-button type="primary" link @click="handleEdit(scope.row)">编辑</el-button>
          <el-popconfirm
            title="确定删除该轮播图吗？"
            @confirm="handleDelete(scope.row.id)"
          >
            <template #reference>
              <el-button type="danger" link>删除</el-button>
            </template>
          </el-popconfirm>
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
    
    <!-- 添加/编辑对话框 -->
    <el-dialog
      :title="dialogType === 'add' ? '添加轮播图' : '编辑轮播图'"
      v-model="dialogVisible"
      width="600px"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入标题" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入描述" />
        </el-form-item>
        <el-form-item label="图片" prop="imageUrl">
          <el-upload
            class="carousel-uploader"
            action="#"
            :auto-upload="true"
            :show-file-list="false"
            :http-request="customUploadImage"
            :before-upload="beforeImageUpload"
          >
            <img v-if="form.imageUrl" :src="formatImageUrl(form.imageUrl)" class="carousel-image" />
            <el-icon v-else class="carousel-uploader-icon"><Plus /></el-icon>
          </el-upload>
          <div class="upload-tip">建议尺寸：1920 x 500 像素，格式：JPG、PNG</div>
        </el-form-item>
        <el-form-item label="链接地址" prop="linkUrl">
          <el-input v-model="form.linkUrl" placeholder="请输入链接地址" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" :max="999" placeholder="数值越小越靠前" />
          <span class="form-tip">数值越小，排序越靠前</span>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm">确认</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Picture, Plus } from '@element-plus/icons-vue'
import request from '@/utils/request'

// 表格数据
const loading = ref(false)
const tableData = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 搜索表单
const searchForm = reactive({
  title: '',
  status: null
})

// 对话框
const dialogVisible = ref(false)
const dialogType = ref('add') // add 或 edit
const formRef = ref(null)
const form = reactive({
  id: null,
  title: '',
  description: '',
  imageUrl: '',
  linkUrl: '',
  sort: 0,
  status: 1
})

// 表单验证规则
const rules = {
  title: [
    { required: true, message: '请输入标题', trigger: 'blur' },
    { max: 100, message: '标题长度不能超过100个字符', trigger: 'blur' }
  ],
  imageUrl: [
    { required: true, message: '请上传图片', trigger: 'change' }
  ],
  linkUrl: [
    { max: 255, message: '链接地址长度不能超过255个字符', trigger: 'blur' }
  ]
}

// 基础API路径
const baseAPI = import.meta.env.VITE_BASE_API || '/api'

// 初始化
onMounted(() => {
  fetchCarousels()
})

// 获取轮播图列表
const fetchCarousels = async () => {
  loading.value = true
  try {
    await request.get('/admin/carousels', {
      title: searchForm.title,
      status: searchForm.status,
      currentPage: currentPage.value,
      size: pageSize.value
    }, {
      onSuccess: (res) => {
        tableData.value = res.records||[]
        total.value = res.total||0
      }
    })
  } catch (error) {
    console.error('获取轮播图列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  fetchCarousels()
}

// 重置搜索
const resetSearch = () => {
  searchForm.title = ''
  searchForm.status = null
  handleSearch()
}

// 处理分页大小变化
const handleSizeChange = (val) => {
  pageSize.value = val
  fetchCarousels()
}

// 处理页码变化
const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchCarousels()
}

// 添加轮播图
const handleAdd = () => {
  dialogType.value = 'add'
  resetForm()
  dialogVisible.value = true
}

// 编辑轮播图
const handleEdit = (row) => {
  dialogType.value = 'edit'
  resetForm()
  Object.assign(form, row)
  dialogVisible.value = true
}

// 重置表单
const resetForm = () => {
  form.id = null
  form.title = ''
  form.description = ''
  form.imageUrl = ''
  form.linkUrl = ''
  form.sort = 0
  form.status = 1

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
          await request.post('/admin/carousels', form, {
            successMsg: '添加成功'
          })
        } else {
          await request.put(`/admin/carousels/${form.id}`, form, {
            successMsg: '更新成功'
          })
        }
        dialogVisible.value = false
        fetchCarousels()
      } catch (error) {
        console.error('保存轮播图失败:', error)
      }
    }
  })
}

// 删除轮播图
const handleDelete = async (id) => {
  try {
    await request.delete(`/admin/carousels/${id}`, {
      successMsg: '删除成功'
    })
    fetchCarousels()
  } catch (error) {
    console.error('删除轮播图失败:', error)
  }
}

// 切换状态
const handleStatusChange = async (row, newStatus) => {
  try {
    await request.put(`/admin/carousels/${row.id}/status`, null, {
      params: { status: newStatus },
      successMsg: newStatus === 1 ? '已启用' : '已禁用'
    })
    // 不需要重新获取，因为 v-model 已经更新了本地状态
  } catch (error) {
    console.error('更新状态失败:', error)
    // 失败时恢复原状态
    row.status = newStatus === 1 ? 0 : 1
  }
}

// 格式化图片URL
const formatImageUrl = (path) => {
  if (!path) return ''
  if (path.startsWith('http')) return path
  return baseAPI + path
}

// 图片上传前的校验
const beforeImageUpload = (file) => {
  const isJPG = file.type === 'image/jpeg'
  const isPNG = file.type === 'image/png'
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isJPG && !isPNG) {
    ElMessage.error('图片只能是 JPG 或 PNG 格式!')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB!')
    return false
  }
  return true
}

// 自定义图片上传方法
const customUploadImage = async (options) => {
  try {
    const { file } = options

    // 创建 FormData 对象
    const formData = new FormData()
    formData.append('file', file)

    // 设置自定义上传选项
    const uploadOptions = {
      headers: {
        token: localStorage.getItem('token') || '',
      },
      // 不进行JSON处理
      transformRequest: [(data) => data],
      // 自定义成功消息
      successMsg: '图片上传成功',
      // 自定义错误消息
      errorMsg: '图片上传失败',
      // 成功回调
      onSuccess: async (data) => {
        // 更新表单中的图片URL
        form.imageUrl = data

        // 通知上传成功
        options.onSuccess({ data })
      },
      // 错误回调
      onError: (error) => {
        console.error('图片上传错误:', error)
        options.onError(new Error(error.message || '上传失败'))
      },
    }

    // 发送上传请求
    await request.post('/file/upload/img', formData, uploadOptions)
  } catch (error) {
    options.onError(error)
    console.error('图片上传过程发生错误:', error)
  }
}
</script>

<style lang="scss" scoped>
.carousel-container {
  padding: 20px;
  
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    
    h2 {
      margin: 0;
      font-size: 22px;
      color: #303133;
    }
  }
  
  .search-container {
    margin-bottom: 20px;
    padding: 18px;
    background-color: #fff;
    border-radius: 4px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
  }
  
  .pagination-container {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
  
  .carousel-uploader {
    border: 1px dashed #d9d9d9;
    border-radius: 6px;
    cursor: pointer;
    position: relative;
    overflow: hidden;
    width: 300px;
    height: 160px;
    display: flex;
    justify-content: center;
    align-items: center;
    
    &:hover {
      border-color: #409EFF;
    }
    
    .carousel-image {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
    
    .carousel-uploader-icon {
      font-size: 28px;
      color: #8c939d;
      width: 40px;
      height: 40px;
    }
  }
  
  .upload-tip {
    color: #909399;
    font-size: 12px;
    margin-top: 5px;
  }

  .form-tip {
    color: #909399;
    font-size: 12px;
    margin-left: 10px;
  }
  
  .image-error {
    display: flex;
    justify-content: center;
    align-items: center;
    width: 100%;
    height: 100%;
    background-color: #f5f7fa;
    color: #c0c4cc;
    
    .el-icon {
      font-size: 24px;
    }
  }
}

// 修复图片预览层级问题
:deep(.el-image-viewer__wrapper) {
  position: fixed;
  z-index: 9999 !important;
}

:deep(.el-image-viewer__mask) {
  position: fixed;
  z-index: 9998 !important;
}
</style> 