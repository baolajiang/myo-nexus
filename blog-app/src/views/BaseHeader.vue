<template>
  <div class="header-monitor">
    <div class="header-wrapper" ref="headerWrapper" :class="{ 'mobile-open': isMobileMenuOpen }">

      <div class="gold-line-top" ref="goldLine"></div>

      <div class="header-content">
        <div class="flex-header">
          <div class="logo-box">
            <router-link to="/" class="header-logo" @mouseenter.native="onLogoEnter" @mouseleave.native="onLogoLeave" @click.native="closeMobileMenu">
              <span class="moon-icon" ref="moonIcon">☾</span>
              <span class="logo-text" ref="logoText">{{ $myName || '月之别邸' }}</span>
              <span class="sakura-icon" ref="sakuraIcon">❀</span>
            </router-link>
          </div>

          <div class="nav-box hidden-xs-only">
            <ul class="nav-list">
              <li v-for="(item, index) in navItems" :key="item.path" class="nav-item" :class="{ active: activeIndex === item.path }"
                  @mouseenter="onNavEnter($event)" @mouseleave="onNavLeave($event)">
                <router-link :to="item.path">
                  <div class="nav-text-container">
                    <span class="nav-text-en">{{ item.en }}</span>
                    <span class="nav-text-cn">{{ item.name }}</span>
                  </div>
                </router-link>
                <div class="nav-underline"></div>
              </li>
              <li v-if="user.login" class="nav-item write-btn" @mouseenter="onNavEnter($event)" @mouseleave="onNavLeave($event)">
                <router-link to="/write">
                  <div class="nav-text-container">
                    <span class="nav-text-en">Write</span>
                    <span class="nav-text-cn">創作</span>
                  </div>
                </router-link>
                <div class="nav-underline"></div>
              </li>
            </ul>
          </div>

          <div class="user-box hidden-xs-only" ref="userBox">
            <template v-if="!user.login">
              <div class="user-trigger guest-trigger" @click.stop="togglePanel('guest')">
                <div class="avatar-wrapper">
                  <el-avatar :size="36" :src="guestAvatarDisplay" icon="el-icon-user-solid" class="guest-avatar"></el-avatar>
                  <div class="avatar-ring-guest"></div>
                </div>
                <span class="guest-label">{{ guest.nickname || '旅人' }}</span>
              </div>

              <transition name="pop-fade">
                <div v-if="activePanel === 'guest'" class="custom-popover-panel" @click.stop>
                  <div class="user-card-content guest-panel" ref="guestPanel">
                    <div class="guest-tabs">
                      <div class="tab-item" :class="{ active: guestTab === 'info' }" @click="guestTab = 'info'">旅人身份</div>
                      <div class="tab-item" :class="{ active: guestTab === 'auth' }" @click="guestTab = 'auth'">登入 / 註冊</div>
                      <div class="tab-cursor" :style="{ left: guestTab === 'info' ? '0%' : '50%' }"></div>
                    </div>
                    <div class="smooth-height-box" ref="smoothBox">
                      <transition name="tab-slide" mode="out-in" @before-leave="beforeLeave" @enter="enter" @after-enter="afterEnter">
                        <div v-if="guestTab === 'info'" key="info" class="guest-info-content">
                          <div class="guest-header-top">
                            <div class="guest-big-avatar-box clickable" @click="openEditModal('email')">
                              <el-avatar :size="70" :src="guestAvatarDisplay" icon="el-icon-user-solid" class="guest-big-avatar"></el-avatar>
                              <div class="guest-avatar-border"></div>
                              <div class="avatar-edit-overlay"><i class="el-icon-camera"></i><span>更換</span></div>
                            </div>
                            <div class="guest-greeting-text">貴安，{{ guest.nickname || '旅人' }}</div>
                            <div class="guest-uid-deco">UID: {{ guest.uuid ? guest.uuid.substring(0, 8).toUpperCase() : 'UNKNOWN' }}</div>
                          </div>
                          <div class="luna-alert"><i class="el-icon-info"></i><div class="alert-text">旅人身份保存於本地，清除緩存將丟失。<br>僅用於評論與點贊。</div></div>
                          <div class="info-list">
                            <div class="info-row">
                              <span class="info-label">ID</span><span class="info-val id-font">{{ guest.uuid }}</span>
                              <span class="info-btn" @click="copyText(guest.uuid)">複製</span>
                            </div>
                            <div class="info-row">
                              <span class="info-label">名字</span><span class="info-val">{{ guest.nickname }}</span>
                              <span class="info-btn" @click="openEditModal('nickname')">修改</span>
                            </div>
                            <div class="info-row">
                              <span class="info-label">郵箱</span>
                              <span class="info-val placeholder" v-if="!guest.email">點擊填寫以獲取頭像</span>
                              <span class="info-val" v-else>{{ guest.email }}</span>
                              <span class="info-btn" @click="openEditModal('email')">編輯</span>
                            </div>
                            <div class="info-row last">
                              <span class="info-label">網站</span>
                              <span class="info-val placeholder" v-if="!guest.website">未填寫</span>
                              <span class="info-val" v-else>{{ guest.website }}</span>
                              <span class="info-btn" @click="openEditModal('website')">編輯</span>
                            </div>
                          </div>
                        </div>
                        <div v-else key="auth" class="guest-auth-content">
                          <div class="auth-welcome-box">
                            <div class="welcome-star">✦</div>
                            <div class="welcome-title">貴安，旅人</div>
                            <div class="welcome-desc">歡迎來到月之別邸。<br>登入後即可發表文章、收藏內容并同步數據。</div>
                          </div>
                          <div class="guest-actions">
                            <div class="luna-btn primary-btn" @click="login">立即登入</div>
                            <div class="luna-btn outline-btn" @click="register">註冊新帳號</div>
                          </div>
                        </div>
                      </transition>
                    </div>
                    <div class="uc-footer-deco"><span>☾</span> LUNA GUEST SYSTEM <span>❀</span></div>
                  </div>
                </div>
              </transition>
            </template>

            <template v-else>
              <div class="user-trigger" @click.stop="togglePanel('user')">
                <div class="avatar-wrapper">
                  <el-avatar :size="36" :src="user.avatar || require('@/assets/img/default_avatar.png')" class="luna-avatar"></el-avatar>
                  <div class="avatar-glow"></div>
                </div>
              </div>

              <transition name="pop-fade">
                <div v-if="activePanel === 'user'" class="custom-popover-panel" @click.stop>
                  <div class="user-card-content" ref="userPanel">
                    <div class="uc-header">
                      <div class="uc-avatar-box">
                        <el-avatar :size="70" :src="user.avatar || require('@/assets/img/default_avatar.png')" class="uc-avatar"></el-avatar>
                        <div class="uc-avatar-border"></div>
                      </div>
                      <div class="uc-name">{{ user.nickname || 'Unknown User' }}</div>
                      <div class="uc-email">{{ user.email || 'Welcome back to the manor' }}</div>
                      <div class="uc-manage-btn" @click="navTo('/user/profile')">個人中心</div>
                    </div>
                    <div class="uc-divider"></div>
                    <div class="uc-menu">
                      <div class="uc-menu-item" @click="navTo('/articles')"><i class="el-icon-document"></i> <span>我的文章</span></div>
                      <div class="uc-menu-item" @click="jumpToAdmin"><i class="el-icon-setting"></i> <span>系統設置</span></div>
                      <div class="uc-menu-item logout-item" @click="logout"><i class="el-icon-switch-button"></i> <span>登出帳戶</span></div>
                    </div>
                    <div class="uc-footer-deco"><span>☾</span> LUNA SYSTEM <span>❀</span></div>
                  </div>
                </div>
              </transition>
            </template>
          </div>
          <div class="mobile-toggle hidden-sm-and-up" @click="toggleMobileMenu">
            <div class="hamburger" :class="{ 'is-active': isMobileMenuOpen }">
              <span class="line line-1"></span><span class="line line-2"></span><span class="line line-3"></span>
            </div>
          </div>
        </div>
      </div>

      <transition @enter="enterMobileMenu" @leave="leaveMobileMenu">
        <div v-show="isMobileMenuOpen" class="mobile-menu-overlay">
          <div class="menu-bg-moon">☾</div>
          <div class="mobile-menu-content">
            <div class="mobile-user-section mobile-item">
              <template v-if="user.login">
                <div class="m-avatar-box"><el-avatar :size="60" :src="user.avatar || require('@/assets/img/default_avatar.png')" class="luna-avatar"></el-avatar><div class="m-username">貴安, {{ user.nickname }}</div></div>
                <div class="m-auth-actions"><span @click="logoutAndClose">登出帳戶</span></div>
              </template>
              <template v-else>
                <div class="m-login-box"><div class="m-login-btn" @click="navTo('/login')">登入</div><div class="m-register-btn" @click="register">註冊</div></div>
              </template>
            </div>
            <div class="mobile-divider mobile-item"></div>
            <ul class="mobile-nav-list">
              <li v-for="item in navItems" :key="item.path" class="mobile-nav-item mobile-item" @click="navTo(item.path)"><span class="m-en">{{ item.en }}</span><span class="m-cn">{{ item.name }}</span></li>
              <li v-if="user.login" class="mobile-nav-item mobile-item" @click="navTo('/write')"><span class="m-en">Write</span><span class="m-cn">創作</span></li>
            </ul>
          </div>
        </div>
      </transition>

      <transition name="luna-modal">
        <div v-if="showEditModal" class="luna-modal-overlay" @click.self="closeEditModal">
          <div class="luna-modal-card">
            <div class="luna-modal-title">
              修改{{ editFieldMap[editField] }}
            </div>
            <div class="luna-modal-body">
              <input
                type="text"
                v-model="editValue"
                class="luna-input"
                :placeholder="'請輸入' + editFieldMap[editField]"
                @keyup.enter="confirmEdit"
              >
              <div v-if="editField === 'email'" class="luna-modal-tip">
                * 輸入 QQ 郵箱可自動獲取頭像
              </div>
            </div>
            <div class="luna-modal-footer">
              <button class="luna-btn-cancel" @click="closeEditModal">取消</button>
              <button class="luna-btn-confirm" @click="confirmEdit">確定</button>
            </div>
          </div>
        </div>
      </transition>

    </div>
  </div>
