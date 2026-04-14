const app = getApp()

Page({
  data: {
    userInfo: null,
    displayName: '旅行者',
    destinations: [
      { id: 1, name: '北京', desc: '经典古都路线' },
      { id: 2, name: '上海', desc: '城市漫游路线' },
      { id: 3, name: '杭州', desc: '西湖休闲路线' },
      { id: 4, name: '成都', desc: '美食慢游路线' }
    ]
  },

  onShow() {
    const userInfo = app.globalData.userInfo
    this.setData({
      userInfo,
      displayName: userInfo && userInfo.nickName ? userInfo.nickName : '旅行者'
    })
  },

  goToLogin() {
    wx.navigateTo({ url: '/pages/login/login' })
  },

  goToPlan() {
    wx.navigateTo({ url: '/pages/plan/plan' })
  },

  goToMyItineraries() {
    wx.navigateTo({ url: '/pages/my-itineraries/my-itineraries' })
  },

  selectDestination(event) {
    const destination = event.currentTarget.dataset.destination
    wx.navigateTo({
      url: `/pages/plan/plan?destination=${encodeURIComponent(destination)}`
    })
  }
})
