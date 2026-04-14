const app = getApp()

function request(url, method, data) {
  return new Promise((resolve, reject) => {
    const header = {
      'content-type': 'application/json'
    }

    if (app.globalData.token) {
      header.Authorization = `Bearer ${app.globalData.token}`
    }

    wx.request({
      url: `${app.globalData.baseUrl}${url}`,
      method,
      data,
      header,
      success(res) {
        const body = res.data || {}
        if (res.statusCode === 200 && body.code === 200) {
          resolve(body.data)
          return
        }

        const message = body.message || '请求失败'
        wx.showToast({
          title: message,
          icon: 'none'
        })
        reject(body)
      },
      fail(err) {
        wx.showToast({
          title: '网络连接失败',
          icon: 'none'
        })
        reject(err)
      }
    })
  })
}

module.exports = {
  createPlan(data) {
    return request('/plan', 'POST', data)
  },
  getPlanStatus(taskId) {
    return request(`/plan/${taskId}`, 'GET')
  },
  saveItinerary(data) {
    return request('/itineraries', 'POST', data)
  },
  getItineraryList() {
    return request('/itineraries', 'GET')
  },
  getItineraryDetail(itineraryId) {
    return request(`/itineraries/${itineraryId}`, 'GET')
  },
  deleteItinerary(itineraryId) {
    return request(`/itineraries/${itineraryId}`, 'DELETE')
  }
}
