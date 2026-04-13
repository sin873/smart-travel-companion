// pages/index/index.js
const app = getApp()

Page({
  data: {
    userInfo: null,
    destinations: [
      {
        id: 1,
        name: '北京',
        desc: '千年古都,文化之旅',
        image: 'https://picsum.photos/300/280?random=1'
      },
      {
        id: 2,
        name: '上海',
        desc: '现代都市,时尚之旅',
        image: 'https://picsum.photos/300/280?random=2'
      },
      {
        id: 3,
        name: '杭州',
        desc: '西湖美景,诗意之旅',
        image: 'https://picsum.photos/300/280?random=3'
      },
      {
        id: 4,
        name: '成都',
        desc: '美食天堂,休闲之旅',
        image: 'https://picsum.photos/300/280?random=4'
      }
    ]
  },

  onLoad() {
    this.setData({
      userInfo: app.globalData.userInfo
    })
  },

  onShow() {
    this.setData({
      userInfo: app.globalData.userInfo
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
    const dest = e.currentTarget.dataset.dest
    wx.navigateTo({
      url: '/pages/plan/plan',
      success: () => {
        const pages = getCurrentPages()
        const currentPage = pages[pages.length - 1]
        if (currentPage && currentPage.setDestination) {
          currentPage.setDestination(dest)
        }
      }
    })
  },

  showComingSoon() {
    wx.showToast({
      title: '功能开发中,敬请期待',
      icon: 'none'
    })
  }
})
