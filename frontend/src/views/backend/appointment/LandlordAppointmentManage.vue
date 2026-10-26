<template>
  <div class="appointment-manage-container">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <h2>预约管理</h2>
          <el-radio-group v-model="filterStatus" @change="fetchAppointments">
            <el-radio-button value="">全部</el-radio-button>
            <el-radio-button :value="0">待确认</el-radio-button>
            <el-radio-button :value="1">已确认</el-radio-button>
            <el-radio-button :value="2">已拒绝</el-radio-button>
            <el-radio-button :value="3">已完成</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <el-table :data="appointments" stripe>
        <el-table-column prop="house.title" label="房源" width="200" />
        <el-table-column prop="tenant.name" label="租客姓名" width="100" />
        <el-table-column prop="contactPhone" label="联系电话" width="120" />
        <el-table-column prop="appointmentDate" label="预约日期" width="120" />
        <el-table-column prop="appointmentTime" label="预约时间" width="150" />
        <el-table-column prop="message" label="留言" min-width="150" />
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag v-if="scope.row.status === 0" type="warning">待确认</el-tag>
            <el-tag v-else-if="scope.row.status === 1" type="success">已确认</el-tag>
            <el-tag v-else-if="scope.row.status === 2" type="danger">已拒绝</el-tag>
            <el-tag v-else-if="scope.row.status === 3" type="info">已完成</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="rejectReason" label="拒绝原因" width="150" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="scope">
            <el-button
              type="info"
              size="small"
              @click="chatWithTenant(scope.row)"
              icon="ChatDotRound"
            >
              联系租客
            </el-button>
            <template v-if="scope.row.status === 0">
              <el-button type="success" size="small" @click="confirmAppointment(scope.row)">
                确认
              </el-button>
              <el-button type="danger" size="small" @click="showRejectDialog(scope.row)">
                拒绝
              </el-button>
            </template>
            <template v-else-if="scope.row.status === 1">
              <el-button type="primary" size="small" @click="completeAppointment(scope.row)">
                完成看房
              </el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="fetchAppointments"
        style="margin-top: 20px; justify-content: center"
      />
    </el-card>

    <!-- 拒绝对话框 -->
    <el-dialog v-model="showReject" title="拒绝预约" width="400px">
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
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ChatDotRound } from '@element-plus/icons-vue'
import request from '@/utils/request'

const router = useRouter()

const appointments = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const filterStatus = ref('')
const showReject = ref(false)
const currentRow = ref(null)

const rejectForm = reactive({
  reason: ''
})

// 获取预约列表
const fetchAppointments = async () => {
  try {
    const params = {
      currentPage: currentPage.value,
      size: pageSize.value
    }
    if (filterStatus.value !== '') {
      params.status = filterStatus.value
    }

    const res = await request.get('/appointments/landlord', params)
    appointments.value = res.records || []
    total.value = res.total || 0
  } catch (error) {
    console.error('获取预约列表失败:', error)
  }
}

// 确认预约
const confirmAppointment = async (row) => {
  try {
    await ElMessageBox.confirm('确认接受该预约吗?', '提示', {
      type: 'warning'
    })

    await request.put(`/appointments/${row.id}/confirm`)
    ElMessage.success('预约已确认')
    fetchAppointments()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('确认失败:', error)
    }
  }
}

// 完成看房
const completeAppointment = async (row) => {
  try {
    await ElMessageBox.confirm('确认租客已完成看房吗?', '提示', {
      type: 'info'
    })

    await request.put(`/appointments/${row.id}/complete`)
    ElMessage.success('预约已标记为完成')
    fetchAppointments()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('完成失败:', error)
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
    await request.put(`/appointments/${currentRow.value.id}/reject`, null, {
      params: { rejectReason: rejectForm.reason }
    })

    ElMessage.success('预约已拒绝')
    showReject.value = false
    fetchAppointments()
  } catch (error) {
    console.error('拒绝失败:', error)
  }
}

// 联系租客
const chatWithTenant = (row) => {
  if (!row.tenant || !row.tenant.id) {
    ElMessage.warning('租客信息不完整')
    return
  }

  // 跳转到消息中心，并在URL中携带租客ID和房源ID
  router.push({
    path: '/back/messages',
    query: {
      userId: row.tenant.id,
      houseId: row.house?.id
    }
  })
}

onMounted(() => {
  fetchAppointments()
})
</script>

<style lang="scss" scoped>
.appointment-manage-container {
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
</style>
