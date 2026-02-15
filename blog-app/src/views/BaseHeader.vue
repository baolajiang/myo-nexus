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
              <li v-for="(item, index) in navItems" :key="item.path" class="nav-item" :class="{ active: activeIndex === item.path }" @mouseenter="onNavEnter($event)" @mouseleave="onNavLeave($event)">
                <router-link :to="item.path">
                  <div class="nav-text-container"><span class="nav-text-en">{{ item.en }}</span><span class="nav-text-cn">{{ item.name }}</span></div>
                </router-link>
                <div class="nav-underline"></div>
              </li>
              <li v-if="user.login" class="nav-item write-btn" @mouseenter="onNavEnter($event)" @mouseleave="onNavLeave($event)">
                <router-link to="/write"><div class="nav-text-container"><span class="nav-text-en">Write</span><span class="nav-text-cn">創作</span></div></router-link><div class="nav-underline"></div>
              </li>
            </ul>
          </div>
          <div class="user-box hidden-xs-only" ref="userBox">
            <template v-if="!user.login">
              <div class="user-trigger guest-trigger" @click.stop="togglePanel('guest')">
                <div class="avatar-wrapper"><el-avatar :size="36" :src="guestAvatarDisplay" icon="el-icon-user-solid" class="guest-avatar"></el-avatar><div class="avatar-ring-guest"></div></div>
                <span class="guest-label">{{ guest.nickname || '旅人' }}</span>
              </div>
              <transition name="pop-fade"><div v-if="activePanel === 'guest'" class="custom-popover-panel" @click.stop><div class="user-card-content guest-panel" ref="guestPanel"><div class="guest-tabs"><div class="tab-item" :class="{ active: guestTab === 'info' }" @click="guestTab = 'info'">旅人身份</div><div class="tab-item" :class="{ active: guestTab === 'auth' }" @click="guestTab = 'auth'">登入 / 註冊</div><div class="tab-cursor" :style="{ left: guestTab === 'info' ? '0%' : '50%' }"></div></div><div class="smooth-height-box" ref="smoothBox"><transition name="tab-slide" mode="out-in" @before-leave="beforeLeave" @enter="enter" @after-enter="afterEnter"><div v-if="guestTab === 'info'" key="info" class="guest-info-content"><div class="guest-header-top"><div class="guest-big-avatar-box clickable" @click="openEditModal('email')"><el-avatar :size="70" :src="guestAvatarDisplay" icon="el-icon-user-solid" class="guest-big-avatar"></el-avatar><div class="guest-avatar-border"></div><div class="avatar-edit-overlay"><i class="el-icon-camera"></i><span>更換</span></div></div><div class="guest-greeting-text">貴安，{{ guest.nickname || '旅人' }}</div><div class="guest-uid-deco">UID: {{ guest.uuid ? guest.uuid.substring(0, 8).toUpperCase() : 'UNKNOWN' }}</div></div><div class="luna-alert"><i class="el-icon-info"></i><div class="alert-text">旅人身份保存於本地，清除緩存將丟失。<br>僅用於評論與點贊。</div></div><div class="info-list"><div class="info-row"><span class="info-label">ID</span><span class="info-val id-font">{{ guest.uuid }}</span><span class="info-btn" @click="copyText(guest.uuid)">複製</span></div><div class="info-row"><span class="info-label">名字</span><span class="info-val">{{ guest.nickname }}</span><span class="info-btn" @click="openEditModal('nickname')">修改</span></div><div class="info-row"><span class="info-label">郵箱</span><span class="info-val placeholder" v-if="!guest.email">點擊填寫以獲取頭像</span><span class="info-val" v-else>{{ guest.email }}</span><span class="info-btn" @click="openEditModal('email')">編輯</span></div><div class="info-row last"><span class="info-label">網站</span><span class="info-val placeholder" v-if="!guest.website">未填寫</span><span class="info-val" v-else>{{ guest.website }}</span><span class="info-btn" @click="openEditModal('website')">編輯</span></div></div></div><div v-else key="auth" class="guest-auth-content"><div class="auth-welcome-box"><div class="welcome-star">✦</div><div class="welcome-title">貴安，旅人</div><div class="welcome-desc">歡迎來到月之別邸。<br>登入後即可發表文章、收藏內容并同步數據。</div></div><div class="guest-actions"><div class="luna-btn primary-btn" @click="login">立即登入</div><div class="luna-btn outline-btn" @click="register">註冊新帳號</div></div></div></transition></div><div class="uc-footer-deco"><span>☾</span> LUNA GUEST SYSTEM <span>❀</span></div></div></div></transition>
            </template>
            <template v-else>
              <div class="user-trigger" @click.stop="togglePanel('user')">
                <div class="avatar-wrapper"><el-avatar :size="36" :src="user.avatar || require('@/assets/img/default_avatar.png')" class="luna-avatar"></el-avatar><div class="avatar-glow"></div></div>
              </div>
              <transition name="pop-fade"><div v-if="activePanel === 'user'" class="custom-popover-panel" @click.stop><div class="user-card-content" ref="userPanel"><div class="uc-header"><div class="uc-avatar-box"><el-avatar :size="70" :src="user.avatar || require('@/assets/img/default_avatar.png')" class="uc-avatar"></el-avatar><div class="uc-avatar-border"></div></div><div class="uc-name">{{ user.nickname || 'Unknown User' }}</div><div class="uc-email">{{ user.email || 'Welcome back to the manor' }}</div><div class="uc-manage-btn" @click="openPersonalCenter">個人中心</div></div><div class="uc-divider"></div><div class="uc-menu">
                <div class="uc-menu-item" @click="navTo('/space/' + user.id)">
                  <i class="el-icon-document"></i> <span>我的文章</span>
                </div>
                <div class="uc-menu-item" @click="jumpToAdmin"><i class="el-icon-setting"></i> <span>系統設置</span></div><div class="uc-menu-item logout-item" @click="logout"><i class="el-icon-switch-button"></i> <span>登出帳戶</span></div></div><div class="uc-footer-deco"><span>☾</span> LUNA SYSTEM <span>❀</span></div></div></div></transition>
            </template>
          </div>
          <div class="mobile-toggle hidden-sm-and-up" @click="toggleMobileMenu"><div class="hamburger" :class="{ 'is-active': isMobileMenuOpen }"><span class="line line-1"></span><span class="line line-2"></span><span class="line line-3"></span></div></div>
        </div>
      </div>
      <transition @enter="enterMobileMenu" @leave="leaveMobileMenu"><div v-show="isMobileMenuOpen" class="mobile-menu-overlay"><div class="menu-bg-moon">☾</div><div class="mobile-menu-content"><div class="mobile-user-section mobile-item"><template v-if="user.login"><div class="m-avatar-box"><el-avatar :size="60" :src="user.avatar || require('@/assets/img/default_avatar.png')" class="luna-avatar"></el-avatar><div class="m-username">貴安, {{ user.nickname }}</div></div><div class="m-auth-actions"><span @click="logoutAndClose">登出帳戶</span></div></template><template v-else><div class="m-login-box"><div class="m-login-btn" @click="navTo('/login')">登入</div><div class="m-register-btn" @click="register">註冊</div></div></template></div><div class="mobile-divider mobile-item"></div><ul class="mobile-nav-list"><li v-for="item in navItems" :key="item.path" class="mobile-nav-item mobile-item" @click="navTo(item.path)"><span class="m-en">{{ item.en }}</span><span class="m-cn">{{ item.name }}</span></li><li v-if="user.login" class="mobile-nav-item mobile-item" @click="navTo('/write')"><span class="m-en">Write</span><span class="m-cn">創作</span></li></ul></div></div></transition>
      <transition name="luna-modal"><div v-if="showEditModal" class="luna-modal-overlay" @click.self="closeEditModal"><div class="luna-modal-card"><div class="luna-modal-title">修改{{ editFieldMap[editField] }}</div><div class="luna-modal-body"><input type="text" v-model="editValue" class="luna-input" :placeholder="'請輸入' + editFieldMap[editField]" @keyup.enter="confirmEdit"><div v-if="editField === 'email'" class="luna-modal-tip">* 輸入 QQ 郵箱可自動獲取頭像</div></div><div class="luna-modal-footer"><button class="luna-btn-cancel" @click="closeEditModal">取消</button><button class="luna-btn-confirm" @click="confirmEdit">確定</button></div></div></div></transition>
    </div>

    <el-dialog
      title="裁剪頭像"
      :visible.sync="cropperVisible"
      width="600px"  append-to-body
      :close-on-click-modal="false"
      custom-class="cropper-dialog"
    >
      <div class="cropper-content">
        <div class="cropper-box">
          <vueCropper
            ref="cropper"
            :img="cropperImg"
            :outputSize="option.size"
            :outputType="option.outputType"
            :info="true"
            :full="option.full"
            :canMove="option.canMove"
            :canMoveBox="option.canMoveBox"
            :original="option.original"
            :autoCrop="option.autoCrop"
            :autoCropWidth="option.autoCropWidth"
            :autoCropHeight="option.autoCropHeight"
            :fixedBox="option.fixedBox"
            :centerBox="option.centerBox"
            :high="option.high"
          ></vueCropper>
        </div>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="cropperVisible = false" size="small">取 消</el-button>
        <el-button type="primary" @click="finishCrop" :loading="cropLoading" size="small">确认裁剪</el-button>
      </span>
    </el-dialog>

    <el-dialog
      :visible.sync="personalInfoVisible"
      width="420px" :close-on-click-modal="false"
      append-to-body
      :lock-scroll="false"
      custom-class="luna-profile-dialog"
      :show-close="false"
      top="8vh"
    >
      <div class="luna-profile-card-pro">

        <input type="file" ref="avatarInput" accept="image/*" style="display: none" @change="handleFileChange">

        <div class="pro-cover">
          <img class="cover-img" src="https://img.paulzzh.com/touhou/random?12" alt="cover">
          <div class="cover-mask"></div>
          <div class="pro-close-btn" @click="handleClose"><i class="el-icon-close"></i></div>
        </div>

        <div class="pro-info-container">
          <div class="pro-avatar-box">
            <el-avatar :size="84" :src="form.avatar" class="pro-avatar"></el-avatar>

            <div class="pro-avatar-action" @click="handleAvatarAction">
              <i :class="isEditing ? 'el-icon-camera-solid' : 'el-icon-zoom-in'"></i>
            </div>
          </div>

          <el-dialog
            :visible.sync="previewVisible"
            :lock-scroll="false"
            append-to-body
            custom-class="avatar-preview-dialog"
            width="400px"
          >
            <img :src="form.avatar" style="width: 100%; border-radius: 4px;">
          </el-dialog>

          <div class="pro-identity center">
            <div class="name-row">
              <div class="name-wrapper">
                <input v-if="isEditing" v-model="form.nickname" class="name-input" placeholder="请输入昵称" ref="nameInput">
                <span v-else class="pro-name">{{ form.nickname || '未命名' }}</span>
              </div>

            </div>
            <div class="pro-uid">UID: {{ form.account || '000000' }}</div>
          </div>

          <div class="pro-stats-bar">
            <div class="stat-item"><span class="stat-num">0</span><span class="stat-label">文章</span></div>
            <div class="stat-item"><span class="stat-num">0</span><span class="stat-label">评论</span></div>
            <div class="stat-item"><span class="stat-num">0</span><span class="stat-label">获赞</span></div>
            <div class="stat-item"><span class="stat-num">0</span><span class="stat-label">收藏</span></div>
          </div>

          <div class="detail-list">
            <div class="detail-item fixed-height-row">
              <div class="row-icon"><i class="el-icon-mobile-phone"></i></div>
              <div class="row-content">
                <template v-if="!isEditing"><span class="item-text" :class="{ empty: !form.mobilePhoneNumber }">{{ form.mobilePhoneNumber || '未绑定手机' }}</span></template>
                <input v-else v-model="form.mobilePhoneNumber" class="detail-input" placeholder="请输入手机号">
              </div>
            </div>
            <div class="detail-item fixed-height-row">
              <div class="row-icon"><i class="el-icon-message"></i></div>
              <div class="row-content">
                <template v-if="!isEditing"><span class="item-text" :class="{ empty: !form.email }">{{ form.email || '未绑定邮箱' }}</span></template>
                <input v-else v-model="form.email" class="detail-input" placeholder="请输入邮箱">
              </div>
            </div>
          </div>

          <div class="pro-footer fixed-footer">
            <button v-if="!isEditing" class="pro-btn-outline" @click="startEdit">编辑资料</button>
            <div v-else class="action-btns">
              <button class="pro-btn-ghost" @click="cancelEdit">取消</button>
              <button class="pro-btn-save" @click="submitUserInfo" :disabled="loading">{{ loading ? '保存中...' : '保存' }}</button>
            </div>
          </div>

        </div>
      </div>
    </el-dialog>

  </div>
