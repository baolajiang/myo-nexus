<template>
  <div id="login" v-title data-title="月之别邸 - 契约">
    <div class="login-bg"></div>

    <div class="scene" :style="{ height: isFlipped ? '700px' : '520px' }">
      <div class="luna-card" ref="lunaCard">

        <div class="card-face face-front" :style="{ zIndex: isFlipped ? 0 : 2 }">
          <div class="card-content">
            <div class="header-deco">
              <span class="moon-icon">☾</span>
              <span class="title-en">Luna Manor</span>
            </div>
            <h2 class="main-title">欢迎回来</h2>
            <p class="sub-title">穿越回廊，重返你的精神驻地</p>

            <el-form ref="loginForm" :model="loginForm" :rules="loginRules" class="luna-form">
              <el-form-item prop="account">
                <div class="input-wrapper">
                  <input class="luna-input" placeholder="契约之名 (账号)" v-model="loginForm.account" />
                  <span class="focus-border"></span>
                </div>
              </el-form-item>

              <el-form-item prop="password">
                <div class="input-wrapper">
                  <input class="luna-input" placeholder="言灵 (密码)" type="password" v-model="loginForm.password" />
                  <span class="focus-border"></span>
                </div>
              </el-form-item>
            </el-form>

            <div class="actions">
              <div class="luna-btn primary" @click.prevent="login('loginForm')">
                <span>确认登入</span>
              </div>
            </div>

            <div class="footer-switch">
              <span class="switch-text">还没有契约？</span>
              <span class="switch-btn" @click="flipCard(true)">去缔结新约 <i class="el-icon-right"></i></span>
            </div>
          </div>
        </div>

        <div class="card-face face-back" :style="{ zIndex: isFlipped ? 2 : 0 }">
          <div class="card-content">
            <div class="header-deco">
              <span class="sakura-icon">❀</span>
              <span class="title-en">New Contract</span>
            </div>

            <h2 class="main-title">缔结契约</h2>
            <p class="sub-title">以此为证，记录你在这个世界的故事</p>

            <el-form ref="registerForm" :model="registerForm" :rules="registerRules" class="luna-form">


              <el-form-item prop="nickname">
                <div class="input-wrapper">
                  <input class="luna-input" placeholder="行走世间之名 (昵称)" v-model="registerForm.nickname" />
                  <span class="focus-border"></span>
                </div>
              </el-form-item>

              <el-form-item prop="account">
                <div class="input-wrapper">
                  <input class="luna-input" placeholder="拟定名讳 (账号)" v-model="registerForm.account" />
                  <span class="focus-border"></span>
                </div>
              </el-form-item>

              <el-form-item prop="password">
                <div class="input-wrapper">
                  <input class="luna-input" placeholder="铭刻言灵 (密码)" type="password" v-model="registerForm.password" autocomplete="new-password" name="registerPassword"/>
                  <span class="focus-border"></span>
                </div>
              </el-form-item>

              <el-form-item prop="email">
                <div class="input-wrapper">
                  <input class="luna-input" placeholder="信笺投递处 (邮箱)" v-model="registerForm.email" />
                  <span class="focus-border"></span>
                </div>
              </el-form-item>

              <el-form-item prop="code">
                <div class="input-wrapper verify-wrapper">
                  <input class="luna-input" placeholder="信物 (验证码)" v-model="registerForm.code" style="padding-right: 90px;"/>
                  <span class="focus-border"></span>
                  <span class="send-code-btn" :class="{ 'disabled': isSending }" @click="handleSendCode">
                    {{ isSending ? `${countDown}s后重发` : '获取验证码' }}
                  </span>
                </div>
              </el-form-item>

            </el-form>

            <div class="actions">
              <div class="luna-btn primary" @click.prevent="register('registerForm')">
                <span>确认缔结</span>
              </div>
            </div>

            <div class="footer-switch">
              <i class="el-icon-back" style="margin-right: 5px;"></i>
              <span class="switch-btn" @click="flipCard(false)">返回登入</span>
            </div>
          </div>
        </div>

      </div>
    </div>
  </div>
</template>

<script>
import gsap from 'gsap';
// 引入 api
import { sendCode } from '@/api/login'

