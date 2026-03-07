<template>
  <div class="auth-root" v-title :data-title="isRegister ? '月之别邸 · 缔结' : '月之别邸 · 归来'">

    <!-- 背景 -->
    <div class="bg"></div>

    <!-- 卡片容器（翻转轴） -->
    <div class="card-scene">
      <div class="card" :class="{ flipped: isRegister }">

        <!-- ===== 正面：登录 ===== -->
        <div class="card-face card-front">
          <div class="card-inner">
            <div class="card-head">
              <p class="card-sub">LUNA MANOR</p>
              <h2 class="card-title">欢迎回来</h2>
            </div>

            <div class="fl-wrap">
              <input class="fl-input" id="l-account" v-model="loginForm.account"
                     autocomplete="off" placeholder=" " />
              <label class="fl-label" for="l-account">账号</label>
              <div class="fl-bar"></div>
              <div class="fl-err"><span v-if="loginErr.account">{{ loginErr.account }}</span></div>
            </div>

            <div class="fl-wrap">
              <input class="fl-input" id="l-password" type="password" v-model="loginForm.password"
                     placeholder=" " />
              <label class="fl-label" for="l-password">密码</label>
              <div class="fl-bar"></div>
              <div class="fl-err"><span v-if="loginErr.password">{{ loginErr.password }}</span></div>
            </div>

            <!-- 图形验证码 -->
            <div class="captcha-row">
              <div class="fl-wrap captcha-input-wrap">
                <input class="fl-input" id="l-captcha" v-model="loginForm.verCode"
                       placeholder=" " autocomplete="off" />
                <label class="fl-label" for="l-captcha">验证码</label>
                <div class="fl-bar"></div>
                <div class="fl-err"><span v-if="loginErr.verCode">{{ loginErr.verCode }}</span></div>
              </div>
              <div class="captcha-img-wrap" @click="fetchCaptcha" title="点击刷新">
                <img v-if="captchaImg" :src="captchaImg" class="captcha-img" />
                <div v-else class="captcha-loading">加载中…</div>
              </div>
            </div>

            <button class="main-btn" @click="doLogin">
              登 入<span class="btn-arrow">→</span>
            </button>

            <p class="switch-tip">
              还没有账号？
              <span class="sw-link" @click="go(true)">缔结新约</span>
            </p>
          </div>
        </div>

        <!-- ===== 背面：注册 ===== -->
        <div class="card-face card-back">
          <div class="card-inner card-inner-reg">

            <div class="card-head">
              <p class="card-sub">NEW CONTRACT</p>
              <h2 class="card-title">缔结契约</h2>
            </div>

            <input type="file" ref="avatarInput" accept="image/*"
                   style="display:none" @change="handleFileChange" />

            <!-- 头像 -->
            <div class="av-row" @click="$refs.avatarInput.click()">
              <div class="av-circle">
                <el-avatar :size="44" :src="avatarPreviewUrl">
                  <span style="font-size:18px;color:#bbb">✦</span>
                </el-avatar>
                <div class="av-hover">📷</div>
              </div>
              <div class="av-text">
                <span class="av-main">{{ avatarPreviewUrl ? '头像已选择' : '上传头像（可选）' }}</span>
                <span class="av-sub">点击选择图片</span>
              </div>
            </div>

            <!-- 性别 -->
            <div class="sex-row">
              <span class="sex-lbl">性别</span>
              <button v-for="s in sexOptions" :key="s.value"
                      class="sex-chip" :class="{ on: registerForm.sex === s.value }"
                      @click.prevent="registerForm.sex = s.value">
                {{ s.label }}
              </button>
            </div>

            <!-- 两列：昵称 + 账号 -->
            <div class="col2">
              <div class="fl-wrap">
                <input class="fl-input" id="r-nick" v-model="registerForm.nickname" placeholder=" " />
                <label class="fl-label" for="r-nick">昵称</label>
                <div class="fl-bar"></div>
                <div class="fl-err"><span v-if="regErr.nickname">{{ regErr.nickname }}</span></div>
              </div>
              <div class="fl-wrap">
                <input class="fl-input" id="r-acct" v-model="registerForm.account" placeholder=" " />
                <label class="fl-label" for="r-acct">账号</label>
                <div class="fl-bar"></div>
                <div class="fl-err"><span v-if="regErr.account">{{ regErr.account }}</span></div>
              </div>
            </div>

            <div class="fl-wrap">
              <input class="fl-input" id="r-pwd" type="password"
                     v-model="registerForm.password" placeholder=" " autocomplete="new-password" />
              <label class="fl-label" for="r-pwd">密码</label>
              <div class="fl-bar"></div>
              <div class="fl-err"><span v-if="regErr.password">{{ regErr.password }}</span></div>
            </div>

            <!-- 两列：邮箱 + 验证码 -->
            <div class="col2">
              <div class="fl-wrap">
                <input class="fl-input" id="r-email" v-model="registerForm.email" placeholder=" " />
                <label class="fl-label" for="r-email">邮箱</label>
                <div class="fl-bar"></div>
                <div class="fl-err"><span v-if="regErr.email">{{ regErr.email }}</span></div>
              </div>
              <div class="fl-wrap">
                <div class="code-line">
                  <input class="fl-input" id="r-code" v-model="registerForm.code" placeholder=" " />
                  <label class="fl-label" for="r-code">验证码</label>
                  <button class="send-btn" :class="{ off: isSending }" @click.prevent="sendCode">
                    {{ isSending ? countDown + 's' : '发送' }}
                  </button>
                </div>
                <div class="fl-bar"></div>
                <div class="fl-err"><span v-if="regErr.code">{{ regErr.code }}</span></div>
              </div>
            </div>

            <button class="main-btn" :disabled="registering" @click="doRegister">
              {{ registering ? '缔结中…' : '确认缔结' }}<span class="btn-arrow">→</span>
            </button>

            <p class="switch-tip">
              已有账号？
              <span class="sw-link" @click="go(false)">返回登入</span>
            </p>

          </div>
        </div>

      </div>
    </div>

    <!-- 裁剪弹窗 -->
    <el-dialog title="裁剪头像" :visible.sync="cropperVisible" width="800px"
               append-to-body :close-on-click-modal="false" custom-class="bili-cropper-dialog">
      <div class="bili-cropper-layout">
        <div class="cropper-left">
          <div class="cropper-box-wrap">
            <vueCropper ref="cropper" :img="cropperImg" :outputSize="1" outputType="png"
                        :info="true" :full="false" :canMove="true" :canMoveBox="true"
                        :autoCrop="true" :autoCropWidth="200" :autoCropHeight="200"
                        :fixedBox="true" :centerBox="true" :high="true" @realTime="realTime">
            </vueCropper>
          </div>
          <p style="margin-top:12px;font-size:12px;color:#999">支持 JPG、PNG，小于 2MB</p>
        </div>
        <div class="cropper-right">
          <p class="preview-title">预览</p>
          <div class="preview-item">
            <div class="preview-circle-box" style="width:100px;height:100px">
              <div :style="ps(100)"><div :style="previews.div">
                <img :src="previews.url" :style="previews.img">
              </div></div>
            </div><p class="size-text">100px</p>
          </div>
          <div class="preview-item">
            <div class="preview-circle-box" style="width:50px;height:50px">
              <div :style="ps(50)"><div :style="previews.div">
                <img :src="previews.url" :style="previews.img">
              </div></div>
            </div><p class="size-text">50px</p>
          </div>
        </div>
      </div>
      <span slot="footer">
        <el-button @click="cropperVisible = false" plain>取消</el-button>
        <el-button type="primary" @click="finishCrop" :loading="cropLoading">确认裁剪</el-button>
      </span>
    </el-dialog>

  </div>
