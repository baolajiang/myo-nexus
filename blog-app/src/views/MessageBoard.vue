<template>
  <div class="message-board-container" v-title :data-title="title">

    <div class="luna-wrap">
      <div class="luna-card message-card" :class="themeClass">

        <div class="moon-crest">
          <svg viewBox="0 0 100 100" class="moon-svg">
            <path d="M50 0 C20 0 0 20 0 50 C0 80 20 100 50 100 C40 80 40 20 50 0 Z" fill="currentColor" />
          </svg>
        </div>
        <div class="sakura-container">
          <span class="petal p1">❀</span>
          <span class="petal p2">❀</span>
        </div>

        <div class="board-header">
          <div class="fancy-line-top"></div>
          <h2 class="board-title">留言板</h2>
          <p class="board-desc">在这里留下你的足迹，无论是吐槽还是鼓励 (oﾟvﾟ)ノ</p>
        </div>

        <div class="input-section">
          <div class="avatar-col">
            <el-avatar :size="48" :src="userAvatar" class="user-avatar"></el-avatar>
          </div>
          <div class="input-col">
            <div class="input-wrapper">
              <el-input
                type="textarea"
                :rows="3"
                placeholder="发一条友善的留言..."
                v-model="newMessage"
                maxlength="200"
                show-word-limit
                resize="none"
                class="luna-textarea"
              ></el-input>
              <div class="input-footer">
                <div class="emoji-btn"><i class="el-icon-cherry"></i> 表情</div>
                <button class="luna-btn" @click="submitMessage">
                  <span>发布留言</span>
                  <i class="el-icon-position"></i>
                </button>
              </div>
            </div>
          </div>
        </div>

        <div class="comment-list">
          <div class="list-divider">
            <span class="divider-text">共 {{ totalMessages }} 条留言</span>
          </div>

          <div class="comment-item" v-for="(item, index) in messages" :key="item.id">
            <div class="root-avatar-box">
              <el-avatar :size="42" :src="item.avatar" class="root-avatar"></el-avatar>
            </div>

            <div class="content-box">
              <div class="user-meta">
                <span class="nickname" :class="{ 'is-admin': item.isAdmin }">{{ item.nickname }}</span>
                <span v-if="item.isAdmin" class="badge admin-badge">站长</span>
                <span v-if="item.level" class="badge level-badge">Lv.{{ item.level }}</span>
                <span class="floor-tag">#{{ messages.length - index }}</span>
              </div>

              <div class="text-content">{{ item.content }}</div>

              <div class="action-bar">
                <span class="time-tag">{{ item.time }}</span>
                <span class="action-btn" :class="{ liked: item.isLiked }" @click="handleLike(item)">
                  <i :class="item.isLiked ? 'el-icon-star-on' : 'el-icon-star-off'"></i>
                  {{ item.like > 0 ? item.like : '点赞' }}
                </span>
                <span class="action-btn" @click="handleReply(item)">
                  <i class="el-icon-chat-round"></i> 回复
                </span>
              </div>

              <div class="sub-reply-wrapper" v-if="item.children && item.children.length > 0">

                <div
                  class="sub-item"
                  v-for="sub in item.children.slice(0, 2)"
                  :key="sub.id"
                >
                  <div class="sub-line">
                    <span class="sub-name" :class="{ 'is-admin': sub.isAdmin }">{{ sub.nickname }}</span>
                    <span v-if="sub.isAdmin" class="badge admin-badge-small">UP</span>
                    <span v-if="sub.targetUser" class="reply-target"> 回复 @{{sub.targetUser}}</span>
                    <span class="colon">：</span>
                    <span class="sub-text">{{ sub.content }}</span>
                  </div>
                  <div class="sub-footer">
                    <span class="sub-time">{{ sub.time }}</span>
                    <span class="sub-act" @click="handleReply(item, sub)">回复</span>
                  </div>
                </div>

                <div
                  class="collapsible-box"
                  :class="{ 'is-expanded': item.expanded }"
                >
                  <div class="sub-item" v-for="sub in item.children.slice(2)" :key="sub.id">
                    <div class="sub-line">
                      <span class="sub-name" :class="{ 'is-admin': sub.isAdmin }">{{ sub.nickname }}</span>
                      <span v-if="sub.targetUser" class="reply-target"> 回复 @{{sub.targetUser}}</span>
                      <span class="colon">：</span>
                      <span class="sub-text">{{ sub.content }}</span>
                    </div>
                    <div class="sub-footer">
                      <span class="sub-time">{{ sub.time }}</span>
                      <span class="sub-act" @click="handleReply(item, sub)">回复</span>
                    </div>
                  </div>
                </div>

                <div class="expand-control" v-if="item.children.length > 2">
                  <span v-if="!item.expanded" class="expand-link" @click="toggleExpand(item, true)">
                    查看剩余 {{ item.children.length - 2 }} 条回复 <i class="el-icon-caret-bottom"></i>
                  </span>
                  <span v-else class="expand-link collapse-link" @click="toggleExpand(item, false)">
                    收起回复 <i class="el-icon-caret-top"></i>
                  </span>
                </div>

              </div>
            </div>
          </div>

          <div class="load-more-box">
            <span class="load-text" @click="loadMore" v-if="!loading">点击加载更多</span>
            <span class="load-text loading" v-else><i class="el-icon-loading"></i> 加载中...</span>
          </div>
        </div>

      </div>
    </div>
  </div>
