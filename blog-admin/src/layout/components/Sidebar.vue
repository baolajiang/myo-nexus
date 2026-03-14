<template>
  <el-menu
      :default-active="route.path"
      class="el-menu-vertical-demo"
      background-color="#304156"
      text-color="#bfcbd9"
      active-text-color="#409EFF"
      :collapse="false"
      router
  >
    <div class="logo-container">
      <h2 class="logo-title">月之別邸后台管理</h2>
    </div>

    <el-menu-item index="/dashboard">
      <el-icon><Odometer /></el-icon>
      <span>儀表盤</span>
    </el-menu-item>

    <el-sub-menu index="blog">
      <template #title>
        <el-icon><Document /></el-icon>
        <span>博客管理</span>
      </template>
      <el-menu-item index="/article/write">
        <el-icon><Edit /></el-icon>
        <span>寫文章</span>
      </el-menu-item>
      <el-menu-item index="/article/list">
        <el-icon><List /></el-icon>
        <span>文章列表</span>
      </el-menu-item>
      <el-menu-item index="/category/index">
        <el-icon><Folder /></el-icon>
        <span>分類管理</span>
      </el-menu-item>
    </el-sub-menu>

    <el-sub-menu index="operation">
      <template #title>
        <el-icon><Operation /></el-icon>
        <span>運營管理</span>
      </template>
      <el-menu-item index="/comment/index">
        <el-icon><ChatDotRound /></el-icon>
        <span>評論管理</span>
      </el-menu-item>
      <el-menu-item index="/link/index">
        <el-icon><Link /></el-icon>
        <span>友鏈管理</span>
      </el-menu-item>
    </el-sub-menu>

    <el-sub-menu index="user">
      <template #title>
        <el-icon><User /></el-icon>
        <span>用戶管理</span>
      </template>
      <el-menu-item index="/user/index">
        <el-icon><UserFilled /></el-icon>
        <span>用戶列表</span>
      </el-menu-item>
    </el-sub-menu>

    <el-sub-menu index="system">
      <template #title>
        <el-icon><Setting /></el-icon>
        <span>系統管理</span>
      </template>

      <el-menu-item index="/ai/chat">
        <el-icon><ChatDotRound /></el-icon>
        <template #title>AI 指挥官</template>
      </el-menu-item>

      <el-menu-item index="/system/media">
        <el-icon><Picture /></el-icon>
        <span>附件管理</span>
      </el-menu-item>

      <el-menu-item index="/system/cache">
        <el-icon><Refresh /></el-icon>
        <span>緩存監控</span>
      </el-menu-item>

      <el-menu-item index="/system/ip">
        <el-icon><Warning /></el-icon>
        <span>IP 黑名單</span>
      </el-menu-item>

      <el-menu-item index="/system/log">
        <el-icon><DataLine /></el-icon>
        <span>操作日誌</span>
      </el-menu-item>
    </el-sub-menu>

      <el-menu-item index="/system/task">
        <el-icon><Timer /></el-icon>
        <span>任務管理</span>
      </el-menu-item>

  </el-menu>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'
// 引入所有需要的圖標
import {
  Odometer, Document, Edit, List, Folder,
  Operation, ChatDotRound, Link,
  User, UserFilled,
  Setting, Warning, DataLine, Picture, Refresh, Timer
} from '@element-plus/icons-vue'

const route = useRoute()
</script>

<style scoped>
.el-menu-vertical-demo {
  height: 100%;
  border-right: none;
  overflow-y: auto; /* 允許垂直滾動 */
  overflow-x: hidden; /* 禁止水平滾動 */
}

/* === 核心修復代碼：隱藏滾動條 === */
.el-menu-vertical-demo::-webkit-scrollbar {
  width: 0;    /* Safari, Chrome */
  height: 0;   /* Safari, Chrome */
  display: none; /* 徹底隱藏 */
}

/* 兼容 Firefox */
.el-menu-vertical-demo {
  scrollbar-width: none; /* Firefox */
  -ms-overflow-style: none; /* IE 10+ */
}

.logo-container {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #2b3649;
  /* 確保 Logo 區不會跟著菜單滾動，如果希望 Logo 固定在頂部，可以加 position: sticky */
  position: sticky;
  top: 0;
  z-index: 10;
}

.logo-title {
  color: #fff;
  font-weight: 600;
  font-size: 20px;
}
</style>