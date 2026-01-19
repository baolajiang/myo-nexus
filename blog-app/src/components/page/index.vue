<template>
  <div class="moon-library-container">

    <div class="bg-deep-night"></div>
    <div class="bg-vignette"></div>
    <div class="svg-bg-layer" ref="svgLayer">
      <svg class="svg-canvas" viewBox="0 0 800 800">
        <defs>
          <linearGradient id="moonGrad" x1="0%" y1="0%" x2="0%" y2="100%">
            <stop offset="0%" style="stop-color:#b088ff;stop-opacity:0.2" />
            <stop offset="100%" style="stop-color:#d45d79;stop-opacity:0.0" />
          </linearGradient>
        </defs>

        <g class="magic-circle spin-slow" opacity="0.15">
          <circle cx="400" cy="400" r="350" stroke="#b088ff" stroke-width="1" fill="none" stroke-dasharray="20,10" />
          <circle cx="400" cy="400" r="300" stroke="#b088ff" stroke-width="2" fill="none" />
          <polygon points="400,100 660,550 140,550" stroke="#b088ff" stroke-width="1" fill="none" />
          <polygon points="400,700 660,250 140,250" stroke="#b088ff" stroke-width="1" fill="none" />
        </g>

        <path d="M-100,800 Q400,200 900,800" fill="none" stroke="url(#moonGrad)" stroke-width="2" opacity="0.5" />
      </svg>
    </div>

    <div class="floating-layer">
      <div v-for="n in 6" :key="'book-'+n" class="float-item book" :style="getRandomStyle('book')">
        <i class="el-icon-notebook-1"></i>
      </div>
      <div v-for="n in 20" :key="'flower-'+n" class="float-item flower" :style="getRandomStyle('flower')">
        ✿
      </div>
      <div v-for="n in 15" :key="'dust-'+n" class="float-item dust" :style="getRandomStyle('dust')"></div>
    </div>

    <div class="content-wrapper" ref="content">

      <div class="avatar-seal">
        <div class="seal-ring outer"></div>
        <div class="seal-ring inner"></div>
        <div class="avatar-core">
          <img
            src="/static/img/露娜切露德7.png"
            class="user-img"
            alt="Avatar"
            @error="handleImgError"
          >
        </div>
      </div>

      <div class="text-group">
        <div class="top-tag">
          <span class="rune">✦</span>
          <span>MOON'S VILLA</span>
        </div>

        <h1 class="main-title">
          月之<br>
          <span class="highlight">別邸</span>
        </h1>

        <div class="divider-line"></div>

        <p class="desc-text">
          「 於靜謐的書海與星光之間，<br>
          編織代碼與夢境的篇章。 」
        </p>
      </div>

      <div class="nav-list">
        <div
          class="nav-item"
          v-for="(item, index) in menuItems"
          :key="index"
          @click="navigate(item.path)"
          @mouseenter="onHover($event)"
          @mouseleave="onLeave($event)"
        >
          <div class="nav-bar"></div>
          <i :class="item.icon" class="nav-icon"></i>
          <span class="nav-text">{{ item.cn }}</span>
          <span class="hover-spark">✨</span>
        </div>
      </div>

    </div>

    <div class="corner-decor">
      <span>The Library of Moon</span>
      <div class="decor-bar"></div>
    </div>

  </div>
</template>

<script>
import { gsap } from 'gsap';

