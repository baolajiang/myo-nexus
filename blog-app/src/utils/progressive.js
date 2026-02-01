
/**
 * 获取缩略图 URL
 * 规则：将文件名后缀替换为 _thumb.jpg
 * 例如: https://example.com/cover.png -> https://example.com/cover_thumb.jpg
 * @param {String} url 原图链接
 * @returns {String} 缩略图链接
 */
export function getThumbnailUrl(url) {
  if (!url) return ''
  // 正则说明：(\.[^.]+)$ 匹配 URL 最后的 .xxx 后缀
  return url.replace(/(\.[^.]+)$/, '_thumb.jpg')
}

/**
 * 渐进式图片加载器
 * @param {String} originalUrl 原图链接
 * @param {Function} onUpdate  状态回调函数 (currentUrl, isLoaded) => {}
 */
export function loadProgressiveImage(originalUrl, onUpdate) {
  if (!originalUrl) return

  // 1. 立即返回缩略图，并标记为“未加载完成(模糊)”
  const thumbUrl = getThumbnailUrl(originalUrl)
  onUpdate(thumbUrl, false)

  // 2. 创建隐藏的 Image 对象，在后台静默下载原图
  const img = new Image()
  img.src = originalUrl

  img.onload = () => {
    // 3. 原图下载完毕：返回原图链接，并标记为“已加载完成(清晰)”
    onUpdate(originalUrl, true)
  }

  img.onerror = () => {
    // 4. 容错处理：如果原图加载失败，也标记为完成（去掉模糊），
    // 避免一直显示模糊的毛玻璃，让 img 标签显示自带的裂图或 alt
    onUpdate(originalUrl, true)
  }
}
