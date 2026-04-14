const app = getApp()

Page({
  data: {
    nickname: '演示用户'
  },

  onNicknameInput(event) {
    this.setData({
      nickname: event.detail.value
    })
  },

  useDemoLogin() {
    const userInfo = {
      nickName: this.data.nickname || '演示用户',
      avatarUrl: ''
    }

    app.setUserSession('demo-token', userInfo)

    wx.showToast({
      title: '已登录',
      icon: 'success'
    })

    setTimeout(() => {
      wx.navigateBack({ delta: 1 })
    }, 300)
  },

  clearLogin() {
    app.clearUserSession()
    wx.showToast({
      title: '已退出',
      icon: 'success'
    })
  }
})
