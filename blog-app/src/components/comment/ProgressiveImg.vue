
<template>
  <div class="progressive-wrap" :class="customClass">
    <img
      :src="currentImg"
      class="progressive-img"
      :class="{ 'is-loading': !imgLoaded }"
      :alt="alt"
      @click="$emit('click')"
    >
  </div>
</template>

<script>
import { loadProgressiveImage } from "@/utils/progressive";

export default {
  name: 'ProgressiveImg',
  props: {
    src: { type: String, required: true },
    alt: { type: String, default: '' },
    customClass: { type: String, default: '' } // 允许外部传入样式类调整大小
  },
  data() {
    return {
      imgLoaded: false,
      currentImg: ''
    }
  },
  watch: {
    // 监听 src 变化（防止组件复用时图片不更新）
    src: {
      handler(val) {
        this.loadImage(val);
      },
      immediate: true
    }
  },
  methods: {
    loadImage(url) {
      // 重置状态
      this.imgLoaded = false;
      loadProgressiveImage(url, (currentUrl, isLoaded) => {
        this.currentImg = currentUrl;
        this.imgLoaded = isLoaded;
      });
    }
  }
}
</script>

<style scoped>
.progressive-wrap {
  width: 100%;
  height: 100%;
  overflow: hidden; /* 防止放大时溢出 */
  position: relative;
}

.progressive-img {
  width: 100%;
  height: 100%;
  object-fit: cover; /* 默认铺满 */
  transition: filter 1.2s ease, transform 0.6s ease; /* 动画时间稍微给长一点 */
  will-change: transform, filter;
}

.progressive-img.is-loading {
  filter: blur(15px);
  transform: scale(1.1); /* 模糊时稍微放大，防止白边 */
}
</style>
