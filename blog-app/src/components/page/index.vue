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
import bgImg from '../../../static/img/anime-sunset-art-wallpaper-2560x1080_14.jpg';

export default {
  name: "HeaderTop",
  data() {
    return {
      bgImage: bgImg,
      width: 0,
      height: 0,
      animationFrameId: null,
      resizeTimer: null, // 添加resizeTimer声明

      // --- 组1：背景氛围 (星尘 + 流星) ---
      bgCtx: null,
      bgParticles: [], // 上升的星尘
      meteors: [],     // [新增] 流星数组

      // --- 组2：交互文字 ---
      textCtx: null,
      textParticles: [],
      // 鼠标交互参数
      mouse: { x: -1000, y: -1000, radius: 40 }, // 稍微调大了一点 radius 方便测试，你可以改回 20

      heroText: "测试数据",
      textSize: 120,       // 稍微调大字体
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
      this.textCtx = cvs2.getContext('2d', { willReadFrequently: true });

      this.createBgParticles();
      this.createTextParticles();

      // 开始动画
      this.animate();
    },

    // --- 背景星尘 (保持你原有的唯美上升效果) ---
    createBgParticles() {
      this.bgParticles = [];
      // 根据窗口大小动态调整粒子数量
      const count = Math.min(80, Math.floor((this.width * this.height) / 50000));
      for (let i = 0; i < count; i++) {
        this.bgParticles.push({
          x: Math.random() * this.width,
          y: Math.random() * this.height,
          size: Math.random() * 2 + 0.5,
          speedY: Math.random() * -0.3 - 0.1, // 减慢速度，更梦幻
          opacity: Math.random() * 0.5 + 0.1,
          color: Math.random() > 0.8 ? '255, 215, 0' : '255, 255, 255' // 金色和白色
        });
      }
    },

    // --- [核心新增] 流星绘制逻辑 ---
    drawMeteors() {
      // 1. 随机生成流星 (概率控制频率)
      // 0.015 意味着大约每 60 帧(1秒)出现 1 个流星
      if (Math.random() < 0.015) {
        this.meteors.push({
          x: Math.random() * this.width + 200, // 倾向于从右侧出现
          y: -10,
          speed: Math.random() * 8 + 4,        // 速度
          length: Math.random() * 80 + 40,     // 尾巴长度
          angle: Math.PI / 4,                  // 45度角
          opacity: 1
        });
      }

      // 2. 绘制与更新
      for (let i = 0; i < this.meteors.length; i++) {
        const m = this.meteors[i];

        // 移动 (向左下角飞)
        m.x -= m.speed;
        m.y += m.speed;

        // 绘制拖尾
        this.bgCtx.beginPath();
        // 创建渐变：头部白 -> 尾部透明
        const gradient = this.bgCtx.createLinearGradient(m.x, m.y, m.x + m.length, m.y - m.length);
        gradient.addColorStop(0, "rgba(255, 255, 255, 1)");
        gradient.addColorStop(0.4, "rgba(255, 255, 255, 0.4)");
        gradient.addColorStop(1, "rgba(255, 255, 255, 0)");

        this.bgCtx.strokeStyle = gradient;
        this.bgCtx.lineWidth = 2;
        this.bgCtx.lineCap = "round";
        this.bgCtx.moveTo(m.x, m.y);
        this.bgCtx.lineTo(m.x + m.length, m.y - m.length);
        this.bgCtx.stroke();

        // 移除飞出屏幕的流星
        if (m.x < -100 || m.y > this.height + 100) {
          this.meteors.splice(i, 1);
          i--;
        }
      }
    },

    // --- 文字粒子逻辑 (优化粒子数量) ---
    createTextParticles() {
      this.textParticles = [];
      this.textCtx.font = `900 ${this.textSize}px 'Montserrat', sans-serif`;
      this.textCtx.fillStyle = 'white';
      this.textCtx.textAlign = 'center';
      this.textCtx.textBaseline = 'middle';
      this.textCtx.fillText(this.heroText, this.width / 2, this.height / 2);

      const imgData = this.textCtx.getImageData(0, 0, this.width, this.height).data;
      this.textCtx.clearRect(0, 0, this.width, this.height);

      // 根据窗口大小动态调整粒子密度
      const gap = Math.max(4, Math.min(8, Math.floor(Math.sqrt(this.width * this.height) / 100)));

      for (let y = 0; y < this.height; y += gap) {
        for (let x = 0; x < this.width; x += gap) {
          const index = (y * this.width + x) * 4 + 3;
          if (imgData[index] > 128) {
            this.textParticles.push({
              x: x, y: y,
              originX: x, originY: y,
              // 使用淡金色，配合黄昏
              color: `rgba(255, 240, 200, ${Math.random() * 0.4 + 0.6})`,
              size: Math.random() * 1.5 + 1,
              vx: 0, vy: 0,
              friction: Math.random() * 0.05 + 0.90,
              ease: Math.random() * 0.05 + 0.05
            });
          }
        }
      }

      // 限制最大粒子数量
      const maxParticles = 1500;
      if (this.textParticles.length > maxParticles) {
        // 随机采样粒子
        const sampledParticles = [];
        const step = this.textParticles.length / maxParticles;
        for (let i = 0; i < maxParticles; i++) {
          sampledParticles.push(this.textParticles[Math.floor(i * step)]);
        }
        this.textParticles = sampledParticles;
      }
    },

    // --- 动画循环 (优化鼠标交互计算) ---
    animate() {
      // 1. 清空画布
      this.bgCtx.clearRect(0, 0, this.width, this.height);
      this.textCtx.clearRect(0, 0, this.width, this.height);

      // 2. 绘制星尘 (背景层)
      for (let p of this.bgParticles) {
        this.bgCtx.beginPath();
        this.bgCtx.arc(p.x, p.y, p.size, 0, Math.PI * 2);
        this.bgCtx.fillStyle = `rgba(${p.color}, ${p.opacity})`;
        this.bgCtx.fill();
        p.y += p.speedY; // 缓慢上升
        if (p.y < 0) {
          p.y = this.height;
          p.x = Math.random() * this.width;
        }
      }

      // 3. 绘制流星 (新增)
      this.drawMeteors();

      // 4. 绘制文字粒子 (前景层) - 优化鼠标交互
      this.textCtx.shadowBlur = 0; // 性能优化：不用 shadowBlur，或者调低

      // 计算鼠标影响区域
      const mouseRadiusSq = this.mouse.radius * this.mouse.radius;

      for (let p of this.textParticles) {
        // 只计算鼠标附近的粒子
        const dx = this.mouse.x - p.x;
        const dy = this.mouse.y - p.y;
        const distSq = dx * dx + dy * dy;

        if (distSq < mouseRadiusSq) {
          const dist = Math.sqrt(distSq);
          const force = (this.mouse.radius - dist) / this.mouse.radius;
          const angle = Math.atan2(dy, dx);
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

      // 使用箭头函数，确保 this 指向正确
      this.animationFrameId = requestAnimationFrame(() => this.animate());
    },

    handleMouseMove(e) {
      // [修复] 使用 getBoundingClientRect 确保坐标相对于 Canvas
      // 这样即使页面滚动，或者 Canvas 不是全屏，交互依然准确
      if(this.$refs.canvasText) {
        const rect = this.$refs.canvasText.getBoundingClientRect();
        this.mouse.x = e.clientX - rect.left;
        this.mouse.y = e.clientY - rect.top;
      }
    },
    handleResize() {
      if (this.resizeTimer) clearTimeout(this.resizeTimer);
      this.resizeTimer = setTimeout(() => {
        // 取消当前动画，避免冲突
        if (this.animationFrameId) {
          cancelAnimationFrame(this.animationFrameId);
        }
        this.init();
      }, 200);
    },
    scrollDown() {
      // 滚动一屏的高度
      window.scrollTo({ top: window.innerHeight, behavior: 'smooth' });
    }
  }
};
</script>

<style scoped>
/* 你的 CSS 基本不用动，但我建议稍微加深 overlay 以突显流星 */
@import url('https://fonts.googleapis.com/css2?family=Montserrat:wght@900&display=swap');

.ethereal-hero {
  position: relative;
  width: 100%;
  height: 100vh;
  overflow: hidden;
  user-select: none;
  font-family: 'Montserrat', sans-serif;
  background: #000; /* 兜底背景色 */
}

/* 背景层 */
.hero-bg {
  position: absolute;
  top: 0; left: 0; width: 100%; height: 100%;
  background-size: cover;
  background-position: center;
  z-index: 1;
  /* 稍微放慢动画，不晕 */
  animation: bgZoom 40s linear infinite alternate;
}
@keyframes bgZoom {
  0% { transform: scale(1); }
  100% { transform: scale(1.15); }
}

/* 遮罩层 */
.hero-overlay {
  position: absolute;
  top: 0; left: 0; width: 100%; height: 100%;
  z-index: 2;
  /* 渐变遮罩：上面深一点(看流星)，下面浅一点 */
  background: linear-gradient(to bottom, rgba(20,10,30,0.6), rgba(0,0,0,0.2));
}

.canvas-layer {
  position: absolute;
  top: 0; left: 0; width: 100%; height: 100%;
}
.bg-canvas { z-index: 3; pointer-events: none; }
.text-canvas { z-index: 4; }

.scroll-down {
  position: absolute;
  bottom: 30px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 10;
  color: rgba(255,255,255,0.7);
  cursor: pointer;
  text-align: center;
  transition: all 0.3s;
}
.scroll-down:hover {
  color: #fff;
  text-shadow: 0 0 10px #ff9a8b; /* 呼应晚霞色 */
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
