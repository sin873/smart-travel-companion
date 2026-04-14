App({
  globalData: {
    baseUrl: 'http://localhost:8081/api/v1/travel',
    token: '',
    userInfo: null
  },

  onLaunch() {
    const token = wx.getStorageSync('token') || ''
    const userInfo = wx.getStorageSync('userInfo') || null

    this.globalData.token = token
    this.globalData.userInfo = userInfo
  },

  setUserSession(token, userInfo) {
    this.globalData.token = token || ''
    this.globalData.userInfo = userInfo || null
    wx.setStorageSync('token', this.globalData.token)
    wx.setStorageSync('userInfo', this.globalData.userInfo)
  },

  clearUserSession() {
    this.globalData.token = ''
    this.globalData.userInfo = null
    wx.removeStorageSync('token')
    wx.removeStorageSync('userInfo')
  }
})
