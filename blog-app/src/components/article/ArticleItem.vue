<template>
  <div class="luna-wrap" ref="cardWrap">
    <div
      class="luna-card"
      :class="themeClass"
      ref="card"
      @click="view(id)"
      @mouseenter="setHoverLevel(1)"
      @mouseleave="setHoverLevel(0)"
    >
      <div class="moon-crest" ref="moon">
        <svg viewBox="0 0 100 100" class="moon-svg">
          <path d="M50 0 C20 0 0 20 0 50 C0 80 20 100 50 100 C40 80 40 20 50 0 Z" fill="currentColor" />
        </svg>
      </div>

      <div class="sakura-container" ref="sakuraContainer">
        <span class="petal p1">❀</span>
        <span class="petal p2">❀</span>
      </div>

      <div
        class="frame-box"
        @mouseenter.stop="setHoverLevel(2)"
        @mouseleave.stop="setHoverLevel(1)"
      >
        <div class="img-mask">
          <img
            :src="currentImg"
            class="luna-img"
            :class="{ 'is-loading': !imgLoaded }"
            ref="img"
            alt="cover"
            loading="lazy"
          >
        </div>
        <div class="gold-sheen" ref="sheen"></div>
        <div class="gold-border-overlay" ref="borderOverlay"></div>
      </div>

      <div class="text-panel">
        <div class="fancy-line-top" ref="lineTop"></div>

        <div class="meta-row">
          <span class="chapter-num">CHAPTER.{{ index + 1 }}</span>
          <span class="luna-date">{{ createDate | format }}</span>
        </div>

        <h3 class="luna-title" ref="title">{{ title }}</h3>

        <p class="luna-summary">{{ summary }}</p>

        <div class="luna-footer">
          <div class="tag-ribbon">
            <span>{{ tags && tags.length > 0 ? tags[0].tagName : 'Story' }}</span>
          </div>

          <div class="read-more-btn" ref="btn">
            Read More <span class="flower-icon">✿</span>
          </div>
        </div>

        <div class="fancy-line-bottom" ref="lineBottom"></div>
      </div>

    </div>
  </div>
</template>

<script>
import { formatTime } from "@/utils/time";
import { gsap } from "gsap";
import { loadProgressiveImage } from "@/utils/progressive";

