<template>
  <div class="user-list">
    <!-- 搜索区域 -->
    <div class="search-container">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="用户名">
          <el-input v-model="searchForm.username" placeholder="请输入用户名" clearable />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="searchForm.name" placeholder="请输入姓名" clearable />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="searchForm.roleCode" placeholder="请选择角色" clearable>
            <el-option label="租户" value="TENANT" />
            <el-option label="房东" value="LANDLORD" />
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
      <el-button type="primary" @click="handleAdd">添加用户</el-button>
      <el-button type="danger" @click="handleBatchDelete" :disabled="selectedIds.length === 0">批量删除</el-button>
    </div>

    <!-- 表格 -->
    <el-table
      v-loading="loading"
      :data="tableData"
      @selection-change="handleSelectionChange"
      border
    >
      <el-table-column type="selection" width="55" />
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" min-width="120" />
      <el-table-column prop="name" label="姓名" min-width="120" />
      <el-table-column prop="roleCode" label="角色" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.roleCode === 'LANDLORD' ? 'success' : 'info'">
            {{ scope.row.roleCode === 'LANDLORD' ? '房东' : scope.row.roleCode === 'TENANT' ? '租户' : '未知' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sex" label="性别" width="80">
        <template #default="scope">
          {{ scope.row.sex === 'male' ? '男' : scope.row.sex === 'female' ? '女' : '未知' }}
        </template>
      </el-table-column>
      <el-table-column prop="email" label="邮箱" min-width="180" />
      <el-table-column prop="phone" label="手机号" width="120" />
      <el-table-column prop="idCard" label="身份证号" min-width="180" show-overflow-tooltip />
      <el-table-column label="实名认证" width="100">
        <template #default="scope">
          <el-tag :type="getVerifyStatusType(scope.row.verifyStatus)">
            {{ getVerifyStatusText(scope.row.verifyStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="头像" width="80">
        <template #default="scope">
          <el-avatar :size="40" :src="scope.row.avatar ? baseAPI + scope.row.avatar : ''">
            <img src="https://cube.elemecdn.com/e/fd/0fc7d20532fdaf769a25683617711png.png"/>
          </el-avatar>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">
            {{ scope.row.status === 1 ? '正常' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="250" fixed="right">
        <template #default="scope">
          <el-button type="primary" link @click="handleEdit(scope.row)">编辑</el-button>
          <el-button 
            type="primary" 
            link 
            @click="handleToggleStatus(scope.row)"
          >
            {{ scope.row.status === 1 ? '禁用' : '启用' }}
          </el-button>
          <el-button type="primary" link @click="handleResetPassword(scope.row)">重置密码</el-button>
          <el-popconfirm
            title="确定删除该用户吗？"
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
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <!-- 添加/编辑对话框 -->
    <el-dialog
      :title="dialogType === 'add' ? '添加用户' : '编辑用户'"
      v-model="dialogVisible"
      width="500px"
      @closed="handleDialogClosed"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" :disabled="dialogType === 'edit'" />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="请输入姓名" :disabled="isUserVerified" />
          <div v-if="isUserVerified" class="verified-tip">
            <el-icon><Lock /></el-icon> 该用户已通过实名认证，无法修改
          </div>
        </el-form-item>
        <el-form-item label="角色" prop="roleCode">
          <el-radio-group v-model="form.roleCode">
            <el-radio value="TENANT">租户</el-radio>
            <el-radio value="LANDLORD">房东</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="性别" prop="sex">
          <el-radio-group v-model="form.sex">
            <el-radio value="male">男</el-radio>
            <el-radio value="female">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="身份证号" prop="idCard">
          <el-input v-model="form.idCard" placeholder="身份证号通过实名认证填写" disabled />
          <div class="verified-tip">
            <template v-if="isUserVerified">
              <el-icon><Lock /></el-icon> 该用户已通过实名认证
            </template>
            <template v-else>
              身份证号需用户通过实名认证提交
            </template>
          </div>
        </el-form-item>
        <el-form-item label="头像" prop="avatar">
          <el-upload
            class="avatar-uploader"
            action="#"
            :auto-upload="true"
            :show-file-list="false"
            :http-request="customUploadImage"
            :before-upload="beforeAvatarUpload"
          >
            <img v-if="form.avatar" :src="baseAPI + form.avatar" class="avatar" />
            <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
          </el-upload>
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="dialogType === 'add'">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Lock } from '@element-plus/icons-vue'
import request from '@/utils/request'

// API基础路径
const baseAPI = import.meta.env.VITE_BASE_API || '/api'

// 表格数据
const loading = ref(false)
const tableData = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const selectedIds = ref([])

// 搜索表单
const searchForm = reactive({
  username: '',
  name: '',
  roleCode: ''
})

// 对话框
const dialogVisible = ref(false)
const dialogType = ref('add')
const formRef = ref(null)
const form = reactive({
  id: null,
  username: '',
  name: '',
  sex: '',
  email: '',
  phone: '',
  idCard: '',
  avatar: '',
  password: '',
  status: 1,
  roleCode: 'TENANT', // 默认为租户
  verifyStatus: 0 // 实名认证状态
})

// 判断当前编辑的用户是否已通过实名认证
const isUserVerified = computed(() => {
  return dialogType.value === 'edit' && form.verifyStatus === 2
})

// 获取实名认证状态类型（用于标签颜色）
const getVerifyStatusType = (status) => {
  const typeMap = {
    0: 'info',     // 未认证
    1: 'warning',  // 待审核
    2: 'success',  // 已认证
    3: 'danger'    // 审核拒绝
  }
  return typeMap[status] || 'info'
}

// 获取实名认证状态文本
const getVerifyStatusText = (status) => {
  const textMap = {
    0: '未认证',
    1: '待审核',
    2: '已认证',
    3: '已拒绝'
  }
  return textMap[status] || '未知'
}

// 表单验证规则
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '长度在 3 到 50 个字符', trigger: 'blur' }
  ],
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' }
  ],
  roleCode: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  idCard: [
    { pattern: /(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/, message: '请输入正确的身份证号', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
  ]
}

// 头像上传前的校验
const beforeAvatarUpload = (file) => {
  const isJPG = file.type === 'image/jpeg'
  const isPNG = file.type === 'image/png'
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isJPG && !isPNG) {
    ElMessage.error('头像只能是 JPG 或 PNG 格式!')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('头像大小不能超过 2MB!')
    return false
  }
  return true
}

// 自定义头像上传方法
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
      successMsg: '头像上传成功',
      // 自定义错误消息
      errorMsg: '头像上传失败',
      // 成功回调
      onSuccess: async (data) => {
        // 更新表单中的头像URL
        form.avatar = data

        // 通知上传成功
        options.onSuccess({ data })
      },
      // 错误回调
      onError: (error) => {
        console.error('头像上传错误:', error)
        options.onError(new Error(error.message || '上传失败'))
      },
    }

    // 发送上传请求
    await request.post('/file/upload/img', formData, uploadOptions)
  } catch (error) {
    options.onError(error)
    console.error('头像上传过程发生错误:', error)
  }
}

