<template>
  <div class="person-info">
    <el-card class="info-card">
      <template #header>
        <div class="card-header">
          <span>个人信息</span>
          <el-button type="primary" @click="handleEdit" v-if="!isEditing">
            编辑信息
          </el-button>
          <div v-else>
            <el-button type="primary" @click="handleSave" :loading="saving">
              保存
            </el-button>
            <el-button @click="handleCancel">取消</el-button>
          </div>
        </div>
      </template>

      <div class="info-content">
        <!-- 添加头像上传部分 -->
        <div class="avatar-container">
          <el-avatar :size="100" :src="avatarUrl" @error="() => false" />
          <el-upload
            class="avatar-uploader"
            action="#"
            :auto-upload="true"
            :show-file-list="false"
            :http-request="customUploadAvatar"
            :before-upload="beforeAvatarUpload"
            :disabled="!isEditing"
          >
            <el-button size="small" type="primary" :disabled="!isEditing">更换头像</el-button>
          </el-upload>
        </div>

        <el-form 
          ref="formRef"
          :model="form"
          :rules="rules"
          label-width="100px"
          :disabled="!isEditing"
          class="info-form"
        >
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" disabled />
          </el-form-item>

          <el-form-item label="姓名" prop="name">
            <el-input
              v-model="form.name"
              placeholder="请输入姓名"
              :disabled="verifyStatus === 2"
            />
            <div v-if="verifyStatus === 2" class="field-tip verified-tip">
              <el-icon><Lock /></el-icon>
              已通过实名认证，无法修改
            </div>
          </el-form-item>

          <!-- 添加性别选择 -->
          <el-form-item label="性别" prop="sex">
            <el-radio-group v-model="form.sex">
              <el-radio value="男">男</el-radio>
              <el-radio value="女">女</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="邮箱" prop="email">
            <el-input v-model="form.email" placeholder="请输入邮箱" />
          </el-form-item>

          <el-form-item label="手机号" prop="phone">
            <el-input v-model="form.phone" placeholder="请输入手机号" />
          </el-form-item>

          <el-form-item label="身份证号" prop="idCard">
            <el-input
              v-model="form.idCard"
              placeholder="请通过实名认证填写"
              disabled
            />
            <div class="field-tip">
              <template v-if="verifyStatus === 2">
                <el-icon><Lock /></el-icon>
                已通过实名认证
              </template>
              <template v-else>
                请在下方"实名认证"区域填写身份证号
              </template>
            </div>
          </el-form-item>
        </el-form>
      </div>
    </el-card>

    <!-- 实名认证卡片 -->
    <el-card class="verify-card">
      <template #header>
        <div class="card-header">
          <span>实名认证</span>
          <el-tag :type="getVerifyStatusType(verifyStatus)" effect="dark">
            {{ getVerifyStatusText(verifyStatus) }}
          </el-tag>
        </div>
      </template>

      <div class="verify-content">
        <!-- 未认证状态 -->
        <template v-if="verifyStatus === 0">
          <el-alert
            title="您还未完成实名认证"
            type="warning"
            :closable="false"
            show-icon
          >
            <template #default>
              <p>完成实名认证后，您可以：</p>
              <ul>
                <li>发布房源信息</li>
                <li>获得更高的信任度</li>
                <li>享受更多平台功能</li>
              </ul>
            </template>
          </el-alert>

          <el-form
            ref="verifyFormRef"
            :model="verifyForm"
            :rules="verifyRules"
            label-width="100px"
            class="verify-form"
          >
            <el-form-item label="真实姓名" prop="name">
              <el-input v-model="verifyForm.name" placeholder="请输入真实姓名" />
            </el-form-item>
            <el-form-item label="身份证号" prop="idCard">
              <el-input v-model="verifyForm.idCard" placeholder="请输入身份证号" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="submitVerification" :loading="submittingVerify">
                提交认证
              </el-button>
            </el-form-item>
          </el-form>
        </template>

        <!-- 待审核状态 -->
        <template v-else-if="verifyStatus === 1">
          <el-result icon="info" title="认证审核中" sub-title="您的实名认证申请正在审核中，请耐心等待">
            <template #extra>
              <el-descriptions :column="1" border>
                <el-descriptions-item label="提交姓名">{{ form.name }}</el-descriptions-item>
                <el-descriptions-item label="身份证号">{{ maskIdCard(form.idCard) }}</el-descriptions-item>
              </el-descriptions>
            </template>
          </el-result>
        </template>

        <!-- 已认证状态 -->
        <template v-else-if="verifyStatus === 2">
          <el-result icon="success" title="已完成实名认证" sub-title="您已通过实名认证，可以正常使用所有功能">
            <template #extra>
              <el-descriptions :column="1" border>
                <el-descriptions-item label="认证姓名">{{ form.name }}</el-descriptions-item>
                <el-descriptions-item label="身份证号">{{ maskIdCard(form.idCard) }}</el-descriptions-item>
              </el-descriptions>
            </template>
          </el-result>
        </template>

        <!-- 认证被拒绝 -->
        <template v-else-if="verifyStatus === 3">
          <el-result icon="error" title="认证未通过" :sub-title="verifyRejectReason || '您的实名认证申请未通过审核'">
            <template #extra>
              <el-form
                ref="verifyFormRef"
                :model="verifyForm"
                :rules="verifyRules"
                label-width="100px"
                class="verify-form"
              >
                <el-form-item label="真实姓名" prop="name">
                  <el-input v-model="verifyForm.name" placeholder="请输入真实姓名" />
                </el-form-item>
                <el-form-item label="身份证号" prop="idCard">
                  <el-input v-model="verifyForm.idCard" placeholder="请输入身份证号" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="submitVerification" :loading="submittingVerify">
                    重新提交
                  </el-button>
                </el-form-item>
              </el-form>
            </template>
          </el-result>
        </template>
      </div>
    </el-card>

    <el-card class="password-card">
      <template #header>
        <div class="card-header">
          <span>修改密码</span>
        </div>
      </template>

      <el-form
        ref="passwordFormRef"
        :model="passwordForm"
        :rules="passwordRules"
        label-width="100px"
      >
        <el-form-item label="原密码" prop="oldPassword">
          <el-input 
            v-model="passwordForm.oldPassword" 
            type="password"
            placeholder="请输入原密码"
            show-password
          />
        </el-form-item>

        <el-form-item label="新密码" prop="newPassword">
          <el-input 
            v-model="passwordForm.newPassword" 
            type="password"
            placeholder="请输入新密码"
            show-password
          />
        </el-form-item>

        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input 
            v-model="passwordForm.confirmPassword" 
            type="password"
            placeholder="请再次输入新密码"
            show-password
          />
        </el-form-item>

        <el-form-item>
          <el-button 
            type="primary" 
            @click="handleChangePassword"
            :loading="changingPassword"
          >
            修改密码
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import request from '@/utils/request'

