const api = require('../../utils/api.js')

Page({
  data: {
    loading: false,
    isEmpty: true,
    items: []
  },

  onShow() {
    this.loadList()
  },

  loadList() {
    this.setData({ loading: true })

    api.getItineraryList().then((result) => {
      const items = (result.items || []).map((item) => ({
        ...item,
        displayTitle: item.title || '未命名行程',
        displayTravelerCount: item.travelerCount || 0
      }))

      this.setData({
        loading: false,
        isEmpty: items.length === 0,
        items
      })
    }).catch((err) => {
      this.setData({
        loading: false,
        isEmpty: true
      })
      console.error('load itinerary list failed', err)
    })
  },

  viewDetail(event) {
    const itineraryId = event.currentTarget.dataset.id
    wx.navigateTo({
      url: `/pages/itinerary/itinerary?itineraryId=${itineraryId}`
    })
  },

  deleteItem(event) {
    const itineraryId = event.currentTarget.dataset.id

    wx.showModal({
      title: '删除确认',
      content: '确认删除这条行程吗？',
      success: (res) => {
        if (!res.confirm) {
          return
        }

        api.deleteItinerary(itineraryId).then(() => {
          wx.showToast({
            title: '已删除',
            icon: 'success'
          })
          this.loadList()
        }).catch((err) => {
          console.error('delete itinerary failed', err)
        })
      }
    })
  }
})
