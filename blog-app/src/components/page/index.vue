<template>
  <div class="ethereal-hero">
    <div class="hero-bg" :style="{ backgroundImage: `url(${bgImage})` }"></div>

    <div class="hero-overlay"></div>

    <canvas ref="canvasBg" class="canvas-layer bg-canvas"></canvas>

    <canvas ref="canvasText" class="canvas-layer text-canvas"></canvas>

    <div class="scroll-down" @click="scrollDown">
      <span class="scroll-text">Start Reading</span>
      <i class="el-icon-arrow-down scroll-arrow"></i>
    </div>
  </div>
</template>

<script>
// 请确保这里的路径是你项目中正确的图片路径
import bgImg from '../../../static/img/anime-sunset-art-wallpaper-2560x1080_14.jpg';

export default {
  name: "HeaderTop",
  data() {
    return {
      bgImage: bgImg,
      width: 0,
      height: 0,
      animationFrameId: null,

      // --- 组1：背景星尘参数 (氛围) ---
      bgCtx: null,
      bgParticles: [],

      // --- 组2：文字粒子参数 (交互) ---
      textCtx: null,
      textParticles: [],
      // radius 改为 20，只有鼠标非常靠近时才触发，消除"圆圈感"
      mouse: { x: -1000, y: -1000, radius: 20 },

      heroText: "测试文本", // 博客标题
      textSize: 100, // 字号
    };
  },
  mounted() {
    this.init();
    window.addEventListener('resize', this.handleResize);
    window.addEventListener('mousemove', this.handleMouseMove);
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.handleResize);
    window.removeEventListener('mousemove', this.handleMouseMove);
    cancelAnimationFrame(this.animationFrameId);
  },
  methods: {
    init() {
      this.width = window.innerWidth;
      this.height = window.innerHeight;

      // 1. 初始化背景 Canvas
      const cvs1 = this.$refs.canvasBg;
      cvs1.width = this.width;
      cvs1.height = this.height;
      this.bgCtx = cvs1.getContext('2d');

      // 2. 初始化文字 Canvas
      const cvs2 = this.$refs.canvasText;
      cvs2.width = this.width;
      cvs2.height = this.height;
      // willReadFrequently 优化频繁读取操作
      this.textCtx = cvs2.getContext('2d', { willReadFrequently: true });

      this.createBgParticles();
      this.createTextParticles();
      this.animate();
    },

    // --- 背景星尘逻辑 (保持唯美风格) ---
    createBgParticles() {
      this.bgParticles = [];
      const count = 60;
      for (let i = 0; i < count; i++) {
        this.bgParticles.push({
          x: Math.random() * this.width,
          y: Math.random() * this.height,
          size: Math.random() * 2 + 0.5,
          speedY: Math.random() * -0.5 - 0.2, // 缓慢上升
          opacity: Math.random() * 0.5 + 0.2,
          color: Math.random() > 0.6 ? '251,114,153' : '255,255,255'
        });
      }
    },

    // --- 文字粒子逻辑 (采样) ---
    createTextParticles() {
      this.textParticles = [];

      // 1. 绘制文字
      this.textCtx.font = `900 ${this.textSize}px 'Montserrat', sans-serif`;
      this.textCtx.fillStyle = 'white';
      this.textCtx.textAlign = 'center';
      this.textCtx.textBaseline = 'middle';
      this.textCtx.fillText(this.heroText, this.width / 2, this.height / 2);

      // 2. 采样像素
      const imgData = this.textCtx.getImageData(0, 0, this.width, this.height).data;
      this.textCtx.clearRect(0, 0, this.width, this.height);

      // 3. 生成粒子
      const gap = 4; // 采样间隔
      for (let y = 0; y < this.height; y += gap) {
        for (let x = 0; x < this.width; x += gap) {
          const index = (y * this.width + x) * 4 + 3;
          if (imgData[index] > 128) {
            this.textParticles.push({
              x: x,
              y: y,
              originX: x,
              originY: y,
              color: 'rgba(255, 255, 255, 0.95)',
              size: Math.random() * 1.5 + 1,
              vx: 0,
              vy: 0,
              friction: Math.random() * 0.05 + 0.90,
              ease: Math.random() * 0.05 + 0.05
            });
          }
        }
      }
    },

    // --- 动画循环 ---
    animate() {
      this.bgCtx.clearRect(0, 0, this.width, this.height);
      this.textCtx.clearRect(0, 0, this.width, this.height);

      // 1. 绘制背景星尘
      for (let p of this.bgParticles) {
        this.bgCtx.beginPath();
        this.bgCtx.arc(p.x, p.y, p.size, 0, Math.PI * 2);
        this.bgCtx.fillStyle = `rgba(${p.color}, ${p.opacity})`;
        this.bgCtx.fill();
        p.y += p.speedY;
        if (p.y < 0) {
          p.y = this.height;
          p.x = Math.random() * this.width;
        }
      }

      // 2. 绘制文字粒子 (核心交互)
      // 开启发光，增加氛围
      this.textCtx.shadowBlur = 4;
      this.textCtx.shadowColor = "rgba(251,114,153,0.3)";

      for (let p of this.textParticles) {
        // --- 物理计算 ---
        const dx = this.mouse.x - p.x;
        const dy = this.mouse.y - p.y;
        // 不开方，用距离平方比较，性能更好
        const distSq = dx * dx + dy * dy;
        const radiusSq = this.mouse.radius * this.mouse.radius;

        // 【关键逻辑】：只有距离小于 30px (radius) 时才受力
        if (distSq < radiusSq) {
          const dist = Math.sqrt(distSq);
          const force = (this.mouse.radius - dist) / this.mouse.radius;
          const angle = Math.atan2(dy, dx);

          // 施加推力 + 随机扰动 (消除整齐的圆圈边缘)
          // 这里的 Math.random() 是关键，让粒子像沙子一样散开，而不是像一个圈
          const scatterX = Math.cos(angle) * force * 15 + (Math.random() - 0.5) * 5;
          const scatterY = Math.sin(angle) * force * 15 + (Math.random() - 0.5) * 5;

          p.vx -= scatterX;
          p.vy -= scatterY;
        }

        // 回归逻辑
        p.vx += (p.originX - p.x) * p.ease;
        p.vy += (p.originY - p.y) * p.ease;

        // 摩擦力
        p.vx *= p.friction;
        p.vy *= p.friction;

        // 更新位置
        p.x += p.vx;
        p.y += p.vy;

        // 绘制矩形粒子 (比画圆性能好，且更有数码感)
        this.textCtx.fillStyle = p.color;
        this.textCtx.fillRect(p.x, p.y, p.size, p.size);
      }

      this.animationFrameId = requestAnimationFrame(this.animate);
    },

    handleMouseMove(e) {
      // 记录鼠标位置
      this.mouse.x = e.clientX;
      this.mouse.y = e.clientY;
    },
    handleResize() {
      if (this.resizeTimer) clearTimeout(this.resizeTimer);
      this.resizeTimer = setTimeout(() => {
        this.init();
      }, 200);
    },
    scrollDown() {
      window.scrollTo({ top: window.innerHeight, behavior: 'smooth' });
    }
  }
};
</script>