</template>

<script>
import defaultAvatar from '@/assets/img/default_avatar.png'

export default {
  name: 'MessageBoard',
  data() {
    return {
      newMessage: '',
      loading: false,
      messages: [
        {
          id: 1,
          nickname: '路过的一只',
          level: 5,
          avatar: 'https://wpimg.wallstcn.com/f778738c-e4f8-4870-b634-56703b4acafe.gif',
          content: '这个主题风格真的很赞！非常符合博主 BaseHeader 和 ArticleItem 的设计语言，尤其是那个月亮和樱花的装饰，太有感觉了！',
          time: '2026-01-30 14:00',
          like: 23,
          isAdmin: false,
          isLiked: true,
          expanded: false, // 控制折叠
          children: [
            { id: 101, nickname: '博主', isAdmin: true, targetUser: '路过的一只', content: '谢谢夸奖！特意调整了样式来匹配主题。', time: '14:05' },
            { id: 102, nickname: '路过的一只', targetUser: '博主', content: '展开收起的动画也很丝滑，爱了爱了。', time: '14:10' },
            { id: 103, nickname: '围观群众', content: '我是第三条回复，默认是折叠的。', time: '14:15' },
            { id: 104, nickname: '测试员', content: '我是第四条，测试一下长列表效果。', time: '14:20' }
          ]
        },
        {
          id: 2,
          nickname: 'Bika老粉',
          avatar: defaultAvatar,
          content: '这就对味了，这种灰底的子评论区结构清晰，很像哔咔的风格，但是配色又很清新。',
          time: '2026-01-29 18:30',
          like: 5,
          isAdmin: false,
          children: []
        }
      ]
    }
  },
  computed: {
    title() { return '留言板 - My Blog' },
    // 根据 vuex 中的 theme 状态切换 class (如果有的话)
    themeClass() { return 'is-light' }, // 预留接口
    userAvatar() { return this.$store.state.avatar || defaultAvatar },
    totalMessages() {
      let count = this.messages.length;
      this.messages.forEach(m => { if(m.children) count += m.children.length });
      return count;
    }
  },
  methods: {
    submitMessage() {
      if (!this.newMessage.trim()) return this.$message.warning('写点什么吧~');
      this.messages.unshift({
        id: Date.now(),
        nickname: this.$store.state.nickname || '我',
        avatar: this.userAvatar,
        content: this.newMessage,
        time: '刚刚',
        like: 0,
        children: [],
        expanded: false
      });
      this.newMessage = '';
      this.$message.success('留言发布成功');
    },
    handleLike(item) {
      if (item.isLiked) { item.like--; item.isLiked = false; }
      else { item.like++; item.isLiked = true; }
    },
    handleReply(item, sub) {
      const target = sub ? sub.nickname : item.nickname;
      this.newMessage = `回复 @${target} : `;
      const textarea = document.querySelector('.luna-textarea textarea');
      if(textarea) textarea.focus();
    },
    toggleExpand(item, status) {
      // 必须保证 vue 响应式更新
      this.$set(item, 'expanded', status);
    },
    loadMore() {
      this.loading = true;
      setTimeout(() => { this.loading = false; this.$message.info('没有更多了~'); }, 800);
    }
  }
}
</script>