// 初始化
onMounted(() => {
  fetchUsers()
})

// 获取用户列表
const fetchUsers = async () => {
  loading.value = true
  try {
    const res = await request.get('/user/page', {
      username: searchForm.username,
      name: searchForm.name,
      roleCode: searchForm.roleCode,
      currentPage: currentPage.value,
      size: pageSize.value
    })
    tableData.value = res.records||[]
    total.value = res.total||0
  } catch (error) {
    console.error('获取用户列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  fetchUsers()
}

// 重置搜索
const resetSearch = () => {
  searchForm.username = ''
  searchForm.name = ''
  searchForm.roleCode = ''
  handleSearch()
}

// 选择变化
const handleSelectionChange = (selection) => {
  selectedIds.value = selection.map(item => item.id)
}

// 添加用户
const handleAdd = () => {
  dialogType.value = 'add'
  resetForm()
  dialogVisible.value = true
}

// 编辑用户
const handleEdit = (row) => {
  dialogType.value = 'edit'
  resetForm()
  Object.assign(form, row)
  dialogVisible.value = true
}

// 重置表单
const resetForm = () => {
  form.id = null
  form.username = ''
  form.name = ''
  form.sex = ''
  form.email = ''
  form.phone = ''
  form.idCard = ''
  form.avatar = ''
  form.password = ''
  form.status = 1
  form.roleCode = 'TENANT'
  form.verifyStatus = 0

  if (formRef.value) {
    formRef.value.resetFields()
  }
}

// 添加对话框关闭处理函数
const handleDialogClosed = () => {
  resetForm()
}

// 提交表单
const submitForm = () => {
  formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        if (dialogType.value === 'add') {
          await request.post('/user/add', form)
          ElMessage.success('添加成功')
        } else {
          await request.put(`/user/${form.id}`, form)
          ElMessage.success('更新成功')
        }
        dialogVisible.value = false
        fetchUsers()
      } catch (error) {
        console.error('保存用户失败:', error)
      }
    }
  })
}

