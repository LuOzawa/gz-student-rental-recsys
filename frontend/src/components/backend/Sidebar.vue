<template>
  <div class="sidebar-container" :class="{ 'is-collapsed': isCollapsed }">
    <div class="logo">
      <span class="logo-icon">🏠</span>
      <span class="logo-text" v-show="!isCollapsed">广寓租房</span>
      <div class="logo-decoration"></div>
    </div>
    <div class="menu-wrapper">
      <el-menu :default-active="activeMenu" :collapse="isCollapsed" :collapse-transition="false" mode="vertical" class="sidebar-menu"
        text-color="#bfcbd9" active-text-color="#409EFF" router>
        
        <!-- 固定菜单项 -->
        <el-menu-item index="/back/dashboard">
          <el-icon><HomeFilled /></el-icon>
          <template #title>首页</template>
        </el-menu-item>
        
        <el-menu-item v-if="userStore.userInfo?.roleCode === 'ADMIN'" index="/back/house-type">
          <el-icon><Menu /></el-icon>
          <template #title>房屋类型管理</template>
        </el-menu-item>
        
        <el-menu-item index="/back/house">
          <el-icon><House /></el-icon>
          <template #title>房屋管理</template>
        </el-menu-item>
        
        <el-menu-item index="/back/order">
          <el-icon><Tickets /></el-icon>
          <template #title>订单管理</template>
        </el-menu-item>
        
        <el-menu-item index="/back/transaction">
          <el-icon><Money /></el-icon>
          <template #title>收支明细</template>
        </el-menu-item>
        
        <el-menu-item v-if="userStore.userInfo?.roleCode === 'ADMIN'" index="/back/user">
          <el-icon><User /></el-icon>
          <template #title>用户管理</template>
        </el-menu-item>
        
        <el-menu-item index="/back/profile">
          <el-icon><UserFilled /></el-icon>
          <template #title>个人信息</template>
        </el-menu-item>
        
        <el-menu-item index="/back/lease">
          <el-icon><Document /></el-icon>
          <template #title>租赁管理</template>
        </el-menu-item>
        
        <el-menu-item v-if="userStore.userInfo?.roleCode === 'ADMIN'" index="/back/carousel">
          <el-icon><Picture /></el-icon>
          <template #title>轮播图管理</template>
        </el-menu-item>
        
        <el-menu-item v-if="userStore.userInfo?.roleCode === 'ADMIN'" index="/back/announcement">
          <el-icon><Bell /></el-icon>
          <template #title>公告管理</template>
        </el-menu-item>

        <!-- 管理员审核菜单 -->
        <el-menu-item v-if="userStore.userInfo?.roleCode === 'ADMIN'" index="/back/verification-audit">
          <el-icon><Checked /></el-icon>
          <template #title>实名认证审核</template>
        </el-menu-item>

        <el-menu-item v-if="userStore.userInfo?.roleCode === 'ADMIN'" index="/back/house/audit">
          <el-icon><DocumentChecked /></el-icon>
          <template #title>房源审核</template>
        </el-menu-item>

        <el-menu-item v-if="userStore.userInfo?.roleCode === 'ADMIN'" index="/back/comments">
          <el-icon><ChatDotRound /></el-icon>
          <template #title>评价留言管理</template>
        </el-menu-item>

        <el-menu-item v-if="userStore.userInfo?.roleCode === 'ADMIN'" index="/back/forum-posts">
          <el-icon><ChatLineSquare /></el-icon>
          <template #title>帖子管理</template>
        </el-menu-item>

        <el-menu-item v-if="userStore.userInfo?.roleCode === 'ADMIN'" index="/back/violations">
          <el-icon><Warning /></el-icon>
          <template #title>举报管理</template>
        </el-menu-item>

        <el-menu-item v-if="userStore.userInfo?.roleCode === 'ADMIN'" index="/back/messages">
          <el-icon><ChatDotRound /></el-icon>
          <template #title>消息中心</template>
        </el-menu-item>

        <!-- 房东专属菜单 -->
        <el-menu-item v-if="userStore.userInfo?.roleCode === 'LANDLORD'" index="/back/landlord-comments">
          <el-icon><ChatDotRound /></el-icon>
          <template #title>评价留言管理</template>
        </el-menu-item>

        <el-menu-item v-if="userStore.userInfo?.roleCode === 'LANDLORD'" index="/back/appointments/landlord">
          <el-icon><Calendar /></el-icon>
          <template #title>预约管理</template>
        </el-menu-item>

        <el-menu-item v-if="userStore.userInfo?.roleCode === 'LANDLORD'" index="/back/messages">
          <el-icon><ChatDotRound /></el-icon>
          <template #title>消息中心</template>
        </el-menu-item>

      </el-menu>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAppStore } from '@/store/app'
