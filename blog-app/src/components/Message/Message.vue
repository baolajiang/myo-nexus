<template>
  <transition name="message-float">

    <div v-show="visible" class="luna-message" :class="type">

      <div class="gold-line"></div>

      <div class="bg-pattern">❀</div>

      <div class="content-wrapper">
        <div class="icon-box">
          <i :class="iconClass"></i>
        </div>
        <div class="text-content">
          <div class="msg-title">{{ typeTitle }}</div>
          <div class="msg-desc">{{ content }}</div>
        </div>
      </div>

      <i v-if="showClose" class="el-icon-close close-btn" @click.stop="close"></i>

      <div v-if="duration > 0" class="progress-bar">
        <div ref="progress" class="progress-inner"></div>
      </div>
    </div>
  </transition>
</template>

<script>
export default {
  name: 'LunaMessage',
  data() {
    return {
      // === 状态控制 ===
      visible: false, // 控制弹窗显示
      closed: false,  // 标记是否已执行过关闭逻辑（防抖锁）

      // === 外部传入参数 (默认值) ===
      type: 'info',   // 消息类型: success, warning, error, info
      content: '',    // 消息内容
      duration: 3000, // 停留时间，单位毫秒。0 表示不自动关闭
      showClose: false, // 是否显示关闭按钮

      // === 内部变量 ===
      timer: null     // 保存定时器 ID，用于销毁时清除
    }
  },
  computed: {
    // 根据 type 返回对应的 Element UI 图标类名
    iconClass() {
      const map = {
        success: 'el-icon-check',
        warning: 'el-icon-warning-outline',
        error: 'el-icon-close',
        info: 'el-icon-bell'
      }
      return map[this.type] || map.info
    },
    // 根据 type 返回对应的英文标题
    typeTitle() {
      const map = {
        success: 'Success',
        warning: 'Warning',
        error: 'Error',
        info: 'Notification'
      }
      return map[this.type] || 'Message'
    }
  },
  mounted() {
    // 组件挂载完成后，立即开始倒计时逻辑
    this.startTimer();
  },
  methods: {
    // === 核心逻辑：开始倒计时 ===
    startTimer() {
      // 只有设置了有效时长才启动
      if (this.duration > 0) {

        // 1. 设置 JS 逻辑定时器：时间一到，执行关闭方法
        this.timer = setTimeout(() => {
          if (!this.closed) {
            this.close();
          }
        }, this.duration);

        // 2. 设置 CSS 动画逻辑：控制底部进度条缩减
        // 使用 setTimeout 延迟 50ms 是为了确保 DOM 已经完全渲染上树
        // 否则浏览器可能会跳过动画，直接显示最终状态
        setTimeout(() => {
          if (this.$refs.progress) {
            // 动态设置 CSS transition 时长，使其与 duration 同步
            this.$refs.progress.style.transition = `width ${this.duration}ms linear`;

            // 强制浏览器重绘 (Reflow)，这是触发 CSS 动画的关键技巧
            this.$refs.progress.offsetHeight;

            // 将宽度设置为 0%，开始播放“缩减”动画
            this.$refs.progress.style.width = '0%';
          }
        }, 50);
      }
    },

    // === 关闭逻辑 ===
    close() {
      this.closed = true;
      this.visible = false; // 触发 v-show="false"，播放离场动画

      // 如果还有定时器没跑完，手动清除，防止内存泄漏
      if (this.timer) clearTimeout(this.timer);

      // 等待 400ms (离场动画播放完毕) 后，彻底销毁组件
      setTimeout(() => {
        this.$destroy(); // 销毁 Vue 实例
        // 手动从 DOM 树中移除该节点
        if (this.$el && this.$el.parentNode) {
          this.$el.parentNode.removeChild(this.$el);
        }
      }, 400);
    }
  }
}
</script>

<style scoped>

