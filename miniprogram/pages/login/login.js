// pages/login/login.js
const auth = require('../../utils/auth.js')
const app = getApp()

Page({
  data: {

  },

  onLoad() {

  },

  async onGetUserInfo(e) {
    if (e.detail.userInfo) {
      wx.showLoading({
        title: '登录中...'
      })

      try {
        // 模拟微信登录
        await this.mockLogin()
        
        wx.hideLoading()
        wx.showToast({
          title: '登录成功',
          icon: 'success'
        })

        setTimeout(() => {
          wx.switchTab({
            url: '/pages/index/index'
          })
        }, 1500)
      } catch (err) {
        wx.hideLoading()
        wx.showToast({
          title: '登录失败',
          icon: 'none'
        })
      }
    }
  },

  async mockLogin() {
    // 模拟登录,创建一个演示用户
    const mockUser = {
      userId: 'demo_user_001',
      nickName: '智能伴旅体验官',
      avatarUrl: ''
    }

    const mockToken = 'mock_token_' + Date.now()

    app.setToken(mockToken)
    app.setUserInfo(mockUser)
  },

  guestLogin() {
    // 游客模式,直接进入首页
    const guestUser = {
      userId: 'guest_user',
      nickName: '游客',
      avatarUrl: ''
    }

    app.setUserInfo(guestUser)
    
    wx.switchTab({
      url: '/pages/index/index'
    })
  }
})
