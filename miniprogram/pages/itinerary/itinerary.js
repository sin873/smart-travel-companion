const api = require('../../utils/api.js')

Page({
  data: {
    itinerary: null,
    from: '',
    dateRange: '',
    overviewSummary: '',
    agentAdvice: '',
    ragHints: []
  },

  onLoad(options) {
    const from = options.from || ''

    this.setData({ from })

    if (from === 'planning') {
      const currentItinerary = wx.getStorageSync('currentItinerary')
      if (currentItinerary) {
        this.processItinerary(currentItinerary)
        return
      }
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
      attractions: (day.attractions || day.items || []).map(item => ({
        ...item,
        displayTime: this.buildDisplayTime(item.startTime, item.endTime)
      }))
    }))

    const summaryInfo = this.parseSummary(itinerary.summary)

    this.setData({
      itinerary: {
        ...itinerary,
        days
      },
      dateRange: this.buildDateRange(itinerary.startDate, itinerary.endDate),
      overviewSummary: summaryInfo.overviewSummary,
      agentAdvice: summaryInfo.agentAdvice,
      ragHints: summaryInfo.ragHints
    })
  },

  parseSummary(summary) {
    const text = summary ? String(summary).trim() : ''
    if (!text) {
      return {
        overviewSummary: '',
        agentAdvice: '',
        ragHints: []
      }
    }

    const marker = '智能伴旅Agent建议:'
    const markerIndex = text.indexOf(marker)
    const overviewSummary = markerIndex >= 0 ? text.slice(0, markerIndex).trim() : text
    const agentAdvice = markerIndex >= 0 ? text.slice(markerIndex + marker.length).trim() : ''
    const ragHints = agentAdvice
      .split('\n')
      .map(line => line.trim())
      .filter(line => line && this.isKnowledgeHint(line))

    return {
      overviewSummary,
      agentAdvice,
      ragHints
    }
  },

  isKnowledgeHint(line) {
    return ['知识库', 'RAG', '本地资料', '补充', '提示'].some(keyword => line.includes(keyword))
  },

  buildDisplayTime(startTime, endTime) {
    const start = this.formatTime(startTime)
    const end = this.formatTime(endTime)

    if (start && end) {
      return `${start} - ${end}`
    }

    return start || end || '时间待定'
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

    const normalized = String(dateStr).replace(/-/g, '/')
    const date = new Date(normalized)
    if (Number.isNaN(date.getTime())) return String(dateStr)

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

  buildSavePayload() {
    const itinerary = this.data.itinerary
    if (!itinerary) {
      return null
    }

    return {
      itineraryId: itinerary.itineraryId,
      title: itinerary.title,
      destination: itinerary.destination,
      startDate: itinerary.startDate,
      endDate: itinerary.endDate,
      travelerCount: itinerary.travelerCount,
      budget: itinerary.budget,
      status: itinerary.status,
      summary: itinerary.summary,
      days: (itinerary.days || []).map(day => ({
        dayId: day.dayId,
        dayNumber: day.dayNumber,
        travelDate: day.travelDate || day.date,
        title: day.title,
        notes: day.notes,
        items: (day.items || day.attractions || []).map(item => ({
          itemId: item.itemId,
          attractionId: item.attractionId,
          attractionName: item.attractionName,
          address: item.address,
          latitude: item.latitude,
          longitude: item.longitude,
          startTime: item.startTime,
          endTime: item.endTime,
          durationMinutes: item.durationMinutes,
          orderIndex: item.orderIndex,
          notes: item.notes
        }))
      }))
    }
  },

  async saveItinerary() {
    try {
      const payload = this.buildSavePayload()
      if (!payload) {
        return false
      }

      await api.saveItinerary(payload)
      wx.removeStorageSync('currentItinerary')
      this.setData({ from: 'saved' })
      wx.showToast({
        title: '保存成功',
        icon: 'success'
      })
      return true
    } catch (err) {
      console.error('Save itinerary failed:', err)
      return false
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

  async confirmItinerary() {
    if (this.data.from === 'planning') {
      const saved = await this.saveItinerary()
      if (!saved) {
        return
      }
    }

    setTimeout(() => {
      wx.navigateTo({
        url: '/pages/my-itineraries/my-itineraries'
      })
    }, 800)
  }
})
