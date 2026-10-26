<template>
  <div class="landlord-houses-container">
    <div class="page-header">
      <h1 class="page-title">我的房源</h1>
      <el-button type="primary" @click="handleAddHouse">发布新房源</el-button>
    </div>

    <div class="house-stats">
      <el-row :gutter="20">
        <el-col :xs="24" :sm="8">
          <el-card class="stat-card" shadow="hover">
            <div class="stat-content">
              <div class="stat-icon">
                <el-icon><House /></el-icon>
              </div>
              <div class="stat-info">
                <h3>全部房源</h3>
                <div class="stat-number">{{ stats.total || 0 }}</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="8">
          <el-card class="stat-card" shadow="hover">
            <div class="stat-content">
              <div class="stat-icon rent">
                <el-icon><Notification /></el-icon>
              </div>
              <div class="stat-info">
                <h3>出租中</h3>
                <div class="stat-number">{{ stats.renting || 0 }}</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="8">
          <el-card class="stat-card" shadow="hover">
            <div class="stat-content">
              <div class="stat-icon view">
                <el-icon><View /></el-icon>
              </div>
              <div class="stat-info">
                <h3>预约看房</h3>
                <div class="stat-number">{{ stats.bookings || 0 }}</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 表格区域 -->
    <el-card shadow="hover" class="table-card">
      <template #header>
        <div class="card-header">
          <div class="header-title">房源列表</div>
          <div class="header-operations">
            <el-input
              v-model="searchId"
              placeholder="搜索房源ID"
              clearable
              @clear="fetchMyHouses"
              style="width: 140px"
            />
            <el-input
              v-model="searchText"
              placeholder="搜索房源标题"
              clearable
              @clear="fetchMyHouses"
              @keyup.enter="handleSearch"
              style="width: 200px"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <el-button type="primary" @click="handleSearch">搜索</el-button>
            <el-button @click="fetchMyHouses">刷新</el-button>
          </div>
        </div>
      </template>

      <div v-if="loading" class="loading-container">
        <el-skeleton :rows="10" animated />
      </div>
      <div v-else>
        <el-empty v-if="houseList.length === 0" description="您还没有发布任何房源" />
        <el-table v-else :data="houseList" style="width: 100%" border stripe>
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="title" label="房源标题" min-width="200" show-overflow-tooltip />
          <el-table-column label="缩略图" width="100">
            <template #default="scope">
              <el-image 
                :src="getHouseImage(scope.row)" 
                fit="cover"
                style="width: 80px; height: 60px; border-radius: 4px;"
                :preview-src-list="formatImageList(scope.row.images)"
              >
                <template #error>
                  <div class="image-error">
                    <el-icon><Picture /></el-icon>
                  </div>
                </template>
              </el-image>
            </template>
          </el-table-column>
          <el-table-column prop="typeName" label="类型" width="100" />
          <el-table-column prop="area" label="面积" width="100">
            <template #default="scope">
              {{ scope.row.area }} ㎡
            </template>
          </el-table-column>
          <el-table-column prop="price" label="租金" width="120">
            <template #default="scope">
              ¥{{ scope.row.price }} / 月
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="scope">
              <el-tag :type="getStatusType(scope.row.status)">
                {{ getStatusText(scope.row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="审核状态" width="120">
            <template #default="scope">
              <el-tag :type="getAuditStatusType(scope.row.auditStatus)">
                {{ getAuditStatusText(scope.row.auditStatus) }}
              </el-tag>
              <el-tooltip v-if="scope.row.auditStatus === 2 && scope.row.auditReason"
                :content="scope.row.auditReason"
                placement="top"
              >
                <el-icon style="margin-left: 5px; cursor: pointer;"><Warning /></el-icon>
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="发布时间" width="180" />
          <el-table-column label="操作" width="240" fixed="right">
            <template #default="scope">
              <el-button 
                type="primary" 
                size="small" 
                @click="viewHouseDetail(scope.row.id)"
                plain
              >
                查看
              </el-button>
              <el-button 
                type="success" 
                size="small" 
                @click="handleEditHouse(scope.row)"
                plain
              >
                编辑
              </el-button>
              <el-button 
                :type="scope.row.status === 1 ? 'warning' : 'info'" 
                size="small" 
                @click="handleChangeStatus(scope.row)"
                plain
              >
                {{ scope.row.status === 1 ? '下架' : '上架' }}
              </el-button>
              <el-button 
                type="danger" 
                size="small" 
                @click="handleDeleteHouse(scope.row)"
                plain
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-container">
          <el-pagination
            :current-page="currentPage"
            :page-size="pageSize"
            :page-sizes="[10, 20, 30, 50]"
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
    </el-card>

    <!-- 房屋编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogType === 'add' ? '发布新房源' : '编辑房源'"
      width="60%"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form 
        ref="houseFormRef"
        :model="houseForm" 
        :rules="houseRules" 
        label-width="100px"
        label-position="right"
      >
        <el-form-item label="房源标题" prop="title">
          <el-input v-model="houseForm.title" placeholder="请输入房源标题" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="房屋类型" prop="typeId">
              <el-select v-model="houseForm.typeId" placeholder="请选择房屋类型" style="width: 100%">
                <el-option
                  v-for="item in houseTypeOptions"
                  :key="item.id"
                  :label="item.name"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="面积" prop="area">
              <el-input-number
                v-model="houseForm.area"
                :min="1"
                :precision="0"
                style="width: 100%"
                placeholder="请输入面积"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="租金" prop="price">
              <el-input-number
                v-model="houseForm.price"
                :min="1"
                :precision="0"
                style="width: 100%"
                placeholder="请输入月租金"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="装修情况" prop="decoration">
              <el-select v-model="houseForm.decoration" placeholder="请选择装修情况" style="width: 100%">
                <el-option label="毛坯" :value="0" />
                <el-option label="简装" :value="1" />
                <el-option label="精装" :value="2" />
                <el-option label="豪装" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="楼层" prop="floor">
              <el-input-number
                v-model="houseForm.floor"
                :min="1"
                :precision="0"
                style="width: 100%"
                placeholder="所在楼层"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="总楼层" prop="totalFloors">
              <el-input-number
                v-model="houseForm.totalFloors"
                :min="1"
                :precision="0"
                style="width: 100%"
                placeholder="总楼层数"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="朝向" prop="orientation">
              <el-select v-model="houseForm.orientation" placeholder="请选择朝向" style="width: 100%">
                <el-option label="东" :value="0" />
                <el-option label="南" :value="1" />
                <el-option label="西" :value="2" />
                <el-option label="北" :value="3" />
                <el-option label="东南" :value="4" />
                <el-option label="西南" :value="5" />
                <el-option label="东北" :value="6" />
                <el-option label="西北" :value="7" />
                <el-option label="南北通透" :value="8" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="建筑年代">
              <el-input-number
                v-model="houseForm.buildYear"
                :min="1980"
                :max="2030"
                :precision="0"
                style="width: 100%"
                placeholder="如：2010"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="房屋类型" prop="buildingType">
              <el-select v-model="houseForm.buildingType" placeholder="请选择" style="width: 100%">
                <el-option label="普通住宅" :value="0" />
                <el-option label="公寓" :value="1" />
                <el-option label="别墅" :value="2" />
                <el-option label="loft" :value="3" />
                <el-option label="复式" :value="4" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="押付方式" prop="paymentMethod">
              <el-select v-model="houseForm.paymentMethod" placeholder="请选择押付方式" style="width: 100%" @change="handlePaymentMethodChange">
                <el-option label="押一付一" :value="0" />
                <el-option label="押一付三" :value="1" />
                <el-option label="押三付一" :value="2" />
                <el-option label="押一付六" :value="3" />
                <el-option label="半年付" :value="4" />
                <el-option label="年付" :value="5" />
                <el-option label="无押金" :value="6" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="最短租期">
              <el-input-number
                v-model="houseForm.minLeaseTerm"
                :min="1"
                :max="60"
                :precision="0"
                style="width: 100%"
                placeholder="月数"
              />
              <span style="margin-left: 5px; color: #999">个月（根据押付方式自动填充，可修改）</span>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="服务费">
              <el-input-number
                v-model="houseForm.serviceFee"
                :min="0"
                :precision="0"
                style="width: 100%"
                placeholder="服务费/月"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="小区名称">
              <el-input v-model="houseForm.communityName" placeholder="请输入小区名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话">
              <el-input v-model="houseForm.contactPhone" placeholder="请输入联系电话" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="详细地址" prop="address">
          <el-input v-model="houseForm.address" placeholder="请输入详细地址" />
        </el-form-item>
        <el-form-item label="详细描述" prop="description">
          <el-input
            v-model="houseForm.description"
            type="textarea"
            :rows="4"
            placeholder="请输入房屋详细描述"
          />
        </el-form-item>
        <el-form-item label="配套设施" prop="facilities">
          <div class="facilities-checkboxes">
            <el-checkbox v-model="houseForm.facilities.wifi">WiFi</el-checkbox>
            <el-checkbox v-model="houseForm.facilities.elevator">电梯</el-checkbox>
            <el-checkbox v-model="houseForm.facilities.airConditioner">空调</el-checkbox>
            <el-checkbox v-model="houseForm.facilities.washer">洗衣机</el-checkbox>
            <el-checkbox v-model="houseForm.facilities.refrigerator">冰箱</el-checkbox>
            <el-checkbox v-model="houseForm.facilities.television">电视</el-checkbox>
            <el-checkbox v-model="houseForm.facilities.waterHeater">热水器</el-checkbox>
          </div>
        </el-form-item>
        <el-form-item label="上传图片" prop="images">
          <el-upload
            :file-list="fileList"
            action="#"
            list-type="picture-card"
            :auto-upload="false"
            :limit="9"
            :on-preview="handlePictureCardPreview"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            :on-update:file-list="(val) => fileList = val"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
          <el-dialog v-model="previewVisible" width="50%">
            <img :src="previewUrl" alt="Preview Image" style="width: 100%">
          </el-dialog>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleSubmitHouse">确认</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { House, Notification, View, Search, Picture, Plus, Warning } from '@element-plus/icons-vue'
import request from '@/utils/request'

const router = useRouter()

// 基础API路径
const baseAPI = import.meta.env.VITE_BASE_API || '/api'

// 状态数据
const houseList = ref([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const searchText = ref('')
const searchId = ref('')
const stats = reactive({
  total: 0,
  renting: 0,
  bookings: 0
})

// 房屋类型选项
const houseTypeOptions = ref([])

// 对话框相关
const dialogVisible = ref(false)
const dialogType = ref('add') // 'add' 或 'edit'
const submitLoading = ref(false)
const houseFormRef = ref(null)

// 图片预览
const previewVisible = ref(false)
const previewUrl = ref('')
const fileList = ref([])

// 房屋表单
const houseForm = reactive({
  id: null,
  title: '',
  typeId: null,
  area: null,
  price: null,
  address: '',
  description: '',
  decoration: 1,
  floor: 1,
  totalFloors: 1,
  orientation: null,
  buildYear: null,
  buildingType: 0,
  paymentMethod: 0,
  minLeaseTerm: 1,
  serviceFee: 0,
  communityName: '',
  contactPhone: '',
  facilities: {
    wifi: false,
    elevator: false,
    airConditioner: false,
    washer: false,
    refrigerator: false,
    television: false,
    waterHeater: false
  },
  images: []
})

// 表单验证规则
const houseRules = {
  title: [
    { required: true, message: '请输入房源标题', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  typeId: [
    { required: true, message: '请选择房屋类型', trigger: 'change' }
  ],
  area: [
    { required: true, message: '请输入面积', trigger: 'blur' }
  ],
  price: [
    { required: true, message: '请输入租金', trigger: 'blur' }
  ],
  address: [
    { required: true, message: '请输入详细地址', trigger: 'blur' }
  ],
  decoration: [
    { required: true, message: '请选择装修情况', trigger: 'change' }
  ],
  floor: [
    { required: true, message: '请输入所在楼层', trigger: 'blur' }
  ],
  totalFloors: [
    { required: true, message: '请输入总楼层', trigger: 'blur' }
  ],
  orientation: [
    { required: true, message: '请选择朝向', trigger: 'change' }
  ],
  buildingType: [
    { required: true, message: '请选择房屋类型', trigger: 'change' }
  ],
  paymentMethod: [
    { required: true, message: '请选择押付方式', trigger: 'change' }
  ],
  images: [
    {
      required: true,
      validator: (rule, value, callback) => {
        if (!fileList.value || fileList.value.length === 0) {
          callback(new Error('请至少上传一张房屋照片'))
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ],
  facilities: [
    {
      required: true,
      validator: (rule, value, callback) => {
        const hasFacility = Object.values(houseForm.facilities).some(v => v === true)
        if (!hasFacility) {
          callback(new Error('请至少选择一项配套设施'))
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ]
}

// 初始化
onMounted(() => {
  fetchHouseTypeList()
  fetchMyHouses()
  fetchLandlordStats()
})

// 获取房屋类型列表
const fetchHouseTypeList = async () => {
  try {
    await request.get('/house-types', {}, {
      onSuccess: (res) => {
        houseTypeOptions.value = res||[]
      }
    })
  } catch (error) {
    console.error('获取房屋类型列表失败:', error)
  }
}

// 获取我的房源列表
const fetchMyHouses = async () => {
  loading.value = true
  try {
    await request.get('/houses/landlord', {
      id: searchId.value || undefined,
      title: searchText.value,
      currentPage: currentPage.value,
      size: pageSize.value
    }, {
      onSuccess: (res) => {
        houseList.value = res.records||[]
        total.value = res.total||0
      }
    })
  } catch (error) {
    console.error('获取我的房源失败:', error)
  } finally {
    loading.value = false
  }
}

// 获取房东统计数据
const fetchLandlordStats = async () => {
  try {
    await request.get('/houses/landlord/stats', {}, {
      onSuccess: (res) => {
        stats.total = res.total||0
        stats.renting = res.renting||0
        stats.bookings = res.bookings||0
      }
    })
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

// 获取房屋图片
const getHouseImage = (house) => {
  if (!house.images) return ''
  try {
    const imageArray = JSON.parse(house.images)
    if (imageArray.length === 0) return ''
    const imagePath = imageArray[0]
    return formatImageUrl(imagePath)
  } catch (e) {
    return ''
  }
}

// 格式化图片URL
const formatImageUrl = (path) => {
  if (!path) return ''
  if (path.startsWith('http')) return path
  return baseAPI + path
}

// 格式化图片列表
const formatImageList = (images) => {
  if (!images) return []
  try {
    const imageArray = JSON.parse(images)
    return imageArray.map(img => formatImageUrl(img))
  } catch (e) {
    return []
  }
}

// 获取状态类型
const getStatusType = (status) => {
  switch (status) {
    case 0: return 'info'
    case 1: return 'success'
    case 2: return 'warning'
    default: return 'info'
  }
}

// 获取状态文本
const getStatusText = (status) => {
  switch (status) {
    case 0: return '已下架'
    case 1: return '出租中'
    case 2: return '已出租'
    default: return '未知'
  }
}

// 获取审核状态类型
const getAuditStatusType = (status) => {
  const typeMap = {
    0: 'warning',
    1: 'success',
    2: 'danger'
  }
  return typeMap[status] || 'info'
}

// 获取审核状态文本
const getAuditStatusText = (status) => {
  const textMap = {
    0: '待审核',
    1: '审核通过',
    2: '审核拒绝'
  }
  return textMap[status] || '未知'
}

// 处理搜索
const handleSearch = () => {
  currentPage.value = 1
  fetchMyHouses()
}

// 每页数量变化
const handleSizeChange = (val) => {
  pageSize.value = val
  fetchMyHouses()
}

// 当前页变化
const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchMyHouses()
}

// 查看房屋详情
const viewHouseDetail = (id) => {
  router.push(`/houses/${id}`)
}

// 处理添加房屋
const handleAddHouse = () => {
  dialogType.value = 'add'
  resetHouseForm()
  dialogVisible.value = true
}

// 押付方式改变时自动填充最短租期
const handlePaymentMethodChange = (value) => {
  // 根据押付方式设置最短租期（月）
  const minLeaseMap = {
    0: 1,  // 押一付一 -> 至少1个月
    1: 3,  // 押一付三 -> 至少3个月
    2: 1,  // 押三付一 -> 至少1个月
    3: 6,  // 押一付六 -> 至少6个月
    4: 6,  // 半年付 -> 至少6个月
    5: 12, // 年付 -> 至少12个月
    6: 1   // 无押金 -> 至少1个月
  }
  houseForm.minLeaseTerm = minLeaseMap[value] || 1
}

// 处理编辑房屋
const handleEditHouse = (row) => {
  dialogType.value = 'edit'
  resetHouseForm()

  // 设置表单数据
  houseForm.id = row.id
  houseForm.title = row.title
  houseForm.typeId = row.typeId
  houseForm.area = row.area
  houseForm.price = row.price
  houseForm.address = row.address
  houseForm.description = row.description

  // 设置新增字段
  houseForm.decoration = row.decoration ?? 1
  houseForm.floor = row.floor ?? 1
  houseForm.totalFloors = row.totalFloors ?? 1
  houseForm.orientation = row.orientation
  houseForm.buildYear = row.buildYear
  houseForm.buildingType = row.buildingType ?? 0
  houseForm.paymentMethod = row.paymentMethod ?? 0
  houseForm.minLeaseTerm = row.minLeaseTerm ?? 1
  houseForm.serviceFee = row.serviceFee ?? 0
  houseForm.communityName = row.communityName || ''
  houseForm.contactPhone = row.contactPhone || ''

  // 设置配套设施
  if (row.facilities) {
    try {
      const facilities = JSON.parse(row.facilities)
      houseForm.facilities = { ...houseForm.facilities, ...facilities }
    } catch (e) {
      console.error('解析配套设施失败:', e)
    }
  }

  // 设置图片
  if (row.images) {
    try {
      const images = JSON.parse(row.images)
      houseForm.images = images
      fileList.value = images.map((url, index) => ({
        name: `image-${index}.jpg`,
        url: formatImageUrl(url)
      }))
    } catch (e) {
      console.error('解析图片失败:', e)
    }
  }

  dialogVisible.value = true
}

// 处理改变状态
const handleChangeStatus = (row) => {
  const newStatus = row.status === 1 ? 0 : 1
  const actionText = newStatus === 1 ? '上架' : '下架'
  
  ElMessageBox.confirm(
    `确定要${actionText}该房源吗？`,
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await request.put(`/houses/${row.id}/status`, {
        status: newStatus
      }, {
        onSuccess: () => {
          ElMessage.success(`${actionText}成功`)
          fetchMyHouses()
          fetchLandlordStats()
        }
      })
    } catch (error) {
      console.error(`${actionText}失败:`, error)
    }
  }).catch(() => {})
}

// 处理删除房屋
const handleDeleteHouse = (row) => {
  ElMessageBox.confirm(
    '确定要删除该房源吗？删除后无法恢复。',
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await request.delete(`/houses/${row.id}`, {}, {
        onSuccess: () => {
          ElMessage.success('删除成功')
          fetchMyHouses()
          fetchLandlordStats()
        }
      })
    } catch (error) {
      console.error('删除失败:', error)
    }
  }).catch(() => {})
}

// 重置房屋表单
const resetHouseForm = () => {
  if (houseFormRef.value) {
    houseFormRef.value.resetFields()
  }

  houseForm.id = null
  houseForm.title = ''
  houseForm.typeId = null
  houseForm.area = null
  houseForm.price = null
  houseForm.address = ''
  houseForm.description = ''
  houseForm.decoration = 1
  houseForm.floor = 1
  houseForm.totalFloors = 1
  houseForm.orientation = null
  houseForm.buildYear = null
  houseForm.buildingType = 0
  houseForm.paymentMethod = 0
  houseForm.minLeaseTerm = 1
  houseForm.serviceFee = 0
  houseForm.communityName = ''
  houseForm.contactPhone = ''
  houseForm.facilities = {
    wifi: false,
    elevator: false,
    airConditioner: false,
    washer: false,
    refrigerator: false,
    television: false,
    waterHeater: false
  }
  houseForm.images = []
  fileList.value = []
}

// 处理图片预览
const handlePictureCardPreview = (file) => {
  previewUrl.value = file.url
  previewVisible.value = true
}

// 处理文件改变
const handleFileChange = (file) => {
  // 由于我们使用的是自定义上传，这里主要是对文件进行验证
  const isImage = file.raw.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    fileList.value = fileList.value.filter(item => item.uid !== file.uid)
    return
  }

  const isLt5M = file.raw.size / 1024 / 1024 < 5
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过 5MB!')
    fileList.value = fileList.value.filter(item => item.uid !== file.uid)
    return
  }

  // 触发表单验证，清除可能的错误提示
  if (houseFormRef.value) {
    houseFormRef.value.validateField('images')
  }
}

// 处理文件移除
const handleFileRemove = (file) => {
  // 触发表单验证，检查是否还有图片
  if (houseFormRef.value) {
    houseFormRef.value.validateField('images')
  }
}

// 处理提交房屋表单
const handleSubmitHouse = async () => {
  if (!houseFormRef.value) return

  await houseFormRef.value.validate(async (valid) => {
    if (!valid) return

    // 额外检查：确保至少有一张图片
    if (!fileList.value || fileList.value.length === 0) {
      ElMessage.error('请至少上传一张房屋照片')
      return
    }

    submitLoading.value = true

    try {
      // 处理图片上传
      const uploadTasks = fileList.value
        .filter(file => file.raw) // 只处理新上传的文件
        .map(file => {
          const formData = new FormData()
          formData.append('file', file.raw)
          return request.upload('/upload', formData)
        })

      // 等待所有图片上传完成
      const uploadResults = await Promise.all(uploadTasks)

      // 合并已有图片和新上传的图片
      const existingImages = fileList.value
        .filter(file => !file.raw)
        .map(file => {
          // 从URL中提取图片路径
          const url = file.url
          if (url.startsWith(baseAPI)) {
            return url.substring(baseAPI.length)
          }
          return url
        })

      const allImages = [...existingImages, ...uploadResults]

      // 再次检查最终图片数量
      if (allImages.length === 0) {
        ElMessage.error('请至少上传一张房屋照片')
        submitLoading.value = false
        return
      }

      // 准备提交的数据
      const houseData = {
        title: houseForm.title,
        typeId: houseForm.typeId,
        area: houseForm.area,
        price: houseForm.price,
        address: houseForm.address,
        description: houseForm.description,
        decoration: houseForm.decoration,
        floor: houseForm.floor,
        totalFloors: houseForm.totalFloors,
        orientation: houseForm.orientation,
        buildYear: houseForm.buildYear,
        buildingType: houseForm.buildingType,
        paymentMethod: houseForm.paymentMethod,
        minLeaseTerm: houseForm.minLeaseTerm,
        serviceFee: houseForm.serviceFee,
        communityName: houseForm.communityName,
        contactPhone: houseForm.contactPhone,
        facilities: JSON.stringify(houseForm.facilities),
        images: JSON.stringify(allImages)
      }
      
      if (dialogType.value === 'add') {
        // 新增房源
        await request.post('/houses', houseData, {
          onSuccess: () => {
            ElMessage.success('发布房源成功')
            dialogVisible.value = false
            fetchMyHouses()
            fetchLandlordStats()
          }
        })
      } else {
        // 编辑房源
        await request.put(`/houses/${houseForm.id}`, houseData, {
          onSuccess: () => {
            ElMessage.success('更新房源成功')
            dialogVisible.value = false
            fetchMyHouses()
            fetchLandlordStats()
          }
        })
      }
    } catch (error) {
      console.error('提交房源失败:', error)
      ElMessage.error('提交失败，请稍后再试')
    } finally {
      submitLoading.value = false
    }
  })
}
</script>

<style lang="scss" scoped>
.landlord-houses-container {
  padding: 20px;
  
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    
    .page-title {
      font-size: 24px;
      font-weight: 600;
      color: #1d4ed8;
      margin: 0;
    }
  }
  
  .house-stats {
    margin-bottom: 20px;
    
    .stat-card {
      margin-bottom: 20px;
      border-radius: 8px;
      
      .stat-content {
        display: flex;
        align-items: center;
        
        .stat-icon {
          width: 60px;
          height: 60px;
          border-radius: 8px;
          background-color: rgba(59, 130, 246, 0.1);
          display: flex;
          justify-content: center;
          align-items: center;
          font-size: 30px;
          color: #3b82f6;
          margin-right: 20px;
          
          &.rent {
            background-color: rgba(16, 185, 129, 0.1);
            color: #10b981;
          }
          
          &.view {
            background-color: rgba(245, 158, 11, 0.1);
            color: #f59e0b;
          }
        }
        
        .stat-info {
          h3 {
            font-size: 16px;
            color: #64748b;
            margin: 0 0 5px 0;
            font-weight: normal;
          }
          
          .stat-number {
            font-size: 28px;
            font-weight: 600;
            color: #1e293b;
          }
        }
      }
    }
  }
  
  .table-card {
    margin-bottom: 20px;
    
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      
      .header-title {
        font-size: 18px;
        font-weight: 600;
        color: #1d4ed8;
      }
      
      .header-operations {
        display: flex;
        gap: 10px;
      }
    }
    
    .loading-container {
      padding: 20px;
    }
    
    .image-error {
      width: 100%;
      height: 100%;
      display: flex;
      justify-content: center;
      align-items: center;
      background-color: #f5f7fa;
      color: #c0c4cc;
    }
    
    .pagination-container {
      margin-top: 20px;
      display: flex;
      justify-content: center;
    }
  }
  
  .facilities-checkboxes {
    display: flex;
    flex-wrap: wrap;
    gap: 20px;
  }
}

// 响应式调整
@media (max-width: 768px) {
  .landlord-houses-container {
    .page-header {
      flex-direction: column;
      align-items: flex-start;
      
      .page-title {
        margin-bottom: 10px;
      }
    }
    
    .table-card {
      .card-header {
        flex-direction: column;
        align-items: flex-start;
        
        .header-title {
          margin-bottom: 10px;
        }
        
        .header-operations {
          width: 100%;
          flex-wrap: wrap;
        }
      }
    }
    
    .facilities-checkboxes {
      gap: 10px;
    }
  }
}
</style> 