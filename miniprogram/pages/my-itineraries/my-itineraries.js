// pages/my-itineraries/my-itineraries.js
const api = require('../../utils/api.js')
const app = getApp()

Page({
  data: {
    userInfo: null,
    itineraries: [],
    stats: {
      total: 0,
      planned: 0,
      completed: 0
    },
    deletingItineraryId: null // 防止重复点击
  },

  onLoad() {
    console.log('【my-itineraries】页面 onLoad')
    console.log('【my-itineraries】app.globalData.userInfo:', app.globalData.userInfo)

    this.setData({
      userInfo: app.globalData.userInfo
    })

    // 直接加载，使用 demo_user_001
    this.loadItineraries()
  },

  testClick() {
    console.log('【my-itineraries】testClick 被点击了！')
    wx.showToast({
      title: '测试按钮点击成功！',
      icon: 'success'
    })
  },

  onShow() {
    this.setData({
      userInfo: app.globalData.userInfo
    })

    // 直接加载，使用 demo_user_001
    this.loadItineraries()
  },

  async loadItineraries() {
    try {
      const result = await api.getItineraryList()

      const itineraries = result.items || []
      const stats = {
        total: itineraries.length,
        planned: itineraries.filter(i => i.status === 'DRAFT' || i.status === 'PLANNED').length,
        completed: itineraries.filter(i => i.status === 'COMPLETED').length
      }

      this.setData({ itineraries, stats })
    } catch (err) {
      console.error('Load itineraries failed:', err)
    }
  },

  statusText(status) {
    const map = {
      'DRAFT': '草稿',
      'PLANNED': '已规划',
      'COMPLETED': '已完成'
    }
    return map[status] || status
  },

  formatDateRange(start, end) {
    if (!start || !end) return ''
    const startDate = new Date(start)
    const endDate = new Date(end)
    return `${startDate.getMonth() + 1}.${startDate.getDate()} - ${endDate.getMonth() + 1}.${endDate.getDate()}`
  },

  viewItinerary(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({
      url: `/pages/itinerary/itinerary?mode=saved&itineraryId=${id}`
    })
  },

  async deleteItinerary(e) {
    e.stopPropagation()
    const id = e.currentTarget.dataset.id

    console.log('【my-itineraries】点击删除按钮，itineraryId:', id)

    // 防止重复点击
    if (this.data.deletingItineraryId === id) {
      console.log('【my-itineraries】删除中，请勿重复点击')
      return
    }

    wx.showModal({
      title: '确认删除',
      content: '确定要删除这个行程吗？删除后无法恢复！',
      confirmColor: '#ee5a5a',
      success: async (res) => {
        if (res.confirm) {
          console.log('【my-itineraries】用户确认删除')
          this.setData({ deletingItineraryId: id })

          wx.showLoading({ title: '删除中...' })
          try {
            console.log('【my-itineraries】调用删除接口...')
            await api.deleteItinerary(id)
            console.log('【my-itineraries】删除接口调用成功')

            wx.hideLoading()
            wx.showToast({
              title: '删除成功',
              icon: 'success',
              duration: 1500
            })

            // 刷新列表
            console.log('【my-itineraries】刷新列表...')
            await this.loadItineraries()
            console.log('【my-itineraries】列表刷新完成')

          } catch (err) {
            wx.hideLoading()
            console.error('【my-itineraries】删除失败:', err)
            wx.showToast({
              title: '删除失败: ' + (err.message || '未知错误'),
              icon: 'none',
              duration: 2000
            })
          } finally {
            this.setData({ deletingItineraryId: null })
          }
        } else {
          console.log('【my-itineraries】用户取消删除')
        }
      }
    })
  },

  goToPlan() {
    wx.navigateTo({
      url: '/pages/plan/plan'
    })
  },

  goToLogin() {
    wx.navigateTo({
      url: '/pages/login/login'
    })
  }
})
