// pages/planning/planning.js
const api = require('../../utils/api.js')

Page({
  data: {
    taskId: '',
    status: 'QUEUED',
    progress: 0,
    message: '任务已创建,正在排队...',
    statusTitle: '正在为你规划行程',
    currentStep: 1,
    itinerary: null,
    pollingTimer: null,
    hasNavigated: false
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

  onHide() {
    if (this.data.pollingTimer) {
      clearInterval(this.data.pollingTimer)
      this.setData({ pollingTimer: null })
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
      console.log('【planning】开始轮询 taskId:', this.data.taskId)
      const result = await api.getPlanStatus(this.data.taskId)

      console.log('【planning】任务状态返回:', result)

      this.updateStatus(result)

      if (result.status === 'COMPLETED' || result.status === 'FAILED') {
        if (this.data.pollingTimer) {
          clearInterval(this.data.pollingTimer)
          this.setData({ pollingTimer: null })
        }

        if (result.status === 'COMPLETED' && !this.data.hasNavigated) {
          this.navigateToDetail(result.itinerary)
        }
      }
    } catch (err) {
      console.error('Poll status failed:', err)
    }
  },

  updateStatus(result) {
    let statusTitle = '正在为你规划行程'
    let message = result.message || ''

    if (result.status === 'QUEUED') {
      statusTitle = '正在为你规划行程'
      message = message || '正在排队生成行程...'
    } else if (result.status === 'PROCESSING') {
      statusTitle = '正在为你规划行程'
      message = message || '正在生成行程，请稍候...'
    } else if (result.status === 'COMPLETED') {
      statusTitle = '行程规划完成!'
      message = message || '即将跳转到详情页...'
    } else if (result.status === 'FAILED') {
      statusTitle = '行程规划失败'
      message = message || '生成失败，请重试'
    }

    this.setData({
      status: result.status,
      progress: result.progress || 0,
      message: message,
      statusTitle: statusTitle,
      currentStep: this.getStepFromProgress(result.progress),
      itinerary: result.itinerary
    })
  },

  getStepFromProgress(progress) {
    if (!progress) return 1
    if (progress < 30) return 1
    if (progress < 50) return 2
    if (progress < 70) return 3
    return 4
  },

  async navigateToDetail(itinerary) {
    console.log('【planning】准备自动保存并跳转 itinerary:', itinerary)

    if (!itinerary) {
      wx.showToast({
        title: '行程数据异常',
        icon: 'none'
      })
      return
    }

    this.setData({ hasNavigated: true })

    try {
      // 自动保存行程
      console.log('【planning】开始自动保存行程...')
      const saveResult = await api.saveItinerary(itinerary)
      console.log('【planning】自动保存成功，返回:', saveResult)

      // 保存成功后直接跳转到我的行程页
      wx.showToast({
        title: '保存成功',
        icon: 'success',
        duration: 1000
      })

      setTimeout(() => {
        wx.redirectTo({
          url: '/pages/my-itineraries/my-itineraries'
        })
      }, 1000)

    } catch (err) {
      console.error('【planning】自动保存失败:', err)
      wx.showToast({
        title: '保存失败: ' + (err.message || '未知错误'),
        icon: 'none',
        duration: 2000
      })

      // 如果保存失败，仍然可以跳转到详情页手动保存
      setTimeout(() => {
        wx.setStorageSync('currentItinerary', itinerary)
        wx.redirectTo({
          url: '/pages/itinerary/itinerary?mode=generated'
        })
      }, 2000)
    }
  },

  viewItinerary() {
    if (this.data.itinerary && !this.data.hasNavigated) {
      this.navigateToDetail(this.data.itinerary)
    }
  },

  retryPlan() {
    wx.navigateBack()
  },

  goBack() {
    wx.navigateBack()
  }
})
