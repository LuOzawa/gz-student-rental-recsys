<template>
  <div class="verification-audit-container">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <h2>实名认证审核</h2>
          <el-tag type="warning" effect="dark">
            待审核：{{ total }} 条
          </el-tag>
        </div>
      </template>

      <!-- 空状态 -->
      <el-empty v-if="verifications.length === 0" description="暂无待审核的实名认证申请" />

      <!-- 审核列表 -->
      <el-table v-else :data="verifications" stripe style="width: 100%" v-loading="loading">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column label="用户角色" width="100" align="center">
          <template #default="scope">
            <el-tag v-if="scope.row.roleCode === 'LANDLORD'" type="warning">房东</el-tag>
            <el-tag v-else-if="scope.row.roleCode === 'TENANT'" type="success">租客</el-tag>
            <el-tag v-else type="info">{{ scope.row.roleCode }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="真实姓名" width="120" />
        <el-table-column prop="idCard" label="身份证号" width="180">
          <template #default="scope">
            <span class="id-card">{{ scope.row.idCard }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="联系电话" width="130" />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column label="提交时间" width="170">
          <template #default="scope">
            {{ formatDateTime(scope.row.updateTime || scope.row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="scope">
            <el-button type="success" size="small" @click="approve(scope.row)" :icon="Check">
              通过
            </el-button>
            <el-button type="danger" size="small" @click="showRejectDialog(scope.row)" :icon="Close">
              拒绝
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-if="total > 0"
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="fetchVerifications"
        @size-change="fetchVerifications"
        style="margin-top: 20px; justify-content: center"
      />
    </el-card>

    <!-- 拒绝对话框 -->
    <el-dialog v-model="showReject" title="拒绝实名认证" width="400px">
      <el-form :model="rejectForm" label-width="100px">
        <el-form-item label="拒绝原因">
          <el-input
            v-model="rejectForm.reason"
            type="textarea"
            :rows="4"
            placeholder="请输入拒绝原因"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showReject = false">取消</el-button>
        <el-button type="danger" @click="confirmReject">确认拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Close } from '@element-plus/icons-vue'
import request from '@/utils/request'
import DateUtils from '@/utils/dateUtils'

const verifications = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const showReject = ref(false)
const currentRow = ref(null)

const rejectForm = reactive({
  reason: ''
})

// 格式化日期时间
const formatDateTime = (dateTime) => {
  return DateUtils.formatDateTime(dateTime)
}

// 获取待审核列表
const fetchVerifications = async () => {
  loading.value = true
  try {
    const res = await request.get('/user/verify/pending', {
      currentPage: currentPage.value,
      size: pageSize.value
    })
    verifications.value = res.records || []
    total.value = res.total || 0
  } catch (error) {
    console.error('获取待审核列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 通过
const approve = async (row) => {
  try {
    await ElMessageBox.confirm('确认通过该用户的实名认证吗?', '提示', {
      type: 'warning'
    })

    await request.put(`/user/verify/approve/${row.id}`)
    ElMessage.success('审核通过')
    fetchVerifications()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('审核失败:', error)
    }
  }
}

// 显示拒绝对话框
const showRejectDialog = (row) => {
  currentRow.value = row
  rejectForm.reason = ''
  showReject.value = true
}

// 确认拒绝
const confirmReject = async () => {
  if (!rejectForm.reason.trim()) {
    ElMessage.warning('请输入拒绝原因')
    return
  }

  try {
    await request.put(`/user/verify/reject/${currentRow.value.id}`, null, {
      params: { reason: rejectForm.reason }
    })

    ElMessage.success('审核已拒绝')
    showReject.value = false
    fetchVerifications()
  } catch (error) {
    console.error('拒绝失败:', error)
  }
}

onMounted(() => {
  fetchVerifications()
})
</script>

<style lang="scss" scoped>
.verification-audit-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;

  h2 {
    margin: 0;
    font-size: 18px;
    font-weight: 600;
    color: #1d4ed8;
  }
}

.id-card {
  font-family: monospace;
  letter-spacing: 1px;
}

:deep(.el-table) {
  .el-table__header-wrapper th {
    background-color: #EFF6FF;
    color: #1D4ED8;
  }
}
</style>
