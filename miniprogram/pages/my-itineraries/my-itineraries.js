const api = require('../../utils/api.js')
const app = getApp()

Page({
  data: {
    userInfo: null,
    itineraries: [],
    loading: false,
    stats: {
      total: 0,
      planned: 0,
      completed: 0
    }
  },

  onLoad() {
    this.syncUser()
  },

  onShow() {
    this.syncUser()
    if (this.data.userInfo) {
      this.loadItineraries()
    }
  },

  syncUser() {
    this.setData({
      userInfo: app.globalData.userInfo
    })
  },

  async loadItineraries() {
    this.setData({ loading: true })

    try {
      const result = await api.getItineraryList()
      const items = (result.items || []).map(item => ({
        ...item,
        statusText: this.getStatusText(item.status),
        dateRangeText: this.getDateRangeText(item.startDate, item.endDate),
        summaryText: this.getSummaryText(item.summary)
      }))

      this.setData({
        itineraries: items,
        stats: {
          total: items.length,
          planned: items.filter(item => item.status === 'DRAFT' || item.status === 'PLANNED').length,
          completed: items.filter(item => item.status === 'COMPLETED').length
        }
      })
    } catch (err) {
      console.error('Load itineraries failed:', err)
    } finally {
      this.setData({ loading: false })
    }
  },

  getStatusText(status) {
    const map = {
      DRAFT: '草稿',
      PLANNED: '已规划',
      COMPLETED: '已完成'
    }
    return map[status] || status || '未知状态'
  },

  getDateRangeText(start, end) {
    if (!start || !end) return ''
    return `${start} - ${end}`
  },

  getSummaryText(summary) {
    if (!summary) {
      return ''
    }

    return String(summary).split('\n').filter(Boolean)[0] || ''
  },

  viewItinerary(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({
      url: `/pages/itinerary/itinerary?itineraryId=${id}`
    })
  },

  goToPlan() {
    wx.navigateTo({
      url: '/pages/plan/plan'
    })
  },

  goToLogin() {
    wx.navigateTo({
      url: '/pages/login/login'
    })
  }
})