.luna-message {
  position: fixed; /* 固定定位，悬浮在页面之上 */
  top: 90px;       /* 距离顶部的距离 */
  left: 50%;
  transform: translateX(-50%); /* 水平居中 */
  z-index: 99999;  /* 确保在最顶层，不被其他元素遮挡 */

  min-width: 360px;
  max-width: 500px;
  background: #ffffff;
  /* 阴影效果：让卡片有悬浮感 */
  box-shadow: 0 10px 40px rgba(166, 143, 88, 0.15), 0 4px 12px rgba(0,0,0,0.05);
  border-radius: 6px;
  overflow: hidden;

  display: flex;
  flex-direction: column;
  user-select: none; /* 禁止文字被选中 */
  font-family: 'Noto Serif SC', serif;
}

/* 顶部金色线条渐变色 */
.gold-line { height: 3px; width: 100%; background: linear-gradient(90deg, #e0d0b0, #d4af37, #e0d0b0); }
/* 背景花纹装饰 */
.bg-pattern { position: absolute; right: -10px; top: -15px; font-size: 80px; color: #d4af37; opacity: 0.05; font-family: serif; pointer-events: none; transform: rotate(20deg); }

.content-wrapper { padding: 18px 22px; display: flex; align-items: flex-start; position: relative; z-index: 2; }
/* 图标容器圆圈 */
.icon-box { width: 36px; height: 36px; border-radius: 50%; display: flex; align-items: center; justify-content: center; margin-right: 15px; flex-shrink: 0; font-size: 18px; background: #fffaf5; transition: all 0.3s; }
.text-content { flex: 1; display: flex; flex-direction: column; justify-content: center; min-height: 36px; }
.msg-title { font-family: 'Playfair Display', serif; font-size: 12px; color: #999; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 2px; }
.msg-desc { color: #4a4a4a; font-size: 15px; font-weight: 600; line-height: 1.5; }

/* === 各种状态下的颜色定义 === */
/* 成功 (Success) - 绿色 */
.luna-message.success .icon-box { background: rgba(103, 194, 58, 0.1); color: #67c23a; }
.luna-message.success .msg-title { color: #67c23a; }

/* 警告 (Warning) - 橙色 */
.luna-message.warning .icon-box { background: rgba(230, 162, 60, 0.1); color: #e6a23c; }
.luna-message.warning .msg-title { color: #e6a23c; }

/* 错误 (Error) - 红色 */
.luna-message.error .icon-box { background: rgba(245, 108, 108, 0.1); color: #f56c6c; }
.luna-message.error .msg-title { color: #f56c6c; }

/* 通知 (Info) - 金色 */
.luna-message.info .icon-box { background: rgba(212, 175, 55, 0.1); color: #d4af37; }
.luna-message.info .msg-title { color: #d4af37; }

/* === 进度条样式 === */
.progress-bar {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 3px; /* 进度条高度 */
  background: rgba(0,0,0,0.05); /* 底槽颜色 */
}

.progress-inner {
  height: 100%;
  width: 100%; /* 初始宽度满格 */
  background: #d4af37;
  /* width 的变化由 JS 中的 transition 控制 */
}

/* === 进度条颜色适配 === */
/* 使用 !important 提高优先级，防止被默认样式覆盖 */
.luna-message.success .progress-inner { background-color: #67c23a !important; }
.luna-message.error .progress-inner { background-color: #f56c6c !important; }
.luna-message.warning .progress-inner { background-color: #e6a23c !important; }
.luna-message.info .progress-inner { background-color: #d4af37 !important; }

/* 关闭按钮样式 */
.close-btn { position: absolute; top: 10px; right: 10px; padding: 5px; cursor: pointer; color: #ccc; font-size: 14px; transition: color 0.3s; z-index: 10; }
.close-btn:hover { color: #d4af37; }

/* === 进场/离场 动画关键帧 === */
.message-float-enter-active { animation: floatIn 0.5s cubic-bezier(0.2, 0.8, 0.2, 1); }
.message-float-leave-active { animation: floatOut 0.4s ease-in; }

/* 进场：从上方下落并淡入 */
@keyframes floatIn {
  0% { opacity: 0; transform: translate(-50%, -40px) scale(0.95); }
  100% { opacity: 1; transform: translate(-50%, 0) scale(1); }
}
/* 离场：向上飘并淡出 */
@keyframes floatOut {
  0% { opacity: 1; transform: translate(-50%, 0) scale(1); }
  100% { opacity: 0; transform: translate(-50%, -20px) scale(0.95); }
}
</style>