export default {
  name: "IndexMoonGrimoire",
  data() {
    return {
      menuItems: [
        { cn: '魔導藏書', path: '/write', icon: 'el-icon-collection' },
        { cn: '時光軌跡', path: '/archives', icon: 'el-icon-date' },
        { cn: '訪客留言', path: '/messageBoard', icon: 'el-icon-chat-line-square' },
        { cn: '關於別邸', path: '/Resume', icon: 'el-icon-user' }
      ]
    };
  },
  mounted() {
    this.entranceAnim();
  },
  methods: {
    navigate(path) {
      // 离场：向左虚化消失
      const tl = gsap.timeline({ onComplete: () => this.$router.push(path) });
      tl.to('.content-wrapper', { x: -50, opacity: 0, filter: 'blur(10px)', duration: 0.6 })
        .to('.moon-library-container', { opacity: 0, duration: 0.5 }, "-=0.3");
    },
    handleImgError(e) {
      e.target.style.display = 'none';
      e.target.parentNode.style.backgroundColor = '#3e2a5f'; // 深紫兜底
    },
    // 生成随机漂浮物样式
    getRandomStyle(type) {
      const left = Math.random() * 100 + '%';
      const duration = (Math.random() * 10 + 10) + 's';
      const delay = (Math.random() * 5) + 's';

      let style = {
        left: left,
        animationDuration: duration,
        animationDelay: delay
      };

      if (type === 'book') {
        style.fontSize = (Math.random() * 20 + 15) + 'px';
        style.color = '#d45d79'; // 洋红色书本
        style.top = Math.random() * 100 + '%'; // 书本全屏随机
      } else if (type === 'flower') {
        style.fontSize = (Math.random() * 10 + 10) + 'px';
        style.color = Math.random() > 0.5 ? '#b088ff' : '#ffb7c5'; // 紫/粉花瓣
        style.top = '-10%'; // 花瓣从顶落下
      } else { // dust
        style.width = Math.random() * 4 + 'px';
        style.height = style.width;
        style.backgroundColor = '#ffd700'; // 金色光尘
        style.top = Math.random() * 100 + '%';
      }
      return style;
    },

    // === 修改点 3：恢复了菜单交互动画 (onHover/onLeave) ===
    onHover(e) {
      // 悬停：左侧条变长，文字变色
      gsap.to(e.currentTarget.querySelector('.nav-bar'), { height: '100%', backgroundColor: '#ffd700', duration: 0.3 });
      gsap.to(e.currentTarget.querySelector('.nav-text'), { x: 10, color: '#fff', duration: 0.3 });
      gsap.to(e.currentTarget.querySelector('.nav-icon'), { color: '#ffd700', scale: 1.1, duration: 0.3 });
      gsap.to(e.currentTarget.querySelector('.hover-spark'), { opacity: 1, scale: 1.2, rotation: 180, duration: 0.4 });
    },
    onLeave(e) {
      gsap.to(e.currentTarget.querySelector('.nav-bar'), { height: '0%', backgroundColor: 'transparent', duration: 0.3 });
      gsap.to(e.currentTarget.querySelector('.nav-text'), { x: 0, color: '#ccc', duration: 0.3 });
      gsap.to(e.currentTarget.querySelector('.nav-icon'), { color: '#b088ff', scale: 1, duration: 0.3 });
      gsap.to(e.currentTarget.querySelector('.hover-spark'), { opacity: 0, scale: 0, rotation: 0, duration: 0.3 });
    },

    // 注意：删除了 handleMouseMove 方法，因为不再需要视差效果

    // === 进场动画 ===
    entranceAnim() {
      const tl = gsap.timeline();
      // 这里 from 里的 opacity: 0 是正确的，它是让元素从“不可见”渐变到“可见”
      tl.from('.content-wrapper', { x: -50, opacity: 0, duration: 1.2, ease: "power3.out" })
        .from('.avatar-core', { scale: 0, rotation: -90, duration: 1, ease: "back.out(1.5)" }, "-=0.8")
        .from('.seal-ring', { scale: 1.5, opacity: 0, rotation: 180, duration: 1.2 }, "-=0.8")
        .from(['.main-title', '.desc-text'], { x: -30, opacity: 0, stagger: 0.1, duration: 0.8 }, "-=0.6")
        .from('.nav-item', { x: -20, opacity: 0, stagger: 0.1, duration: 0.6 }, "-=0.4");
    }
  }
};
</script>

<style scoped>

.moon-library-container {
  position: relative; width: 100%; height: 100vh;
  overflow: hidden;
  font-family: 'Cinzel', 'Noto Serif TC', serif;
  color: #e0d0ff;
  background: #1a1226; /* 深紫底色 */
}