export default {
  name: 'Login',
  data() {
    return {
      isFlipped: false,
      // 验证码倒计时相关
      isSending: false,
      countDown: 60,
      timer: null,

      loginForm: { account: '', password: '' },
      registerForm: { account: '', nickname: '', email: '', code: '', password: '' },

      loginRules: {
        account: [{required: true, message: '请输入契约之名', trigger: 'blur'}],
        password: [{required: true, message: '请输入言灵', trigger: 'blur'}]
      },
      registerRules: {
        account: [{required: true, message: '请输入契约之名', trigger: 'blur'}],
        nickname: [{required: true, message: '请输入昵称', trigger: 'blur'}],
        email: [
          {required: true, message: '请输入邮箱', trigger: 'blur'},
          {type: 'email', message: '邮箱格式不正确', trigger: 'blur'}
        ],
        code: [{required: true, message: '请输入验证码', trigger: 'blur'}],
        password: [{required: true, message: '请输入言灵', trigger: 'blur'}]
      }
    }
  },
  mounted() {
    // 检查当前路径是否是/register或者查询参数包含type=register
    if (this.$route.path === '/register' || this.$route.query.type === 'register') {
      // 如果是，直接设置状态为"已翻转"
      this.isFlipped = true;
      // 使用 GSAP 瞬间设置角度到 -180度
      gsap.set(this.$refs.lunaCard, { rotationY: -180 });
    }
  },
  methods: {
    // 切换卡片显示状态
    flipCard(toBack) {
      this.isFlipped = toBack;
      gsap.to(this.$refs.lunaCard, {
        rotationY: toBack ? -180 : 0,
        duration: 0.8,
        ease: "power2.inOut",
      });
    },

    // 发送验证码逻辑
    handleSendCode() {
      if (this.isSending) return; // 倒计时中不可点

      // 简单校验邮箱是否为空
      if (!this.registerForm.email) {
        this.$myMessage({ content: '请先填写邮箱地址', type: 'warning' });
        return;
      }
      // 校验邮箱格式 (使用 ElementUI 的校验逻辑或者正则)
      const emailRegex = /^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,6}$/;
      if (!emailRegex.test(this.registerForm.email)) {
        this.$myMessage({ content: '邮箱格式不正确', type: 'warning' });
        return;
      }

      // 开始倒计时 UI
      this.isSending = true;
      this.startCountDown();

      // 调用后端 API
      sendCode(this.registerForm.email).then(res => {
        this.$myMessage({ content: '验证码已发送，请查收', type: 'success',duration: 3000,offset: 60 });
      }).catch(err => {
        this.$myMessage({ content: err || '发送失败', type: 'error' });
        // 发送失败则重置倒计时
        this.resetCountDown();
      });
    },

    startCountDown() {
      this.countDown = 60;
      this.timer = setInterval(() => {
        this.countDown--;
        if (this.countDown <= 0) {
          this.resetCountDown();
        }
      }, 1000);
    },

    resetCountDown() {
      this.isSending = false;
      this.countDown = 60;
      if (this.timer) {
        clearInterval(this.timer);
        this.timer = null;
      }
    },

    login(formName) {
      let that = this
      this.$refs[formName].validate((valid) => {
        if (valid) {
          that.$store.dispatch('login', that.loginForm).then(() => {
            if (window.history.length > 1) { that.$router.go(-1) } else { that.$router.push({path: '/'}) }
          }).catch((error) => {
            if (error !== 'error') { that.$myMessage({content: error, type: 'error', duration: 3000}); }
          })
        }
      });
    },
    register(formName) {
      let that = this
      this.$refs[formName].validate((valid) => {
        if (valid) {
          that.$store.dispatch('register', that.registerForm).then(() => {
            that.$myMessage({ content: '契约缔结成功', type: 'success', duration: 3000 });
            that.flipCard(false);
          }).catch((error) => {
            if (error !== 'error') { that.$myMessage({ content: error, type: 'error', duration: 3000 }); }
          })
        }
      });
    }
  }
}
</script>

<style scoped>

