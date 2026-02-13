<template>
  <div class="navbar">
    <div class="left">
      <h3>後台管理系統</h3>
    </div>
    <div class="right">
      <el-dropdown>
        <span class="el-dropdown-link">
          {{ userInfo.nickname  }}
          <el-icon class="el-icon--right"><arrow-down /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item>個人中心</el-dropdown-item>
            <el-dropdown-item divided @click="handleLogout">退出登錄</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ArrowDown } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ref, onMounted } from 'vue'
import { getCurrentUser } from '../../api/user'
import { getConfig } from '../../utils/config'

const { frontendLoginUrl } = getConfig()

// 用户信息
const userInfo = ref({
  nickname: '',
  account: '',
  avatar: ''
})


// 获取当前用户信息
const fetchUserInfo = async () => {
  try {
    const response = await getCurrentUser()
    if (response.data.success) {
      userInfo.value = response.data.data
    } else {
      // API调用成功但业务逻辑失败（比如token失效）
      const errorMessage = response.data.message || '获取用户信息失败'
      ElMessage.error(errorMessage)
      }
  } catch (error) {
    console.error('获取用户信息失败:', error)
  }
}


// 处理退出登录
const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    // 1. 清除本地存储的 Token
    localStorage.removeItem('token')

    // 2. 提示退出成功
    ElMessage.success('退出成功')

    // 3. 强制跳转回前台登录页 (请确保端口号对应前台项目)
    window.location.href = frontendLoginUrl
  }).catch(() => {
    // 取消退出，不做操作
  })
}

// 组件挂载时获取用户信息
onMounted(() => {
  fetchUserInfo()
})
</script>

// ... existing code ...
<style scoped>
/* 基础样式 */
.navbar {
  height: 60px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  box-shadow: 0 1px 4px rgba(0,21,41,.08);
  background: #fff;
}

.left h3 {
  margin: 0;
  color: #333;
  font-size: 18px;
}

/* 右侧用户信息样式 */
.right {
  display: flex;
  align-items: center;
}

.el-dropdown-link {
  cursor: pointer;
  display: flex;
  align-items: center;
  padding: 8px 12px;
  border-radius: 4px;
  transition: background-color 0.3s;
  color: #606266;
  font-size: 14px;
}

.el-dropdown-link:hover {
  background-color: #f5f7fa;
}

/* 调试样式 - 确保文字可见 */
.debug-info {
  font-size: 12px;
  color: #67c23a;
  margin-left: 8px;
  padding: 2px 6px;
  background: #f0f9ff;
  border-radius: 3px;
  border: 1px solid #b3e19d;
}

.debug-loading {
  font-size: 12px;
  color: #f56c6c;
  margin-left: 8px;
  padding: 2px 6px;
  background: #fef0f0;
  border-radius: 3px;
  border: 1px solid #fbc4c4;
}

/* 确保文字不会被隐藏 */
.user-nickname {
  font-weight: 500;
  color: #303133;
  min-width: 60px; /* 确保有足够空间显示文字 */
  text-overflow: ellipsis;
  overflow: hidden;
  white-space: nowrap;
  max-width: 120px;
}
</style>