<style scoped>
/* 引入 nice 的字体 */
@import url('https://fonts.googleapis.com/css2?family=Montserrat:wght@900&display=swap');

.ethereal-hero {
  position: relative;
  width: 100%;
  height: 100vh;
  overflow: hidden;
  user-select: none; /* 禁止选中文本，保证交互体验 */
  font-family: 'Montserrat', sans-serif;
}

/* 背景层 */
.hero-bg {
  position: absolute;
  top: 0; left: 0; width: 100%; height: 100%;
  background-size: cover;
  background-position: center;
  z-index: 1;
  animation: bgZoom 30s linear infinite alternate;
}
@keyframes bgZoom {
  0% { transform: scale(1); }
  100% { transform: scale(1.1); }
}

/* 遮罩层 */
.hero-overlay {
  position: absolute;
  top: 0; left: 0; width: 100%; height: 100%;
  z-index: 2;
  background: rgba(0,0,0,0.3); /* 纯粹压暗一点，突出文字 */
}

/* Canvas 层 */
.canvas-layer {
  position: absolute;
  top: 0; left: 0; width: 100%; height: 100%;
}
.bg-canvas { z-index: 3; pointer-events: none; }
.text-canvas { z-index: 4; }

/* 滚动提示 */
.scroll-down {
  position: absolute;
  bottom: 30px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 10;
  color: rgba(255,255,255,0.8);
  cursor: pointer;
  text-align: center;
  transition: all 0.3s;
}
.scroll-down:hover {
  color: #fff;
  text-shadow: 0 0 10px rgba(255,255,255,0.5);
}
.scroll-text {
  display: block;
  font-size: 10px;
  letter-spacing: 3px;
  margin-bottom: 5px;
  text-transform: uppercase;
}
.scroll-arrow {
  font-size: 20px;
  animation: floatDown 2s infinite;
}
@keyframes floatDown {
  0%, 100% { transform: translateY(0); opacity: 0.5; }
  50% { transform: translateY(8px); opacity: 1; }
}
</style>