</template>

<script>
import { gsap } from 'gsap'
import md5 from 'js-md5'
import { getTicket } from '@/api/login'
import { APP_CONFIG } from '../../config/config.js'
export default {
  name: 'BaseHeader',
  props: { activeIndex: { type: String, default: '/' } },
  data() {
    return {
      isScrolled: false, ticking: false, isMobileMenuOpen: false,
      activePanel: null, guestTab: 'info',

      // 游客数据：这里不需要定义了，因为通过 computed 从 Store 获取
      // guest: { ... }, <--- 删除了

      showEditModal: false, editField: '', editValue: '',
      editFieldMap: { nickname: '名字', email: '郵箱', website: '網站' },

      navItems: [
        { name: '首頁', en: 'Home', path: '/', icon: 'el-icon-s-home' },
        { name: '文章', en: 'articles', path: '/articles', icon: 'el-icon-document' },
        { name: '分類', en: 'Category', path: '/category/all', icon: 'el-icon-menu' },
        { name: '標籤', en: 'Tags', path: '/tag/all', icon: 'el-icon-price-tag' },
        { name: '導航', en: 'Links', path: '/nav', icon: 'el-icon-compass' },
        { name: '關於', en: 'Resume', path: '/Resume', icon: 'el-icon-info' },
        { name: '留言', en: 'Guestbook', path: '/messageBoard', icon: 'el-icon-chat-dot-round' }
      ]
    }
  },
  computed: {
    user() {
      console.log(APP_CONFIG)
      let login = this.$store.state.account ? this.$store.state.account.length != 0 : false
      let avatar = this.$store.state.avatar; let nickname = this.$store.state.name; let email = this.$store.state.email
      return { login, avatar, nickname, email }
    },
    //  从 Store 获取游客信息 (全局数据)
    guest() {

      return this.$store.state.guest;
    },
/*    guestAvatarDisplay() {
      if (this.guest.email && this.guest.email.match(/^\d+@qq\.com$/)) {
        const qq = this.guest.email.replace('@qq.com', '');
        return `https://q1.qlogo.cn/g?b=qq&nk=${qq}&s=100`;
      }
      return this.guest.avatar || '';
    },*/
    guestAvatarDisplay() {
      const email = this.guest.email || '';
      // 情况 A: 如果是 QQ 数字邮箱，直接用腾讯官方接口 (速度最快)
      if (email.match(/^\d+@qq\.com$/)) {
        const qq = email.replace('@qq.com', '');
        return `https://q1.qlogo.cn/g?b=qq&nk=${qq}&s=100`;
      }
      // 情况 B: 如果有其他邮箱，使用 Cravatar (Gravatar 的国内镜像)
      if (email) {
        // Gravatar 要求：去除首尾空格 -> 转小写 -> 生成 MD5
        const hash = md5(email.trim().toLowerCase());
        // d=identicon 表示如果没头像，自动生成一个几何图案
        // 也可以换成:
        // d=monsterid (小怪兽)
        // d=wavatar (卡通脸)
        // d=retro (复古像素)
        // d=robohash (机器人)
        return `https://cravatar.cn/avatar/${hash}?s=100&d=identicon`;
      }
      // 情况 C: 没填邮箱，显示默认头像

      return this.guest.avatar || ''; // 或者返回一个本地默认图路径
    },
  },
  watch: {
    // 监听滚动
    isScrolled(newVal) { if (!this.isMobileMenuOpen) { newVal ? this.animateToCapsule() : this.animateToFull() } },
    // 监听面板开关状态
    activePanel(newVal) {
      // 当面板关闭 (newVal === null) 时，重置 Tab
      if (!newVal) {
        // 等待 CSS 动画（pop-fade）完全结束后再重置，避免用户看到 Tab 跳变的瞬间
        // 这里的 300ms 对应 CSS 中的 transition: all 0.3s
        setTimeout(() => {
          this.guestTab = 'info';
        }, 300);
      }
    }
  },
  mounted() {
    window.addEventListener('scroll', this.handleScroll);
    document.addEventListener('click', this.closeAllPanels);
    this.animateToFull(true);
    //  注意：初始化数据的 initGuest 已经移到了 App.vue 的 created 中
    // 这里不需要再调用了，避免重复读取
  },
  destroyed() {
    window.removeEventListener('scroll', this.handleScroll);
    document.removeEventListener('click', this.closeAllPanels);
  },
  methods: {
    togglePanel(type) {
      if (this.activePanel === type) { this.activePanel = null; }
      else { this.activePanel = type; this.$nextTick(() => { if(type === 'guest') this.onGuestShow(); if(type === 'user') this.onUserShow(); }); }
    },
    closeAllPanels() { this.activePanel = null; },

    openEditModal(field) {
      this.closeAllPanels();
      this.editField = field;
      this.editValue = this.guest[field]; // 从 computed 读取 Store 数据
      this.showEditModal = true;
    },
    closeEditModal() { this.showEditModal = false; },
    confirmEdit() {
      // 1. 去除首尾空格
      let value = this.editValue.trim();

      if (!value) {
        // 如果允许清空（比如不想填了），直接保存空字符串并关闭
        // 如果必填，可以在这里加 return this.$message.warning('內容不能為空');
        this.saveAndClose(value);
        return;
      }

      // 2. 针对不同字段进行正则校验
      if (this.editField === 'email') {
        // 更严格的邮箱正则
        const emailRegex = /^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,6}$/;
        if (!emailRegex.test(value)) {

          this.$myMessage.error('請輸入正確的郵箱格式 (例如: user@example.com)');
          return;
        }
      }

      else if (this.editField === 'website') {
        // 网站正则：要求必须包含 http/https 或者 www 开头，或者至少像个域名
        // 这里做一个兼容性较好的校验
        const urlRegex = /^(https?:\/\/)?([\da-z\.-]+)\.([a-z\.]{2,6})([\/\w \.-]*)*\/?$/;
        if (!urlRegex.test(value)) {

          this.$myMessage({
            content: '請輸入正確的網站網址 (例如: https://blog.example.com)',
            type: 'error',
            duration: 1000
          });
          return;
        }

        // 可选优化：如果用户没填 http://，自动给他补上（体验更好）
        if (!/^https?:\/\//.test(value)) {
          value = 'https://' + value;
        }
      }

      // 3. 校验通过，保存
      this.saveAndClose(value);
    },

    // 抽离出的保存逻辑
    saveAndClose(finalValue) {
      this.$store.commit('UPDATE_GUEST', {
        [this.editField]: finalValue
      });
      this.$myMessage.success('更新成功');
      this.closeEditModal();
    },



    copyText(text) {
      const input = document.createElement('input'); input.setAttribute('readonly', 'readonly'); input.setAttribute('value', text);
      document.body.appendChild(input); input.select();
      if (document.execCommand('copy')) { this.$myMessage.success('複製成功'); }
      document.body.removeChild(input);
    },

    // === 动画与基础逻辑 (保持不变) ===
    onGuestShow() { this.$nextTick(() => { const el = this.$refs.guestPanel; if(el) gsap.fromTo(el, { y: 15, opacity: 0, scale: 0.96 }, { y: 0, opacity: 1, scale: 1, duration: 0.4, ease: "back.out(1.7)", overwrite: true }); }); },
    onUserShow() { this.$nextTick(() => { const el = this.$refs.userPanel; if(el) gsap.fromTo(el, { y: 15, opacity: 0, scale: 0.96 }, { y: 0, opacity: 1, scale: 1, duration: 0.4, ease: "back.out(1.7)", overwrite: true }); }); },
    closePopovers() { this.closeAllPanels(); },
    beforeLeave(el) {if (this.$refs.smoothBox) {this.$refs.smoothBox.style.height = this.$refs.smoothBox.scrollHeight + 'px';}},
    enter(el) { if (this.$refs.smoothBox) {this.$refs.smoothBox.style.height = el.scrollHeight + 'px'; }},
    afterEnter(el) {if (this.$refs.smoothBox) { this.$refs.smoothBox.style.height = 'auto'; }},
    onLogoEnter() {
      // 删除了 scale: 1.2
      gsap.to(this.$refs.moonIcon, { rotation: -20, color: '#d4af37', duration: 0.4, ease: 'back.out' });
      gsap.to(this.$refs.sakuraIcon, { rotation: 20, color: '#ffb7c5', duration: 0.4, ease: 'back.out' });
      // 删除了 letterSpacing: '2px'
      gsap.to(this.$refs.logoText, { color: '#d4af37', duration: 0.4 });
    },
    onLogoLeave() {
      // 删除了 scale: 1
      gsap.to(this.$refs.moonIcon, { rotation: 0, color: '#4a4a4a', duration: 0.4 });
      gsap.to(this.$refs.sakuraIcon, { rotation: 0, color: '#4a4a4a', duration: 0.4 });
      // 删除了 letterSpacing: '1px'
      gsap.to(this.$refs.logoText, { color: '#4a4a4a', duration: 0.4 });
    },
    onNavEnter(e) { const target = e.currentTarget; const enText = target.querySelector('.nav-text-en'); const cnText = target.querySelector('.nav-text-cn'); const underline = target.querySelector('.nav-underline'); gsap.killTweensOf([enText, cnText, underline]); gsap.to(enText, { y: -5, color: '#d4af37', fontWeight: '700', duration: 0.3, ease: 'power2.out' }); gsap.to(cnText, { y: 2, opacity: 1, color: '#888', duration: 0.3, ease: 'power2.out' }); gsap.to(underline, { width: '100%', opacity: 1, duration: 0.4, ease: 'power2.out' }); },
    onNavLeave(e) { const target = e.currentTarget; const enText = target.querySelector('.nav-text-en'); const cnText = target.querySelector('.nav-text-cn'); const underline = target.querySelector('.nav-underline'); gsap.killTweensOf([enText, cnText, underline]); gsap.to(enText, { y: 0, color: '#555', fontWeight: '600', duration: 0.3 }); gsap.to(cnText, { y: 10, opacity: 0, duration: 0.3 }); gsap.to(underline, { width: '0%', opacity: 0, duration: 0.3 }); },
    handleScroll() { if (!this.ticking) { window.requestAnimationFrame(() => { const scrollTop = window.pageYOffset || document.documentElement.scrollTop; const shouldScroll = scrollTop > 60; if (this.isScrolled !== shouldScroll) { this.isScrolled = shouldScroll } this.ticking = false }); this.ticking = true } },
    animateToCapsule() { if (this.isMobileMenuOpen) return; gsap.to(this.$refs.headerWrapper, { top: 15, width: '95%', maxWidth: '1200px', height: '60px', borderRadius: '30px', backgroundColor: 'rgba(255, 250, 245, 0.98)', border: '1px solid rgba(212, 175, 55, 0.3)', boxShadow: '0 8px 30px rgba(212, 175, 55, 0.15)', duration: 0.6, ease: 'power3.out' }); gsap.to(this.$refs.goldLine, { opacity: 0, duration: 0.3 }); },
    animateToFull(immediate = false) { const duration = immediate ? 0 : 0.6; gsap.to(this.$refs.headerWrapper, { top: 0, width: '100%', maxWidth: '100%', height: '65px', borderRadius: 0, backgroundColor: 'rgba(255, 250, 245, 0.8)', border: 'none', borderBottom: '1px solid rgba(212, 175, 55, 0.1)', boxShadow: 'none', duration: duration, ease: 'power3.out' }); gsap.to(this.$refs.goldLine, { opacity: 1, duration: 0.3 }); },
    toggleMobileMenu() { this.isMobileMenuOpen = !this.isMobileMenuOpen; if (this.isMobileMenuOpen) { this.animateToFull(); document.body.style.overflow = 'hidden'; } else { document.body.style.overflow = ''; if (this.isScrolled) this.animateToCapsule(); } },
    closeMobileMenu() { this.isMobileMenuOpen = false; document.body.style.overflow = ''; if (this.isScrolled) this.animateToCapsule(); },
    navTo(path) { this.closeAllPanels(); this.$router.push({ path }); this.closeMobileMenu(); },
    // 跳转到管理后台
    jumpToAdmin() {
      // 1. 关闭用户面板
      this.closeAllPanels();

      // 2. 获取当前 Token
      const token = this.$store.state.token;
      if (!token) {
        this.$myMessage.error('請先登入');
        return;
      }

      // 3. 配置后台管理系统的地址 (从环境变量获取)
      const adminBaseUrl = APP_CONFIG.adminUrl;

      // 4. 调用接口获取 Ticket
      getTicket(token).then(res => {
        if (res.success) {
          const ticket = res.data;
          // 5. 拼接 URL 并在新标签页打开
          const adminUrl = `${adminBaseUrl}?ticket=${ticket}`;
          window.open(adminUrl, '_blank'); // 修改这里：使用 _blank 在新标签页打开
        } else {
          this.$myMessage.error(res.msg || '无法获取跳转凭证');
        }
      }).catch(err => {
        console.error(err);
        this.$myMessage.error('跳转失败，请稍后重试');
      });
    },
    logoutAndClose() { this.$store.dispatch('logout').then(() => { this.$router.push({path: '/'}); this.closeMobileMenu(); }); },
    logout() { this.closeAllPanels(); this.$store.dispatch('logout').then(() => { this.$router.push({path: '/'}) }) },
    login() { this.closeAllPanels(); this.$router.push({path: '/login'}) },
    register() {
      this.closeAllPanels();
      this.closeMobileMenu(); // 確保手機端點擊也能關閉菜單
      this.$router.push({
        path: '/register'
      }) },
    enterMobileMenu(el, done) { gsap.fromTo(el, { opacity: 0 }, { opacity: 1, duration: 0.4 }); const items = el.querySelectorAll('.mobile-item'); gsap.fromTo(items, { y: 30, opacity: 0 }, { y: 0, opacity: 1, duration: 0.5, stagger: 0.05, ease: "power2.out", onComplete: done }); gsap.fromTo(el.querySelector('.menu-bg-moon'), { rotation: -30, opacity: 0, scale: 0.8 }, { rotation: 0, opacity: 0.1, scale: 1, duration: 1, ease: "power2.out" }); },
    leaveMobileMenu(el, done) { gsap.to(el, { opacity: 0, duration: 0.3, onComplete: done }); },
  }
}
</script>

