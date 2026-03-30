// pages/my-itineraries/my-itineraries.js
const api = require('../../utils/api.js')
const app = getApp()

Page({
  data: {
    userInfo: null,
    itineraries: [],
    stats: {
      total: 0,
      planned: 0,
      completed: 0
    }
  },

  onLoad() {
    this.setData({
      userInfo: app.globalData.userInfo
    })
    
    if (app.globalData.userInfo) {
      this.loadItineraries()
    }
  },

  onShow() {
    this.setData({
      userInfo: app.globalData.userInfo
    })
    
    if (app.globalData.userInfo) {
      this.loadItineraries()
    }
  },

  async loadItineraries() {
    try {
      const result = await api.get('/itinerary')
      
      const itineraries = result.items || []
      const stats = {
        total: itineraries.length,
        planned: itineraries.filter(i => i.status === 'DRAFT' || i.status === 'PLANNED').length,
        completed: itineraries.filter(i => i.status === 'COMPLETED').length
      }
      
      this.setData({ itineraries, stats })
    } catch (err) {
      console.error('Load itineraries failed:', err)
    }
  },

  statusText(status) {
    const map = {
      'DRAFT': '草稿',
      'PLANNED': '已规划',
      'COMPLETED': '已完成'
    }
    return map[status] || status
  },

  formatDateRange(start, end) {
    if (!start || !end) return ''
    const startDate = new Date(start)
    const endDate = new Date(end)
    return `${startDate.getMonth() + 1}.${startDate.getDate()} - ${endDate.getMonth() + 1}.${endDate.getDate()}`
  },

  viewItinerary(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({
      url: `/pages/itinerary/itinerary?itineraryId=${id}`
    })
  },

  goToPlan() {
    wx.switchTab({
      url: '/pages/plan/plan'
    })
  },

  goToLogin() {
    wx.navigateTo({
      url: '/pages/login/login'
    })
  }
})