export default {
  name: 'ArticleItem',
  props: {
    // 接收主题参数：'light' (默认/朝日) 或 'dark' (夜晚/露娜)
    theme: { type: String, default: 'light' },
    id: String, weight: Number, title: String, commentCounts: Number, viewCounts: Number,
    summary: String, author: [String, Object], tags: Array, createDate: String, cover: String,
    index: { type: Number, default: 0 }
  },
  data() {
    return {
      // 0 = 离开, 1 = 卡片悬浮, 2 = 图片悬浮
      hoverLevel: 0,
      imgLoaded: false, // 标记大图是否加载完成
      currentImg: '',   // 当前显示的图片链接
    }
  },
  computed: {
    themeClass() {
      return this.theme === 'dark' ? 'is-dark' : 'is-light';
    }
  },
  watch: {
    hoverLevel(newVal) {
      this.animateState(newVal);
    },
    // 监听主题变化，强制刷新动画状态以应用新颜色
    theme() {
      this.setHoverLevel(0);
    }
  },
  mounted() {
    loadProgressiveImage(this.cover, (url, loaded) => {
      this.currentImg = url;
      this.imgLoaded = loaded;
    });
    gsap.from(this.$refs.cardWrap, {
      opacity: 0,
      y: 20,
      duration: 1,
      ease: "power2.out",
      delay: this.index * 0.15
    });
  },
  methods: {
    view(id) {  // 判断是否为短ID，如果是短ID则使用短ID路由
      if (/^\d{1,6}$/.test(id)) {
        this.$router.push({ path: `/view/${id}` })
      } else {
        // 长ID保持原路由
        this.$router.push({ path: `/view/${id}` })
      } },

    setHoverLevel(level) {
      this.hoverLevel = level;
    },

    animateState(level) {
      const commonOpts = { overwrite: true, duration: 0.5, ease: "power2.out" };

      // ★ 关键修复：不再使用 scale 1.08，统一最大缩放为 1.05
      // 这样从文字滑到图片时，图片大小不会变，只有光效变化
      const targetScale = 1.05;

      if (level === 0) {
        // === State 0: 离开 (复原) ===
        gsap.to(this.$refs.img, { scale: 1, ...commonOpts });

        // 阴影和边框复原
        gsap.to(this.$refs.card, { boxShadow: "var(--shadow-base)", ...commonOpts });
        gsap.to(this.$refs.borderOverlay, { borderColor: "var(--border-dim)", boxShadow: "inset 0 0 10px var(--accent-alpha)", ...commonOpts });

        // 线条复原 (透明度降低)
        gsap.to([this.$refs.lineTop, this.$refs.lineBottom], { opacity: 0.5, ...commonOpts });
        // 背景颜色复原 (用 clearProps 清除 GSAP 的内联样式，让 CSS 变量接管)
        gsap.set([this.$refs.lineTop, this.$refs.lineBottom], { background: (i) => i===0 ? "linear-gradient(90deg, var(--accent-color), transparent)" : "linear-gradient(90deg, transparent, var(--accent-color))" });

        // 装饰元素复原
        gsap.to(this.$refs.moon, { rotation: 0, opacity: 0.2, duration: 1, overwrite: true });
        gsap.to(this.$refs.sakuraContainer, { y: 0, opacity: 0.4, duration: 1, overwrite: true });

        // ★ 颜色复原：清除 GSAP 样式， CSS 定义的 theme 颜色
        gsap.to(this.$refs.title, { color: "var(--text-main)", x: 0, duration: 0.3, overwrite: true });
        gsap.to(this.$refs.btn, { color: "var(--text-sub)", x: 0, duration: 0.3, overwrite: true });

      } else if (level === 1) {
        // === State 1: 卡片悬浮 (文字区) ===
        gsap.to(this.$refs.img, { scale: targetScale, duration: 1.2, ease: "power2.out", overwrite: true });

        gsap.to(this.$refs.card, { boxShadow: "var(--shadow-hover)", ...commonOpts });
        gsap.to(this.$refs.borderOverlay, { borderColor: "var(--accent-color)", boxShadow: "inset 0 0 15px var(--accent-alpha)", ...commonOpts });
        gsap.to([this.$refs.lineTop, this.$refs.lineBottom], { opacity: 1, background: "var(--accent-color)", ...commonOpts });

        gsap.to(this.$refs.moon, { rotation: 10, opacity: 0.6, duration: 1, overwrite: true });
        gsap.to(this.$refs.sakuraContainer, { y: 5, opacity: 0.8, duration: 1, overwrite: true });

        // 文字变色 (使用 CSS 变量)
        gsap.to(this.$refs.title, { color: "var(--accent-color)", x: 2, duration: 0.3, overwrite: true });
        gsap.to(this.$refs.btn, { color: "var(--accent-color)", x: -2, duration: 0.3, overwrite: true });

      } else if (level === 2) {
        // === State 2: 图片悬浮 (仅扫光) ===
        // ★ 修复：保持缩放比例不变 (1.05)，消除顿挫感
        gsap.to(this.$refs.img, { scale: targetScale, duration: 1.2, ease: "power2.out", overwrite: true });

        // 触发扫光
        gsap.fromTo(this.$refs.sheen,
          { x: "-100%", opacity: 0 },
          { x: "100%", opacity: 0.4, duration: 0.6, ease: "power2.inOut", overwrite: true }
        );

        // 保持 State 1 的外观
        gsap.to(this.$refs.card, { boxShadow: "var(--shadow-hover)", ...commonOpts });
        gsap.to(this.$refs.borderOverlay, { borderColor: "var(--accent-color)", boxShadow: "inset 0 0 15px var(--accent-alpha)", ...commonOpts });
        gsap.to([this.$refs.lineTop, this.$refs.lineBottom], { opacity: 1, background: "var(--accent-color)", ...commonOpts });

        // 保持文字高亮
        gsap.to(this.$refs.title, { color: "var(--accent-color)", x: 2, duration: 0.3, overwrite: true });
      }
    }
  },
  filters: {
    format: formatTime
  }
}
</script>

<style scoped>

/* ================= 1. CSS 变量定义 (主题管控) ================= */

/* --- Light Theme (默认/朝日) --- */
.is-light {
  --bg-card: #fffaf5;
  --bg-img-box: #fff;
  --text-main: #4a4a4a;
  --text-sub: #aaa;
  --accent-color: #d4af37; /* 金色 */
  --accent-alpha: rgba(212, 175, 55, 0.3);
  --border-outer: #e8e0d5;
  --border-dim: rgba(212, 175, 55, 0.5);
  --shadow-base: 0 5px 15px rgba(0,0,0,0.03);
  --shadow-hover: 0 10px 40px rgba(0,0,0,0.08);
  --moon-color: #d4af37;
  --petal-color: #ffb7c5;
}

/* --- Dark Theme (夜间/露娜) --- */
.is-dark {
  --bg-card: #222233;     /* 深蓝夜色 */
  --bg-img-box: #2a2a3a;
  --text-main: #e0d8c8;   /* 米白 */
  --text-sub: #888899;
  --accent-color: #ffd700; /* 亮金 */
  --accent-alpha: rgba(255, 215, 0, 0.3);
  --border-outer: #444455;
  --border-dim: rgba(255, 215, 0, 0.3);
  --shadow-base: 0 5px 15px rgba(0,0,0,0.5);
  --shadow-hover: 0 10px 40px rgba(255, 215, 0, 0.15);
  --moon-color: #ffd700;
  --petal-color: #e0e0e0; /* 银白花瓣 */
}

/* ================= 2. 布局样式 ================= */

.luna-wrap {
  padding: 15px 0;
  width: 100%;
  display: flex;
  justify-content: center;
}