<style>
/* 移除原来的 .luna-user-popover 样式，不再需要覆盖 Element */

/* 复用卡片内容样式 */
.user-card-content { font-family: 'Noto Serif SC', 'Playfair Display', serif; width: 100%; }
.guest-panel { padding: 0; background: #fffaf5; }
.guest-tabs { display: flex; position: relative; border-bottom: 1px solid #e0d0b0; }
.tab-item { flex: 1; text-align: center; padding: 15px 0; font-size: 14px; font-weight: 600; color: #999; cursor: pointer; transition: color 0.3s; }
.tab-item.active { color: #d4af37; }
.tab-cursor { position: absolute; bottom: 0; height: 2px; width: 50%; background: #d4af37; transition: left 0.3s ease; }
.smooth-height-box { transition: height 0.4s cubic-bezier(0.25, 0.8, 0.25, 1); overflow: hidden; }
/* ...保持原有内容样式不变... */
.guest-header-top { display: flex; flex-direction: column; align-items: center; padding: 25px 0 10px; background: linear-gradient(to bottom, #fffaf5, #fff); }
.guest-big-avatar-box { position: relative; margin-bottom: 10px; cursor: pointer; }
.guest-big-avatar { background: #f0f0f0; color: #ccc; border: 3px solid #fff; box-shadow: 0 5px 15px rgba(0,0,0,0.05); transition: filter 0.3s; }
.guest-avatar-border { position: absolute; top: -5px; left: -5px; right: -5px; bottom: -5px; border: 1px dashed #d4af37; border-radius: 50%; opacity: 0.5; transition: all 0.3s; }
.avatar-edit-overlay { position: absolute; top: 0; left: 0; width: 100%; height: 100%; border-radius: 50%; background: rgba(0,0,0,0.4); color: #fff; display: flex; flex-direction: column; align-items: center; justify-content: center; opacity: 0; transition: opacity 0.3s; font-size: 12px; pointer-events: none; }
.guest-big-avatar-box:hover .avatar-edit-overlay { opacity: 1; }
.guest-big-avatar-box:hover .guest-avatar-border { transform: scale(1.05); opacity: 1; border-style: solid; }
.guest-greeting-text { font-size: 18px; font-weight: 700; color: #4a4a4a; letter-spacing: 1px; }
.guest-uid-deco { font-size: 10px; color: #bbb; margin-top: 4px; font-family: monospace; letter-spacing: 1px; }
.luna-alert { margin: 15px; background: rgba(212, 175, 55, 0.1); border: 1px solid rgba(212, 175, 55, 0.3); border-radius: 4px; padding: 10px; display: flex; gap: 10px; font-size: 12px; color: #8a6d3b; align-items: flex-start; }
.luna-alert i { font-size: 16px; margin-top: 2px; }
.info-list { padding: 0 15px 15px; }
.info-row { display: flex; align-items: center; padding: 12px 0; border-bottom: 1px dashed #eee; transition: background 0.3s; }
.info-row:hover { background: rgba(212, 175, 55, 0.05); }
.info-row.last { border-bottom: none; }
.info-label { width: 50px; color: #999; font-size: 13px; font-weight: 600; }
.info-val { flex: 1; color: #4a4a4a; font-size: 13px; margin-left: 10px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; font-family: 'Noto Serif SC', serif; }
.info-val.id-font { font-family: monospace; letter-spacing: -0.5px; }
.info-val.placeholder { color: #ccc; font-style: italic; }
.info-btn { color: #d4af37; font-size: 12px; cursor: pointer; margin-left: 10px; opacity: 0.7; transition: all 0.2s; }
.info-btn:hover { opacity: 1; text-decoration: underline; font-weight: 700; }
.guest-auth-content { padding: 30px 25px; text-align: center; }
.auth-welcome-box { margin-bottom: 30px; }
.welcome-star { font-size: 40px; color: #d4af37; margin-bottom: 15px; opacity: 0.8; animation: floatStar 3s infinite ease-in-out; }
.welcome-title { font-size: 20px; font-weight: 700; color: #4a4a4a; margin-bottom: 10px; letter-spacing: 2px; }
.welcome-desc { font-size: 13px; color: #888; line-height: 1.6; }
@keyframes floatStar { 0%, 100% { transform: translateY(0); opacity: 0.8; } 50% { transform: translateY(-5px); opacity: 1; text-shadow: 0 0 10px #d4af37; } }
.guest-actions { display: flex; flex-direction: column; gap: 15px; }
.luna-btn { text-align: center; padding: 12px 0; border-radius: 4px; font-size: 14px; font-weight: 700; cursor: pointer; transition: all 0.3s; letter-spacing: 1px; }
.primary-btn { background: #d4af37; color: #fff; border: 1px solid #d4af37; box-shadow: 0 4px 10px rgba(212, 175, 55, 0.3); }
.primary-btn:hover { background: #c5a028; border-color: #c5a028; transform: translateY(-2px); }
.outline-btn { background: transparent; color: #d4af37; border: 1px solid #d4af37; }
.outline-btn:hover { background: rgba(212, 175, 55, 0.1); }
.tab-slide-enter-active, .tab-slide-leave-active { transition: all 0.4s cubic-bezier(0.25, 0.8, 0.25, 1); }
.tab-slide-enter { opacity: 0; transform: translateY(20px); }
.tab-slide-leave-to { opacity: 0; transform: translateY(-20px); }
.uc-header { display: flex; flex-direction: column; align-items: center; padding: 30px 20px 20px; text-align: center; background: linear-gradient(to bottom, #fffaf5, #fff); }
.uc-avatar-box { position: relative; margin-bottom: 15px; }
.uc-avatar { border: 3px solid #fff; box-shadow: 0 5px 15px rgba(0,0,0,0.1); }
.uc-avatar-border { position: absolute; top: -5px; left: -5px; right: -5px; bottom: -5px; border: 1px solid #d4af37; border-radius: 50%; opacity: 0.5; }
.uc-name { font-size: 18px; font-weight: 700; color: #4a4a4a; margin-bottom: 5px; }
.uc-email { font-size: 12px; color: #999; margin-bottom: 15px; font-style: italic; }
.uc-manage-btn { padding: 6px 20px; border: 1px solid #d4af37; border-radius: 20px; font-size: 12px; color: #d4af37; font-weight: 600; cursor: pointer; transition: all 0.3s; }
.uc-manage-btn:hover { background: #d4af37; color: #fff; }
.uc-divider { height: 1px; background: #e8e0d5; width: 100%; margin: 0; }
.uc-menu { padding: 10px 0; }
.uc-menu-item { padding: 12px 25px; display: flex; align-items: center; gap: 15px; font-size: 14px; color: #666; cursor: pointer; transition: background 0.2s; }
.uc-menu-item:hover { background: rgba(212, 175, 55, 0.05); color: #d4af37; }
.uc-menu-item i { font-size: 16px; }
.logout-item { border-top: 1px solid #f5f0e6; margin-top: 5px; padding-top: 15px; }
.logout-item:hover { color: #ff6b6b; background: rgba(255, 107, 107, 0.05); }
.uc-footer-deco { text-align: center; font-size: 10px; color: #d4af37; opacity: 0.3; padding-bottom: 10px; letter-spacing: 2px; }

/* ================= 自定义弹窗样式 (回归标准样式) ================= */
.luna-modal-overlay {
  position: fixed; top: 0; left: 0; width: 100vw; height: 100vh;
  /* 回归简单的半透明黑，不使用 backdrop-filter */
  background: rgba(0, 0, 0, 0.5);
  z-index: 3000;
  display: flex; align-items: center; justify-content: center;
}
.luna-modal-card {
  width: 320px;
  background: #fffaf5;
  border-radius: 8px;
  border: 1px solid #d4af37;
  box-shadow: 0 10px 40px rgba(212, 175, 55, 0.2);
  padding: 25px;
  text-align: center;
  font-family: 'Noto Serif SC', serif;
}
.luna-modal-title { font-size: 18px; color: #4a4a4a; font-weight: 700; margin-bottom: 20px; }
.luna-input {
  width: 100%; padding: 10px; border: 1px solid #e0d0b0; border-radius: 4px;
  background: #fff; color: #4a4a4a; outline: none; transition: border-color 0.3s;
  box-sizing: border-box; /* 防止 padding 撑大宽度 */
}
.luna-input:focus { border-color: #d4af37; }
.luna-modal-tip { font-size: 12px; color: #999; margin-top: 5px; text-align: left; }
.luna-modal-footer { display: flex; justify-content: center; gap: 15px; margin-top: 25px; }
.luna-btn-confirm {
  background: #d4af37; color: #fff; border: none; padding: 8px 25px; border-radius: 4px; cursor: pointer; transition: all 0.2s;
}
.luna-btn-confirm:hover { background: #c5a028; transform: translateY(-2px); }
.luna-btn-cancel {
  background: transparent; color: #999; border: 1px solid #ddd; padding: 8px 25px; border-radius: 4px; cursor: pointer; transition: all 0.2s;
}
.luna-btn-cancel:hover { border-color: #4a4a4a; color: #4a4a4a; }

/* 弹窗动画 */
.luna-modal-enter-active, .luna-modal-leave-active { transition: opacity 0.3s; }
.luna-modal-enter, .luna-modal-leave-to { opacity: 0; }
.luna-modal-enter-active .luna-modal-card { animation: modalPop 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275); }
.luna-modal-leave-active .luna-modal-card { animation: modalPop 0.3s reverse; }
@keyframes modalPop {
  0% { opacity: 0; transform: scale(0.8) translateY(20px); }
  100% { opacity: 1; transform: scale(1) translateY(0); }
}

/* ================= 新增：自定义下拉面板样式 ================= */
.custom-popover-panel {
  position: absolute;
  top: 60px; /* 距离顶部的距离 */
  right: -20px; /* 略微向右偏移，保证对齐视觉舒适 */
  width: 320px;
  background: #fffaf5;
  border: 1px solid #d4af37;
  border-radius: 8px;
  box-shadow: 0 10px 40px rgba(212, 175, 55, 0.2);
  z-index: 3000;
  overflow: hidden;
}

/* 面板淡入淡出动画 */
.pop-fade-enter-active, .pop-fade-leave-active {
  transition: all 0.3s ease;
}
.pop-fade-enter, .pop-fade-leave-to {
  opacity: 0;
  transform: translateY(10px);
}
</style>

<style scoped>

ul { list-style: none; margin: 0; padding: 0; }
a { text-decoration: none; }
.header-wrapper { position: fixed; top: 0; left: 0; right: 0; margin: 0 auto; z-index: 2000; background: rgba(255, 250, 245, 0.8); backdrop-filter: blur(10px); will-change: width, top, border-radius, background; font-family: 'Noto Serif SC', 'Playfair Display', serif; }
.gold-line-top { position: absolute; top: 0; left: 0; width: 100%; height: 3px; background: linear-gradient(90deg, transparent, #d4af37, transparent); }
.header-content { height: 100%; padding: 0 30px; width: 100%; box-sizing: border-box; }
.flex-header { display: flex; justify-content: space-between; align-items: center; height: 100%; }
.logo-box { flex-shrink: 0; z-index: 2002; }
.header-logo { display: flex; align-items: center; gap: 8px; cursor: pointer; }
.logo-text { font-size: 24px; font-weight: 700; color: #4a4a4a; letter-spacing: 1px; }
.moon-icon, .sakura-icon { font-size: 18px; color: #4a4a4a; }
.nav-box { flex-grow: 1; display: flex; justify-content: center; }
.nav-list { display: flex; gap: 30px; }
.nav-item { position: relative; cursor: pointer; padding: 0 5px; height: 60px; display: flex; align-items: center; }
.nav-text-container { display: flex; flex-direction: column; align-items: center; justify-content: center; position: relative; }
.nav-text-en { font-size: 16px; font-weight: 600; color: #555; transition: color 0.3s; }
.nav-text-cn { font-size: 11px; color: #888; position: absolute; bottom: -12px; opacity: 0; white-space: nowrap; font-family: 'Noto Serif SC', serif; }
.nav-underline { position: absolute; bottom: 12px; left: 50%; transform: translateX(-50%); width: 0%; height: 2px; background: #d4af37; opacity: 0; pointer-events: none; }
.nav-item.active .nav-text-en { color: #d4af37; }
.nav-item.active .nav-underline { width: 100%; opacity: 1; bottom: 10px; }
.user-box { flex-shrink: 0; display: flex; align-items: center; position: relative; /* 确保子元素 absolute 定位相对于此 */ }
.user-trigger { cursor: pointer; padding: 5px; display: flex; align-items: center; gap: 10px; }
.avatar-wrapper { position: relative; }
.luna-avatar { border: 2px solid #fff; box-shadow: 0 2px 8px rgba(0,0,0,0.1); display: block; }
.avatar-glow { position: absolute; top: -3px; left: -3px; right: -3px; bottom: -3px; border: 1px solid rgba(212, 175, 55, 0.4); border-radius: 50%; transition: all 0.3s; }
.user-trigger:hover .avatar-glow { transform: scale(1.15); border-color: #d4af37; opacity: 0.8; }
.guest-avatar { background: #f0f0f0; color: #ccc; }
.avatar-ring-guest { position: absolute; top: -2px; left: -2px; right: -2px; bottom: -2px; border: 1px dashed #ccc; border-radius: 50%; transition: all 0.3s; }
.guest-trigger:hover .avatar-ring-guest { border-color: #d4af37; border-style: solid; }
.guest-label { font-size: 14px; color: #666; font-weight: 600; transition: color 0.3s; }
.guest-trigger:hover .guest-label { color: #d4af37; }
.mobile-toggle { display: flex; align-items: center; justify-content: center; width: 40px; height: 40px; cursor: pointer; z-index: 2002; }
.hamburger { width: 24px; height: 18px; position: relative; display: flex; flex-direction: column; justify-content: space-between; }
.line { display: block; width: 100%; height: 2px; background: #d4af37; border-radius: 2px; transition: all 0.3s; }
.hamburger.is-active .line-1 { transform: rotate(45deg) translate(5px, 6px); }
.hamburger.is-active .line-2 { opacity: 0; }
.hamburger.is-active .line-3 { transform: rotate(-45deg) translate(5px, -6px); }

/* ============ 回归稳定版：不透明遮罩层 ============ */
.mobile-menu-overlay {
  position: fixed;
  top: 65px; /* 从 Header 下方开始，不覆盖 Logo */
  left: 0;
  width: 100%;
  height: calc(100vh - 65px); /* 剩余高度 */
  /* 使用高不透明度的米白色，不使用 blur */
  background: rgba(255, 250, 245, 0.98);
  z-index: 2001;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }

.menu-bg-moon { position: absolute; bottom: -50px; right: -50px; font-size: 300px; color: #d4af37; opacity: 0.05; pointer-events: none; }
.mobile-menu-content { padding: 40px 30px; flex: 1; display: flex; flex-direction: column; align-items: center; }
.mobile-user-section { margin-bottom: 30px; text-align: center; }
.m-avatar-box { margin-bottom: 15px; }
.m-username { font-size: 18px; color: #4a4a4a; font-weight: 700; margin-top: 10px; }
.m-auth-actions span { font-size: 14px; color: #999; text-decoration: underline; cursor: pointer; }
.m-login-box { display: flex; gap: 20px; }
.m-login-btn, .m-register-btn { padding: 10px 30px; border: 1px solid #d4af37; border-radius: 4px; color: #d4af37; font-weight: 700; font-size: 16px; cursor: pointer; }
.m-login-btn { background: #d4af37; color: #fff; }
.mobile-divider { width: 60px; height: 1px; background: #e0d0b0; margin-bottom: 30px; }
.mobile-nav-list { width: 100%; text-align: center; }
.mobile-nav-item { padding: 15px 0; cursor: pointer; display: flex; flex-direction: column; align-items: center; transition: transform 0.2s; }
.mobile-nav-item:active { transform: scale(0.95); }
.m-en { font-size: 24px; font-weight: 700; color: #4a4a4a; }
.m-cn { font-size: 12px; color: #d4af37; margin-top: 5px; letter-spacing: 2px; }

@media (max-width: 768px) {
  .header-content { padding: 0 20px; }
  .hidden-xs-only { display: none !important; }
  .hidden-sm-and-up { display: flex !important; }
}
@media (min-width: 769px) {
  .hidden-sm-and-up { display: none !important; }
}

@media (max-width: 1280px) {
  .header-content { padding: 0 15px; }
  .nav-list { gap: 15px; }
  .logo-text { font-size: 20px; }
}

@media (max-width: 1000px) {
  .nav-list { gap: 10px; }
  .nav-text-en { display: none; }
  .nav-text-cn {
    position: static; opacity: 1; transform: none; font-size: 14px; color: #555;
  }
  .nav-item { height: 60px; justify-content: center; }
}
</style>
