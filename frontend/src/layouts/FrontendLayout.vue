<template>
    <div class="frontend-layout">
      <!-- 顶部导航栏 -->
      <header class="header" :class="{ 'scrolled': isScrolled }">
        <div class="header-content">
          <div class="logo">
            <router-link to="/">
              <span class="logo-icon-wrapper">
                <span class="logo-icon">🏠</span>
              </span>
              <span class="logo-text">
                <span class="logo-main">广寓</span>
                <span class="logo-sub">Guangzhou Housing</span>
              </span>
            </router-link>
          </div>

          <nav class="nav-menu">
            <router-link to="/" custom v-slot="{ navigate, isExactActive }">
              <a href="javascript:;" @click="navigate" :class="{ 'router-link-active': isExactActive }">
                <span class="nav-icon">🏠</span>
                <span>首页</span>
              </a>
            </router-link>
            <router-link to="/houses">
              <span class="nav-icon">🏘️</span>
              <span>房源</span>
            </router-link>
            <router-link to="/forum">
              <span class="nav-icon">💬</span>
              <span>论坛</span>
            </router-link>
            <router-link to="/announcements">
              <span class="nav-icon">📢</span>
              <span>公告</span>
            </router-link>

            <template v-if="isLoggedIn">
              <router-link v-if="isTenant" to="/my-favorites">
                <span class="nav-icon">⭐</span>
                <span>收藏</span>
              </router-link>
              <router-link v-if="isTenant" to="/recommendations" class="nav-link-highlight">
                <span class="nav-icon">✨</span>
                <span>推荐</span>
              </router-link>
              <router-link v-if="isTenant" to="/messages">
                <span class="nav-icon">📨</span>
                <span>消息</span>
              </router-link>
              <router-link v-if="isLandlord" to="/landlord/houses">
                <span class="nav-icon">🔑</span>
                <span>房源</span>
              </router-link>
              <router-link v-if="isTenant" to="/my-orders">
                <span class="nav-icon">📋</span>
                <span>订单</span>
              </router-link>
              <router-link v-if="isTenant" to="/my-leases">
                <span class="nav-icon">📑</span>
                <span>租赁</span>
              </router-link>
              <router-link v-if="isTenant" to="/my-appointments">
                <span class="nav-icon">📅</span>
                <span>预约</span>
              </router-link>
              <router-link to="/verification">
                <span class="nav-icon">✅</span>
                <span>认证</span>
              </router-link>

              <div class="user-dropdown">
                <button class="user-btn" @click="toggleUserMenu">
                  <span class="user-avatar">{{ userInitial }}</span>
                  <span class="user-name">{{ userName }}</span>
                  <span class="dropdown-arrow" :class="{ 'open': userMenuOpen }">▼</span>
                </button>
                <div class="dropdown-menu" :class="{ 'show': userMenuOpen }">
                  <router-link to="/profile" @click="userMenuOpen = false">
                    <span>👤</span> 个人中心
                  </router-link>
                  <a href="javascript:;" @click="handleLogout">
                    <span>🚪</span> 退出登录
                  </a>
                </div>
              </div>
            </template>
            <template v-else>
              <router-link to="/login" class="auth-btn login-btn">
                <span>登录</span>
              </router-link>
              <router-link to="/register" class="auth-btn register-btn">
                <span>注册</span>
              </router-link>
            </template>
          </nav>

          <!-- 移动端菜单按钮 -->
          <div class="mobile-menu-btn" @click="toggleMobileMenu">
            <span class="hamburger" :class="{ 'active': mobileMenuActive }">
              <span></span>
              <span></span>
              <span></span>
            </span>
          </div>
        </div>

        <!-- 移动端菜单 -->
        <div class="mobile-menu" :class="{ 'active': mobileMenuActive }">
          <router-link to="/" custom v-slot="{ navigate, isExactActive }">
            <a href="javascript:;" @click="navigateAndClose(navigate)" :class="{ 'router-link-active': isExactActive }">
              <span class="nav-icon">🏠</span>
              <span>首页</span>
            </a>
          </router-link>
          <router-link to="/houses" @click="mobileMenuActive = false">
            <span class="nav-icon">🏘️</span>
            <span>房源</span>
          </router-link>
          <router-link to="/forum" @click="mobileMenuActive = false">
            <span class="nav-icon">💬</span>
            <span>论坛</span>
          </router-link>
          <router-link to="/announcements" @click="mobileMenuActive = false">
            <span class="nav-icon">📢</span>
            <span>公告</span>
          </router-link>

          <template v-if="isLoggedIn">
            <div class="mobile-divider"></div>
            <router-link v-if="isTenant" to="/my-favorites" @click="mobileMenuActive = false">
              <span class="nav-icon">⭐</span>
              <span>收藏</span>
            </router-link>
            <router-link v-if="isTenant" to="/recommendations" @click="mobileMenuActive = false">
              <span class="nav-icon">✨</span>
              <span>推荐</span>
            </router-link>
            <router-link v-if="isTenant" to="/messages" @click="mobileMenuActive = false">
              <span class="nav-icon">📨</span>
              <span>消息</span>
            </router-link>
            <router-link v-if="isLandlord" to="/landlord/houses" @click="mobileMenuActive = false">
              <span class="nav-icon">🔑</span>
              <span>房源管理</span>
            </router-link>
            <router-link v-if="isTenant" to="/my-orders" @click="mobileMenuActive = false">
              <span class="nav-icon">📋</span>
              <span>我的订单</span>
            </router-link>
            <router-link v-if="isTenant" to="/my-leases" @click="mobileMenuActive = false">
              <span class="nav-icon">📑</span>
              <span>我的租赁</span>
            </router-link>
            <router-link v-if="isTenant" to="/my-appointments" @click="mobileMenuActive = false">
              <span class="nav-icon">📅</span>
              <span>我的预约</span>
            </router-link>
            <router-link to="/verification" @click="mobileMenuActive = false">
              <span class="nav-icon">✅</span>
              <span>实名认证</span>
            </router-link>
            <router-link to="/profile" @click="mobileMenuActive = false">
              <span class="nav-icon">👤</span>
              <span>个人中心</span>
            </router-link>
            <a href="javascript:;" @click="handleLogout" class="logout-link">
              <span class="nav-icon">🚪</span>
              <span>退出登录</span>
            </a>
          </template>
          <template v-else>
            <div class="mobile-divider"></div>
            <router-link to="/login" @click="mobileMenuActive = false" class="mobile-auth-btn">
              <span>登录</span>
            </router-link>
            <router-link to="/register" @click="mobileMenuActive = false" class="mobile-auth-btn primary">
              <span>注册</span>
            </router-link>
          </template>
        </div>
      </header>

      <!-- 主要内容区域 -->
      <main class="main-content">
        <router-view />
      </main>

      <!-- 页脚 -->
      <footer class="footer">
        <div class="footer-content">
          <div class="footer-brand">
            <div class="footer-logo">
              <span class="logo-icon">🏠</span>
              <span class="logo-text">广寓租房</span>
            </div>
            <p class="footer-slogan">为广州大学生打造的智能租房平台</p>
            <div class="footer-social">
              <a href="javascript:;" class="social-link">微信</a>
              <a href="javascript:;" class="social-link">微博</a>
              <a href="javascript:;" class="social-link">小红书</a>
            </div>
          </div>

          <div class="footer-links">
            <div class="footer-section">
              <h4>房源</h4>
              <router-link to="/houses">全部房源</router-link>
              <router-link to="/top-rated-houses">高分房源</router-link>
              <router-link to="/recommendations">智能推荐</router-link>
            </div>
            <div class="footer-section">
              <h4>社区</h4>
              <router-link to="/forum">租房论坛</router-link>
              <router-link to="/announcements">平台公告</router-link>
            </div>
            <div class="footer-section">
              <h4>关于</h4>
              <a href="javascript:;">关于我们</a>
              <a href="javascript:;">联系方式</a>
              <a href="javascript:;">帮助中心</a>
            </div>
          </div>
        </div>

        <div class="footer-bottom">
          <p>&copy; 2025 广寓租房 All Rights Reserved.</p>
          <p class="footer-made">Made with ❤️ for 广州大学生</p>
        </div>
      </footer>

      <!-- 装饰性背景元素 -->
      <div class="background-decoration">
        <div class="blob blob-1"></div>
        <div class="blob blob-2"></div>
        <div class="blob blob-3"></div>
      </div>
    </div>
  </template>

  <script setup>
  import { ref, computed, onMounted, onUnmounted } from 'vue'
  import { useUserStore } from '@/store/user'
  import { useRouter } from 'vue-router'

  const userStore = useUserStore()
  const router = useRouter()
  const mobileMenuActive = ref(false)
  const userMenuOpen = ref(false)
  const isScrolled = ref(false)

  const isLoggedIn = computed(() => userStore.isLoggedIn)
  const isLandlord = computed(() => userStore.userInfo?.roleCode === 'LANDLORD')
  const isTenant = computed(() => userStore.userInfo?.roleCode === 'TENANT')
  const userName = computed(() => userStore.userInfo?.name || userStore.userInfo?.username || '用户')
  const userInitial = computed(() => {
    const name = userStore.userInfo?.name || userStore.userInfo?.username || 'U'
    return name.charAt(0).toUpperCase()
  })

  const handleLogout = () => {
    userStore.clearUserInfo()
    router.push('/login')
    mobileMenuActive.value = false
    userMenuOpen.value = false
  }

  const toggleMobileMenu = () => {
    mobileMenuActive.value = !mobileMenuActive.value
  }

  const toggleUserMenu = () => {
    userMenuOpen.value = !userMenuOpen.value
  }

  const navigateAndClose = (navigate) => {
    navigate()
    mobileMenuActive.value = false
  }

  // 滚动监听
  const handleScroll = () => {
    isScrolled.value = window.scrollY > 50
  }

  onMounted(() => {
    window.addEventListener('scroll', handleScroll)
  })

  onUnmounted(() => {
    window.removeEventListener('scroll', handleScroll)
  })

  // 点击外部关闭用户菜单
  document.addEventListener('click', (e) => {
    if (!e.target.closest('.user-dropdown')) {
      userMenuOpen.value = false
    }
  })
  </script>

  <style lang="scss" scoped>
  @import '@/styles/design-system.scss';

  .frontend-layout {
    display: flex;
    flex-direction: column;
    min-height: 100vh;
    position: relative;
    background: var(--color-bg-primary);
  }

  // ==================== 头部导航 ====================
  .header {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    z-index: 100;
    background: rgba(255, 255, 255, 0.9);
    backdrop-filter: blur(20px);
    -webkit-backdrop-filter: blur(20px);
    border-bottom: 1px solid transparent;
    transition: all var(--transition-base);

    &.scrolled {
      background: rgba(255, 255, 255, 0.98);
      box-shadow: var(--shadow-md);
      border-bottom-color: var(--color-border-light);
    }

    .header-content {
      max-width: 1400px;
      margin: 0 auto;
      padding: 0 24px;
      height: 72px;
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
  }

  // Logo
  .logo {
    a {
      display: flex;
      align-items: center;
      gap: 12px;
      text-decoration: none;
    }

    .logo-icon-wrapper {
      width: 44px;
      height: 44px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: var(--color-primary-gradient);
      border-radius: var(--radius-md);
      box-shadow: var(--shadow-colored);

      .logo-icon {
        font-size: 22px;
        filter: drop-shadow(0 2px 4px rgba(0,0,0,0.1));
      }
    }

    .logo-text {
      display: flex;
      flex-direction: column;
      line-height: 1.1;

      .logo-main {
        font-family: $font-display;
        font-size: 22px;
        font-weight: 800;
        background: var(--color-primary-gradient);
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
        background-clip: text;
      }

      .logo-sub {
        font-size: 10px;
        color: var(--color-text-tertiary);
        font-weight: 500;
        letter-spacing: 0.5px;
      }
    }
  }

  // 导航菜单
  .nav-menu {
    display: flex;
    gap: 8px;
    align-items: center;

    a {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 10px 16px;
      color: var(--color-text-secondary);
      text-decoration: none;
      font-size: 14px;
      font-weight: 500;
      font-family: $font-display;
      border-radius: var(--radius-full);
      transition: all var(--transition-base);
      white-space: nowrap;

      .nav-icon {
        font-size: 16px;
        opacity: 0.8;
        transition: transform var(--transition-base);
      }

      &:hover {
        color: var(--color-primary);
        background: rgba(255, 107, 107, 0.08);

        .nav-icon {
          transform: scale(1.1);
        }
      }

      &.router-link-active {
        color: var(--color-primary);
        background: rgba(255, 107, 107, 0.12);
        font-weight: 600;
      }

      &.nav-link-highlight {
        background: var(--color-primary-gradient);
        color: white;
        box-shadow: 0 4px 12px rgba(255, 107, 107, 0.3);

        &:hover {
          box-shadow: 0 6px 16px rgba(255, 107, 107, 0.4);
          transform: translateY(-1px);
        }

        &.router-link-active {
          background: var(--color-primary-gradient);
        }
      }

      &.auth-btn {
        padding: 10px 20px;
        font-weight: 600;

        &.login-btn {
          color: var(--color-primary);
          border: 2px solid var(--color-primary);

          &:hover {
            background: var(--color-primary);
            color: white;
          }
        }

        &.register-btn {
          background: var(--color-primary-gradient);
          color: white;
          border: 2px solid transparent;

          &:hover {
            box-shadow: var(--shadow-colored);
            transform: translateY(-1px);
          }
        }
      }
    }

    // 用户下拉菜单
    .user-dropdown {
      position: relative;
      margin-left: 8px;

      .user-btn {
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 6px 12px 6px 6px;
        background: var(--color-bg-tertiary);
        border: none;
        border-radius: var(--radius-full);
        cursor: pointer;
        transition: all var(--transition-base);

        &:hover {
          background: var(--color-border);
        }

        .user-avatar {
          width: 32px;
          height: 32px;
          display: flex;
          align-items: center;
          justify-content: center;
          background: var(--color-primary-gradient);
          color: white;
          font-weight: 600;
          font-size: 14px;
          border-radius: 50%;
        }

        .user-name {
          font-size: 14px;
          font-weight: 500;
          color: var(--color-text-primary);
          max-width: 80px;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        .dropdown-arrow {
          font-size: 10px;
          color: var(--color-text-tertiary);
          transition: transform var(--transition-base);

          &.open {
            transform: rotate(180deg);
          }
        }
      }

      .dropdown-menu {
        position: absolute;
        top: calc(100% + 8px);
        right: 0;
        min-width: 160px;
        background: white;
        border-radius: var(--radius-lg);
        box-shadow: var(--shadow-lg);
        border: 1px solid var(--color-border-light);
        opacity: 0;
        visibility: hidden;
        transform: translateY(-8px);
        transition: all var(--transition-base);

        &.show {
          opacity: 1;
          visibility: visible;
          transform: translateY(0);
        }

        a {
          display: flex;
          align-items: center;
          gap: 10px;
          padding: 12px 16px;
          color: var(--color-text-secondary);

          &:hover {
            background: var(--color-bg-tertiary);
          }

          &:first-child {
            border-radius: var(--radius-lg) var(--radius-lg) 0 0;
          }

          &:last-child {
            border-radius: 0 0 var(--radius-lg) var(--radius-lg);
          }
        }
      }
    }
  }

  // 移动端菜单按钮
  .mobile-menu-btn {
    display: none;
    cursor: pointer;

    .hamburger {
      display: flex;
      flex-direction: column;
      gap: 5px;
      width: 24px;

      span {
        width: 100%;
        height: 2px;
        background: var(--color-text-primary);
        border-radius: 2px;
        transition: all var(--transition-base);
        transform-origin: center;
      }

      &.active {
        span {
          &:nth-child(1) {
            transform: translateY(7px) rotate(45deg);
          }
          &:nth-child(2) {
            opacity: 0;
          }
          &:nth-child(3) {
            transform: translateY(-7px) rotate(-45deg);
          }
        }
      }
    }
  }

  // 移动端菜单
  .mobile-menu {
    display: none;
    flex-direction: column;
    background: white;
    border-top: 1px solid var(--color-border-light);
    max-height: 0;
    overflow: hidden;
    transition: max-height var(--transition-slow);

    &.active {
      max-height: 500px;
      overflow-y: auto;
    }

    a {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 16px 24px;
      color: var(--color-text-secondary);
      text-decoration: none;
      font-size: 15px;
      border-bottom: 1px solid var(--color-border-light);

      &:last-child {
        border-bottom: none;
      }

      &.router-link-active {
        color: var(--color-primary);
        background: rgba(255, 107, 107, 0.06);
      }

      .nav-icon {
        font-size: 18px;
        width: 24px;
        text-align: center;
      }
    }

    .mobile-divider {
      height: 1px;
      background: var(--color-border-light);
      margin: 8px 0;
    }

    .mobile-auth-btn {
      display: block;
      text-align: center;
      padding: 14px 24px;
      margin: 8px 16px;
      border-radius: var(--radius-md);
      font-weight: 600;
      background: var(--color-bg-tertiary);
      color: var(--color-text-primary);

      &.primary {
        background: var(--color-primary-gradient);
        color: white;
      }
    }

    .logout-link {
      color: var(--color-error);
    }
  }

  // ==================== 主内容区 ====================
  .main-content {
    flex: 1;
    margin-top: 72px;
    min-height: calc(100vh - 72px - 200px);
    position: relative;
  }

  // ==================== 页脚 ====================
  .footer {
    position: relative;
    background: linear-gradient(180deg, var(--color-bg-secondary) 0%, #F8F9FC 100%);
    border-top: 1px solid var(--color-border-light);
    padding: 48px 0 24px;
    margin-top: var(--space-3xl);
    overflow: hidden;

    &::before {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      height: 1px;
      background: linear-gradient(90deg, transparent, var(--color-primary) 50%, transparent);
    }

    .footer-content {
      max-width: 1400px;
      margin: 0 auto;
      padding: 0 24px;
      display: flex;
      justify-content: space-between;
      gap: 48px;
    }

    .footer-brand {
      flex: 0 0 280px;

      .footer-logo {
        display: flex;
        align-items: center;
        gap: 10px;
        margin-bottom: 16px;

        .logo-icon {
          font-size: 28px;
        }

        .logo-text {
          font-family: $font-display;
          font-size: 20px;
          font-weight: 700;
          color: var(--color-text-primary);
        }
      }

      .footer-slogan {
        color: var(--color-text-tertiary);
        font-size: 14px;
        margin-bottom: 20px;
      }

      .footer-social {
        display: flex;
        gap: 12px;

        .social-link {
          padding: 8px 16px;
          background: var(--color-bg-tertiary);
          border-radius: var(--radius-full);
          font-size: 13px;
          font-weight: 500;
          color: var(--color-text-secondary);
          transition: all var(--transition-base);

          &:hover {
            background: var(--color-primary);
            color: white;
            transform: translateY(-2px);
          }
        }
      }
    }

    .footer-links {
      display: flex;
      gap: 48px;
      flex: 1;
      justify-content: flex-end;

      .footer-section {
        h4 {
          font-family: $font-display;
          font-size: 16px;
          font-weight: 600;
          color: var(--color-text-primary);
          margin-bottom: 16px;
        }

        a {
          display: block;
          color: var(--color-text-tertiary);
          text-decoration: none;
          font-size: 14px;
          margin-bottom: 10px;
          transition: color var(--transition-base);

          &:hover {
            color: var(--color-primary);
          }
        }
      }
    }

    .footer-bottom {
      max-width: 1400px;
      margin: 32px auto 0;
      padding: 24px 24px 0;
      border-top: 1px solid var(--color-border-light);
      display: flex;
      justify-content: space-between;
      align-items: center;

      p {
        color: var(--color-text-tertiary);
        font-size: 13px;
      }

      .footer-made {
        display: flex;
        align-items: center;
        gap: 4px;
      }
    }
  }

  // ==================== 背景装饰 ====================
  .background-decoration {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    pointer-events: none;
    z-index: -1;
    overflow: hidden;

    .blob {
      position: absolute;
      border-radius: 30% 70% 70% 30% / 30% 30% 70% 70%;
      animation: float 8s ease-in-out infinite;
    }

    .blob-1 {
      width: 400px;
      height: 400px;
      background: var(--color-primary-gradient);
      opacity: 0.06;
      filter: blur(80px);
      top: -100px;
      right: -100px;
    }

    .blob-2 {
      width: 300px;
      height: 300px;
      background: var(--color-secondary-gradient);
      opacity: 0.08;
      filter: blur(60px);
      bottom: 20%;
      left: -50px;
      animation-delay: -2s;
    }

    .blob-3 {
      width: 200px;
      height: 200px;
      background: var(--color-accent);
      opacity: 0.1;
      filter: blur(50px);
      top: 50%;
      right: 20%;
      animation-delay: -4s;
    }
  }

  // ==================== 响应式 ====================
  @media (max-width: 1200px) {
    .nav-menu {
      gap: 4px;

      a {
        padding: 8px 12px;
        font-size: 13px;
      }
    }
  }

  @media (max-width: 1024px) {
    .header {
      .header-content {
        padding: 0 16px;
      }
    }

    .nav-menu {
      display: none;
    }

    .mobile-menu-btn {
      display: block;
    }

    .mobile-menu {
      display: flex;
    }

    .footer {
      .footer-content {
        flex-direction: column;
        gap: 32px;
      }

      .footer-brand {
        flex: 0 0 auto;
      }

      .footer-links {
        flex-wrap: wrap;
        justify-content: flex-start;
      }
    }

    .footer-bottom {
      flex-direction: column;
      gap: 12px;
      text-align: center;
    }
  }
  </style>