<style scoped>
/* 容器布局 */
.message-board-container {
  min-height: 100vh;
  padding-top: 80px; /* 避开 Fixed Header */
  padding-bottom: 40px;
  background-color: var(--bg-color, #f9f9f9); /* 使用主题背景色 */
}

.luna-wrap {
  width: 100%;
  display: flex;
  justify-content: center;
}

/* 核心卡片：复刻 ArticleItem 的样式 */
.message-card {
  position: relative;
  width: 100%;
  max-width: 850px;
  background: #fff;
  border-radius: 8px;
  /* 虚线边框，符合 Macaron 主题 */
  border: 1px dashed var(--border-color, #eee);
  padding: 40px;
  box-shadow: 0 5px 15px rgba(0,0,0,0.05);
  overflow: hidden;
}

/* 装饰元素 (月亮/樱花) */
.moon-crest {
  position: absolute; top: -20px; right: -20px; width: 100px; height: 100px;
  opacity: 0.1; color: var(--accent-color, #ccc); pointer-events: none;
  transform: rotate(15deg);
}
.sakura-container {
  position: absolute; bottom: 10px; left: 10px; pointer-events: none;
  color: var(--border-color, #ffb7c5); opacity: 0.3; font-size: 20px;
}
.petal { position: absolute; }
.p1 { bottom: 0; left: 0; transform: rotate(-15deg); }
.p2 { bottom: 15px; left: 20px; transform: rotate(10deg) scale(0.8); }

/* 头部样式 */
.board-header {
  text-align: center;
  margin-bottom: 40px;
}
.board-title {
  font-family: 'M PLUS Rounded 1c', sans-serif;
  font-size: 28px;
  color: #444;
  margin: 10px 0;
  letter-spacing: 2px;
}
.board-desc {
  font-family: 'Patrick Hand', cursive, sans-serif;
  color: #999;
  font-size: 14px;
}
.fancy-line-top {
  height: 3px;
  width: 60px;
  background: var(--accent-color, #ccc);
  margin: 0 auto;
  border-radius: 2px;
}

/* 输入区域 */
.input-section {
  display: flex;
  margin-bottom: 40px;
  padding: 20px;
  background: #fdfdfd;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
}
.avatar-col { margin-right: 20px; }
.user-avatar { border: 2px solid #fff; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
.input-col { flex: 1; }

/* 输入框样式定制 */
.luna-textarea >>> .el-textarea__inner {
  border: 1px solid var(--border-color, #eee);
  background-color: #fff;
  border-radius: 6px;
  font-family: inherit;
  transition: all 0.3s;
}
.luna-textarea >>> .el-textarea__inner:focus {
  border-color: var(--accent-color, #ccc);
  box-shadow: 0 0 5px rgba(0,0,0,0.05);
}

.input-footer {
  display: flex; justify-content: space-between; align-items: center; margin-top: 12px;
}
.emoji-btn { color: #999; cursor: pointer; font-size: 13px; transition: color 0.3s; }
.emoji-btn:hover { color: var(--accent-color, #ff7f50); }

.luna-btn {
  background: var(--accent-color, #ccc);
  color: #fff;
  border: none;
  padding: 6px 18px;
  border-radius: 20px;
  cursor: pointer;
  font-size: 13px;
  display: flex; align-items: center; gap: 5px;
  transition: all 0.3s;
}
.luna-btn:hover {
  filter: brightness(0.95);
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

/* 列表分割 */
.list-divider {
  border-bottom: 1px dashed var(--border-color, #eee);
  margin-bottom: 30px;
  padding-bottom: 10px;
}
.divider-text {
  background: #fff;
  padding-right: 15px;
  color: #999;
  font-size: 14px;
  font-weight: bold;
}

/* 单条留言样式 (Bika Layout) */
.comment-item {
  display: flex;
  margin-bottom: 30px;
}
.root-avatar-box { margin-right: 18px; flex-shrink: 0; }
.content-box { flex: 1; border-bottom: 1px solid #f9f9f9; padding-bottom: 20px; }
.comment-item:last-child .content-box { border-bottom: none; }

/* 用户信息 */
.user-meta { display: flex; align-items: center; margin-bottom: 8px; }
.nickname {
  font-weight: 700; color: #444; margin-right: 8px; cursor: pointer; font-size: 15px;
  transition: color 0.2s;
}
.nickname:hover { color: var(--accent-color, #ff7f50); }
.nickname.is-admin { color: var(--accent-color, #ff7f50); }

.badge {
  font-size: 10px; padding: 1px 5px; border-radius: 3px; margin-right: 6px;
  height: 16px; line-height: 15px;
}
.admin-badge { background: var(--accent-color, #ff7f50); color: #fff; }
.level-badge { background: #f0f0f0; color: #999; }
.floor-tag { margin-left: auto; font-size: 12px; color: #ccc; font-family: 'Patrick Hand', cursive; }

.text-content {
  font-size: 14px; color: #555; line-height: 1.6; margin-bottom: 10px; white-space: pre-wrap;
}

.action-bar { display: flex; align-items: center; font-size: 12px; color: #999; margin-bottom: 12px; }
.time-tag { margin-right: 15px; font-family: 'Patrick Hand', cursive; }
.action-btn { cursor: pointer; margin-right: 15px; transition: color 0.2s; }
.action-btn:hover { color: var(--accent-color, #ff7f50); }
.action-btn.liked { color: #ff6b6b; }

/* === 子回复区域 (Bika 风格) === */
.sub-reply-wrapper {
  background-color: #f9f9f9; /* 浅灰底色 */
  border-radius: 4px;
  padding: 15px;
  margin-top: 10px;
  font-size: 13px;
}

.sub-item { margin-bottom: 12px; }
.sub-item:last-child { margin-bottom: 0; }

.sub-line { line-height: 1.5; margin-bottom: 3px; }
.sub-name { font-weight: 600; color: #666; cursor: pointer; margin-right: 4px; }
.sub-name.is-admin { color: var(--accent-color, #ff7f50); }
.sub-name:hover { color: var(--accent-color, #ff7f50); }
.admin-badge-small { background: var(--accent-color, #ff7f50); color: #fff; font-size: 10px; padding: 0 2px; border-radius: 2px; margin-right: 4px; }
.reply-target { color: var(--accent-color, #999); }
.sub-text { color: #333; }

.sub-footer { font-size: 12px; color: #aaa; display: flex; }
.sub-time { margin-right: 15px; font-family: 'Patrick Hand', cursive; }
.sub-act { cursor: pointer; }
.sub-act:hover { color: var(--accent-color, #ff7f50); }

/* 折叠动画容器 */
.collapsible-box {
  max-height: 0;
  overflow: hidden;
  transition: max-height 0.5s ease-in-out; /* 丝滑过渡 */
}
.collapsible-box.is-expanded {
  max-height: 2000px; /* 足够大的高度 */
}

/* 展开按钮 */
.expand-control {
  margin-top: 10px;
  font-size: 12px;
  color: var(--accent-color, #888);
}
.expand-link { cursor: pointer; transition: opacity 0.2s; }
.expand-link:hover { opacity: 0.8; }

.load-more-box { text-align: center; margin-top: 30px; }
.load-text { font-size: 13px; color: #999; cursor: pointer; padding: 5px 15px; border-radius: 15px; transition: background 0.2s; }
.load-text:hover { background: #f5f5f5; }

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .message-card { padding: 20px; }
  .input-section { flex-direction: column; }
  .avatar-col { display: none; } /* 移动端隐藏输入框头像，省空间 */
}
</style>