</template>

<script>
import { sendCode, getCaptcha, verifyCaptcha } from '@/api/login'
import { upload }   from '@/api/upload'

export default {
  name: 'Login',
  data() {
    return {
      isRegister: false,
      isSending: false, countDown: 60, timer: null,
      registering: false,
      uploadFile: null, avatarPreviewUrl: '',
      cropperVisible: false, cropperImg: '', cropLoading: false, previews: {},
      loginErr: { account: '', password: '', verCode: '' },
      regErr:   { nickname: '', account: '', password: '', email: '', code: '' },
      sexOptions: [
        { value: 0, label: '保密' },
        { value: 1, label: '男生' },
        { value: 2, label: '女生' }
      ],
      captchaImg: '', captchaKey: '',
      loginForm:    { account: '', password: '', verCode: '' },
      registerForm: { account: '', nickname: '', email: '', code: '', password: '', avatar: '', sex: 0 }
    }
  },
  mounted() {
    if (this.$route.path === '/register' || this.$route.query.type === 'register') {
      this.isRegister = true
    }
    this.fetchCaptcha()
  },
  methods: {
    go(toReg) {
      this.isRegister = toReg
      this.loginErr = { account: '', password: '', verCode: '' }
      this.regErr   = { nickname: '', account: '', password: '', email: '', code: '' }
      if (!toReg) {
        this.registerForm = { account: '', nickname: '', email: '', code: '', password: '', avatar: '', sex: 0 }
        this.uploadFile = null; this.avatarPreviewUrl = ''
        this.isSending = false
        if (this.timer) { clearInterval(this.timer); this.timer = null }
      }
    },
    handleFileChange(e) {
      const file = e.target.files[0]; if (!file) return
      if (file.size > 2 * 1024 * 1024) { this.$myMessage({ content: '图片不能超过 2MB', type: 'warning' }); return }
      if (file.type === 'image/gif') {
        this.uploadFile = file; this.avatarPreviewUrl = URL.createObjectURL(file); e.target.value = ''; return
      }
      const r = new FileReader()
      r.onload = ev => { this.cropperImg = ev.target.result; this.cropperVisible = true }
      r.readAsDataURL(file); e.target.value = ''
    },
    finishCrop() {
      this.cropLoading = true
      this.$refs.cropper.getCropBlob(data => {
        this.uploadFile = new File([data], 'avatar.png', { type: 'image/png' })
        this.avatarPreviewUrl = URL.createObjectURL(data)
        this.cropperVisible = false; this.cropLoading = false
      })
    },
    realTime(data) { this.previews = data },
    ps(s) {
      if (!this.previews.w) return {}
      const sc = s / this.previews.w
      return { width: this.previews.w + 'px', height: this.previews.h + 'px',
        transform: `scale(${sc})`, transformOrigin: 'top left', position: 'relative' }
    },
    sendCode() {
      if (this.isSending) return
      if (!this.registerForm.email) { this.$myMessage({ content: '请先填写邮箱', type: 'warning' }); return }
      if (!/^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,6}$/.test(this.registerForm.email)) {
        this.$myMessage({ content: '邮箱格式不正确', type: 'warning' }); return
      }
      this.isSending = true; this.countDown = 60
      this.timer = setInterval(() => {
        if (--this.countDown <= 0) { this.isSending = false; clearInterval(this.timer) }
      }, 1000)
      sendCode(this.registerForm.email)
        .then(() => this.$myMessage({ content: '验证码已发送', type: 'success', duration: 3000 }))
        .catch(err => { this.$myMessage({ content: err || '发送失败', type: 'error' }); this.isSending = false; clearInterval(this.timer) })
    },
    async fetchCaptcha() {
      try {
        const res = await getCaptcha()
        if (res.success) {
          this.captchaImg = res.data.image
          this.captchaKey = res.data.key
          this.loginForm.verCode = ''
        }
      } catch (e) { console.error('验证码获取失败', e) }
    },
    doLogin() {
      this.loginErr = { account: '', password: '', verCode: '' }
      let e = false
      if (!this.loginForm.account)  { this.loginErr.account  = '请输入账号'; e = true }
      if (!this.loginForm.password) { this.loginErr.password = '请输入密码'; e = true }
      if (!this.loginForm.verCode)  { this.loginErr.verCode  = '请输入验证码'; e = true }
      if (e) return
      // 先校验图形验证码
      verifyCaptcha({ verKey: this.captchaKey, verCode: this.loginForm.verCode })
        .then(res => {
          const msg = res.data
          if (msg !== '验证码正确') {
            this.loginErr.verCode = msg || '验证码错误'
            this.fetchCaptcha()
            return
          }
          // 验证码通过，再走登录
          this.$store.dispatch('login', { account: this.loginForm.account, password: this.loginForm.password })
            .then(() => { window.history.length > 1 ? this.$router.go(-1) : this.$router.push('/') })
            .catch(err => {
              if (err !== 'error') this.$myMessage({ content: err, type: 'error', duration: 3000 })
              this.fetchCaptcha()
            })
        })
        .catch(() => { this.$myMessage({ content: '验证码校验失败', type: 'error' }); this.fetchCaptcha() })
    },
    async doRegister() {
      this.regErr = { nickname: '', account: '', password: '', email: '', code: '' }
      let e = false
      if (!this.registerForm.nickname) { this.regErr.nickname = '请输入昵称'; e = true }
      if (!this.registerForm.account)  { this.regErr.account  = '请输入账号'; e = true }
      if (!this.registerForm.password) { this.regErr.password = '请输入密码'; e = true }
      if (!this.registerForm.email)    { this.regErr.email    = '请输入邮箱'; e = true }
      else if (!/^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,6}$/.test(this.registerForm.email))
      { this.regErr.email = '格式不正确'; e = true }
      if (!this.registerForm.code) { this.regErr.code = '请输入验证码'; e = true }
      if (e) return
      this.registering = true
      try {
        if (this.uploadFile) {
          const fd = new FormData(); fd.append('image', this.uploadFile); fd.append('path', 'avatar')
          const res = await upload(fd)
          if (res.success) { this.registerForm.avatar = res.data }
          else { this.$myMessage({ content: res.msg || '头像上传失败', type: 'error' }); return }
        }
        await this.$store.dispatch('register', this.registerForm)
        this.$myMessage({ content: '注册成功，欢迎来到月之别邸', type: 'success', duration: 3000 })
        this.registerForm = { account: '', nickname: '', email: '', code: '', password: '', avatar: '', sex: 0 }
        this.uploadFile = null; this.avatarPreviewUrl = ''
        this.isSending = false; clearInterval(this.timer)
        window.history.length > 1 ? this.$router.go(-1) : this.$router.push('/')
      } catch (err) {
        if (err !== 'error') this.$myMessage({ content: err, type: 'error', duration: 3000 })
      } finally { this.registering = false }
    }
  }
}
</script>

