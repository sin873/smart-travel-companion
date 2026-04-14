const api = require('../../utils/api.js')

Page({
  data: {
    taskId: '',
    status: 'QUEUED',
    progress: 0,
    message: '任务已创建，正在排队...',
    statusTitle: 'AI 正在生成行程',
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
    const pollingTimer = setInterval(() => {
      this.pollStatus()
    }, 2000)

    this.setData({ pollingTimer })
  },

  async pollStatus() {
    try {
      const result = await api.getPlanStatus(this.data.taskId)
      this.updateStatus(result)

      if (result.status === 'COMPLETED' || result.status === 'FAILED') {
        clearInterval(this.data.pollingTimer)
        this.setData({ pollingTimer: null })
      }
    } catch (err) {
      console.error('Poll status failed:', err)
    }
  },

  updateStatus(result) {
    const progress = result.progress || 0
    const statusTitleMap = {
      QUEUED: '任务排队中',
      PROCESSING: 'AI 正在生成行程',
      COMPLETED: '行程已生成',
      FAILED: '生成失败'
    }

    this.setData({
      status: result.status,
      progress,
      message: result.message || '',
      statusTitle: statusTitleMap[result.status] || '处理中',
      currentStep: this.getCurrentStep(progress, result.status),
      itinerary: result.itinerary || null
    })
  },

  getCurrentStep(progress, status) {
    if (status === 'COMPLETED') return 4
    if (progress < 30) return 1
    if (progress < 60) return 2
    if (progress < 90) return 3
    return 4
  },

  viewItinerary() {
    if (!this.data.itinerary) {
      return
    }

    wx.setStorageSync('currentItinerary', this.data.itinerary)
    wx.redirectTo({
      url: '/pages/itinerary/itinerary?from=planning'
    })
  },

  retryPlan() {
    wx.navigateBack({
      delta: 1
    })
  },

  goBack() {
    wx.navigateBack({
      delta: 1
    })
  }
})
