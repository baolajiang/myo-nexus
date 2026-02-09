<template>
  <div class="ethereal-hero">

    <div class="hero-bg" :style="{ backgroundImage: `url(${bgImage})` }"></div>

    <div class="hero-overlay"></div>

    <canvas ref="canvasBg" class="hero-canvas"></canvas>

    <div class="hero-content">

      <div class="avatar-wrap">
        <img :src="$store.state.avatar || defaultAvatar" class="avatar" alt="avatar" />
        <div class="avatar-glow"></div>
      </div>

      <h1 class="hero-name">{{ $store.state.name || "Master" }}</h1>

      <div class="hero-motto">
        <span class="type-text">{{ displayedText }}</span><span class="cursor" v-show="showCursor">|</span>
      </div>

      <div class="hero-meta">
        <span class="meta-item">Lv.7</span>
        <span class="meta-divider"></span>
        <span class="meta-item">Front-End Developer</span>
        <span class="meta-divider"></span>
        <span class="meta-item">ACGN Lover</span>
      </div>

    </div>

    <div class="scroll-down" @click="scrollDown">
      <span class="scroll-text">Discover</span>
      <i class="el-icon-arrow-down scroll-arrow"></i>
    </div>

  </div>
</template>

<script>
// 使用 require 确保静态资源路径在 webpack 打包后正确
import defaultAvatarImg from '@/assets/img/default_avatar.png';
// 你的日落背景图
import bgImg from '../../../static/img/anime-sunset-art-wallpaper-2560x1080_14.jpg';

export default {
  name: "HeaderTop",
  data() {
    return {
      bgImage: bgImg,
      defaultAvatar: defaultAvatarImg,

      // 打字机状态
      fullText: "There is always light behind the clouds.", // 你的签名
      displayedText: "",
      showCursor: true,

      // Canvas 粒子数据
      canvas: null,
      ctx: null,
      particles: [],
      animationFrameId: null
    };
  },
  mounted() {
    this.startTypewriter();
    this.initCanvas();
    window.addEventListener('resize', this.resizeCanvas);
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.resizeCanvas);
    if (this.animationFrameId) {
      cancelAnimationFrame(this.animationFrameId);
    }
  },
  methods: {
    // 1. 极简打字机效果
    startTypewriter() {
      let index = 0;
      const typeSpeed = 100; // 打字速度

      const typeInterval = setInterval(() => {
        if (index < this.fullText.length) {
          this.displayedText += this.fullText.charAt(index);
          index++;
        } else {
          clearInterval(typeInterval);
          // 打字完成后让光标继续闪烁
          setInterval(() => {
            this.showCursor = !this.showCursor;
          }, 500);
        }
      }, typeSpeed);
    },

    // 2. 初始化 Canvas (唯美星尘粒子)
    initCanvas() {
      this.canvas = this.$refs.canvasBg;
      this.ctx = this.canvas.getContext('2d');
      this.resizeCanvas();
      this.createParticles();
      this.animateParticles();
    },

    resizeCanvas() {
      if (!this.canvas) return;
      this.canvas.width = window.innerWidth;
      this.canvas.height = window.innerHeight;
    },

    createParticles() {
      const particleCount = 80; // 粒子数量
      for (let i = 0; i < particleCount; i++) {
        this.particles.push({
          x: Math.random() * this.canvas.width,
          y: Math.random() * this.canvas.height,
          size: Math.random() * 2.5 + 0.5, // 大小
          speedX: Math.random() * 0.5 - 0.25, // 左右漂移
          speedY: Math.random() * -1 - 0.2,   // 向上漂浮
          opacity: Math.random() * 0.5 + 0.1,
          glow: Math.random() > 0.5 ? '#fb7299' : '#ffffff' // 你的主题粉色与白色交替
        });
      }
    },

    animateParticles() {
      this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);

      for (let i = 0; i < this.particles.length; i++) {
        let p = this.particles[i];

        // 绘制粒子 (带发光效果)
        this.ctx.beginPath();
        this.ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2);
        this.ctx.fillStyle = `rgba(${p.glow === '#fb7299' ? '251,114,153' : '255,255,255'}, ${p.opacity})`;
        this.ctx.shadowBlur = 10;
        this.ctx.shadowColor = p.glow;
        this.ctx.fill();

        // 移动粒子
        p.x += p.speedX;
        p.y += p.speedY;

        // 如果粒子飘出屏幕顶部，让它从底部重新出现
        if (p.y < 0) {
          p.y = this.canvas.height;
          p.x = Math.random() * this.canvas.width;
        }
        // 左右出界处理
        if (p.x > this.canvas.width || p.x < 0) {
          p.speedX = -p.speedX;
        }
      }

      this.animationFrameId = requestAnimationFrame(this.animateParticles);
    },

    // 3. 平滑滚动
    scrollDown() {
      window.scrollTo({
        top: window.innerHeight,
        behavior: 'smooth'
      });
    }
  }
};
</script>

<style scoped>
/* 引入谷歌字体，打造 d-d.design 那种高级排版感 */
@import url('https://fonts.googleapis.com/css2?family=Montserrat:wght@300;400;700&family=Noto+Serif+SC:wght@300;700&display=swap');

