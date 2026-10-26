<template>
  <div class="user-management">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
        </div>
      </template>
      <UserList />
    </el-card>
  </div>
</template>

<script setup>
import UserList from './UserList.vue'
</script>

<style lang="scss" scoped>
.user-management {
  padding: 20px;
  
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  
  .box-card {
    margin-bottom: 20px;
  }
}
</style> 