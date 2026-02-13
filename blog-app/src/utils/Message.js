import Vue from 'vue'
import MessageComponent from '@/components/Message/Message'

const MessageConstructor = Vue.extend(MessageComponent)

// 1. 定义一个变量，用来存储当前正在显示的实例
let activeInstance = null;

const Message = (options) => {
  if (typeof options === 'string') {
    options = { content: options }
  }

  // 2. 核心逻辑：如果有上一个实例，直接强制移除，不要等待它淡出
  if (activeInstance) {
    // 手动移除 DOM，实现“秒切”效果，防止重叠
    if (activeInstance.$el && activeInstance.$el.parentNode) {
      activeInstance.$el.parentNode.removeChild(activeInstance.$el);
    }
    // 销毁 Vue 实例，防止内存泄漏
    activeInstance.$destroy();
    activeInstance = null;
  }

  // 3. 实例化新的组件
  const instance = new MessageConstructor({
    data: options
  })

  instance.$mount()
  document.body.appendChild(instance.$el)
  instance.visible = true

  // 4. 更新当前实例引用
  activeInstance = instance;

  // 5. 监听组件自身的关闭事件（比如时间到了自动关闭），清理引用
  // 这一步是为了防止：弹窗自然消失后，activeInstance 此时还存着旧引用
  // 稍微 hack 一下 close 方法，或者通过 $watch
  // 但最简单的方式是重写实例的 close 方法
  const originalClose = instance.close;
  instance.close = function() {
    originalClose.call(instance); // 执行原有的关闭逻辑
    if (activeInstance === instance) {
      activeInstance = null; // 清空引用
    }
  }

  return instance
}

// 挂载快捷方法
['success', 'warning', 'info', 'error'].forEach(type => {
  Message[type] = (content) => {
    return Message({
      type,
      content
    })
  }
})

export default Message
