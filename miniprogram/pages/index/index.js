const app = getApp()

Page({
  data: {
    userInfo: null,
    destinations: [
      { id: 1, name: 'Beijing', desc: 'Classic city route' },
      { id: 2, name: 'Shanghai', desc: 'Modern city route' },
      { id: 3, name: 'Hangzhou', desc: 'Lake and culture route' },
      { id: 4, name: 'Chengdu', desc: 'Food and leisure route' }
    ]
  },

  onLoad() {
    this.syncUserInfo()
  },

  onShow() {
    this.syncUserInfo()
  },

  syncUserInfo() {
    this.setData({
      userInfo: app.globalData.userInfo || null
    })
  },

  goToPlan() {
    wx.navigateTo({
      url: '/pages/plan/plan'
    })
  },

  goToMyItineraries() {
    wx.navigateTo({
      url: '/pages/my-itineraries/my-itineraries'
    })
  },

  selectDestination(e) {
    const destination = e.currentTarget.dataset.dest
    wx.navigateTo({
      url: `/pages/plan/plan?destination=${encodeURIComponent(destination)}`
    })
  },

  showComingSoon() {
    wx.showToast({
      title: 'Coming soon',
      icon: 'none'
    })
  }
})
