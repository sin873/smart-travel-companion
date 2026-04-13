// pages/plan/plan.js
const api = require('../../utils/api.js')

Page({
  data: {
    formData: {
      destination: '',
      startDate: '',
      endDate: '',
      travelerCount: 2,
      budget: '',
      preferences: {
        pace: 'moderate',
        interests: [],
        avoidCrowds: false,
        includeMeals: true
      }
    },
    canSubmit: false
  },

  onLoad() {
    this.checkCanSubmit()
  },

  setDestination(dest) {
    this.setData({
      'formData.destination': dest
    })
    this.checkCanSubmit()
  },

  onDestinationInput(e) {
    this.setData({
      'formData.destination': e.detail.value
    })
    this.checkCanSubmit()
  },

  onStartDateChange(e) {
    this.setData({
      'formData.startDate': e.detail.value
    })
    this.checkCanSubmit()
  },

  onEndDateChange(e) {
    this.setData({
      'formData.endDate': e.detail.value
    })
    this.checkCanSubmit()
  },

  increaseCount() {
    const count = this.data.formData.travelerCount
    if (count < 20) {
      this.setData({
        'formData.travelerCount': count + 1
      })
    }
  },

  decreaseCount() {
    const count = this.data.formData.travelerCount
    if (count > 1) {
      this.setData({
        'formData.travelerCount': count - 1
      })
    }
  },

  onBudgetInput(e) {
    this.setData({
      'formData.budget': e.detail.value
    })
  },

  selectPace(e) {
    this.setData({
      'formData.preferences.pace': e.currentTarget.dataset.pace
    })
  },

  toggleInterest(e) {
    const interest = e.currentTarget.dataset.interest
    const interests = this.data.formData.preferences.interests
    const index = interests.indexOf(interest)
    
    if (index > -1) {
      interests.splice(index, 1)
    } else {
      interests.push(interest)
    }
    
    this.setData({
      'formData.preferences.interests': interests
    })
  },

  onAvoidCrowdsChange(e) {
    this.setData({
      'formData.preferences.avoidCrowds': e.detail.value
    })
  },

  onIncludeMealsChange(e) {
    this.setData({
      'formData.preferences.includeMeals': e.detail.value
    })
  },

  checkCanSubmit() {
    const { destination, startDate, endDate } = this.data.formData
    const canSubmit = destination && startDate && endDate
    this.setData({ canSubmit })
  },

  async submitPlan() {
    if (!this.data.canSubmit) {
      wx.showToast({
        title: '请填写完整信息',
        icon: 'none'
      })
      return
    }

    wx.showLoading({
      title: '提交中...'
    })

    console.log('【plan】准备提交表单数据:', this.data.formData)

    try {
      const requestData = {
        destination: this.data.formData.destination,
        startDate: this.data.formData.startDate,
        endDate: this.data.formData.endDate,
        travelerCount: this.data.formData.travelerCount,
        budget: this.data.formData.budget ? parseFloat(this.data.formData.budget) : null,
        preferences: this.data.formData.preferences
      }

      console.log('【plan】请求后端数据:', requestData)
      console.log('【plan】调用 api.createPlan()')

      const result = await api.createPlan(requestData)
      
      console.log('【plan】后端返回结果:', result)

      wx.hideLoading()
      
      // 跳转到规划进度页面
      wx.navigateTo({
        url: `/pages/planning/planning?taskId=${result.taskId}`
      })
    } catch (err) {
      wx.hideLoading()
      console.error('【plan】Submit plan failed:', err)
      console.error('【plan】Error details:', JSON.stringify(err))
      wx.showToast({
        title: '提交失败：' + (err.message || '网络错误'),
        icon: 'none'
      })
    }
  }
})
