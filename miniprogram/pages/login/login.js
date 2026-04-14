const app = getApp()

Page({
  mockLogin() {
    const mockUser = {
      userId: 'demo_user_001',
      nickName: '演示用户',
      avatarUrl: ''
    }

    app.setToken(`mock_token_${Date.now()}`)
    app.setUserInfo(mockUser)

    wx.showToast({
      title: '登录成功',
      icon: 'success'
    })

    setTimeout(() => {
      wx.navigateBack({
        delta: 1
      })
    }, 800)
  },

  guestLogin() {
    app.setUserInfo({
      userId: 'guest_user',
      nickName: '游客',
      avatarUrl: ''
    })

    wx.showToast({
      title: '已进入体验模式',
      icon: 'success'
    })

    setTimeout(() => {
      wx.navigateBack({
        delta: 1
      })
    }, 800)
  }
})
