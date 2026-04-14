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
    interestMap: {
      historical: false,
      natural: false,
      cultural: false,
      shopping: false,
      food: false
    },
    canSubmit: false
  },

  onLoad(options) {
    if (options.destination) {
      this.setData({
        'formData.destination': decodeURIComponent(options.destination)
      })
    }
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
    const interests = [...this.data.formData.preferences.interests]
    const index = interests.indexOf(interest)

    if (index >= 0) {
      interests.splice(index, 1)
    } else {
      interests.push(interest)
    }

    this.setData({
      'formData.preferences.interests': interests,
      [`interestMap.${interest}`]: index < 0
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
    this.setData({
      canSubmit: !!(destination && startDate && endDate)
    })
  },

  async submitPlan() {
    if (!this.data.canSubmit) {
      wx.showToast({
        title: '请先补全必填信息',
        icon: 'none'
      })
      return
    }

    wx.showLoading({
      title: '提交中...'
    })

    try {
      const result = await api.createPlan({
        destination: this.data.formData.destination,
        startDate: this.data.formData.startDate,
        endDate: this.data.formData.endDate,
        travelerCount: this.data.formData.travelerCount,
        budget: this.data.formData.budget ? Number(this.data.formData.budget) : null,
        preferences: this.data.formData.preferences
      })

      wx.hideLoading()
      wx.navigateTo({
        url: `/pages/planning/planning?taskId=${result.taskId}`
      })
    } catch (err) {
      wx.hideLoading()
      console.error('Submit plan failed:', err)
    }
  }
})
