// utils/auth.js
const api = require('./api.js')
const app = getApp()

function wxLogin() {
  return new Promise((resolve, reject) => {
    wx.login({
      success: async (loginRes) => {
        if (loginRes.code) {
          try {
            // 获取用户信息
            wx.getUserProfile({
              desc: '用于完善会员资料',
              success: async (profileRes) => {
                try {
                  const result = await api.post('/auth/login', {
                    code: loginRes.code,
                    userInfo: {
                      nickName: profileRes.userInfo.nickName,
                      avatarUrl: profileRes.userInfo.avatarUrl
                    }
                  })
                  
                  app.setToken(result.token)
                  app.setUserInfo({
                    userId: result.userId,
                    nickName: profileRes.userInfo.nickName,
                    avatarUrl: profileRes.userInfo.avatarUrl
                  })
                  
                  resolve(result)
                } catch (err) {
                  reject(err)
                }
              },
              fail: async () => {
                // 用户拒绝授权,仅使用code登录
                try {
                  const result = await api.post('/auth/login', {
                    code: loginRes.code
                  })
                  
                  app.setToken(result.token)
                  resolve(result)
                } catch (err) {
                  reject(err)
                }
              }
            })
          } catch (err) {
            reject(err)
          }
        } else {
          reject(new Error('获取登录凭证失败'))
        }
      },
      fail: (err) => {
        reject(err)
      }
    })
  })
}

module.exports = {
  wxLogin
}
