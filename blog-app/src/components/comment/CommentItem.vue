<template>
  <div class="me-view-comment-item">
    <div class="me-view-comment-author">

      <el-popover
        placement="top-start"
        width="300"
        trigger="hover"
        :open-delay="200"
      >
        <div class="user-card-container">
          <div class="card-banner">
            <img src="/static/img/anime-sunset-art-wallpaper-2560x1080_14.jpg" class="banner-img">
          </div>

          <div class="card-info">
            <div class="card-avatar-wrapper">
              <el-avatar :size="60" :src="comment.author.avatar || defaultAvatar" class="card-avatar"></el-avatar>
            </div>

            <div class="card-user-row">
              <span class="card-name" @click="goToSpace(comment.author.id)">{{ comment.author.nickname }}</span>
              <span class="card-uid">UID: {{ comment.author.id }}</span>
            </div>

            <div class="card-bio">
              {{ comment.author.summary || '这个人很神秘，什么都没写~' }}
            </div>

            <div class="card-action">
              <div class="card-stats">
                <div class="stat-item"><span class="num">12</span><span class="txt">关注</span></div>
                <div class="stat-item"><span class="num">345</span><span class="txt">粉丝</span></div>
              </div>
              <div class="card-btns">
                <el-button v-if="comment.author.id != $store.state.id" type="primary" size="mini" icon="el-icon-plus" round>关注</el-button>
                <el-button v-if="comment.author.id != $store.state.id" plain size="mini" round>私信</el-button>
                <el-button v-else plain size="mini" round @click="goToSpace(comment.author.id)">我的空间</el-button>
              </div>
            </div>
          </div>
        </div>

        <a slot="reference" class="me-view-comment-author-avatar" href="javascript:void(0)" @click="goToSpace(comment.author.id)">
          <img :src="comment.author.avatar || defaultAvatar">
        </a>
      </el-popover>
      <div class="me-view-comment-author-info">
        <a class="me-view-comment-author-name" href="javascript:void(0)" @click="goToSpace(comment.author.id)">
          {{comment.author.nickname}}
        </a>
        <div class="me-view-comment-time">
          <span>{{rootCommentCounts - index}}楼</span>
          <span>{{comment.createDate | format}}</span>
        </div>
      </div>
    </div>

    <div class="me-view-comment-content">
      <p class="me-view-comment-text">{{comment.content}}</p>
      <div class="me-view-comment-tools">
        <a class="me-view-comment-tool" @click="showComment(-1, comment.author)">
          <i class="me-icon-comment"></i>&nbsp; 评论
        </a>
      </div>
    </div>

    <div class="me-reply-list">
      <div class="me-reply-item" v-for="c in comment.childrens" :key="c.id">
        <div style="font-size: 14px">
          <span class="me-reply-user">
            <span @click="goToSpace(c.author.id)" style="cursor:pointer">{{c.author.nickname}}</span>
            :
          </span>

          <span v-if="c.toUser">
            @<span class="me-reply-user" @click="goToSpace(c.toUser.id)" style="cursor:pointer">{{c.toUser.nickname}}</span>
          </span>

          <span>{{c.content}}</span>
        </div>
        <div class="me-view-comment-tools">
          <span class="me-view-comment-tool">{{c.createDate | format}}</span>
          <a class="me-view-comment-tool" @click="showComment(c.id, c.author)">
            <i class="me-icon-comment"></i>&nbsp;回复
          </a>
        </div>
      </div>
    </div>

  </div>
</template>

<script>
import defaultAvatar from '@/assets/img/default_avatar.png'

export default {
  name: "CommentItem",
  props: {
    articleId: String,
    comment: Object,
    index: Number,
    rootCommentCounts: Number
  },
  data() {
    return {
      defaultAvatar
    }
  },
  methods: {
    showComment(parentId, toUser) {
      this.$emit('comment-reply', parentId, toUser)
    },
    // 跳转到个人空间
    goToSpace(userId) {
      if(userId) {
        this.$router.push({path: `/space/${userId}`})
      }
    }
  }
}
</script>

<style scoped>
.me-view-comment-item {
  margin-top: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.me-view-comment-author {
  margin: 10px 0;
  vertical-align: middle;
}

.me-view-comment-author-avatar {
  width: 40px;
  height: 40px;
  display: inline-block;
  vertical-align: middle;
  cursor: pointer;
}

.me-view-comment-author-avatar img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
}

.me-view-comment-author-info {
  display: inline-block;
  vertical-align: middle;
  margin-left: 10px;
}

.me-view-comment-author-name {
  display: block;
  cursor: pointer;
  color: #409EFF; /* B站蓝 */
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 4px;
  text-decoration: none;
}
.me-view-comment-author-name:hover {
  color: #f56c6c;
}

.me-view-comment-time {
  font-size: 12px;
  color: #969696;
}
.me-view-comment-time span {
  margin-right: 10px;
}

.me-view-comment-content {
  margin-top: 10px;
}

.me-view-comment-text {
  font-size: 14px;
  color: #333;
  line-height: 1.6;
}

.me-view-comment-tools {
  margin-top: 8px;
}

.me-view-comment-tool {
  font-size: 13px;
  color: #a6a6a6;
  padding-right: 14px;
  cursor: pointer;
}
.me-view-comment-tool:hover {
  color: #666;
}

.me-reply-list {
  padding-left: 50px;
  margin-top: 10px;
}
.me-reply-item {
  margin-bottom: 10px;
  padding: 10px;
  background-color: #fafafa;
  border-radius: 4px;
}
.me-reply-user {
  color: #409EFF;
  font-weight: 500;
}

/* ------------------- */
/* 悬浮卡片样式  */
/* ------------------- */
.user-card-container {
  margin: -12px; /* 抵消 el-popover 默认的 padding */
}

.card-banner {
  height: 80px;
  width: 100%;
  overflow: hidden;
  border-radius: 4px 4px 0 0;
  background-color: #eee;
}
.banner-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.card-info {
  padding: 0 20px 20px;
  position: relative;
}

.card-avatar-wrapper {
  position: absolute;
  top: -30px; /* 让头像浮在 banner 上 */
  left: 20px;
}
.card-avatar {
  border: 3px solid #fff;
  background: #fff;
  cursor: pointer;
}

.card-user-row {
  margin-top: 35px; /* 让出头像的位置 */
  display: flex;
  flex-direction: column;
}
.card-name {
  font-weight: bold;
  font-size: 16px;
  color: #fb7299; /* B站粉，或者用 #222 */
  cursor: pointer;
  margin-bottom: 4px;
}
.card-name:hover {
  color: #f25d8e;
}
.card-uid {
  font-size: 12px;
  color: #999;
}

.card-bio {
  margin-top: 10px;
  font-size: 12px;
  color: #666;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2; /* 最多显示2行签名 */
  overflow: hidden;
}

.card-action {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 15px;
}

.card-stats {
  display: flex;
  gap: 15px;
}
.stat-item {
  display: flex;
  flex-direction: column;
  text-align: center;
}
.stat-item .num {
  font-size: 14px;
  font-weight: bold;
  color: #222;
}
.stat-item .txt {
  font-size: 12px;
  color: #999;
}

.card-btns .el-button {
  padding: 6px 15px;
}
</style>
