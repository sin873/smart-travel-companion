App({
  onLaunch() {
    this.checkLoginStatus()
  },

  globalData: {
    userInfo: null,
    token: null,
    baseUrl: 'http://localhost:8081/api/v1/travel'
  },

  checkLoginStatus() {
    const token = wx.getStorageSync('token')
    const userInfo = wx.getStorageSync('userInfo')

    if (token) {
      this.globalData.token = token
    }

    if (userInfo) {
      this.globalData.userInfo = userInfo
    }
  },

  setToken(token) {
    this.globalData.token = token
    wx.setStorageSync('token', token)
  },

  setUserInfo(userInfo) {
    this.globalData.userInfo = userInfo
    wx.setStorageSync('userInfo', userInfo)
  },

  clearAuth() {
    this.globalData.token = null
    this.globalData.userInfo = null
    wx.removeStorageSync('token')
    wx.removeStorageSync('userInfo')
  }
})