<style scoped>
/* ── 页面 ── */
.auth-root {
  position: fixed;
  top: 65px; left: 0; right: 0; bottom: 0;
  display: flex; align-items: center; justify-content: center;
  overflow: hidden;
  font-family: 'Noto Serif SC', Georgia, serif;
  z-index: 10;
}

/* 简约浅灰背景，带微纹 */
.bg {
  position: absolute; inset: 0;
  background: #f0ede8;
  background-image: radial-gradient(circle at 25% 35%, rgba(212,175,55,0.06) 0%, transparent 55%),
  radial-gradient(circle at 75% 70%, rgba(212,175,55,0.05) 0%, transparent 50%);
}

/* ── 卡片翻转场景 ──
   card-scene 提供 perspective
   card 是翻转体，正背两面绝对叠放
   尺寸固定：宽 420px，登录面高 380px，注册面高 580px
   ★ 关键：两面都是同一个 .card，高度取较高的（注册面 580px），
     登录面内容居中即可，这样翻转前后卡片大小完全一致
*/
.card-scene {
  perspective: 1200px;
}

.card {
  width: 420px;
  height: 580px;
  position: relative;
  transform-style: preserve-3d;
  /* 翻转动画：0.7s，缓进缓出 */
  transition: transform 0.7s cubic-bezier(0.4, 0, 0.2, 1);
  will-change: transform;
}

