// ============================================================
// [核心修复] 强制解决 vue-cropper 的 Unable to preventDefault 报错
// 注意：这段代码必须放在最前面执行，且不需要 'default-passive-events'
// ============================================================
(function() {
  if (typeof EventTarget !== "undefined") {
    const originalAddEventListener = EventTarget.prototype.addEventListener;
    const heavyEvents = ['touchstart', 'touchmove', 'touchend', 'touchcancel', 'wheel', 'mousewheel'];
    EventTarget.prototype.addEventListener = function(type, listener, options) {
      let modOptions = options;
      // 只有针对这些高频事件，才强制关掉 passive
      if (heavyEvents.includes(type)) {
        if (typeof options === 'boolean') {
          modOptions = { capture: options, passive: false };
        } else if (typeof options === 'object') {
          modOptions = { ...options, passive: false };
        } else {
          modOptions = { passive: false };
        }
      }
      return originalAddEventListener.call(this, type, listener, modOptions);
    };
  }
})();
// ============================================================

import Vue from 'vue'
import App from './App'
import request from './request' // 引封装好的 service
import router from './router'
import store from './store'

// [关键修改] 请注释掉这一行！它会强制开启 passive，导致上面的修复失效
// import 'default-passive-events'

// 引入写的 Message
import myMessage from './utils/Message.js'

import lodash from 'lodash'

import ElementUI from 'element-ui'
import '@/assets/theme/index.css'
import 'element-ui/lib/theme-chalk/index.css'


import {formatTime} from "./utils/time";
import animate from 'animate.css'


import nprogress from 'nprogress' // 进度条
import 'nprogress/nprogress.css' //进度条样式
import VueCropper from 'vue-cropper' // 引入

Vue.use(VueCropper) // 注册
Vue.use(animate)

Vue.config.productionTip = false
// 将自定义request挂载到Vue原型，替代原生axios
Vue.prototype.$axios = request
// 设置全局变量
Vue.prototype.$myName = '月之别邸';
// 挂载到原型上，起个名字，比如 $myMessage 以免和 element 冲突
Vue.prototype.$myMessage = myMessage
Vue.use(ElementUI)
Object.defineProperty(Vue.prototype, '$_', { value: lodash })


Vue.directive('title',  function (el, binding) {
  document.title = el.dataset.title
})
// 格式话时间
Vue.filter('format', formatTime)

//进度条
router.beforeEach((to, from , next) => {
  //每次切换页面时，调用进度条
  nprogress.start();
  // 这个一定要加，没有next()页面不会跳转的
  next();
});
router.afterEach(() => {
  // 在即将进入新的页面组件前，关闭掉进度条
  nprogress.done()
})

new Vue({
  el: '#app',
  router,
  store,
  template: '<App/>',
  components: { App }
})
