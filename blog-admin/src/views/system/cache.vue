<template>
  <div class="cache-monitor">

    <!-- 智能健康预警 -->
    <el-row :gutter="20" v-if="warnings.length > 0" style="margin-bottom: 20px;">
      <el-col :span="24">
        <el-alert
            v-for="(warn, index) in warnings"
            :key="index"
            :title="warn.title"
            :type="warn.type"
            :description="warn.description"
            show-icon
            style="margin-bottom: 10px;"
        />
      </el-col>
    </el-row>

    <!-- 顶部 4 个数据卡片 -->
    <el-row :gutter="20" class="panel-group">
      <el-col :span="6">
        <el-card class="data-card" shadow="hover">
          <div class="card-title">实时 QPS</div>
          <div class="card-value">{{ qps }} <span class="unit">次/秒</span></div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="data-card" shadow="hover">
          <div class="card-title">网络接收 (Input)</div>
          <div class="card-value">{{ netInput }} <span class="unit">KB/s</span></div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="data-card" shadow="hover">
          <div class="card-title">网络发送 (Output)</div>
          <div class="card-value">{{ netOutput }} <span class="unit">KB/s</span></div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="data-card" shadow="hover">
          <div class="card-title">当前 Key 总数</div>
          <div class="card-value">{{ dbSize }} <span class="unit">个</span></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <!-- Redis 系统配置 -->
      <el-col :span="12">
        <el-card v-loading="loading">
          <template #header><span>Redis 系统配置</span></template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="Redis版本">{{ cacheInfo.redis_version }}</el-descriptions-item>
            <el-descriptions-item label="运行模式">{{ cacheInfo.redis_mode === 'standalone' ? '单机' : '集群' }}</el-descriptions-item>
            <el-descriptions-item label="服务器IP">{{ cacheInfo.tcp_port ? '127.0.0.1:' + cacheInfo.tcp_port : '-' }}</el-descriptions-item>
            <el-descriptions-item label="运行天数">{{ cacheInfo.uptime_in_days }} 天</el-descriptions-item>
            <el-descriptions-item label="已用内存">{{ cacheInfo.used_memory_human }}</el-descriptions-item>
            <el-descriptions-item label="峰值内存">{{ cacheInfo.used_memory_peak_human }}</el-descriptions-item>
            <el-descriptions-item label="系统内存">{{ cacheInfo.total_system_memory_human }}</el-descriptions-item>
            <el-descriptions-item label="内存碎片率">{{ cacheInfo.mem_fragmentation_ratio }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <!-- 命中率仪表盘 -->
      <el-col :span="12">
        <el-card v-loading="loading">
          <template #header><span>核心指标：缓存命中率</span></template>
          <!-- 🔧 修复1：加上 width:100%，防止宽度渲染为 0 -->
          <div ref="hitRateChartRef" style="height: 250px; width: 100%;"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="24">
        <el-card v-loading="loading">
          <template #header><span>命令调用分布 (Top 5)</span></template>
          <!-- 🔧 修复1：同上 -->
          <div ref="commandChartRef" style="height: 350px; width: 100%;"></div>
        </el-card>
      </el-col>
    </el-row>

  </div>
</template>

<script setup lang="ts">
// 🔧 修复2：补上 onUnmounted，页面销毁时释放 echarts 实例，防止内存泄漏
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { getCacheMonitorInfo } from '../../api/monitor'

// -----------------------------------------------------------------------
// 状态
// -----------------------------------------------------------------------
const loading = ref(false)
const cacheInfo = ref<any>({})
const dbSize = ref(0)
const qps = ref(0)
const netInput = ref('0.00')
const netOutput = ref('0.00')

const hitRateChartRef = ref<HTMLElement>()
const commandChartRef = ref<HTMLElement>()

// 🔧 修复2：保存实例引用，销毁时用
let hitRateChart: echarts.ECharts | null = null
let commandChart: echarts.ECharts | null = null

// 健康预警
interface WarningInfo {
  title: string
  type: 'success' | 'warning' | 'info' | 'error'
  description: string
}
const warnings = ref<WarningInfo[]>([])

// -----------------------------------------------------------------------
// 数据获取
// -----------------------------------------------------------------------
const fetchCacheData = async () => {
  loading.value = true
  try {
    const res: any = await getCacheMonitorInfo()
    const backendResult = res.data

    if (backendResult && backendResult.success) {
      const data = backendResult.data
      cacheInfo.value = data.info
      dbSize.value = data.dbSize
      qps.value = data.qps
      netInput.value = data.netInput
      netOutput.value = data.netOutput

      initHitRateChart(data.hitRate)
      initCommandChart(data.commandStats)
      checkSystemHealth(data)
    }
  } catch (e) {
    // request.ts 已统一弹窗，这里静默处理
    console.error('[缓存监控] 数据加载失败', e)
  } finally {
    loading.value = false
  }
}

// -----------------------------------------------------------------------
// 智能健康检查
// -----------------------------------------------------------------------
const checkSystemHealth = (data: any) => {
  warnings.value = []

  // 1. 命中率预警
  if (data.hitRate < 50) {
    warnings.value.push({
      type: 'error',
      title: '红色预警：缓存命中率严重低下',
      description: `当前命中率仅为 ${data.hitRate}%。系统可能正在遭遇缓存穿透攻击，或者正处于大量数据过期的雪崩边缘，底层数据库面临极大压力，请立即排查！`
    })
  } else if (data.hitRate < 80) {
    warnings.value.push({
      type: 'warning',
      title: '黄色预警：缓存命中率偏低',
      description: `当前命中率为 ${data.hitRate}%。若系统刚启动属正常现象；若已运行一段时间，建议检查热点文章的缓存过期时间配置。`
    })
  }

  // 2. 读写倒挂预警
  let getCount = 0
  let setCount = 0
  data.commandStats.forEach((cmd: any) => {
    if (['get', 'hget', 'mget'].includes(cmd.name)) getCount += cmd.value
    if (['set', 'setnx', 'hset', 'setex'].includes(cmd.name)) setCount += cmd.value
  })
  if ((getCount + setCount) > 100 && setCount > getCount) {
    warnings.value.push({
      type: 'error',
      title: '红色预警：读写比例异常倒挂',
      description: `博客系统应为读多写少，但近期 SET 类写入（${setCount}次）超过 GET 类读取（${getCount}次）。极可能是防穿透机制被恶意扫描触发，正在大量写入 NULL 占位符，请立即查看系统日志中的异常 IP！`
    })
  }

  // 3. 内存碎片率预警
  const fragRatio = parseFloat(data.info.mem_fragmentation_ratio || '0')
  if (fragRatio > 1.5) {
    warnings.value.push({
      type: 'warning',
      title: '系统建议：内存碎片率偏高',
      description: `当前内存碎片率为 ${fragRatio}，超过安全线 1.5，存在内存浪费。建议在夜间低峰期执行碎片整理。`
    })
  }
}

// -----------------------------------------------------------------------
// 命中率仪表盘
// -----------------------------------------------------------------------
const initHitRateChart = (rate: number) => {
  // 🔧 修复3：先销毁旧实例，防止重复刷新时图表叠加
  hitRateChart?.dispose()
  if (!hitRateChartRef.value) return
  hitRateChart = echarts.init(hitRateChartRef.value)

  hitRateChart.setOption({
    series: [
      {
        type: 'gauge',
        startAngle: 180,
        endAngle: 0,
        min: 0,
        max: 100,
        splitNumber: 10,
        itemStyle: {
          color: rate > 80 ? '#67C23A' : (rate > 50 ? '#E6A23C' : '#F56C6C'),
          shadowColor: 'rgba(0,138,255,0.45)',
          shadowBlur: 10
        },
        progress: { show: true, width: 20 },
        pointer: { show: false },
        axisLine: { lineStyle: { width: 20 } },
        axisTick: { show: false },
        splitLine: { show: false },
        axisLabel: { show: false },
        title: {
          show: true,
          offsetCenter: [0, '40%'],
          fontSize: 14,
          color: '#909399'
        },
        detail: {
          valueAnimation: true,
          formatter: '{value}%',
          color: 'inherit',
          fontSize: 35,
          offsetCenter: [0, '-10%']
        },
        data: [{ value: rate, name: '命中率' }]
      }
    ]
  })
}

// -----------------------------------------------------------------------
// 命令调用分布圆环图
// -----------------------------------------------------------------------
const initCommandChart = (rawData: any[]) => {
  // 🔧 修复3：先销毁旧实例
  commandChart?.dispose()
  if (!commandChartRef.value) return
  commandChart = echarts.init(commandChartRef.value)

  // 不直接修改原数组，用副本排序
  const sorted = [...rawData].sort((a, b) => b.value - a.value)
  const processedData = sorted.slice(0, 5)
  if (sorted.length > 5) {
    const otherValue = sorted.slice(5).reduce((sum, item) => sum + item.value, 0)
    processedData.push({ name: '其他 (other)', value: otherValue })
  }

  commandChart.setOption({
    tooltip: { trigger: 'item', formatter: '{a} <br/>{b} : {c} 次 ({d}%)' },
    legend: { type: 'scroll', orient: 'vertical', right: '5%', top: 'middle' },
    color: ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#fc8452'],
    series: [
      {
        name: '命令调用',
        type: 'pie',
        radius: ['50%', '70%'],
        center: ['40%', '50%'],
        itemStyle: { borderRadius: 5, borderColor: '#fff', borderWidth: 2 },
        label: { show: true, formatter: '{b}\n{c}次' },
        data: processedData
      }
    ]
  })
}

// -----------------------------------------------------------------------
// 生命周期
// -----------------------------------------------------------------------
onMounted(() => {
  fetchCacheData()
  // 🔧 修复4：窗口 resize 时图表自适应
  window.addEventListener('resize', handleResize)
})

// 🔧 修复2：页面销毁时释放资源
onUnmounted(() => {
  hitRateChart?.dispose()
  commandChart?.dispose()
  window.removeEventListener('resize', handleResize)
})

const handleResize = () => {
  hitRateChart?.resize()
  commandChart?.resize()
}
</script>

<style scoped>
.cache-monitor {
  padding: 20px;
}
.el-card {
  margin-bottom: 20px;
}
.data-card {
  text-align: center;
  padding: 10px 0;
  border-radius: 8px;
}
.card-title {
  font-size: 14px;
  color: #909399;
  margin-bottom: 10px;
}
.card-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}
.card-value .unit {
  font-size: 14px;
  font-weight: normal;
  color: #909399;
}
</style>