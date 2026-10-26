<template>
  <div class="profile-container">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>个人信息</span>
        </div>
      </template>
      
      <el-form :model="userForm" label-width="120px">
        <el-form-item label="用户名">
          <el-input v-model="userForm.username" disabled />
        </el-form-item>
        
        <el-form-item label="姓名">
          <el-input v-model="userForm.name" />
        </el-form-item>
        
        <el-form-item label="角色">
          <el-tag v-if="userStore.role === 'admin'" type="danger">管理员</el-tag>
          <el-tag v-else-if="userStore.role === 'manager'" type="warning">经理</el-tag>
          <el-tag v-else type="success">普通用户</el-tag>
        </el-form-item>
        
        <el-form-item label="电子邮箱">
          <el-input v-model="userForm.email" />
        </el-form-item>
        
        <el-form-item label="手机号码">
          <el-input v-model="userForm.phone" />
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" @click="handleUpdateInfo">更新信息</el-button>
          <el-button @click="handleChangePassword">修改密码</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    
    <!-- 修改密码对话框 -->
    <el-dialog v-model="passwordDialogVisible" title="修改密码" width="30%">
      <el-form :model="passwordForm" label-width="120px">
        <el-form-item label="当前密码">
          <el-input v-model="passwordForm.oldPassword" type="password" />
        </el-form-item>
        
        <el-form-item label="新密码">
          <el-input v-model="passwordForm.newPassword" type="password" />
        </el-form-item>
        
        <el-form-item label="确认新密码">
          <el-input v-model="passwordForm.confirmPassword" type="password" />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="passwordDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitChangePassword">确认</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()

// 用户信息表单
const userForm = reactive({
  username: userStore.username || '',
  name: userStore.name || '',
  email: userStore.email || '',
  phone: userStore.phone || ''
})

// 密码表单和对话框
const passwordDialogVisible = ref(false)
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 加载用户信息
onMounted(() => {
  // 实际开发中，可以在这里调用API获取完整的用户信息
  console.log('加载用户信息')
})

// 更新用户信息
const handleUpdateInfo = () => {
  // 实际开发中，这里应该调用API更新用户信息
  ElMessage.success('个人信息更新成功')
}

// 打开修改密码对话框
const handleChangePassword = () => {
  passwordDialogVisible.value = true
}

// 提交修改密码
const submitChangePassword = () => {
  if (!passwordForm.oldPassword || !passwordForm.newPassword || !passwordForm.confirmPassword) {
    ElMessage.warning('请填写完整的密码信息')
    return
  }
  
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.error('两次输入的新密码不一致')
    return
  }
  
  // 实际开发中，这里应该调用API修改密码
  ElMessage.success('密码修改成功')
  passwordDialogVisible.value = false
  
  // 重置表单
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
}
</script>

<style lang="scss" scoped>
.profile-container {
  padding: 20px 0;
  
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  
  .box-card {
    max-width: 800px;
    margin: 0 auto;
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style> 