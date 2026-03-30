// app.js
App({
  onLaunch() {
    // 展示本地存储能力
    const logs = wx.getStorageSync('logs') || []
    logs.unshift(Date.now())
    wx.setStorageSync('logs', logs)

    // 检查登录状态
    this.checkLoginStatus()
  },

  globalData: {
    userInfo: null,
    token: null,
    baseUrl: 'http://localhost:8080/api/v1/miniprogram'
  },

  checkLoginStatus() {
    const token = wx.getStorageSync('token')
    if (token) {
      this.globalData.token = token
      const userInfo = wx.getStorageSync('userInfo')
      if (userInfo) {
        this.globalData.userInfo = userInfo
      }
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
