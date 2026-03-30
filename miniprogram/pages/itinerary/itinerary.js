// pages/itinerary/itinerary.js
const api = require('../../utils/api.js')

Page({
  data: {
    itinerary: null,
    from: '',
    dateRange: ''
  },

  onLoad(options) {
    this.setData({ from: options.from || '' })
    
    // 从本地存储获取行程数据
    const itinerary = wx.getStorageSync('currentItinerary')
    if (itinerary) {
      this.processItinerary(itinerary)
    } else if (options.itineraryId) {
      this.loadItinerary(options.itineraryId)
    }
  },

  processItinerary(itinerary) {
    // 展开第一天
    if (itinerary.days && itinerary.days.length > 0) {
      itinerary.days[0].expanded = true
    }
    
    // 计算日期范围
    if (itinerary.startDate && itinerary.endDate) {
      const start = this.formatDate(itinerary.startDate)
      const end = this.formatDate(itinerary.endDate)
      this.setData({ dateRange: `${start} 至 ${end}` })
    }
    
    this.setData({ itinerary })
  },

  async loadItinerary(itineraryId) {
    wx.showLoading({ title: '加载中...' })
    
    try {
      const itinerary = await api.get(`/itinerary/${itineraryId}`)
      this.processItinerary(itinerary)
    } catch (err) {
      console.error('Load itinerary failed:', err)
    } finally {
      wx.hideLoading()
    }
  },

  formatDate(dateStr) {
    if (!dateStr) return ''
    const date = new Date(dateStr)
    const month = date.getMonth() + 1
    const day = date.getDate()
    const weekDays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
    const weekDay = weekDays[date.getDay()]
    return `${month}月${day}日 ${weekDay}`
  },

  formatTime(timeStr) {
    if (!timeStr) return ''
    return timeStr.substring(0, 5)
  },

  toggleDay(e) {
    const index = e.currentTarget.dataset.index
    const key = `itinerary.days[${index}].expanded`
    const current = this.data.itinerary.days[index].expanded
    this.setData({
      [key]: !current
    })
  },

  async saveItinerary() {
    wx.showLoading({ title: '保存中...' })
    
    try {
      const result = await api.post('/itinerary', this.data.itinerary)
      
      wx.hideLoading()
      wx.showToast({
        title: '保存成功',
        icon: 'success'
      })
      
      // 更新来源状态
      this.setData({ from: 'saved' })
      
      // 清除本地存储的临时行程
      wx.removeStorageSync('currentItinerary')
    } catch (err) {
      wx.hideLoading()
      console.error('Save itinerary failed:', err)
    }
  },

  shareItinerary() {
    wx.showShareMenu({
      withShareTicket: true
    })
  },

  onShareAppMessage() {
    return {
      title: this.data.itinerary.title,
      path: '/pages/itinerary/itinerary?itineraryId=' + this.data.itinerary.itineraryId
    }
  },

  editItinerary() {
    wx.showToast({
      title: '调整功能开发中',
      icon: 'none'
    })
  },

  confirmItinerary() {
    wx.showModal({
      title: '确认行程',
      content: '确认后您可以在"我的行程"中查看此行程',
      success: (res) => {
        if (res.confirm) {
          if (this.data.from === 'planning') {
            this.saveItinerary()
          }
          
          setTimeout(() => {
            wx.switchTab({
              url: '/pages/my-itineraries/my-itineraries'
            })
          }, 1500)
        }
      }
    })
  }
})