</template>

<script>
import { gsap } from 'gsap'
import md5 from 'js-md5'
import { getTicket } from '@/api/login'
import { updateUser } from '@/api/user'
import { upload } from '@/api/upload'
import { APP_CONFIG } from '../../config/config.js'

export default {
  name: 'BaseHeader',
  props: { activeIndex: { type: String, default: '/' } },
  data() {
    return {
      previewVisible: false, // 控制预览弹窗
      isScrolled: false, ticking: false, isMobileMenuOpen: false,
      activePanel: null, guestTab: 'info',
      showEditModal: false, editField: '', editValue: '',
      editFieldMap: { nickname: '名字', email: '郵箱', website: '網站' },
      navItems: [
        { name: '首頁', en: 'Home', path: '/', icon: 'el-icon-s-home' },
        { name: '文章', en: 'articles', path: '/articles/page/1', icon: 'el-icon-document' },
        { name: '分類', en: 'Category', path: '/category/all', icon: 'el-icon-menu' },
        { name: '標籤', en: 'Tags', path: '/tag/all', icon: 'el-icon-price-tag' },
        { name: '導航', en: 'Links', path: '/nav', icon: 'el-icon-compass' },
        { name: '關於', en: 'Resume', path: '/Resume', icon: 'el-icon-info' },
        { name: '留言', en: 'Guestbook', path: '/messageBoard', icon: 'el-icon-chat-dot-round' }
      ],
      // 个人中心
      personalInfoVisible: false,
      loading: false,
      isEditing: false,
      uploadFile: null,
      form: { id: '', account: '', nickname: '', avatar: '', mobilePhoneNumber: '', email: '', sex: 2 },
      // 裁剪相关
      cropperVisible: false,
      cropperImg: '',
      cropLoading: false,
      option: {
        size: 1,
        full: false,
        outputType: 'png',
        canMove: true,
        fixedBox: true,
        original: false,
        canMoveBox: true,
        autoCrop: true,
        autoCropWidth: 200,
        autoCropHeight: 200,
        centerBox: true,
        high: true
      }
    }
  },
  computed: {
    user() {
      let login = this.$store.state.account ? this.$store.state.account.length != 0 : false
      let avatar = this.$store.state.avatar; let nickname = this.$store.state.name; let email = this.$store.state.email
      let id = this.$store.state.id
      return { login, avatar, nickname, email, id }
    },
    guest() { return this.$store.state.guest; },
    guestAvatarDisplay() {
      const email = this.guest.email || '';
      if (email.match(/^\d+@qq\.com$/)) { return `https://q1.qlogo.cn/g?b=qq&nk=${email.replace('@qq.com', '')}&s=100`; }
      if (email) { const hash = md5(email.trim().toLowerCase()); return `https://cravatar.cn/avatar/${hash}?s=100&d=identicon`; }
      return this.guest.avatar || '';
    },
    userInfo() { return this.$store.state; }
  },
  watch: {
    isScrolled(newVal) { if (!this.isMobileMenuOpen) { newVal ? this.animateToCapsule() : this.animateToFull() } },
    activePanel(newVal) { if (!newVal) { setTimeout(() => { this.guestTab = 'info'; }, 300); } }
  },
  mounted() {
    window.addEventListener('scroll', this.handleScroll);
    document.addEventListener('click', this.closeAllPanels);
    this.animateToFull(true);
  },
  destroyed() {
    window.removeEventListener('scroll', this.handleScroll);
    document.removeEventListener('click', this.closeAllPanels);
  },
  methods: {
    openPersonalCenter() {
      this.closeAllPanels();
      this.personalInfoVisible = true;
      this.isEditing = false;
      this.uploadFile = null;
      this.initForm();
    },
    initForm() {
      if (this.userInfo) {
        this.form = {
          id: this.userInfo.id,
          account: this.userInfo.account,
          nickname: this.userInfo.name || this.userInfo.nickname,
          avatar: this.userInfo.avatar,
          mobilePhoneNumber: this.userInfo.mobilePhoneNumber,
          email: this.userInfo.email,
          sex: this.userInfo.sex !== undefined ? this.userInfo.sex : 2
        };
      }
    },
    startEdit() { this.isEditing = true; this.$nextTick(() => { if(this.$refs.nameInput) this.$refs.nameInput.focus(); }); },
    cancelEdit() { this.isEditing = false; this.uploadFile = null; this.initForm(); },
    handleClose() { this.personalInfoVisible = false; this.isEditing = false; },
    getGenderClass(sex) {
      if (sex === 1) return 'el-icon-male male-color';
      if (sex === 0) return 'el-icon-female female-color';
      return 'el-icon-lock secret-color';
    },

    // 图片上传流程
    handleAvatarClick() { if (this.isEditing) { this.$refs.avatarInput.click(); } },
    handleFileChange(e) {
      const file = e.target.files[0];
      if (!file) return;
      // 限制大小 (GIF通常比较大，建议这里可以单独放宽一点，或者保持一致)
      if (file.size > 5 * 1024 * 1024) { this.$message.warning('图片大小不能超过 5MB'); return; }
      // ============================================================
      // GIF 特殊处理逻辑
      // 如果是 GIF，直接使用原图，跳过裁剪（为了保留动图效果）
      // ============================================================
      if (file.type === 'image/gif') {
        this.uploadFile = file; // 直接存入暂存区
        this.form.avatar = URL.createObjectURL(file); // 直接生成本地预览
        e.target.value = ''; // 清空 input 防止重复选不触发
        this.$message.info('GIF 圖片已自動跳過裁剪以保留動畫效果');
        return;
      }
      // ============================================================
      // 其他格式 (JPG/PNG/WEBP)：进入裁剪流程
      // ============================================================
      const reader = new FileReader();
      reader.onload = (event) => {
        this.cropperImg = event.target.result;
        this.cropperVisible = true; // 打开裁剪框
      };
      reader.readAsDataURL(file);
      e.target.value = '';// 清空 input
    },
    finishCrop() {
      this.cropLoading = true;
      this.$refs.cropper.getCropBlob((data) => {
        const file = new File([data], 'avatar.png', { type: 'image/png' });
        this.uploadFile = file;
        this.form.avatar = URL.createObjectURL(data);
        this.cropperVisible = false;
        this.cropLoading = false;
      })
    },

    async submitUserInfo() {
      this.loading = true;
      try {
        if (this.uploadFile) {
          const formData = new FormData();
          formData.append('image', this.uploadFile);
          formData.append('path', 'avatar');
          //await 关键字起到了关键作用，它的意思就是 只有等这一行执行完，拿到结果了，才会继续执行下一行代码。
          const uploadRes = await upload(formData);
          if (uploadRes.success) {
            this.form.avatar = uploadRes.data;
          } else {
            this.$message.error(uploadRes.msg || '圖片上傳失敗');
            this.loading = false;
            return;
          }
        }
        const updateRes = await updateUser(this.form);
        if (updateRes.success) {
          this.$message.success('資料已更新');
          this.isEditing = false;
          this.uploadFile = null;
          this.$store.commit('SET_NAME', this.form.nickname);
          this.$store.commit('SET_AVATAR', this.form.avatar);
          this.$store.commit('SET_EMAIL', this.form.email);
          this.$store.commit('SET_MOBILE_PHONE_NUMBER', this.form.mobilePhoneNumber);
        } else {
          this.$message.error(updateRes.msg || '修改失敗');
        }
      } catch (error) {
        console.error(error);
        this.$message.error('系統異常');
      } finally {
        this.loading = false;
      }
    },

    togglePanel(type) { if (this.activePanel === type) { this.activePanel = null; } else { this.activePanel = type; this.$nextTick(() => { if(type === 'guest') this.onGuestShow(); if(type === 'user') this.onUserShow(); }); } },
    closeAllPanels() { this.activePanel = null; },
    openEditModal(field) { this.closeAllPanels(); this.editField = field; this.editValue = this.guest[field]; this.showEditModal = true; },
    closeEditModal() { this.showEditModal = false; },
    confirmEdit() {
      let value = this.editValue.trim();
      if (!value) { this.saveAndClose(value); return; }
      if (this.editField === 'email') { if (!/^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,6}$/.test(value)) { this.$myMessage.error('郵箱格式錯誤'); return; } }
      else if (this.editField === 'website') { if (!/^(https?:\/\/)?([\da-z\.-]+)\.([a-z\.]{2,6})([\/\w \.-]*)*\/?$/.test(value)) { this.$myMessage.error('網址格式錯誤'); return; } if (!/^https?:\/\//.test(value)) { value = 'https://' + value; } }
      this.saveAndClose(value);
    },
    saveAndClose(finalValue) { this.$store.commit('UPDATE_GUEST', { [this.editField]: finalValue }); this.$myMessage.success('更新成功'); this.closeEditModal(); },
    copyText(text) { const input = document.createElement('input'); input.setAttribute('readonly', 'readonly'); input.setAttribute('value', text); document.body.appendChild(input); input.select(); if (document.execCommand('copy')) { this.$myMessage.success('複製成功'); } document.body.removeChild(input); },
    onGuestShow() { this.$nextTick(() => { const el = this.$refs.guestPanel; if(el) gsap.fromTo(el, { y: 15, opacity: 0, scale: 0.96 }, { y: 0, opacity: 1, scale: 1, duration: 0.4, ease: "back.out(1.7)", overwrite: true }); }); },
    onUserShow() { this.$nextTick(() => { const el = this.$refs.userPanel; if(el) gsap.fromTo(el, { y: 15, opacity: 0, scale: 0.96 }, { y: 0, opacity: 1, scale: 1, duration: 0.4, ease: "back.out(1.7)", overwrite: true }); }); },
    beforeLeave(el) {if (this.$refs.smoothBox) {this.$refs.smoothBox.style.height = this.$refs.smoothBox.scrollHeight + 'px';}},
    enter(el) { if (this.$refs.smoothBox) {this.$refs.smoothBox.style.height = el.scrollHeight + 'px'; }},
    afterEnter(el) {if (this.$refs.smoothBox) { this.$refs.smoothBox.style.height = 'auto'; }},
    onLogoEnter() { gsap.to(this.$refs.moonIcon, { rotation: -20, color: '#d4af37', duration: 0.4, ease: 'back.out' }); gsap.to(this.$refs.sakuraIcon, { rotation: 20, color: '#ffb7c5', duration: 0.4, ease: 'back.out' }); gsap.to(this.$refs.logoText, { color: '#d4af37', duration: 0.4 }); },
    onLogoLeave() { gsap.to(this.$refs.moonIcon, { rotation: 0, color: '#4a4a4a', duration: 0.4 }); gsap.to(this.$refs.sakuraIcon, { rotation: 0, color: '#4a4a4a', duration: 0.4 }); gsap.to(this.$refs.logoText, { color: '#4a4a4a', duration: 0.4 }); },
    onNavEnter(e) {
      const target = e.currentTarget;
      const enText = target.querySelector('.nav-text-en');
      const cnText = target.querySelector('.nav-text-cn');
      const underline = target.querySelector('.nav-underline');
      gsap.killTweensOf([enText, cnText, underline]);

      if (window.innerWidth <= 1000) {
        // 小屏幕设备：仅改变中文颜色和激活下划线
        gsap.to(cnText, { color: '#d4af37', duration: 0.3 });
        gsap.to(underline, { width: '100%', opacity: 1, duration: 0.4, ease: 'power2.out' });
      } else {
        // 大屏幕设备：保留原有的上下浮动动画
        gsap.to(enText, { y: -5, color: '#d4af37', fontWeight: '700', duration: 0.3, ease: 'power2.out' });
        gsap.to(cnText, { y: 2, opacity: 1, color: '#888', duration: 0.3, ease: 'power2.out' });
        gsap.to(underline, { width: '100%', opacity: 1, duration: 0.4, ease: 'power2.out' });
      }
    },

    onNavLeave(e) {
      const target = e.currentTarget;
      const enText = target.querySelector('.nav-text-en');
      const cnText = target.querySelector('.nav-text-cn');
      const underline = target.querySelector('.nav-underline');
      gsap.killTweensOf([enText, cnText, underline]);

      if (window.innerWidth <= 1000) {
        // 小屏幕设备：仅恢复中文颜色和隐藏下划线，绝对不修改透明度
        gsap.to(cnText, { color: '#555', duration: 0.3 });
        gsap.to(underline, { width: '0%', opacity: 0, duration: 0.3 });
      } else {
        // 大屏幕设备：恢复原来的隐藏逻辑
        gsap.to(enText, { y: 0, color: '#555', fontWeight: '600', duration: 0.3 });
        gsap.to(cnText, { y: 10, opacity: 0, duration: 0.3 });
        gsap.to(underline, { width: '0%', opacity: 0, duration: 0.3 });
      }
    },
    handleScroll() { if (!this.ticking) { window.requestAnimationFrame(() => { const scrollTop = window.pageYOffset || document.documentElement.scrollTop; const shouldScroll = scrollTop > 60; if (this.isScrolled !== shouldScroll) { this.isScrolled = shouldScroll } this.ticking = false }); this.ticking = true } },
    animateToCapsule() { if (this.isMobileMenuOpen) return; gsap.to(this.$refs.headerWrapper, { top: 15, width: '95%', maxWidth: '1200px', height: '60px', borderRadius: '30px', backgroundColor: 'rgba(255, 250, 245, 0.98)', border: '1px solid rgba(212, 175, 55, 0.3)', boxShadow: '0 8px 30px rgba(212, 175, 55, 0.15)', duration: 0.6, ease: 'power3.out' }); gsap.to(this.$refs.goldLine, { opacity: 0, duration: 0.3 }); },
    animateToFull(immediate = false) { const duration = immediate ? 0 : 0.6; gsap.to(this.$refs.headerWrapper, { top: 0, width: '100%', maxWidth: '100%', height: '65px', borderRadius: 0, backgroundColor: 'rgba(255, 250, 245, 0.8)', border: 'none', borderBottom: '1px solid rgba(212, 175, 55, 0.1)', boxShadow: 'none', duration: duration, ease: 'power3.out' }); gsap.to(this.$refs.goldLine, { opacity: 1, duration: 0.3 }); },
    toggleMobileMenu() { this.isMobileMenuOpen = !this.isMobileMenuOpen; if (this.isMobileMenuOpen) { this.animateToFull(); document.body.style.overflow = 'hidden'; } else { document.body.style.overflow = ''; if (this.isScrolled) this.animateToCapsule(); } },
    closeMobileMenu() { this.isMobileMenuOpen = false; document.body.style.overflow = ''; if (this.isScrolled) this.animateToCapsule(); },
    navTo(path) {
      console.log(path)
      this.closeAllPanels(); this.$router.push({ path }); this.closeMobileMenu(); },
    jumpToAdmin() { this.closeAllPanels(); const token = this.$store.state.token; if (!token) { this.$myMessage.error('請先登入'); return; } const adminBaseUrl = APP_CONFIG.adminUrl; getTicket(token).then(res => { if (res.success) { window.open(`${adminBaseUrl}?ticket=${res.data}`, '_blank'); } else { this.$myMessage.error(res.msg || '无法获取跳转凭证'); } }).catch(err => { console.error(err); this.$myMessage.error('跳转失败'); }); },
    logoutAndClose() { this.$store.dispatch('logout').then(() => { this.$router.push({path: '/'}); this.closeMobileMenu(); }); },
    logout() { this.closeAllPanels(); this.$store.dispatch('logout').then(() => { this.$router.push({path: '/'}) }) },
    login() { this.closeAllPanels(); this.$router.push({path: '/login'}) },
    register() { this.closeAllPanels(); this.closeMobileMenu(); this.$router.push({ path: '/register' }) },
    enterMobileMenu(el, done) { gsap.fromTo(el, { opacity: 0 }, { opacity: 1, duration: 0.4 }); const items = el.querySelectorAll('.mobile-item'); gsap.fromTo(items, { y: 30, opacity: 0 }, { y: 0, opacity: 1, duration: 0.5, stagger: 0.05, ease: "power2.out", onComplete: done }); gsap.fromTo(el.querySelector('.menu-bg-moon'), { rotation: -30, opacity: 0, scale: 0.8 }, { rotation: 0, opacity: 0.1, scale: 1, duration: 1, ease: "power2.out" }); },
    leaveMobileMenu(el, done) { gsap.to(el, { opacity: 0, duration: 0.3, onComplete: done }); },
    // 统一处理头像点击动作
    handleAvatarAction() {
      if (this.isEditing) {
        // 编辑模式：触发文件选择
        this.$refs.avatarInput.click();
      } else {
        // 查看模式：打开预览大图
        this.previewVisible = true;
      }
    },
  }
}
</script>

<style>
/* 弹窗重置 */
.luna-profile-dialog { background: transparent !important; box-shadow: none !important; border-radius: 16px; }
.luna-profile-dialog .el-dialog__header { display: none; }
.luna-profile-dialog .el-dialog__body { padding: 0 !important; }

/* 关键修复：响应式弹窗宽度 */
@media (max-width: 768px) {
  .luna-profile-dialog { width: 90% !important; margin-top: 5vh !important; }
  .cropper-dialog { width: 95% !important; }
}

/* 关键修复：阻止 preventDefault 报错 */
.cropper-box { width: 100%; height: 300px; margin-bottom: 20px; touch-action: none; }

/* Popover */
.gender-popper { padding: 5px 0 !important; min-width: 100px !important; border-radius: 8px !important; border: 1px solid #eee !important; box-shadow: 0 4px 12px rgba(0,0,0,0.1) !important; }
.gender-options { display: flex; flex-direction: column; }
.g-opt { padding: 10px 15px; cursor: pointer; display: flex; align-items: center; gap: 10px; font-size: 14px; color: #555; transition: background 0.2s; }
.g-opt:hover { background: #f9f9f9; }
.g-opt.male i { color: #409EFF; }
.g-opt.female i { color: #F56C6C; }
.g-opt.secret i { color: #909399; }
</style>

<style scoped>
/* 继承旧样式 */
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
.user-box { flex-shrink: 0; display: flex; align-items: center; position: relative; }
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
.custom-popover-panel { position: absolute; top: 60px; right: -20px; width: 320px; background: #fffaf5; border: 1px solid #d4af37; border-radius: 8px; box-shadow: 0 10px 40px rgba(212, 175, 55, 0.2); z-index: 3000; overflow: hidden; }
.pop-fade-enter-active, .pop-fade-leave-active { transition: all 0.3s ease; }
.pop-fade-enter, .pop-fade-leave-to { opacity: 0; transform: translateY(10px); }
.user-card-content { font-family: 'Noto Serif SC', 'Playfair Display', serif; width: 100%; }
.guest-panel { padding: 0; background: #fffaf5; }
.guest-tabs { display: flex; position: relative; border-bottom: 1px solid #e0d0b0; }
.tab-item { flex: 1; text-align: center; padding: 15px 0; font-size: 14px; font-weight: 600; color: #999; cursor: pointer; transition: color 0.3s; }
.tab-item.active { color: #d4af37; }
.tab-cursor { position: absolute; bottom: 0; height: 2px; width: 50%; background: #d4af37; transition: left 0.3s ease; }
.smooth-height-box { transition: height 0.4s cubic-bezier(0.25, 0.8, 0.25, 1); overflow: hidden; }
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
.mobile-menu-overlay { position: fixed; top: 65px; left: 0; width: 100%; height: calc(100vh - 65px); background: rgba(255, 250, 245, 0.98); z-index: 2001; display: flex; flex-direction: column; overflow-y: auto; animation: fadeIn 0.3s ease; }
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
.luna-modal-overlay { position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; background: rgba(0, 0, 0, 0.5); z-index: 3000; display: flex; align-items: center; justify-content: center; }
.luna-modal-card { width: 320px; background: #fffaf5; border-radius: 8px; border: 1px solid #d4af37; box-shadow: 0 10px 40px rgba(212, 175, 55, 0.2); padding: 25px; text-align: center; font-family: 'Noto Serif SC', serif; }
.luna-modal-title { font-size: 18px; color: #4a4a4a; font-weight: 700; margin-bottom: 20px; }
.luna-input { width: 100%; padding: 10px; border: 1px solid #e0d0b0; border-radius: 4px; background: #fff; color: #4a4a4a; outline: none; transition: border-color 0.3s; box-sizing: border-box; }
.luna-input:focus { border-color: #d4af37; }
.luna-modal-tip { font-size: 12px; color: #999; margin-top: 5px; text-align: left; }
.luna-modal-footer { display: flex; justify-content: center; gap: 15px; margin-top: 25px; }
.luna-btn-confirm { background: #d4af37; color: #fff; border: none; padding: 8px 25px; border-radius: 4px; cursor: pointer; transition: all 0.2s; }
.luna-btn-confirm:hover { background: #c5a028; transform: translateY(-2px); }
.luna-btn-cancel { background: transparent; color: #999; border: 1px solid #ddd; padding: 8px 25px; border-radius: 4px; cursor: pointer; transition: all 0.2s; }
.luna-btn-cancel:hover { border-color: #4a4a4a; color: #4a4a4a; }
.luna-modal-enter-active, .luna-modal-leave-active { transition: opacity 0.3s; }
.luna-modal-enter, .luna-modal-leave-to { opacity: 0; }
.luna-modal-enter-active .luna-modal-card { animation: modalPop 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275); }
.luna-modal-leave-active .luna-modal-card { animation: modalPop 0.3s reverse; }
@keyframes modalPop { 0% { opacity: 0; transform: scale(0.8) translateY(20px); } 100% { opacity: 1; transform: scale(1) translateY(0); } }

/* ================== 个人中心 CSS (终极防抖版) ================== */

/* 1. 卡片主体 */
.luna-profile-card-pro {
  background: #fff; border-radius: 16px; overflow: hidden; position: relative;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.2);
  /* 统一字体，防止渲染差异 */
  font-family: "Helvetica Neue", Helvetica, "PingFang SC", "Microsoft YaHei", Arial, sans-serif;
  min-height: 520px; /* 设定最小高度，防止高度塌陷 */
}

/* 2. 封面区域 */
.pro-cover { height: 160px; position: relative; overflow: hidden; background: #333; }
.cover-img { width: 100%; height: 100%; object-fit: cover; opacity: 0.9; }
.cover-mask { position: absolute; bottom: 0; left: 0; width: 100%; height: 60px; background: linear-gradient(to top, rgba(0,0,0,0.4), transparent); }
.pro-close-btn { position: absolute; top: 5px; right: 5px; width: 32px; height: 32px; background: rgba(0,0,0,0.3); backdrop-filter: blur(4px); border-radius: 50%; color: #fff; display: flex; align-items: center; justify-content: center; cursor: pointer; transition: all 0.3s; z-index: 20; }
.pro-close-btn:hover { background: rgba(0,0,0,0.6); transform: rotate(90deg); }

/* 3. 信息容器 */
.pro-info-container { padding: 0 25px 25px; position: relative; margin-top: -45px; }

/* 头像 & 相机图标 */
.pro-avatar-box { position: relative; width: 84px; height: 84px; border-radius: 50%; border: 4px solid #fff; background: #fff; box-shadow: 0 4px 12px rgba(0,0,0,0.1); margin-bottom: 10px; }
.pro-avatar-camera {
  position: absolute; bottom: 0; right: 0;
  width: 28px; height: 28px;
  background: #d4af37; border-radius: 50%; border: 2px solid #fff;
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-size: 14px; cursor: pointer;
  box-shadow: 0 2px 6px rgba(0,0,0,0.2);
  transition: transform 0.2s;
  z-index: 10;
}
.pro-avatar-camera:hover { transform: scale(1.1); }

/* 身份信息 (固定高度容器，Flex布局保证对齐) */
.pro-identity { margin-bottom: 20px; }
.name-row {
  display: flex; align-items: center; justify-content: center;
  height: 40px; /* 强制高度，无论内容是文字还是输入框 */
  margin-bottom: 5px;
}
.name-wrapper {
  display: flex; align-items: center; justify-content: center;
  height: 100%;
}


/* 昵称文本 */
.pro-name {
  font-size: 22px; font-weight: 700; color: #333;
  line-height: 1; /* 紧凑行高 */
}
/* 昵称输入框 (去默认样式，模拟纯文本高度) */
.name-input {
  font-size: 22px; font-weight: 700; color: #333;
  text-align: center; border: none; border-bottom: 2px solid #d4af37;
  width: 140px; outline: none; padding: 0; margin: 0;
  background: transparent;
  height: 30px; /* 留足空间 */
  line-height: 30px;
}
.pro-uid { font-size: 12px; color: #ccc; margin-bottom: 8px; font-family: monospace; text-align: center; }

/* 性别图标 */
.sex-icon { padding: 4px; border-radius: 50%; font-size: 14px; transition: all 0.3s; display: block; }
.sex-icon.pointer { cursor: pointer; }
.sex-icon.pointer:hover { transform: scale(1.1); box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
.male-color { color: #409EFF; background: rgba(64, 158, 255, 0.1); }
.female-color { color: #F56C6C; background: rgba(245, 108, 108, 0.1); }
.secret-color { color: #909399; background: rgba(144, 147, 153, 0.1); }

/* 数据统计栏 */
.pro-stats-bar { display: flex; justify-content: space-around; background: #f9f9f9; border-radius: 12px; padding: 12px 0; margin-bottom: 20px; }
.stat-item { display: flex; flex-direction: column; align-items: center; cursor: pointer; transition: transform 0.2s; }
.stat-item:hover { transform: translateY(-2px); }
.stat-num { font-size: 16px; font-weight: 700; color: #333; }
.stat-label { font-size: 11px; color: #999; margin-top: 2px; }

/* 详细信息列表 (每一行必须有固定高度) */
.detail-list { margin-bottom: 30px; padding: 0 10px; }
.fixed-height-row {
  height: 50px; /* 强制行高，防止切换input时高度变化 */
  display: flex; align-items: center;
  border-bottom: 1px dashed #f0f0f0;
  box-sizing: border-box;
}
.row-icon {
  width: 30px; display: flex; align-items: center; justify-content: center;
  color: #d4af37; font-size: 16px;
}
.row-content {
  flex: 1; height: 100%; display: flex; align-items: center;
}
.item-text {
  font-size: 14px; color: #555; width: 100%;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.item-text.empty { color: #ccc; font-style: italic; }

/* 详情输入框 (完全撑满行，去掉边框) */
.detail-input {
  border: none; border-bottom: 1px solid transparent;
  outline: none; font-size: 14px; color: #333;
  width: 100%; background: transparent; padding: 0;
  height: 30px;
  transition: border-color 0.3s;
}
.detail-input:focus { border-bottom-color: #d4af37; }

/* 底部按钮 (固定容器高度) */
.pro-footer { text-align: center; height: 50px; /* 预留按钮高度 */ display: flex; align-items: center; justify-content: center; }
.pro-btn-outline { width: 80%; padding: 10px 0; border-radius: 20px; border: 1px solid #d4af37; background: #fff; color: #d4af37; font-weight: 600; cursor: pointer; transition: all 0.3s; }
.pro-btn-outline:hover { background: #d4af37; color: #fff; }
.action-btns { display: flex; gap: 15px; justify-content: center; width: 100%; }
.pro-btn-ghost { padding: 8px 30px; border-radius: 20px; border: 1px solid #ddd; background: #fff; color: #666; cursor: pointer; transition: all 0.3s; }
.pro-btn-ghost:hover { border-color: #999; color: #333; }
.pro-btn-save { padding: 8px 40px; border-radius: 20px; border: none; background: linear-gradient(90deg, #d4af37, #f6d365); color: #fff; font-weight: 700; cursor: pointer; box-shadow: 0 4px 10px rgba(212, 175, 55, 0.3); }

/* 媒体查询 */
@media (max-width: 768px) {
  .header-content { padding: 0 20px; }
  .hidden-xs-only { display: none !important; }
  .hidden-sm-and-up { display: flex !important; }
}
@media (min-width: 769px) { .hidden-sm-and-up { display: none !important; } }
@media (max-width: 1280px) { .header-content { padding: 0 15px; } .nav-list { gap: 15px; } .logo-text { font-size: 20px; } }
@media (max-width: 1000px) {
  .nav-list { gap: 10px; }
  .nav-text-en { display: none !important; }
  .nav-text-cn {
    position: static !important;
    opacity: 1 !important;
    transform: none !important;
    font-size: 14px;
    color: #555;
  }
  .nav-item.active .nav-text-cn { color: #d4af37 !important; font-weight: 700; }
  .nav-item { height: 60px; justify-content: center; }
}
</style>

/* ================= 全局样式 (必须放在不带 scoped 的 style 标签中) ================= */
<style>
/* 1. 个人中心弹窗样式重置 */
.luna-profile-dialog {
  background: transparent !important;
  box-shadow: none !important;
  border-radius: 16px;
}
.luna-profile-dialog .el-dialog__header { display: none; }
.luna-profile-dialog .el-dialog__body { padding: 0 !important; }

/* 2. 裁剪弹窗样式 (PC默认) */
.cropper-dialog {
  /* PC 端保持默认宽度，样式由 Element 属性控制 */
}
/* [修复] 裁剪区域容器 */
.cropper-content {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.cropper-box {
  width: 100%;
  height: 400px; /* PC端高度 */
  margin-bottom: 20px;
  /* 关键：给盒子加红色边框测试是否生效 (调试用，确认没问题后可删) */
  /* border: 1px solid red; */
}

/* [核心修复] 暴力禁止裁剪框内所有元素的默认触摸/滚动行为 */
.cropper-box,
.cropper-box * {
  touch-action: none !important;
  user-select: none !important;
}

/* ================= 响应式适配 (保持不变) ================= */
@media (max-width: 768px) {
  .luna-profile-dialog {
    width: 95% !important;
    margin-top: 5vh !important;
  }

  .cropper-dialog {
    width: 95% !important;
    margin-top: 5vh !important;
  }

  .cropper-box {
    height: 300px; /* 手机端高度减小 */
  }

  .luna-profile-card-pro {
    min-height: auto;
  }
}
/* Popover 样式 */
.gender-popper {
  padding: 5px 0 !important;
  min-width: 100px !important;
  border-radius: 8px !important;
  border: 1px solid #eee !important;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1) !important;
}
.gender-options { display: flex; flex-direction: column; }
.g-opt {
  padding: 10px 15px; cursor: pointer; display: flex; align-items: center; gap: 10px;
  font-size: 14px; transition: background 0.2s; color: #555;
}
.g-opt:hover { background: #f9f9f9; }
.g-opt.male i { color: #409EFF; }
.g-opt.female i { color: #F56C6C; }
.g-opt.secret i { color: #909399; }

/* 右下角悬浮操作钮 (相机 or 放大镜) */
.pro-avatar-action {
  position: absolute; bottom: 0; right: 0;
  width: 28px; height: 28px;
  background: #d4af37; border-radius: 50%; border: 2px solid #fff;
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-size: 14px; cursor: pointer;
  box-shadow: 0 2px 6px rgba(0,0,0,0.2);
  transition: all 0.3s;
  z-index: 10;

  /* 默认透明度0 (隐藏)，鼠标放上去才显示，或者编辑模式下常驻 */
  opacity: 0;
  transform: scale(0.8);
}

/* 两种情况下显示图标：
   1. 鼠标悬停在头像盒子上 (.pro-avatar-box:hover)
   2. 处于编辑模式时 (.is-editing) -> 如果想编辑模式常亮，加上这个类判断
*/
.pro-avatar-box:hover .pro-avatar-action,
.is-editing .pro-avatar-action {
  opacity: 1;
  transform: scale(1);
}

.pro-avatar-action:hover {
  transform: scale(1.1);
  background: #c5a028;
}

/* ================== 预览弹窗专用样式 ================== */

/* 1. 弹窗本体去背景、去阴影 */
.avatar-preview-dialog {
  background: transparent !important;
  box-shadow: none !important;
  /* 居中显示 */
  display: flex;
  justify-content: center;
  align-items: center;
}

/* 2. 隐藏头部 (那个 X 号和标题) */
.avatar-preview-dialog .el-dialog__header {
  display: none;
}

/* 3. 内容区去内边距 */
.avatar-preview-dialog .el-dialog__body {
  padding: 0 !important;
  background: transparent !important;
  width: 100%;
  display: flex;
  justify-content: center;
}

/* 4. 图片样式 */
.preview-img {
  width: 100%;
  max-width: 350px; /* 限制最大宽度 */
  border-radius: 8px; /* 圆角 */
  box-shadow: 0 10px 30px rgba(0,0,0,0.5); /* 图片加个阴影更立体 */
  cursor: zoom-out; /* 鼠标变成缩小图标 */
  transition: transform 0.3s;
}
.preview-img:hover {
  transform: scale(1.02); /* 悬停微微放大 */
}

</style>
