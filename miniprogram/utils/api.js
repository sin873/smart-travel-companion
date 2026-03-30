// utils/api.js
const app = getApp()

function request(url, method = 'GET', data = {}) {
  return new Promise((resolve, reject) => {
    const header = {
      'content-type': 'application/json'
    }
    
    if (app.globalData.token) {
      header['Authorization'] = 'Bearer ' + app.globalData.token
    }

    wx.request({
      url: app.globalData.baseUrl + url,
      method: method,
      data: data,
      header: header,
      success: (res) => {
        if (res.statusCode === 200) {
          if (res.data.code === 200) {
            resolve(res.data.data)
          } else {
            wx.showToast({
              title: res.data.message || '请求失败',
              icon: 'none'
            })
            reject(res.data)
          }
        } else if (res.statusCode === 401) {
          app.clearAuth()
          wx.navigateTo({
            url: '/pages/login/login'
          })
          reject(res)
        } else {
          wx.showToast({
            title: '网络错误',
            icon: 'none'
          })
          reject(res)
        }
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
  post: (url, data) => request(url, 'POST', data)
}
