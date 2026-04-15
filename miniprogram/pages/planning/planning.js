const api = require('../../utils/api.js')

const POLL_INTERVAL = 2000

Page({
  data: {
    taskId: '',
    status: 'QUEUED',
    progress: 0,
    message: '任务已创建，正在排队...',
    statusTitle: 'AI 正在生成行程',
    currentStep: 1,
    itinerary: null,
    summaryPreview: '',
    dayCount: 0,
    pollingTimer: null
  },

  onLoad(options) {
    if (!options.taskId) {
      wx.showToast({
        title: '缺少任务编号',
        icon: 'none'
      })
      return
    }

    this.setData({ taskId: options.taskId })
    this.startPolling()
  },

  onUnload() {
    this.clearPollingTimer()
  },

  startPolling() {
    this.clearPollingTimer()
    this.pollStatus()

    const pollingTimer = setInterval(() => {
      this.pollStatus()
    }, POLL_INTERVAL)

    this.setData({ pollingTimer })
  },

  clearPollingTimer() {
    if (this.data.pollingTimer) {
      clearInterval(this.data.pollingTimer)
      this.setData({ pollingTimer: null })
    }
  },

  async pollStatus() {
    try {
      const result = await api.getPlanStatus(this.data.taskId)
      this.updateStatus(result)

      if (result.status === 'COMPLETED' || result.status === 'FAILED') {
        this.clearPollingTimer()
      }
    } catch (err) {
      console.error('Poll status failed:', err)
    }
  },

  updateStatus(result) {
    const itinerary = result.itinerary || null
    const progress = typeof result.progress === 'number' ? result.progress : 0
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
      itinerary,
      summaryPreview: this.getSummaryPreview(itinerary),
      dayCount: itinerary && itinerary.days ? itinerary.days.length : 0
    })
  },

  getCurrentStep(progress, status) {
    if (status === 'COMPLETED') return 4
    if (progress < 30) return 1
    if (progress < 60) return 2
    if (progress < 90) return 3
    return 4
  },

  getSummaryPreview(itinerary) {
    if (!itinerary || !itinerary.summary) {
      return ''
    }

    return String(itinerary.summary).split('\n').filter(Boolean)[0] || ''
  },

  viewItinerary() {
    if (!this.data.itinerary) {
      wx.showToast({
        title: '结果尚未生成',
        icon: 'none'
      })
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
