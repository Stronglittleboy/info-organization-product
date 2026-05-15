<template>
  <div class="app-layout">
    <nav class="top-nav">
      <div class="nav-inner">
        <div class="nav-left">
          <router-link
            to="/collect"
            class="nav-link"
            :class="{ active: $route.name === 'collect' }"
          >
            收集
          </router-link>
          <router-link
            to="/browse"
            class="nav-link"
            :class="{ active: $route.name === 'browse' }"
          >
            浏览
          </router-link>
          <router-link
            to="/search"
            class="nav-link"
            :class="{ active: $route.name === 'search' }"
          >
            查找
          </router-link>
        </div>
        <div class="nav-right">
          <router-link
            to="/pending"
            class="nav-link-secondary"
            :class="{ active: $route.name === 'pending' }"
          >
            待处理
          </router-link>
          <router-link
            to="/topics"
            class="nav-link-secondary"
            :class="{ active: $route.name === 'topics' || $route.name === 'topicDetail' }"
          >
            专题
          </router-link>
        </div>
      </div>
    </nav>
    <main class="main-content">
      <router-view />
    </main>
  </div>
</template>

<style>
* {
  box-sizing: border-box;
}
body {
  margin: 0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  background: #f5f7fa;
  color: #303133;
}
</style>

<style scoped>
.app-layout {
  min-height: 100vh;
}
.top-nav {
  position: sticky;
  top: 0;
  z-index: 100;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  box-shadow: 0 1px 4px rgba(0,0,0,.04);
}
.nav-inner {
  max-width: 720px;
  margin: 0 auto;
  padding: 0 16px;
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.nav-left {
  display: flex;
  gap: 24px;
}
.nav-link {
  font-size: 15px;
  font-weight: 500;
  color: #909399;
  text-decoration: none;
  padding: 14px 0;
  border-bottom: 2px solid transparent;
  transition: all .2s;
}
.nav-link:hover {
  color: #303133;
}
.nav-link.active {
  color: #409eff;
  border-bottom-color: #409eff;
}
.nav-right {
  display: flex;
  gap: 16px;
}
.nav-link-secondary {
  font-size: 13px;
  font-weight: 500;
  color: #c0c4cc;
  text-decoration: none;
  padding: 14px 0;
  transition: all .2s;
}
.nav-link-secondary:hover {
  color: #606266;
}
.nav-link-secondary.active {
  color: #409eff;
}
.main-content {
  padding-top: 8px;
}
</style>