/* 翻到背面 */
.card.flipped {
  transform: rotateY(180deg);
}

/* 正面 & 背面公共 */
.card-face {
  position: absolute;
  inset: 0;
  backface-visibility: hidden;
  -webkit-backface-visibility: hidden;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 8px 40px rgba(0,0,0,0.10), 0 2px 8px rgba(0,0,0,0.06);
  overflow: hidden;
}

/* 背面要预先旋转 180° */
.card-back {
  transform: rotateY(180deg);
}

/* 卡片内容容器 */
.card-inner {
  height: 100%;
  padding: 36px 36px 28px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 登录内容垂直居中 */
.card-front .card-inner {
  justify-content: center;
  gap: 0;
}

/* ── 卡片头部 ── */
.card-head {
  margin-bottom: 28px;
}
.card-sub   { font-size: 10px; letter-spacing: 4px; color: #bbb; margin: 0 0 6px; }
.card-title { font-size: 26px; font-weight: 700; color: #1a1a1a; margin: 0; letter-spacing: 1px; }

/* ── 浮动 label 字段 ── */
.fl-wrap {
  position: relative;
  margin-bottom: 6px;
}

.fl-input {
  width: 100%;
  padding: 20px 0 6px;
  background: transparent;
  border: none; outline: none;
  font-size: 14px; color: #1a1a1a;
  font-family: inherit;
  caret-color: #333;
  box-sizing: border-box;
}

/* label 默认在输入框中间（像 placeholder） */
.fl-label {
  position: absolute;
  left: 0;
  top: 20px;
  font-size: 14px;
  color: #aaa;
  pointer-events: none;
  transform-origin: left top;
  transition: transform 0.22s ease, color 0.22s ease, font-size 0.22s ease;
}

/* 有内容或 focus 时，label 上浮 */
.fl-input:focus   + .fl-label,
.fl-input:not(:placeholder-shown) + .fl-label {
  transform: translateY(-16px) scale(0.78);
  color: #888;
  font-size: 14px; /* scale 已缩小，保持声明统一 */
}

/* focus 时 label 高亮 */
.fl-wrap:focus-within .fl-label {
  color: #333;
}

/* 底部线条 */
.fl-bar {
  height: 1px;
  background: #e0e0e0;
  transition: background 0.25s, height 0.2s;
}
.fl-wrap:focus-within .fl-bar {
  background: #1a1a1a;
  height: 1.5px;
}

/* 固定高度错误占位 */
.fl-err {
  height: 16px;
  display: flex; align-items: center;
}
.fl-err span { font-size: 10px; color: #e74c3c; }

/* 两列布局 */
.col2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

/* ── 提交按钮 ── */
.main-btn {
  width: 100%;
  padding: 13px 0;
  margin-top: 10px;
  background: #1a1a1a;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 14px; font-weight: 600; letter-spacing: 3px;
  cursor: pointer;
  font-family: inherit;
  display: flex; align-items: center; justify-content: center; gap: 10px;
  position: relative; overflow: hidden;
  transition: background 0.25s, transform 0.15s;
  flex-shrink: 0;
}
.main-btn:hover   { background: #333; }
.main-btn:active  { transform: scale(0.98); }
.main-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.btn-arrow { font-style: normal; transition: transform 0.25s; }
.main-btn:hover .btn-arrow { transform: translateX(4px); }

/* ── 底部切换提示 ── */
.switch-tip {
  font-size: 12px; color: #aaa;
  text-align: center; margin: 12px 0 0;
  flex-shrink: 0;
}
.sw-link {
  color: #555; cursor: pointer; font-weight: 600;
  border-bottom: 1px solid #ccc;
  transition: color 0.2s, border-color 0.2s;
}
.sw-link:hover { color: #111; border-color: #666; }

/* ── 头像行 ── */
.av-row {
  display: flex; align-items: center; gap: 12px;
  padding: 8px 10px;
  border: 1px dashed #ddd;
  border-radius: 8px;
  cursor: pointer;
  margin-bottom: 10px;
  transition: border-color 0.2s, background 0.2s;
  flex-shrink: 0;
}
.av-row:hover { border-color: #aaa; background: #fafafa; }
.av-circle {
  width: 44px; height: 44px; border-radius: 50%; overflow: hidden;
  border: 1.5px solid #e0e0e0; position: relative; flex-shrink: 0;
}
.av-hover {
  position: absolute; inset: 0; background: rgba(0,0,0,0.35);
  display: flex; align-items: center; justify-content: center;
  font-size: 16px; opacity: 0; transition: opacity 0.2s;
}
.av-row:hover .av-hover { opacity: 1; }
.av-main { font-size: 13px; color: #333; font-weight: 600; display: block; margin-bottom: 2px; }
.av-sub  { font-size: 11px; color: #aaa; }

/* ── 性别 ── */
.sex-row {
  display: flex; align-items: center; gap: 8px;
  margin-bottom: 10px; flex-shrink: 0;
}
.sex-lbl { font-size: 11px; color: #aaa; margin-right: 4px; }
.sex-chip {
  padding: 4px 12px; font-size: 12px;
  border: 1px solid #e0e0e0; background: transparent;
  color: #666; border-radius: 20px; cursor: pointer;
  font-family: inherit; transition: all 0.2s;
}
.sex-chip:hover { border-color: #999; color: #333; }
.sex-chip.on    { background: #1a1a1a; border-color: #1a1a1a; color: #fff; font-weight: 600; }

/* ── 验证码行 ── */
.code-line {
  position: relative;
  display: flex; align-items: flex-end; gap: 6px;
}
.code-line .fl-input { flex: 1; }
.code-line .fl-label { /* 继承 fl-label 样式，浮动正常工作 */ }
.send-btn {
  flex-shrink: 0;
  padding: 4px 10px; margin-bottom: 7px;
  font-size: 11px; font-weight: 600;
  border: 1px solid #e0e0e0; background: transparent;
  color: #666; cursor: pointer; border-radius: 4px;
  font-family: inherit; white-space: nowrap;
  transition: all 0.2s;
}
.send-btn:hover:not(.off) { border-color: #333; color: #333; }
.send-btn.off { opacity: 0.35; cursor: not-allowed; }

/* ── 图形验证码行 ── */
.captcha-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}
.captcha-input-wrap {
  flex: 1;
}
.captcha-img-wrap {
  flex-shrink: 0;
  width: 110px;
  height: 40px;
  margin-top: 10px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  overflow: hidden;
  cursor: pointer;
  transition: border-color 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}
.captcha-img-wrap:hover { border-color: #999; }
.captcha-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.captcha-loading {
  font-size: 11px;
  color: #bbb;
}

/* 注册面内容紧凑些 */
.card-inner-reg {
  padding-top: 28px;
  padding-bottom: 20px;
  gap: 0;
  justify-content: flex-start;
}
.card-inner-reg .card-head { margin-bottom: 16px; }
.card-inner-reg .main-btn  { margin-top: 6px; }
.card-inner-reg .switch-tip { margin-top: 8px; }

/* el-form 覆盖 */
::v-deep .el-form-item        { margin-bottom: 0; }
::v-deep .el-form-item__error { display: none !important; }

/* 裁剪弹窗 */
.bili-cropper-layout { display: flex; height: 360px; gap: 30px; }
.cropper-left  { flex: 1; display: flex; flex-direction: column; }
.cropper-box-wrap { flex: 1; background: #f0f0f0; border: 1px solid #e7e7e7; border-radius: 4px; overflow: hidden; }
.cropper-right { width: 160px; background: #f9f9f9; border-radius: 4px; padding: 20px;
  display: flex; flex-direction: column; align-items: center; border: 1px solid #eee; }
.preview-title { font-size: 14px; font-weight: 700; color: #333; margin: 0 0 16px; }
.preview-item  { display: flex; flex-direction: column; align-items: center; margin-bottom: 16px; }
.preview-circle-box { border-radius: 50%; overflow: hidden; border: 2px solid #fff; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
.size-text { margin-top: 6px; font-size: 12px; color: #666; }
</style>

<style>
.bili-cropper-dialog .el-dialog__body { padding: 20px 30px !important; }
</style>
