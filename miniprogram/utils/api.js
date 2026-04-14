const app = getApp()

function request(url, method = 'GET', data = {}) {
  return new Promise((resolve, reject) => {
    const header = {
      'content-type': 'application/json'
    }

    if (app.globalData.token) {
      header.Authorization = `Bearer ${app.globalData.token}`
    }

    wx.request({
      url: app.globalData.baseUrl + url,
      method,
      data,
      header,
      success: (res) => {
        if (res.statusCode === 200 && res.data && res.data.code === 200) {
          resolve(res.data.data)
          return
        }

        const message = (res.data && res.data.message) || '请求失败'
        wx.showToast({
          title: message,
          icon: 'none'
        })
        reject(res.data || res)
      },
      fail: (err) => {
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
  get: (url, data) => request(url, 'GET', data),
  post: (url, data) => request(url, 'POST', data),
  delete: (url, data) => request(url, 'DELETE', data),
  createPlan: (data) => request('/plan', 'POST', data),
  getPlanStatus: (taskId) => request(`/plan/${taskId}`, 'GET'),
  saveItinerary: (data) => request('/itineraries', 'POST', data),
  getItineraryList: () => request('/itineraries', 'GET'),
  getItineraryDetail: (itineraryId) => request(`/itineraries/${itineraryId}`, 'GET'),
  deleteItinerary: (itineraryId) => request(`/itineraries/${itineraryId}`, 'DELETE')
}
