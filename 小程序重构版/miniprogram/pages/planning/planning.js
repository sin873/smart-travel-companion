const api = require('../../utils/api.js')

Page({
  data: {
    taskId: '',
    status: 'QUEUED',
    progress: 0,
    message: '任务已创建，正在排队处理',
    itinerary: null
  },

  timer: null,

  onLoad(options) {
    if (options.taskId) {
      this.setData({
        taskId: options.taskId
      })
      this.startPolling()
    }
  },

  onUnload() {
    this.stopPolling()
  },

  startPolling() {
    this.pollStatus()
    this.timer = setInterval(() => {
      this.pollStatus()
    }, 2000)
  },

  stopPolling() {
    if (this.timer) {
      clearInterval(this.timer)
      this.timer = null
    }
  },

  pollStatus() {
    api.getPlanStatus(this.data.taskId).then((result) => {
      this.setData({
        status: result.status || 'QUEUED',
        progress: result.progress || 0,
        message: result.message || '',
        itinerary: result.itinerary || null
      })

      if (result.status === 'COMPLETED' || result.status === 'FAILED') {
        this.stopPolling()
      }
    }).catch((err) => {
      console.error('poll status failed', err)
    })
  },

  viewItinerary() {
    if (!this.data.itinerary) {
      return
    }

    wx.setStorageSync('currentItinerary', this.data.itinerary)
    wx.redirectTo({
      url: '/pages/itinerary/itinerary?mode=preview'
    })
  }
})
