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
      animationFrameId: null, // [核心] 用于追踪动画帧ID，防止重复开启

      // --- 组1：背景星尘参数 (悬浮的粒子) ---
      bgCtx: null,
      bgParticles: [],

      // --- 组2：流星参数 (新增功能) ---
      shootingStars: [],      // 存放当前屏幕上的流星
      shootingStarTimer: 0,   // 计时器，控制流星出现的频率

      // --- 组3：文字粒子参数 (核心交互) ---
      textCtx: null,
      textParticles: [],
      // radius: 鼠标感应半径，数值越大，鼠标能推开粒子的范围越远
      mouse: { x: -1000, y: -1000, radius: 40 },

      heroText: "测试数据", // 屏幕中间显示的文字
      textSize: 100,      // 文字大小
    };
  },
  mounted() {
    this.init();
    // 监听窗口大小变化和鼠标移动
    window.addEventListener('resize', this.handleResize);
    window.addEventListener('mousemove', this.handleMouseMove);
  },
  beforeDestroy() {
    // 组件销毁前清理监听器和动画，防止内存泄漏
    window.removeEventListener('resize', this.handleResize);
    window.removeEventListener('mousemove', this.handleMouseMove);
    if (this.animationFrameId) {
      cancelAnimationFrame(this.animationFrameId);
    }
  },
  methods: {
    // --- 初始化核心逻辑 ---
    init() {
      // [修复抖动关键 1]：防止动画循环叠加。
      // 每次 init 前，如果已经有在跑的动画，必须先让它停下来！
      if (this.animationFrameId) {
        cancelAnimationFrame(this.animationFrameId);
        this.animationFrameId = null;
      }

      this.width = window.innerWidth;
      this.height = window.innerHeight;

      // 获取 Canvas 上下文
      const cvs1 = this.$refs.canvasBg;
      const cvs2 = this.$refs.canvasText;

      if (cvs1) {
        cvs1.width = this.width;
        cvs1.height = this.height;
        this.bgCtx = cvs1.getContext('2d');
      }
      if (cvs2) {
        cvs2.width = this.width;
        cvs2.height = this.height;
        // willReadFrequently: 优化 getImageData 频繁读取的性能
        this.textCtx = cvs2.getContext('2d', { willReadFrequently: true });
      }

      // 重置流星数组 (背景粒子不重置，为了视觉连续性)
      this.shootingStars = [];

      // 生成粒子
      this.createBgParticles();
      this.createTextParticles();

      // 启动动画循环
      this.animate();
    },

    // --- 逻辑部分：生成背景悬浮微粒 ---
    createBgParticles() {
      // [修复闪烁关键]：Resize 时复用现有粒子，不要销毁重建，防止背景闪烁
      if (this.bgParticles.length > 0) {
        this.bgParticles.forEach(p => {
          // 修正跑出屏幕的粒子
          if (p.x > this.width) p.x = Math.random() * this.width;
          if (p.y > this.height) p.y = Math.random() * this.height;
        });
        return;
      }

      // 初次生成
      this.bgParticles = [];
      const count = 60; // 粒子数量
      for (let i = 0; i < count; i++) {
        this.bgParticles.push({
          x: Math.random() * this.width,
          y: Math.random() * this.height,
          size: Math.random() * 2 + 0.5,
          speedY: Math.random() * -0.5 - 0.2, // 负数代表向上飘
          opacity: Math.random() * 0.5 + 0.2,
          // 随机颜色：粉色或白色
          color: Math.random() > 0.6 ? '251,114,153' : '255,255,255'
        });
      }
    },

    // --- 逻辑部分：生成流星 (新增) ---
    createShootingStar() {
      this.shootingStars.push({
        x: Math.random() * this.width + 200,      // 初始 X (屏幕右侧外)
        y: Math.random() * this.height * 0.5 - 100, // 初始 Y (偏上)
        len: Math.random() * 80 + 50,  // 尾巴长度 (50-130px)
        speed: Math.random() * 10 + 6, // 飞行速度
        opacity: 1                     // 初始不透明度
      });
    },

    // --- 逻辑部分：生成文字粒子 ---
    createTextParticles() {
      // [平滑过渡关键]：暂存旧粒子，用于计算过渡动画
      const oldParticles = this.textParticles;
      this.textParticles = [];

      if (!this.textCtx) return;

      // 1. 先在画布上画出文字
      this.textCtx.clearRect(0, 0, this.width, this.height);
      this.textCtx.font = `900 ${this.textSize}px 'Montserrat', sans-serif`;
      this.textCtx.fillStyle = 'white';
      this.textCtx.textAlign = 'center';
      this.textCtx.textBaseline = 'middle';
      this.textCtx.fillText(this.heroText, this.width / 2, this.height / 2);

      // 2. 扫描画布像素
      const imgData = this.textCtx.getImageData(0, 0, this.width, this.height).data;
      this.textCtx.clearRect(0, 0, this.width, this.height); // 擦除文字，准备画粒子

      // 3. 根据像素生成粒子
      const gap = 4; // 采样间隔（越小粒子越密，性能消耗越大）
      let oldIndex = 0;

      for (let y = 0; y < this.height; y += gap) {
        for (let x = 0; x < this.width; x += gap) {
          // 像素索引计算 (RGBA 4个通道)
          const index = (y * this.width + x) * 4 + 3; // 获取 Alpha 通道

          // 如果该位置有像素（Alpha > 128 说明是文字部分）
          if (imgData[index] > 128) {

            // ============================================
            // [注释]：在这里修改文字粒子的颜色
            // ============================================
            let particleColor = 'rgba(255, 255, 255, 0.95)'; // 默认白色

            // 示例：如果想让文字变成粉色，解开下面这行注释
            // particleColor = 'rgba(255, 192, 203, 0.95)';

            // 示例：如果想让文字变成金色，解开下面这行注释
            // particleColor = 'rgba(255, 215, 0, 0.95)';

            const p = {
              originX: x,  // 目标归位 X
              originY: y,  // 目标归位 Y
              color: particleColor,
              size: Math.random() * 1.5 + 1, // 粒子大小随机
              friction: Math.random() * 0.05 + 0.90, // 摩擦力
              ease: Math.random() * 0.05 + 0.05      // 回弹系数
            };

            // [平滑过渡逻辑]：继承旧粒子的位置
            // 这样 Resize 时，粒子会从“旧位置”飞到“新位置”，而不是原地闪现
            if (oldIndex < oldParticles.length) {
              const old = oldParticles[oldIndex];
              p.x = old.x;
              p.y = old.y;
              p.vx = old.vx;
              p.vy = old.vy;
              oldIndex++;
            } else {
              // 新增的粒子直接出生在目标位
              p.x = p.originX;
              p.y = p.originY;
              p.vx = 0;
              p.vy = 0;
            }

            this.textParticles.push(p);
          }
        }
      }
    },

    // --- 动画主循环 (每一帧执行) ---
    animate() {
      if (!this.bgCtx || !this.textCtx) return;

      // 清空画布
      this.bgCtx.clearRect(0, 0, this.width, this.height);
      this.textCtx.clearRect(0, 0, this.width, this.height);

      // =========================================
      // 步骤 1：绘制背景悬浮微粒
      // =========================================
      for (let p of this.bgParticles) {
        this.bgCtx.beginPath();
        this.bgCtx.arc(p.x, p.y, p.size, 0, Math.PI * 2);
        this.bgCtx.fillStyle = `rgba(${p.color}, ${p.opacity})`;
        this.bgCtx.fill();
        p.y += p.speedY; // 向上移动
        // 循环逻辑：超出顶部后回到最下方
        if (p.y < 0) {
          p.y = this.height;
          p.x = Math.random() * this.width;
        }
      }

      // =========================================
      // 步骤 2：绘制流星 (新增逻辑)
      // =========================================
      this.shootingStarTimer++;
      // [注释] 下面 150 这个数字控制流星频率：数字越小流星越多，数字越大越少
      if (this.shootingStarTimer > 150 && Math.random() > 0.3) {
        this.createShootingStar();
        this.shootingStarTimer = 0;
      }

      for (let i = this.shootingStars.length - 1; i >= 0; i--) {
        const s = this.shootingStars[i];
        this.bgCtx.beginPath();

        // 创建流星尾巴的渐变效果 (从头到尾透明度降低)
        // (x, y) 是头部，(x+len, y-len) 是尾部 (假设流星往左下角飞)
        const gradient = this.bgCtx.createLinearGradient(s.x, s.y, s.x + s.len, s.y - s.len);
        gradient.addColorStop(0, "rgba(255, 255, 255, " + s.opacity + ")");
        gradient.addColorStop(1, "rgba(255, 255, 255, 0)");

        this.bgCtx.strokeStyle = gradient;
        this.bgCtx.lineWidth = 2; // 流星粗细
        this.bgCtx.lineCap = "round";
        this.bgCtx.moveTo(s.x, s.y);
        this.bgCtx.lineTo(s.x + s.len, s.y - s.len);
        this.bgCtx.stroke();

        // 移动流星：x减小(往左)，y增加(往下)
        s.x -= s.speed;
        s.y += s.speed;
        s.opacity -= 0.01; // 慢慢消失

        // 移除条件：超出屏幕 或 完全透明
        if (s.x < -200 || s.y > this.height + 200 || s.opacity <= 0) {
          this.shootingStars.splice(i, 1);
        }
      }

      // =========================================
      // 步骤 3：绘制文字粒子 (带鼠标交互)
      // =========================================
      // 开启发光效果，增加氛围感
      this.textCtx.shadowBlur = 4;
      this.textCtx.shadowColor = "rgba(251,114,153,0.3)";

      for (let p of this.textParticles) {
        // --- 物理计算 ---
        const dx = this.mouse.x - p.x;
        const dy = this.mouse.y - p.y;
        const distSq = dx * dx + dy * dy; // 距离的平方
        const radiusSq = this.mouse.radius * this.mouse.radius;

        // 如果鼠标距离粒子足够近，施加推力
        if (distSq < radiusSq) {
          const dist = Math.sqrt(distSq);
          const force = (this.mouse.radius - dist) / this.mouse.radius;
          const angle = Math.atan2(dy, dx);

          // 随机扰动让散开的效果更像沙子
          const scatterX = Math.cos(angle) * force * 15 + (Math.random() - 0.5) * 5;
          const scatterY = Math.sin(angle) * force * 15 + (Math.random() - 0.5) * 5;
          p.vx -= scatterX;
          p.vy -= scatterY;
        }

        // 回归计算：粒子想要回到它的 originX/Y
        p.vx += (p.originX - p.x) * p.ease;
        p.vy += (p.originY - p.y) * p.ease;

        // 摩擦力：让速度慢慢降下来
        p.vx *= p.friction;
        p.vy *= p.friction;

        // 更新位置
        p.x += p.vx;
        p.y += p.vy;

        // 绘制粒子
        this.textCtx.fillStyle = p.color;
        this.textCtx.fillRect(p.x, p.y, p.size, p.size);
      }

      // 请求下一帧动画
      this.animationFrameId = requestAnimationFrame(this.animate);
    },

    handleMouseMove(e) {
      this.mouse.x = e.clientX;
      this.mouse.y = e.clientY;
    },
    handleResize() {
      // 防抖：停止调整窗口 100ms 后再重绘
      // 如果感觉窗口拖动时反应太慢，可以把 100 改小
      if (this.resizeTimer) clearTimeout(this.resizeTimer);
      this.resizeTimer = setTimeout(() => {
        this.init();
      }, 100);
    },
    scrollDown() {
      window.scrollTo({ top: window.innerHeight, behavior: 'smooth' });
    }
  }
};
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Montserrat:wght@900&display=swap');

