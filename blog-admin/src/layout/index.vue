<template>
  <div class="common-layout">
    <el-container class="app-wrapper">
      <el-aside width="200px" style="background-color: #304156;">
        <Sidebar />
      </el-aside>

      <el-container>
        <el-header style="padding: 0; height: 60px;">
          <Navbar />
        </el-header>

        <el-main style="padding: 0;">
          <AppMain />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import Sidebar from './components/Sidebar.vue'
import Navbar from './components/Navbar.vue'
import AppMain from './components/AppMain.vue'
</script>

<style scoped>
.common-layout {
  height: 100vh;
  width: 100vw;
  overflow: hidden; /* 关键：干掉浏览器原生的全局滚动条 */
}

.app-wrapper {
  height: 100%; /* 让内部容器继承 100vh 的高度 */
}
</style>