/* 保持之前的全部样式不变 */
#login {
  width: 100%;
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  font-family: 'Noto Serif SC', 'Playfair Display', serif;
  position: relative;
  overflow: hidden;
}
.login-bg {
  position: absolute;
  top: 0; left: 0; width: 100%; height: 100%;
  background-color: #fdfcf8;
  background-image: radial-gradient(circle at 50% 30%, #fffaf5 0%, #f0ece5 100%);
  z-index: 0;
}

/* 修改 4: 添加 transition 和基础宽度，移除固定 height */
.scene {
  width: 380px;
  /* height 由内联样式控制 */
  perspective: 1500px;
  z-index: 10;
  transition: height 0.6s cubic-bezier(0.68, -0.55, 0.27, 1.55); /* 增加弹性动效 */
}

.luna-card {
  width: 100%;
  height: 100%;
  position: relative;
  transform-style: preserve-3d;
  transition: box-shadow 0.3s;
}
.card-face {
  position: absolute;
  width: 100%;
  height: 100%;
  backface-visibility: hidden;
  border-radius: 8px;
  background: #fffaf5;
  border: 1px solid rgba(212, 175, 55, 0.3);
  box-shadow: 0 15px 35px rgba(212, 175, 55, 0.1), 0 5px 15px rgba(0,0,0,0.05);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.face-front { /* z-index 由内联样式控制 */ transform: rotateY(0deg); }
.face-back { transform: rotateY(180deg); background: linear-gradient(to bottom, #fffaf5 0%, #fff5f7 100%); }

.card-content {
  padding: 30px 30px;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  box-sizing: border-box;
}

.header-deco { display: flex; flex-direction: column; align-items: center; margin-bottom: 15px; color: #d4af37; }
.moon-icon, .sakura-icon { font-size: 24px; margin-bottom: 5px; }
.title-en { font-family: 'Playfair Display', serif; font-size: 12px; letter-spacing: 2px; opacity: 0.8; text-transform: uppercase; }
.main-title { font-size: 24px; font-weight: 700; color: #4a4a4a; margin: 0 0 10px 0; letter-spacing: 1px; }
.sub-title { font-size: 12px; color: #999; margin-bottom: 20px; font-weight: 300; }

/* 修改 5: 移除 flex: 1，让内容紧凑，避免登录页按钮沉底 */
.luna-form {
  width: 100%;
  /* flex: 1;  <-- 删除这行 */
  margin-bottom: 20px; /* 增加底部间距 */
}

.input-wrapper { position: relative; width: 100%; margin-bottom: 5px; }
.luna-input {
  width: 100%; padding: 10px 0; font-family: 'Noto Serif SC', serif; font-size: 14px;
  color: #4a4a4a; background: transparent; border: none; border-bottom: 1px solid #e0d0b0;
  outline: none; transition: all 0.3s;
}
.luna-input::placeholder { color: #ccc; font-size: 13px; font-style: italic; }
.focus-border { position: absolute; bottom: 0; left: 0; width: 0; height: 1px; background-color: #d4af37; transition: width 0.4s ease; }
.luna-input:focus ~ .focus-border { width: 100%; }

.verify-wrapper { position: relative; }
.send-code-btn {
  position: absolute;
  right: 0;
  bottom: 8px;
  font-size: 12px;
  color: #d4af37;
  cursor: pointer;
  z-index: 5;
  transition: color 0.3s;
  user-select: none;
}
.send-code-btn:hover { color: #b8860b; font-weight: bold; }
.send-code-btn.disabled { color: #ccc; cursor: not-allowed; }

.actions { width: 100%; margin-top: 5px; }
.luna-btn { width: 100%; padding: 12px 0; text-align: center; cursor: pointer; transition: all 0.3s; border-radius: 4px; font-size: 14px; font-weight: 700; letter-spacing: 2px; }
.luna-btn.primary { background-color: #d4af37; color: #fff; box-shadow: 0 4px 12px rgba(212, 175, 55, 0.3); }
.luna-btn.primary:hover { background-color: #c5a028; transform: translateY(-2px); box-shadow: 0 6px 15px rgba(212, 175, 55, 0.4); }

/* Footer Switch 保持沉底 */
.footer-switch { margin-top: auto; padding-top: 15px; font-size: 12px; color: #888; display: flex; align-items: center; justify-content: center; }
.switch-btn { color: #d4af37; margin-left: 8px; cursor: pointer; font-weight: 600; transition: all 0.3s; border-bottom: 1px dashed transparent; }
.switch-btn:hover { color: #b8860b; border-bottom-color: #b8860b; }
::v-deep .el-form-item { margin-bottom: 20px; }
</style>