const baseAPI = import.meta.env.VITE_BASE_API || '/api'
const userStore = useUserStore()
const formRef = ref(null)
const passwordFormRef = ref(null)
const verifyFormRef = ref(null)
const isEditing = ref(false)
const saving = ref(false)
const changingPassword = ref(false)
const submittingVerify = ref(false)

// 实名认证状态
const verifyStatus = ref(0)
const verifyRejectReason = ref('')

// 实名认证表单
const verifyForm = reactive({
  name: '',
  idCard: ''
})

// 实名认证验证规则
const verifyRules = {
  name: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' },
    { min: 2, max: 20, message: '姓名长度在 2 到 20 个字符', trigger: 'blur' }
  ],
  idCard: [
    { required: true, message: '请输入身份证号', trigger: 'blur' },
    { pattern: /(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/, message: '请输入正确的身份证号', trigger: 'blur' }
  ]
}

// 获取认证状态类型
const getVerifyStatusType = (status) => {
  switch (status) {
    case 0: return 'info'
    case 1: return 'warning'
    case 2: return 'success'
    case 3: return 'danger'
    default: return 'info'
  }
}

// 获取认证状态文字
const getVerifyStatusText = (status) => {
  switch (status) {
    case 0: return '未认证'
    case 1: return '待审核'
    case 2: return '已认证'
    case 3: return '认证失败'
    default: return '未知'
  }
}

// 身份证号脱敏显示
const maskIdCard = (idCard) => {
  if (!idCard || idCard.length < 8) return idCard
  return idCard.substring(0, 4) + '**********' + idCard.substring(idCard.length - 4)
}

// 表单数据
const form = reactive({
  id: '',
  username: '',
  name: '',
  email: '',
  phone: '',
  sex: '男',
  avatar: '',
  idCard: ''
})

// 头像地址
const avatarUrl = computed(() => {
  return form.avatar ? baseAPI + form.avatar : '';
})