/* 容器：铺满全屏，禁止文字选中 */
.ethereal-hero {
  position: relative;
  width: 100%;
  height: 100vh;
  overflow: hidden;
  user-select: none;
  font-family: 'Montserrat', sans-serif;
  background: #000; /* 黑色兜底 */
}

/* 1. 背景层 */
.hero-bg {
  position: absolute;
  top: 0; left: 0; width: 100%; height: 100%;
  background-size: cover;
  background-position: center;
  z-index: 1;
  /* 背景缓慢放大动画 */
  animation: bgZoom 40s linear infinite alternate;
}
@keyframes bgZoom {
  0% { transform: scale(1); }
  100% { transform: scale(1.15); }
}

/* 2. 遮罩层 (重要) */
/* 这个层在背景图和流星之间，稍微压暗背景，让流星更亮眼 */
.hero-overlay {
  position: absolute;
  top: 0; left: 0; width: 100%; height: 100%;
  z-index: 2;
  /* 上方深色(为了看星星)，下方透明 */
  background: linear-gradient(to bottom, rgba(20,10,30,0.6), rgba(0,0,0,0.2));
}

/* 3. Canvas 层 */
.canvas-layer {
  position: absolute;
  top: 0; left: 0; width: 100%; height: 100%;
}
/* 背景Canvas：画星尘和流星，不需要鼠标事件，所以 pointer-events: none */
.bg-canvas { z-index: 3; pointer-events: none; }
/* 文字Canvas：需要鼠标交互 */
.text-canvas { z-index: 4; }

/* 4. 底部按钮样式 */
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
  text-shadow: 0 0 10px #ff9a8b; /* 悬停发光 */
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
  animation: floatDown 2s infinite; /* 箭头上下浮动动画 */
}
@keyframes floatDown {
  0%, 100% { transform: translateY(0); opacity: 0.5; }
  50% { transform: translateY(8px); opacity: 1; }
}
</style>