.ethereal-hero {
  position: relative;
  width: 100%;
  height: 100vh;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: 'Montserrat', 'Noto Serif SC', sans-serif;
}

/* 1. 背景层 */
.hero-bg {
  position: absolute;
  top: 0; left: 0; width: 100%; height: 100%;
  background-size: cover;
  background-position: center;
  z-index: 1;
  /* 极其缓慢的放大动画，增加沉浸感 */
  animation: bgZoom 30s linear infinite alternate;
}
@keyframes bgZoom {
  0% { transform: scale(1); }
  100% { transform: scale(1.1); }
}

/* 2. 遮罩层 (至关重要：压暗背景，让白色文字发光) */
.hero-overlay {
  position: absolute;
  top: 0; left: 0; width: 100%; height: 100%;
  z-index: 2;
  /* 顶部透明，底部深色渐变，融合你的网站背景色 */
  background: linear-gradient(180deg, rgba(0,0,0,0.1) 0%, rgba(0,0,0,0.6) 100%);
}

/* 3. Canvas 粒子层 */
.hero-canvas {
  position: absolute;
  top: 0; left: 0; width: 100%; height: 100%;
  z-index: 3;
  pointer-events: none; /* 防止遮挡点击事件 */
}

/* 4. 内容层 */
.hero-content {
  position: relative;
  z-index: 10;
  text-align: center;
  color: #fff;
  padding: 0 20px;
  /* 入场上浮动画 */
  animation: contentFadeIn 1.5s cubic-bezier(0.23, 1, 0.32, 1) forwards;
  opacity: 0;
  transform: translateY(30px);
}
@keyframes contentFadeIn {
  to { opacity: 1; transform: translateY(0); }
}

/* --- 排版细节 --- */

/* 头像：极简描边，自带光晕 */
.avatar-wrap {
  position: relative;
  width: 100px;
  height: 100px;
  margin: 0 auto 20px;
  border-radius: 50%;
}
.avatar {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid rgba(255,255,255,0.8);
  position: relative;
  z-index: 2;
  transition: transform 0.5s;
}
.avatar-wrap:hover .avatar {
  transform: rotate(360deg);
}
.avatar-glow {
  position: absolute;
  top: 0; left: 0; width: 100%; height: 100%;
  border-radius: 50%;
  background: #fb7299; /* 你的主题粉色 */
  filter: blur(20px);
  opacity: 0.6;
  z-index: 1;
  animation: pulseGlow 3s infinite alternate;
}
@keyframes pulseGlow {
  0% { transform: scale(0.9); opacity: 0.4; }
  100% { transform: scale(1.2); opacity: 0.8; }
}

/* 大标题：极大、极细、极简 */
.hero-name {
  font-size: 4rem;
  font-weight: 700;
  letter-spacing: 4px;
  margin: 0 0 15px 0;
  text-transform: uppercase;
  text-shadow: 0 10px 30px rgba(0,0,0,0.5); /* 阴影替代卡片背景 */
}

/* 签名：优雅的衬线或细体 */
.hero-motto {
  font-size: 1.2rem;
  font-weight: 300;
  letter-spacing: 2px;
  margin-bottom: 40px;
  opacity: 0.9;
  text-shadow: 0 2px 10px rgba(0,0,0,0.5);
}

/* Meta 信息：融合二次元与高级感 */
.hero-meta {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 15px;
  font-size: 14px;
  font-weight: 400;
  letter-spacing: 1px;
  opacity: 0.8;
}
.meta-item {
  padding: 4px 12px;
  border: 1px solid rgba(255,255,255,0.3);
  border-radius: 20px;
  background: rgba(0,0,0,0.2);
  backdrop-filter: blur(4px); /* 仅在小标签上使用微弱的毛玻璃 */
}
.meta-divider {
  width: 4px; height: 4px;
  background: #fff;
  border-radius: 50%;
  opacity: 0.5;
}

/* 5. 滚动提示 */
.scroll-down {
  position: absolute;
  bottom: 30px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 10;
  display: flex;
  flex-direction: column;
  align-items: center;
  color: rgba(255,255,255,0.7);
  cursor: pointer;
  transition: color 0.3s;
}
.scroll-down:hover {
  color: #fff;
}
.scroll-text {
  font-size: 12px;
  letter-spacing: 2px;
  text-transform: uppercase;
  margin-bottom: 5px;
}
.scroll-arrow {
  font-size: 20px;
  animation: floatDown 2s infinite;
}
@keyframes floatDown {
  0%, 100% { transform: translateY(0); opacity: 0.5; }
  50% { transform: translateY(10px); opacity: 1; }
}

/* 响应式 */
@media (max-width: 768px) {
  .hero-name { font-size: 2.5rem; letter-spacing: 2px; }
  .hero-motto { font-size: 1rem; }
  .hero-meta { flex-direction: column; gap: 10px; border: none; background: transparent; }
  .meta-divider { display: none; }
  .meta-item { border: none; padding: 0; background: transparent; backdrop-filter: none; }
}
</style>