import { useUserStore } from '@/store/user'
import {
  HomeFilled,
  User,
  UserFilled,
  Menu,
  House,
  Tickets,
  Money,
  Document,
  Picture,
  Bell,
  Checked,
  DocumentChecked,
  ChatDotRound,
  ChatLineSquare,
  Calendar,
  Warning
} from '@element-plus/icons-vue'

const route = useRoute()
const appStore = useAppStore()
const userStore = useUserStore()
const isCollapsed = computed(() => appStore.sidebarCollapsed)

// 当前激活的菜单
const activeMenu = computed(() => {
  const { meta, path } = route
  if (meta.activeMenu) {
    return meta.activeMenu
  }
  return path
})
</script>

<style lang="scss" scoped>
.sidebar-container {
  height: 100%; 
  min-height: 100vh;
  background: linear-gradient(180deg, #3B82F6 0%, #1D4ED8 100%);
  display: flex;
  flex-direction: column;
  width: 220px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 2px 12px rgba(59, 130, 246, 0.15);
  
  &.is-collapsed {
    width: 64px;
    
    .logo {
      padding: 0;
      justify-content: center;
      
      .logo-icon {
        margin: 0;
      }
    }

    :deep(.el-menu) {
      .el-sub-menu__title span,
      .el-menu-item span {
        opacity: 0;
        transition: opacity 0.2s;
      }
    }
  }
  
  .logo {
    height: 64px;
    flex-shrink: 0;
    line-height: 64px;
    text-align: center;
    background: rgba(255, 255, 255, 0.1);
    backdrop-filter: blur(10px);
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
    display: flex;
    align-items: center;
    padding: 0 16px;
    overflow: hidden;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    position: relative;
    
    .logo-icon {
      font-size: 26px;
      margin-right: 12px;
      transition: margin 0.3s cubic-bezier(0.4, 0, 0.2, 1);
      filter: drop-shadow(0 2px 4px rgba(255, 255, 255, 0.3));
    }
    
    .logo-text {
      color: #ffffff;
      font-size: 18px;
      font-weight: 600;
      white-space: nowrap;
      opacity: 1;
      transition: opacity 0.2s;
      letter-spacing: 0.5px;
      text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
    }
    
    .logo-decoration {
      position: absolute;
      bottom: 0;
      left: 50%;
      transform: translateX(-50%);
      width: 40px;
      height: 3px;
      background: #ffffff;
      border-radius: 3px 3px 0 0;
      opacity: 0.7;
    }
  }

  .menu-wrapper {
    flex: 1;
    overflow-y: auto;
    overflow-x: hidden;

    &::-webkit-scrollbar {
      width: 6px;
    }

    &::-webkit-scrollbar-thumb {
      background: rgba(255, 255, 255, 0.2);
      border-radius: 3px;
    }

    &::-webkit-scrollbar-track {
      background: transparent;
    }
  }

  :deep(.sidebar-menu) {
    border: none;
    background: transparent;
    transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);

    .el-menu-item, .el-sub-menu__title {
      height: 50px;
      line-height: 50px;
      color: rgba(255, 255, 255, 0.85);
      background: transparent;
      transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
      font-weight: 500;
      
      span {
        opacity: 1;
        transition: opacity 0.3s;
      }
      
      &:hover {
        background: rgba(255, 255, 255, 0.15) !important;
        color: #ffffff;
      }
    }

    .el-menu-item.is-active {
      background: rgba(255, 255, 255, 0.2) !important;
      color: #ffffff !important;
      
      &::before {
        content: '';
        position: absolute;
        left: 0;
        top: 0;
        width: 4px;
        height: 100%;
        background: #FFFFFF;
      }
    }

    .el-sub-menu {
      &.is-opened {
        > .el-sub-menu__title {
          color: #ffffff;
          background: rgba(29, 78, 216, 0.8) !important;
        }
      }

      .el-menu {
        background: rgba(29, 78, 216, 0.6);
        
        .el-menu-item {
          background: transparent;
          
          &:hover {
            background: rgba(255, 255, 255, 0.15) !important;
          }
          
          &.is-active {
            background: rgba(255, 255, 255, 0.2) !important;
          }
        }
      }
    }

    // 折叠状态下的弹出菜单样式
    &.el-menu--collapse {
      .el-sub-menu {
        &.is-opened {
          > .el-sub-menu__title {
            background: transparent !important;
          }
        }
      }
    }
  }

  .el-icon {
    vertical-align: middle;
    margin-right: 10px;
    width: 24px;
    height: 24px;
    text-align: center;
    color: inherit;
    font-size: 18px;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  span {
    vertical-align: middle;
  }
}
</style> 