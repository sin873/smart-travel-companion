const api = require('../../utils/api.js')

const PACE_OPTIONS = [
  { value: 'relaxed', label: '休闲', active: false },
  { value: 'moderate', label: '适中', active: true },
  { value: 'intensive', label: '紧凑', active: false }
]

const INTEREST_OPTIONS = [
  { value: 'historical', label: '历史文化', active: false },
  { value: 'nature', label: '自然风光', active: false },
  { value: 'cultural', label: '人文体验', active: false },
  { value: 'shopping', label: '购物休闲', active: false },
  { value: 'food', label: '美食打卡', active: false }
]

function formatToday() {
  const now = new Date()
  const year = now.getFullYear()
  const month = `${now.getMonth() + 1}`.padStart(2, '0')
  const day = `${now.getDate()}`.padStart(2, '0')
  return `${year}-${month}-${day}`
}

function normalizeBudget(value) {
  if (!value) {
    return null
  }

  const budget = Number(value)
  if (Number.isNaN(budget) || budget <= 0) {
    return null
  }

  return budget
}

function buildPaceOptions(selectedValue) {
  return PACE_OPTIONS.map((item) => ({
    ...item,
    active: item.value === selectedValue,
    className: item.value === selectedValue ? 'tag tag-active' : 'tag'
  }))
}

function buildInterestOptions(selectedValues) {
  return INTEREST_OPTIONS.map((item) => ({
    ...item,
    active: selectedValues.includes(item.value),
    className: selectedValues.includes(item.value) ? 'tag tag-active' : 'tag'
  }))
}

Page({
  data: {
    minDate: '',
    endMinDate: '',
    paceOptions: buildPaceOptions('moderate'),
    interestOptions: buildInterestOptions([]),
    formData: {
      destination: '',
      startDate: '',
      endDate: '',
      travelerCount: 2,
      budget: '',
      pace: 'moderate',
      interests: []
    },
    canSubmit: false
  },

  onLoad(options) {
    const minDate = formatToday()
    const updates = {
      minDate,
      endMinDate: minDate
    }

    if (options.destination) {
      updates['formData.destination'] = decodeURIComponent(options.destination)
    }

    this.setData(updates)
    this.checkCanSubmit()
  },

  onDestinationInput(event) {
    this.setData({
      'formData.destination': event.detail.value
    })
    this.checkCanSubmit()
  },

  onBudgetInput(event) {
    this.setData({
      'formData.budget': event.detail.value
    })
  },

  onStartDateInput(event) {
    const value = event.detail.value

    this.setData({
      'formData.startDate': value,
      endMinDate: value || this.data.minDate
    })

    if (this.data.formData.endDate && this.data.formData.endDate < value) {
      this.setData({
        'formData.endDate': ''
      })
    }

    this.checkCanSubmit()
  },

  onEndDateInput(event) {
    this.setData({
      'formData.endDate': event.detail.value
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

  selectPace(event) {
    const value = event.currentTarget.dataset.value

    this.setData({
      'formData.pace': value,
      paceOptions: buildPaceOptions(value)
    })
  },

  toggleInterest(event) {
    const value = event.currentTarget.dataset.value
    const interests = this.data.formData.interests.slice()
    const index = interests.indexOf(value)

    if (index >= 0) {
      interests.splice(index, 1)
    } else {
      interests.push(value)
    }

    this.setData({
      'formData.interests': interests,
      interestOptions: buildInterestOptions(interests)
    })
  },

  checkCanSubmit() {
    const { destination, startDate, endDate } = this.data.formData
    const datePattern = /^\d{4}-\d{2}-\d{2}$/
    const canSubmit = !!(
      destination &&
      startDate &&
      endDate &&
      datePattern.test(startDate) &&
      datePattern.test(endDate) &&
      startDate <= endDate
    )

    this.setData({ canSubmit })
  },

  submitPlan() {
    if (!this.data.canSubmit) {
      wx.showToast({
        title: '请先完整填写信息',
        icon: 'none'
      })
      return
    }

    wx.showLoading({ title: '正在提交' })

    api.createPlan({
      destination: this.data.formData.destination,
      startDate: this.data.formData.startDate,
      endDate: this.data.formData.endDate,
      travelerCount: this.data.formData.travelerCount,
      budget: normalizeBudget(this.data.formData.budget),
      preferences: {
        pace: this.data.formData.pace,
        interests: this.data.formData.interests,
        avoidCrowds: false
      }
    }).then((result) => {
      wx.navigateTo({
        url: `/pages/planning/planning?taskId=${result.taskId}`
      })
    }).catch((err) => {
      console.error('submit plan failed', err)
    }).finally(() => {
      wx.hideLoading()
    })
  }
})