.luna-card {
  position: relative;
  display: flex;
  height: 240px;
  width: 100%;
  max-width: 1000px;
  background: var(--bg-card); /* 应用变量 */
  border-radius: 4px;
  cursor: pointer;
  border: 1px solid var(--border-outer); /* 应用变量 */
  box-shadow: var(--shadow-base); /* 应用变量 */
  overflow: hidden;
  font-family: 'Playfair Display', 'Noto Serif SC', serif;
  will-change: box-shadow;
  transition: background-color 0.3s, border-color 0.3s; /* 主题切换时的过渡 */
}

/* 装饰层 */
.moon-crest {
  position: absolute;
  top: -20px; right: -20px;
  width: 120px; height: 120px;
  opacity: 0.2;
  z-index: 0;
  pointer-events: none;
  color: var(--moon-color); /* 应用变量 */
}
.moon-svg {
  width: 100%; height: 100%;
  filter: drop-shadow(0 0 5px currentColor);
}

.sakura-container {
  position: absolute;
  bottom: 10px; right: 10px;
  pointer-events: none;
  opacity: 0.4;
  z-index: 0;
  color: var(--petal-color); /* 应用变量 */
}
.petal { position: absolute; font-size: 20px; }
.p1 { bottom: 0; right: 0; transform: rotate(15deg); }
.p2 { bottom: 20px; right: 30px; transform: rotate(-10deg) scale(0.8); }

/* 图片区域 */
.frame-box {
  flex: 0 0 320px;
  position: relative;
  margin: 0;
  background: var(--bg-img-box);
  z-index: 2;
  border-radius: 4px 0 0 4px;
  overflow: hidden;
  border-right: 1px solid var(--accent-color); /* 永远是强调色 */
}

.gold-border-overlay {
  position: absolute;
  top: 0; left: 0; width: 100%; height: 100%;
  border: 4px solid transparent; /* 改为透明，让背景透出来 */
  outline: 1px solid var(--border-dim);
  box-shadow: inset 0 0 10px var(--accent-alpha);
  z-index: 3;
  pointer-events: none;
  border-radius: 4px 0 0 4px;
  outline-offset: -5px;
}

.gold-sheen {
  position: absolute;
  top: 0; left: 0; width: 50%; height: 100%;
  background: linear-gradient(to right, transparent, rgba(255,255,255,0.6), transparent);
  transform: skewX(-20deg);
  z-index: 2;
  opacity: 0;
  pointer-events: none;
}

.img-mask { width: 100%; height: 100%; background: var(--bg-img-box); }
.luna-img { width: 100%; height: 100%; object-fit: cover; will-change: transform;
  transition: filter 0.6s ease-in-out, transform 0.6s ease;
}
.luna-img.is-loading{
  filter: blur(15px);
  /* 轻微放大图片，防止模糊导致的边缘白边问题
     (注意：这可能会和 GSAP hover 动画有一点点冲突，
     如果 hover 时有跳动，可以把这个 scale 去掉，或者在 frame-box 上加背景色)
  */
  transform: scale(1.1);
}

/* 内容区域 */
.text-panel {
  flex: 1;
  padding: 25px 35px 25px 25px;
  display: flex;
  flex-direction: column;
  position: relative;
  z-index: 1;
}

.fancy-line-top {
  width: 100%; height: 1px;
  background: linear-gradient(90deg, var(--accent-color), transparent);
  margin-bottom: 15px;
  opacity: 0.5;
}
.fancy-line-bottom {
  width: 100%; height: 1px;
  background: linear-gradient(90deg, transparent, var(--accent-color));
  margin-top: 15px;
  opacity: 0.5;
}

.meta-row {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--text-sub);
  font-style: italic;
  margin-bottom: 8px;
}
.chapter-num { color: var(--accent-color); letter-spacing: 1px; }

.luna-title {
  font-size: 1.6rem;
  color: var(--text-main);
  font-weight: 700;
  margin-bottom: 12px;
  line-height: 1.3;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 1;
  overflow: hidden;



}

.luna-summary {
  font-size: 14px;
  color: var(--text-sub);
  line-height: 1.8;
  margin-bottom: auto;
  display: -webkit-box;
  /*! autoprefixer: off */
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
  text-overflow: ellipsis;
  text-align: left;
}

.luna-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 5px;
}

.tag-ribbon span {
  background: rgba(255,255,255,0.1);
  color: var(--accent-color);
  padding: 2px 10px;
  font-size: 12px;
  border: 1px solid var(--border-dim);
  border-radius: 2px;
}

.read-more-btn {
  font-family: 'Playfair Display', serif;
  font-size: 14px;
  color: var(--text-sub);
  font-style: italic;
  display: flex;
  align-items: center;
  gap: 5px;
}
.flower-icon { font-size: 16px; }

@media (max-width: 768px) {
  .luna-card { flex-direction: column; height: auto; }
  .frame-box { flex: 0 0 200px; border-radius: 4px 4px 0 0; border-right: none; border-bottom: 1px solid var(--accent-color); }
  .gold-border-overlay { border-radius: 4px 4px 0 0; }
  .text-panel { padding: 20px; }
}
</style>
