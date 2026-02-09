<template>
  <div class="gsap-footer-container" @mousemove="handleMouseMove">
    <div class="bg-gradient"></div>

    <div class="shapes-container" ref="shapesContainer">
      <div
        v-for="(item, index) in particles"
        :key="index"
        class="particle"
        :class="item.type"
        :style="item.style"
        ref="particleRefs"
      ></div>
    </div>

    <div class="grid-overlay"></div>
  </div>
</template>

<script>
import { gsap } from "gsap";

export default {
  name: "TopGsap",
  data() {
    return {
      particles: [],
      particleCount: 25, // 粒子数量，可自行调整
    };
  },
  created() {
    // 1. 初始化粒子数据
    this.initParticles();
  },
  mounted() {
    // 2. 组件挂载后开始动画
    this.$nextTick(() => {
      this.animateParticles();
    });
  },
  methods: {
    initParticles() {
      const colors = ["#00a1d6", "#fb7299", "#ffffff", "#6831FF"]; // B站蓝、粉、白、紫

      for (let i = 0; i < this.particleCount; i++) {
        const size = this.random(10, 80); // 随机大小
        const isCircle = Math.random() > 0.5; // 随机形状：圆或方

        this.particles.push({
          type: isCircle ? "circle" : "square",
          style: {
            width: `${size}px`,
            height: `${size}px`,
            left: `${this.random(0, 100)}%`, // 随机水平位置
            top: `${this.random(20, 100)}%`,  // 随机垂直位置
            background: colors[Math.floor(Math.random() * colors.length)],
            opacity: this.random(0.1, 0.4),   // 初始透明度
            filter: `blur(${this.random(2, 10)}px)` // 随机模糊，制造景深感
          }
        });
      }
    },

    animateParticles() {
      const els = this.$refs.particleRefs;
      if (!els) return;

      els.forEach((el) => {
        // A. 漂浮动画 (上下左右不规则运动)
        gsap.to(el, {
          y: `-=${this.random(50, 200)}`, // 向上浮动
          x: `+=${this.random(-50, 50)}`, // 左右微动
          rotation: this.random(0, 360),  // 旋转
          duration: this.random(5, 15),   // 随机时长，错落有致
          repeat: -1,
          yoyo: true, // 往返运动
          ease: "sine.inOut"
        });

        // B. 呼吸动画 (透明度和缩放)
        gsap.to(el, {
          scale: this.random(0.8, 1.2),
          opacity: this.random(0.1, 0.6),
          duration: this.random(2, 6),
          repeat: -1,
          yoyo: true,
          ease: "power1.inOut"
        });
      });
    },

    handleMouseMove(e) {
      // C. 鼠标视差交互 (让粒子稍微跟随鼠标反向移动，产生空间感)
      const x = (e.clientX / window.innerWidth - 0.5) * 40; // 移动幅度
      const y = (e.clientY / window.innerHeight - 0.5) * 40;

      gsap.to(this.$refs.shapesContainer, {
        x: x,
        y: y,
        duration: 1,
        ease: "power2.out"
      });
    },

    random(min, max) {
      return Math.random() * (max - min) + min;
    }
  }
};
</script>

<style scoped>
.gsap-footer-container {
  position: relative;
  width: 100%;
  height: 280px; /* 高度可调 */
  overflow: hidden;
  background: #1a1a2e; /* 深色背景底色 */
}

/* 背景渐变层 */
.bg-gradient {
  position: absolute;
  top: 0; left: 0; width: 100%; height: 100%;
  background: linear-gradient(180deg, transparent 0%, rgba(0, 161, 214, 0.05) 100%);
  z-index: 0;
}

.shapes-container {
  position: absolute;
  top: 0; left: 0; width: 100%; height: 100%;
  z-index: 1;
}

.particle {
  position: absolute;
  border-radius: 50%; /* 默认圆形 */
  mix-blend-mode: screen; /* 滤色模式，让光点重叠变亮 */
  pointer-events: none;
}

/* 方块样式 */
.particle.square {
  border-radius: 4px;
}

/* 网格装饰线（可选） */
.grid-overlay {
  position: absolute;
  top: 0; left: 0; width: 100%; height: 100%;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.03) 1px, transparent 1px);
  background-size: 40px 40px;
  z-index: 0;
  mask-image: linear-gradient(to bottom, transparent, black); /* 网格只在底部显示明显 */
  -webkit-mask-image: linear-gradient(to bottom, transparent, black);
}
</style>
