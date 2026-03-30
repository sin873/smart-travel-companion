// pages/planning/planning.js
const api = require('../../utils/api.js')

Page({
  data: {
    taskId: '',
    status: 'QUEUED',
    progress: 0,
    message: '任务已创建,正在排队...',
    statusTitle: 'AI正在为您规划行程',
    currentStep: 1,
    itinerary: null,
    pollingTimer: null
  },

  onLoad(options) {
    if (options.taskId) {
      this.setData({ taskId: options.taskId })
      this.startPolling()
    }
  },

  onUnload() {
    if (this.data.pollingTimer) {
      clearInterval(this.data.pollingTimer)
    }
  },

  startPolling() {
    this.pollStatus()
    const timer = setInterval(() => {
      this.pollStatus()
    }, 2000)
    this.setData({ pollingTimer: timer })
  },

  async pollStatus() {
    try {
      const result = await api.get(`/itinerary/plan/${this.data.taskId}`)
      
      this.updateStatus(result)
      
      if (result.status === 'COMPLETED' || result.status === 'FAILED') {
        if (this.data.pollingTimer) {
          clearInterval(this.data.pollingTimer)
          this.setData({ pollingTimer: null })
        }
      }
    } catch (err) {
      console.error('Poll status failed:', err)
    }
  },

  updateStatus(result) {
    const statusMap = {
      'QUEUED': { title: 'AI正在为您规划行程', step: 1 },
      'PROCESSING': { title: 'AI正在为您规划行程', step: this.getStepFromProgress(result.progress) },
      'COMPLETED': { title: '行程规划完成!', step: 4 },
      'FAILED': { title: '行程规划失败', step: 1 }
    }

    const statusInfo = statusMap[result.status] || statusMap['QUEUED']

    this.setData({
      status: result.status,
      progress: result.progress || 0,
      message: result.message || '',
      statusTitle: statusInfo.title,
      currentStep: statusInfo.step,
      itinerary: result.itinerary
    })
  },

  getStepFromProgress(progress) {
    if (progress < 30) return 1
    if (progress < 50) return 2
    if (progress < 70) return 3
    return 4
  },

  viewItinerary() {
    if (this.data.itinerary) {
      // 保存行程数据到本地存储,传递给详情页
      wx.setStorageSync('currentItinerary', this.data.itinerary)
      
      wx.redirectTo({
        url: '/pages/itinerary/itinerary?from=planning'
      })
    }
  },

  retryPlan() {
    wx.navigateBack()
  },

  goBack() {
    wx.navigateBack()
  }
})