/* 1. 背景 */
.bg-deep-night {
  position: absolute; top: 0; left: 0; width: 100%; height: 100%;
  background: radial-gradient(circle at 70% 30%, #3e2045 0%, #1a1226 60%, #0d0814 100%);
  z-index: 0;
}
.bg-vignette {
  position: absolute; width: 100%; height: 100%;
  background: radial-gradient(circle, transparent 50%, #000 120%);
  z-index: 1; pointer-events: none;
}

/* SVG 背景 */
.svg-bg-layer {
  position: absolute; width: 100%; height: 100%; pointer-events: none; z-index: 0;
}
.svg-canvas { width: 100%; height: 100%; }
.spin-slow { transform-origin: 400px 400px; animation: spin 60s linear infinite; }
@keyframes spin { 100% { transform: rotate(360deg); } }

/* 2. 漂浮细节 */
.floating-layer { position: absolute; width: 100%; height: 100%; pointer-events: none; z-index: 2; }
.float-item { position: absolute; opacity: 0.6; }

/* 书本浮动动画 */
.book { animation: floatBook linear infinite; color: #d45d79; }
@keyframes floatBook {
  0% { transform: translateY(0) rotate(-10deg); opacity: 0; }
  50% { opacity: 0.8; }
  100% { transform: translateY(-100px) rotate(10deg); opacity: 0; }
}

/* 花瓣飘落动画 */
.flower { animation: fallFlower linear infinite; text-shadow: 0 0 5px rgba(255,255,255,0.3); }
@keyframes fallFlower {
  0% { transform: translate(0, 0) rotate(0deg); opacity: 0; }
  20% { opacity: 0.8; }
  100% { transform: translate(50px, 100vh) rotate(360deg); opacity: 0; }
}

/* 光尘动画 */
.dust { border-radius: 50%; box-shadow: 0 0 5px #ffd700; animation: twinkle 4s infinite ease-in-out; }
@keyframes twinkle { 0%,100% { opacity: 0.2; transform: scale(0.8); } 50% { opacity: 1; transform: scale(1.2); } }


/* 3. 左侧内容 (关键布局) */
.content-wrapper {
  position: absolute;
  left: 10%; top: 50%; transform: translateY(-50%);
  z-index: 10;
  display: flex; flex-direction: column; align-items: flex-start;
  gap: 35px;
}

/* A. 头像组 */
.avatar-seal {
  position: relative; width: 140px; height: 140px;
  display: flex; justify-content: center; align-items: center;
}
.seal-ring {
  position: absolute; border-radius: 50%;
  border: 1px solid rgba(176, 136, 255, 0.4);
}
.outer { width: 100%; height: 100%; border-style: dashed; animation: spin 20s linear infinite; }
.inner { width: 110%; height: 110%; border: 1px solid rgba(212, 93, 121, 0.3); animation: spin 30s linear infinite reverse; }

.avatar-core {
  width: 110px; height: 110px; border-radius: 50%;
  overflow: hidden; z-index: 2;
  border: 3px solid #b088ff; /* 紫色边框 */
  box-shadow: 0 0 25px rgba(160, 68, 255, 0.4);
  background: #2a1b3d;
}
.user-img { width: 100%; height: 100%; object-fit: cover; }

/* B. 文字信息 */
.text-group { text-align: left; }
.top-tag {
  font-size: 12px; color: #d45d79; letter-spacing: 2px; margin-bottom: 5px;
  display: flex; align-items: center; gap: 8px; font-weight: 700;
}
.rune { color: #ffd700; }

.main-title {
  font-size: 3.5rem; font-weight: 700; margin: 0; line-height: 1.1;
  color: #fff; text-shadow: 0 5px 20px rgba(0,0,0,0.6);
  font-family: 'Noto Serif TC', serif;
}
.highlight {
  color: #b088ff; /* 帕秋莉紫 */
}

.divider-line {
  width: 60px; height: 3px; background: linear-gradient(to right, #d45d79, #b088ff);
  margin: 20px 0; border-radius: 2px; opacity: 0.8;
}

.desc-text {
  font-family: 'Noto Serif TC', serif; font-size: 1rem; color: #ccc;
  line-height: 1.8; letter-spacing: 1px;
}

/* C. 导航菜单 */
.nav-list {
  display: flex; flex-direction: column; gap: 12px; margin-top: 10px;
}
.nav-item {
  position: relative;
  display: flex; align-items: center; gap: 15px;
  padding: 10px 0 10px 20px;
  width: 200px; cursor: pointer;
  overflow: hidden; /* 遮住装饰条 */
}

/* 装饰条（默认高度0，由GSAP控制变长） */
.nav-bar {
  position: absolute; left: 0; top: 0; width: 3px; height: 0%;
  background-color: transparent; transition: height 0.3s;
}
.nav-icon { font-size: 1.2rem; color: #b088ff; transition: all 0.3s; }
.nav-text { font-size: 1rem; color: #ccc; font-weight: 600; letter-spacing: 1px; transition: all 0.3s; }
/* 闪光特效（默认opacity 0，由GSAP控制显示） */
.hover-spark {
  position: absolute; right: 20px; font-size: 0.8rem; opacity: 0;
  transition: all 0.3s;
}

/* 右下角 */
.corner-decor {
  position: absolute; bottom: 30px; right: 40px;
  color: rgba(255,255,255,0.3); font-size: 12px; letter-spacing: 2px;
  text-align: right;
}
.decor-bar { width: 40px; height: 1px; background: #fff; opacity: 0.3; margin-top: 5px; margin-left: auto; }

/* 移动端适配 */
@media screen and (max-width: 900px) {
  .content-wrapper { left: 8%; top: 45%; }
  .main-title { font-size: 2.8rem; }
  .svg-canvas { opacity: 0.3; }
}
</style>