// 密码表单
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 表单验证规则
const rules = {
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  sex: [
    { required: true, message: '请选择性别', trigger: 'change' }
  ],
  idCard: [
    { pattern: /(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/, message: '请输入正确的身份证号', trigger: 'blur' }
  ]
}

// 密码验证规则
const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入原密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 获取用户信息
const fetchUserInfo = async () => {
  try {
    // 获取当前用户的最新信息
    const userId = userStore.userInfo.id
    const res = await request.get(`/user/${userId}`, null, {
      showDefaultMsg: false
    })

    // 直接更新表单数据
    form.id = res.id || userStore.userInfo.id
    form.username = res.username || ''
    form.name = res.name || ''
    form.email = res.email || ''
    form.phone = res.phone || ''
    form.sex = res.sex || '男'
    form.avatar = res.avatar || ''
    form.idCard = res.idCard || ''

    // 更新实名认证状态
    verifyStatus.value = res.verifyStatus || 0
    verifyRejectReason.value = res.verifyRejectReason || ''

    // 预填充认证表单
    if (res.name) verifyForm.name = res.name
    if (res.idCard) verifyForm.idCard = res.idCard

    console.log('用户信息加载成功:', form)
  } catch (error) {
    console.error('获取用户信息失败:', error)
    ElMessage.error('获取用户信息失败')
  }
}

// 提交实名认证
const submitVerification = async () => {
  if (!verifyFormRef.value) return

  try {
    await verifyFormRef.value.validate()
    submittingVerify.value = true

    await request.post('/user/verify/submit', {
      name: verifyForm.name,
      idCard: verifyForm.idCard
    }, {
      successMsg: '实名认证申请已提交，请等待审核',
      onSuccess: () => {
        // 刷新用户信息
        fetchUserInfo()
      }
    })
  } catch (error) {
    console.error('提交实名认证失败:', error)
  } finally {
    submittingVerify.value = false
  }
}

// 上传头像前的校验
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
const customUploadAvatar = async (options) => {
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
        // 更新用户头像
        form.avatar = data

        // 保存到后端
        await updateUserAvatar(data)

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

// 更新用户头像信息
const updateUserAvatar = async (avatarPath) => {
  try {
    await request.put(
      `/user/${form.id}`,
      { avatar: avatarPath },
      {
        showDefaultMsg: false,
        successMsg: '头像更新成功',
        onSuccess: (data) => {
          // 更新本地用户信息
          const updatedUserInfo = { ...userStore.userInfo, avatar: avatarPath }
          userStore.updateUserInfo(updatedUserInfo)
        },
        onError: (error) => {
          console.error('头像信息保存失败', error)
          ElMessage.error('头像信息保存失败')
        },
      }
    )
  } catch (error) {
    console.error('头像信息保存失败', error)
    ElMessage.error('头像信息保存失败')
    throw error
  }
}

// 编辑信息
const handleEdit = () => {
  isEditing.value = true
}

// 取消编辑
const handleCancel = () => {
  isEditing.value = false
  fetchUserInfo() // 重新获取数据，恢复原值
}

// 保存信息
const handleSave = async () => {
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
    saving.value = true
    
    await request.put(
      `/user/${form.id}`,
      {
        name: form.name,
        email: form.email,
        phone: form.phone,
        sex: form.sex,
        idCard: form.idCard
      },
      {
        showDefaultMsg: false,
        successMsg: '个人信息更新成功',
        onSuccess: (data) => {
          isEditing.value = false
          // 更新store中的用户信息
          userStore.updateUserInfo({
            ...userStore.userInfo,
            name: form.name,
            email: form.email,
            phone: form.phone,
            sex: form.sex,
            idCard: form.idCard
          })
        },
        onError: (error) => {
          console.error('更新用户信息失败:', error)
          ElMessage.error('更新用户信息失败')
        }
      }
    )
  } catch (error) {
    console.error('更新用户信息失败:', error)
    ElMessage.error('更新用户信息失败')
  } finally {
    saving.value = false
  }
}

// 修改密码
const handleChangePassword = async () => {
  if (!passwordFormRef.value) return

  try {
    await passwordFormRef.value.validate()
    changingPassword.value = true

    await request.put(
      `/user/password/${form.id}`,
      {
        oldPassword: passwordForm.oldPassword,
        newPassword: passwordForm.newPassword
      },
      {
        showDefaultMsg: false,
        successMsg: '密码修改成功',
        onSuccess: (data) => {
          // 清空密码表单
          passwordFormRef.value.resetFields()
          
          // 提示用户重新登录
          ElMessageBox.confirm('密码已修改，需要重新登录', '提示', {
            confirmButtonText: '重新登录',
            cancelButtonText: '取消',
            type: 'warning',
          }).then(() => {
            // 清除用户信息并跳转到登录页
            userStore.clearUserInfo()
            window.location.href = '/login'
          })
        },
        onError: (error) => {
          console.error('密码修改失败', error)
          ElMessage.error('密码修改失败')
        }
      }
    )
  } catch (error) {
    console.error('修改密码失败:', error)
    ElMessage.error('修改密码失败')
  } finally {
    changingPassword.value = false
  }
}

onMounted(() => {
  fetchUserInfo()
})
</script>

<style lang="scss" scoped>
.person-info {
  padding: 20px;

  .info-card,
  .password-card,
  .verify-card {
    margin-bottom: 20px;
    border-radius: 8px;

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
  }

  .verify-card {
    .verify-content {
      .el-alert {
        margin-bottom: 20px;

        ul {
          margin: 10px 0 0 20px;
          padding: 0;

          li {
            line-height: 1.8;
          }
        }
      }

      .verify-form {
        max-width: 400px;
        margin-top: 20px;
      }

      .el-result {
        padding: 20px 0;
      }
    }
  }

  .info-content {
    display: flex;
    flex-direction: column;
    gap: 30px;
    
    @media (min-width: 768px) {
      flex-direction: row;
    }
  }
  
  .avatar-container {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 15px;
    
    .avatar-uploader {
      text-align: center;
      margin-top: 10px;
    }
  }
  
  .info-form {
    flex: 1;
    max-width: 500px;
    margin: 0 auto;
  }

  :deep(.el-form) {
    max-width: 500px;
    margin: 0 auto;
  }

  .field-tip {
    font-size: 12px;
    margin-top: 5px;
    display: flex;
    align-items: center;
    gap: 4px;

    &.verified-tip {
      color: #67C23A;

      .el-icon {
        font-size: 14px;
      }
    }
  }
}
</style> 