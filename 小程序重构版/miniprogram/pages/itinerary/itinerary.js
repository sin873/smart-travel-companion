const api = require('../../utils/api.js')

function pickItems(day) {
  if (day && Array.isArray(day.items) && day.items.length > 0) {
    return day.items
  }

  if (day && Array.isArray(day.attractions) && day.attractions.length > 0) {
    return day.attractions
  }

  return []
}

function toNumber(value) {
  if (value === null || value === undefined || value === '') {
    return null
  }

  const numberValue = Number(value)
  return Number.isNaN(numberValue) ? null : numberValue
}

function buildTimeText(item) {
  const startTime = item.startTime || ''
  const endTime = item.endTime || ''

  if (startTime && endTime) {
    return `${startTime} - ${endTime}`
  }

  if (startTime) {
    return startTime
  }

  if (endTime) {
    return endTime
  }

  return '时间待定'
}

function normalizeItem(item, index) {
  const latitude = toNumber(item.latitude)
  const longitude = toNumber(item.longitude)

  return {
    itemId: item.itemId || `item-${index}`,
    attractionId: item.attractionId || null,
    attractionName: item.attractionName || item.name || '',
    address: item.address || '',
    latitude,
    longitude,
    hasLocation: latitude !== null && longitude !== null,
    startTime: item.startTime || null,
    endTime: item.endTime || null,
    durationMinutes: item.durationMinutes || null,
    orderIndex: item.orderIndex != null ? item.orderIndex : index,
    notes: item.notes || '',
    name: item.attractionName || item.name || '未命名景点',
    timeText: buildTimeText(item),
    addressText: item.address || '暂无地址',
    notesText: item.notes || '暂无备注'
  }
}

function normalizeDay(day, index) {
  const items = pickItems(day).map((item, itemIndex) => normalizeItem(item, itemIndex))

  return {
    dayId: day.dayId || `day-${index}`,
    dayNumber: day.dayNumber || index + 1,
    title: day.title || '',
    travelDate: day.travelDate || day.date || '',
    notes: day.notes || '',
    titleText: day.title || `第${index + 1}天`,
    dateText: day.travelDate || day.date || '',
    notesText: day.notes || '',
    items,
    hasItems: items.length > 0
  }
}

function buildMapData(days) {
  const markers = []
  const coordinates = []

  days.forEach((day) => {
    day.items.forEach((item) => {
      if (!item.hasLocation) {
        return
      }

      coordinates.push({
        latitude: item.latitude,
        longitude: item.longitude
      })

      markers.push({
        id: markers.length + 1,
        latitude: item.latitude,
        longitude: item.longitude,
        width: 26,
        height: 34,
        callout: {
          content: item.name,
          display: 'BYCLICK',
          color: '#0f172a',
          bgColor: '#ffffff',
          borderRadius: 8,
          padding: 6,
          borderWidth: 1,
          borderColor: '#cbd5e1'
        }
      })
    })
  })

  if (coordinates.length === 0) {
    return {
      hasMap: false,
      markers: [],
      latitude: 39.9042,
      longitude: 116.4074
    }
  }

  const latitude =
    coordinates.reduce((sum, item) => sum + item.latitude, 0) / coordinates.length
  const longitude =
    coordinates.reduce((sum, item) => sum + item.longitude, 0) / coordinates.length

  return {
    hasMap: true,
    markers,
    latitude,
    longitude
  }
}

function normalizeItinerary(itinerary) {
  if (!itinerary) {
    return null
  }

  const days = Array.isArray(itinerary.days)
    ? itinerary.days.map((day, index) => normalizeDay(day, index))
    : []
  const mapData = buildMapData(days)

  return {
    itineraryId: itinerary.itineraryId || '',
    titleText: itinerary.title || '未命名行程',
    destinationText: itinerary.destination || '未填写',
    dateRangeText: `${itinerary.startDate || ''} 至 ${itinerary.endDate || ''}`,
    travelerCountText: `${itinerary.travelerCount || 0}`,
    startDate: itinerary.startDate || '',
    endDate: itinerary.endDate || '',
    destination: itinerary.destination || '',
    title: itinerary.title || '',
    travelerCount: itinerary.travelerCount || 0,
    budget: itinerary.budget || null,
    status: itinerary.status || 'PLANNED',
    summary: itinerary.summary || '',
    days,
    hasDays: days.length > 0,
    hasMap: mapData.hasMap,
    mapMarkers: mapData.markers,
    mapLatitude: mapData.latitude,
    mapLongitude: mapData.longitude
  }
}

function buildSavePayload(itinerary) {
  return {
    itineraryId: itinerary.itineraryId || null,
    title: itinerary.title || '',
    destination: itinerary.destination || '',
    startDate: itinerary.startDate || null,
    endDate: itinerary.endDate || null,
    travelerCount: itinerary.travelerCount || 1,
    budget: itinerary.budget,
    status: itinerary.status || 'PLANNED',
    summary: itinerary.summary || '',
    days: (itinerary.days || []).map((day) => ({
      dayId: day.dayId || null,
      dayNumber: day.dayNumber || 1,
      travelDate: day.travelDate || day.dateText || null,
      title: day.title || day.titleText || '',
      notes: day.notes || day.notesText || '',
      items: (day.items || []).map((item, index) => ({
        itemId: item.itemId || null,
        attractionId: item.attractionId || null,
        attractionName: item.attractionName || item.name || '',
        address: item.address || item.addressText || '',
        latitude: item.latitude,
        longitude: item.longitude,
        startTime: item.startTime || null,
        endTime: item.endTime || null,
        durationMinutes: item.durationMinutes || null,
        orderIndex: item.orderIndex != null ? item.orderIndex : index,
        notes: item.notes || item.notesText || ''
      }))
    }))
  }
}

Page({
  data: {
    mode: 'detail',
    isPreview: false,
    itineraryId: '',
    itinerary: null,
    saving: false
  },

  onLoad(options) {
    const mode = options.mode || 'detail'
    const itineraryId = options.itineraryId || ''

    this.setData({
      mode,
      isPreview: mode === 'preview',
      itineraryId
    })

    if (mode === 'preview') {
      const cached = wx.getStorageSync('currentItinerary')
      this.setData({
        itinerary: normalizeItinerary(cached)
      })
      return
    }

    if (itineraryId) {
      this.loadItinerary(itineraryId)
    }
  },

  loadItinerary(itineraryId) {
    wx.showLoading({ title: '加载中' })

    api.getItineraryDetail(itineraryId).then((result) => {
      this.setData({
        itinerary: normalizeItinerary(result)
      })
    }).catch((err) => {
      console.error('load itinerary failed', err)
    }).finally(() => {
      wx.hideLoading()
    })
  },

  saveItinerary() {
    if (!this.data.itinerary || this.data.saving) {
      return
    }

    const payload = buildSavePayload(this.data.itinerary)

    this.setData({ saving: true })
    wx.showLoading({ title: '保存中' })

    api.saveItinerary(payload).then((result) => {
      wx.showToast({
        title: '已保存',
        icon: 'success'
      })

      const itineraryId = result.itineraryId || this.data.itinerary.itineraryId
      wx.redirectTo({
        url: `/pages/itinerary/itinerary?itineraryId=${itineraryId}`
      })
    }).catch((err) => {
      console.error('save itinerary failed', err)
    }).finally(() => {
      this.setData({ saving: false })
      wx.hideLoading()
    })
  }
})
