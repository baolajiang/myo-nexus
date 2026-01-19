<template>
  <footer class="luna-footer">
    <div class="footer-deco-line"></div>

    <div class="footer-content">
      <div class="copyright-info">
        <span class="text">Copyright © 2026</span>
        <span class="brand">{{ $myName || "Moon's Villa" }}</span>
        <span class="separator">|</span>
        <span class="design-by">
          Designed by
          <span class="author-name" @click="resume">Master</span>
        </span>
      </div>

      <div class="legal-info">
        <a href="https://beian.miit.gov.cn/" target="_blank" class="icp-link">
          <img src="@/assets/img/beian.png" class="beian-icon" />
          <span>湘ICP备2022004529号</span>
        </a>
      </div>
    </div>
  </footer>
</template>

<script>
export default {
  name: 'BaseFooter',
  data() {
    return {}
  },
  methods: {
    resume() {
      this.$router.push({ path: `/Resume` })
    }
  }
}
</script>

<style scoped>

.luna-footer {
  /* ★★★ 关键修复：删除 width: 100% 或改为 auto ★★★ */
  width: auto;
  /* ★★★ 关键修复：添加盒模型设置，防止 padding 撑大宽度 ★★★ */
  box-sizing: border-box;

  padding: 30px 20px;
  background-color: transparent;
  font-family: 'Noto Serif SC', serif;
  text-align: center;
  position: relative;
  z-index: 100;
  margin-top: -80px; /*稍微再往上提一点，避免产生多余的垂直滚动条 */
}

/* 顶部装饰金线 */
.footer-deco-line {
  width: 100px;
  height: 2px;
  background: linear-gradient(90deg, transparent, #d4af37, transparent);
  margin: 0 auto 20px;
  opacity: 0.5;
}

.footer-content {
  display: flex;
  flex-direction: column;
  gap: 10px;
  align-items: center;
}

.copyright-info {
  font-size: 14px;
  color: #999; /* 稍微调淡一点颜色 */
  letter-spacing: 1px;
}

.brand {
  font-family: 'Cinzel', serif;
  font-weight: 700;
  color: #666;
  margin: 0 5px;
}

.separator {
  margin: 0 8px;
  color: #d4af37;
  font-size: 12px;
  opacity: 0.6;
}

.author-name {
  color: #666;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  border-bottom: 1px solid transparent;
}

.author-name:hover {
  color: #d4af37;
  border-bottom-color: #d4af37;
}

.legal-info {
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 12px;
}

.icp-link {
  text-decoration: none;
  color: #aaa;
  transition: color 0.3s;
  display: flex;
  align-items: center;
  gap: 6px;
  letter-spacing: 1px;
}

.icp-link:hover {
  color: #d4af37;
}

.beian-icon {
  width: 16px;
  height: 16px;
  display: block;
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .copyright-info {
    font-size: 12px;
    flex-direction: column;
    gap: 5px;
  }
  .separator { display: none; }
}
</style>
