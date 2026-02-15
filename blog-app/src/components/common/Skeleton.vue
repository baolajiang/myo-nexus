<template>
  <transition name="skeleton-fade">
    <div v-if="loading" class="luna-skeleton">

      <div class="skeleton-logo">
        <svg viewBox="0 0 100 100" class="moon-svg">
          <path d="M50 0 C20 0 0 20 0 50 C0 80 20 100 50 100 C40 80 40 20 50 0 Z" fill="currentColor" />
        </svg>
      </div>

      <div class="skeleton-shimmer"></div>

    </div>
  </transition>
</template>

<script>
export default {
  name: 'Skeleton',
  props: {
    // 控制开关：true 显示骨架屏，false 隐藏（触发淡出动画）
    loading: {
      type: Boolean,
      default: true
    }
  }
}
</script>

<style scoped>
/* 核心容器
  使用 absolute 铺满父容器 (前提：父容器需要 position: relative)
*/
.luna-skeleton {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 10; /* 确保盖在图片上方 */

  background-color: var(--bg-card, #fffaf5);

  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  pointer-events: none; /* 让点击穿透，防止挡住底层交互（可选） */
}

/* Logo 样式 */
.skeleton-logo {
  width: 30%;       /* 相对宽度，适应不同大小容器 */
  max-width: 60px;  /* 最大限制 */
  aspect-ratio: 1/1;

  /* 颜色：优先主题金，否则默认金 */
  color: var(--accent-color, #d4af37);
  opacity: 0.2;
  z-index: 11;
  animation: luna-pulse 2s infinite ease-in-out;
}

/* 扫光条 */
.skeleton-shimmer {
  position: absolute;
  top: 0; left: 0;
  width: 100%; height: 100%;
  z-index: 12;
  /* 渐变扫光：优先主题变量 */
  background: linear-gradient(
    100deg,
    transparent 30%,
    var(--accent-alpha, rgba(212, 175, 55, 0.3)) 50%,
    transparent 70%
  );
  transform: skewX(-20deg) translateX(-150%);
  animation: luna-shimmer 1.5s infinite;
}


@keyframes luna-shimmer {
  0% { transform: skewX(-20deg) translateX(-150%); }
  100% { transform: skewX(-20deg) translateX(150%); }
}

@keyframes luna-pulse {
  0% { opacity: 0.2; transform: scale(0.95); }
  50% { opacity: 0.4; transform: scale(1.05); }
  100% { opacity: 0.2; transform: scale(0.95); }
}

/* === Vue Transition 淡出效果 === */
.skeleton-fade-enter-active,
.skeleton-fade-leave-active {
  transition: opacity 0.5s ease;
}
.skeleton-fade-enter,
.skeleton-fade-leave-to {
  opacity: 0;
}
</style>
