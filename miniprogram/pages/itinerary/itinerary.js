const api = require('../../utils/api.js')

Page({
  data: {
    itinerary: null,
    from: '',
    dateRange: ''
  },

  onLoad(options) {
    this.setData({
      from: options.from || ''
    })

    const currentItinerary = wx.getStorageSync('currentItinerary')
    if (currentItinerary) {
      this.processItinerary(currentItinerary)
      return
    }

    if (options.itineraryId) {
      this.loadItinerary(options.itineraryId)
    }
  },

  processItinerary(itinerary) {
    const days = (itinerary.days || []).map((day, index) => ({
      ...day,
      expanded: index === 0,
      displayDate: this.formatDate(day.date || day.travelDate),
      attractions: (day.attractions || day.items || []).map((item) => ({
        ...item,
        displayTime: `${this.formatTime(item.startTime)} - ${this.formatTime(item.endTime)}`
      }))
    }))

    this.setData({
      itinerary: {
        ...itinerary,
        days
      },
      dateRange: this.buildDateRange(itinerary.startDate, itinerary.endDate)
    })
  },

  async loadItinerary(itineraryId) {
    wx.showLoading({
      title: '加载中...'
    })

    try {
      const itinerary = await api.getItineraryDetail(itineraryId)
      this.processItinerary(itinerary)
    } catch (err) {
      console.error('Load itinerary failed:', err)
    } finally {
      wx.hideLoading()
    }
  },

  buildDateRange(startDate, endDate) {
    if (!startDate || !endDate) return ''
    return `${this.formatDate(startDate)} 至 ${this.formatDate(endDate)}`
  },

  formatDate(dateStr) {
    if (!dateStr) return ''
    const date = new Date(dateStr)
    if (Number.isNaN(date.getTime())) return ''

    const weekDays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
    return `${date.getMonth() + 1}月${date.getDate()}日 ${weekDays[date.getDay()]}`
  },

  formatTime(timeStr) {
    if (!timeStr) return ''
    return String(timeStr).slice(0, 5)
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
    try {
      await api.saveItinerary(this.data.itinerary)
      wx.removeStorageSync('currentItinerary')
      this.setData({ from: 'saved' })
      wx.showToast({
        title: '保存成功',
        icon: 'success'
      })
    } catch (err) {
      console.error('Save itinerary failed:', err)
    }
  },

  shareItinerary() {
    wx.showToast({
      title: '分享功能暂未开放',
      icon: 'none'
    })
  },

  editItinerary() {
    wx.showToast({
      title: '调整功能后续迭代',
      icon: 'none'
    })
  },

  confirmItinerary() {
    if (this.data.from === 'planning') {
      this.saveItinerary()
    }

    setTimeout(() => {
      wx.navigateTo({
        url: '/pages/my-itineraries/my-itineraries'
      })
    }, 800)
  }
})
