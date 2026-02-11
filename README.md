<div align="center">
  <h1>  Blog 系统</h1>
  <p>基于 Spring Boot + Vue 2 (前台) + Vue 3 (后台) 的全栈博客</p>
  <br />
</div>

##  项目模块规划

本项目采用前后端分离架构，包含一个后端服务和两个前端应用（前台展示 + 后台管理）。

<table width="100%">
  <tr>
    <td width="33%" valign="top">
      <h3> 后端服务 (blog-api)</h3>
      <ul>
        <li><b>核心框架：</b>Spring Boot</li>
        <li><b>ORM框架：</b>MyBatis Plus</li>
        <li><b>数据库：</b>MySQL + Redis</li>
        <li><b>其他技术：</b>RabbitMQ, AWS S3, IP2Region</li>
      </ul>
    </td>
    <td width="33%" valign="top">
      <h3> 前台展示 (blog-app)</h3>
      <ul>
        <li><b>核心框架：</b>Vue 2.x</li>
        <li><b>构建工具：</b>Webpack</li>
        <li><b>UI 组件：</b>Element UI</li>
        <li><b>主要功能：</b>文章阅读、评论、留言</li>
      </ul>
    </td>
    <td width="33%" valign="top">
      <h3>后台管理 (blog-admin)</h3>
      <ul>
        <li><b>核心框架：</b>Vue 3.x + TypeScript</li>
        <li><b>构建工具：</b>Vite</li>
        <li><b>状态管理：</b>Pinia</li>
        <li><b>UI 组件：</b>Element Plus</li>
      </ul>
    </td>
  </tr>
</table>

---

## 快速上手指南

### 1. 后端环境配置 (blog-api)
<blockquote style="padding: 10px; background-color: #f9f9f9; border-left: 5px solid #2196F3;">
  <b>步骤 A:</b> 修改 <code>src/main/resources/application.properties</code> 中的数据库及 Redis 连接配置。<br/>
  <b>步骤 B:</b> 根目录下运行 <code>mvn clean install</code> 进行项目构建。<br/>
  <b>步骤 C:</b> 运行 <code>BlogApp.java</code> 启动后端服务 (默认端口: 48882)。
</blockquote>

### 2. 后台管理环境配置 (blog-admin)
> 基于 Vue 3 + Vite 的现代化管理端
<blockquote style="padding: 10px; background-color: #f9f9f9; border-left: 5px solid #9C27B0;">
  <b>步骤 A:</b> 进入 <code>blog-admin</code> 目录执行 <code>npm install</code> 安装依赖。<br/>
  <b>步骤 B:</b> 执行 <code>npm run dev</code> 启动开发服务。<br/>
  <b>步骤 C:</b> 访问本地地址进行管理操作。
</blockquote>

### 3. 前台展示环境配置 (blog-app)
> 基于 Vue 2 的经典博客展示端
<blockquote style="padding: 10px; background-color: #f9f9f9; border-left: 5px solid #4CAF50;">
  <b>步骤 A:</b> 进入 <code>blog-app</code> 目录执行 <code>npm install</code> 安装依赖。<br/>
  <b>步骤 B:</b> 执行 <code>npm run dev</code> 开启开发模式。
</blockquote>