// 删除用户
const handleDelete = async (id) => {
  try {
    await request.delete(`/user/delete/${id}`)
    ElMessage.success('删除成功')
    fetchUsers()
  } catch (error) {
    console.error('删除用户失败:', error)
  }
}

// 批量删除
const handleBatchDelete = async () => {
  if (selectedIds.value.length === 0) {
    ElMessage.warning('请选择要删除的用户')
    return
  }
  
  try {
    await ElMessageBox.confirm('确定要删除选中的用户吗？', '提示', {
      type: 'warning'
    })
    await request.delete('/user/deleteBatch', {
      params: { ids: selectedIds.value }
    })
    ElMessage.success('批量删除成功')
    fetchUsers()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量删除失败:', error)
    }
  }
}

// 切换状态
const handleToggleStatus = async (row) => {
  const newStatus = row.status === 1 ? 0 : 1
  try {
    await request.put(`/user/status/${row.id}`, null, {
      params: { status: newStatus }
    })
    ElMessage.success(newStatus === 1 ? '已启用' : '已禁用')
    fetchUsers()
  } catch (error) {
    console.error('更新状态失败:', error)
  }
}

// 重置密码
const handleResetPassword = async (row) => {
  try {
    await ElMessageBox.prompt('请输入新密码', '重置密码', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputType: 'password',
      inputPattern: /.{6,20}/,
      inputErrorMessage: '密码长度在6-20个字符之间'
    })
    .then(async ({ value }) => {
      await request.get('/user/forget', {
        params: {
          email: row.email,
          newPassword: value
        }
      })
      ElMessage.success('密码重置成功')
    })
  } catch (error) {
    if (error !== 'cancel') {
      console.error('重置密码失败:', error)
    }
  }
}

// 分页大小变化
const handleSizeChange = (val) => {
  pageSize.value = val
  fetchUsers()
}

// 页码变化
const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchUsers()
}
</script>

<style lang="scss" scoped>
.user-list {
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

  .avatar-uploader {
    border: 1px dashed #d9d9d9;
    border-radius: 6px;
    cursor: pointer;
    position: relative;
    overflow: hidden;
    width: 178px;
    height: 178px;
    
    &:hover {
      border-color: #409EFF;
    }
    
    .avatar {
      width: 178px;
      height: 178px;
      display: block;
    }
    
    .avatar-uploader-icon {
      font-size: 28px;
      color: #8c939d;
      width: 178px;
      height: 178px;
      text-align: center;
      line-height: 178px;
    }
  }

  .verified-tip {
    display: flex;
    align-items: center;
    gap: 4px;
    margin-top: 5px;
    color: #e6a23c;
    font-size: 12px;

    .el-icon {
      font-size: 14px;
    }
  }
}
</style> 