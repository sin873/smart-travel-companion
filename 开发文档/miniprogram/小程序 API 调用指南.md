# 小程序端 API 调用指南 - 修复 403 错误

## ❌ 问题原因

从日志中看到你的请求是这样的：
```
GET /api/v1/travel/plan?Content-Type=application/json&Authorization=Bearer%20%26lt;token%26gt;&sessionId=...
```

**错误点**：
1. `Authorization` token 被放在了 URL Query 参数中，而不是 HTTP Header 中
2. `Content-Type` 不应该作为查询参数
3. 请求方法可能也有问题（生成行程应该用 POST）

---

## ✅ 正确的调用方式

### 1. 生成行程规划任务

**接口**: `POST /api/v1/travel/plan`

#### ❌ 错误的调用方式
```javascript
// 错误！不要把参数放在 URL 中
wx.request({
  url: 'http://localhost:8081/api/v1/travel/plan?Authorization=Bearer token123',
  method: 'GET', // 错误！应该用 POST
  data: { ... }
})
```

#### ✅ 正确的调用方式
```javascript
// 正确！使用 header 传递 token
wx.request({
  url: 'http://localhost:8081/api/v1/travel/plan',
  method: 'POST', // 使用 POST 方法
  header: {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer ' + token  // token 放在 header 中
  },
  data: {
    destination: '北京',
    startDate: '2024-05-01',
    endDate: '2024-05-03',
    travelerCount: 2,
    budget: 2000,
    preferences: {
      pace: 'moderate',
      interests: ['historical', 'food'],
      avoidCrowds: false
    }
  }
})
```

---

### 2. 查询规划状态

**接口**: `GET /api/v1/travel/plan/{taskId}`

#### ✅ 正确的调用方式
```javascript
wx.request({
  url: 'http://localhost:8081/api/v1/travel/plan/' + taskId,
  method: 'GET',
  header: {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer ' + token  // token 放在 header 中
  }
})
```

---

### 3. 保存行程

**接口**: `POST /api/v1/travel/itineraries`

#### ✅ 正确的调用方式
```javascript
wx.request({
  url: 'http://localhost:8081/api/v1/travel/itineraries',
  method: 'POST',
  header: {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer ' + token
  },
  data: {
    title: '北京 3 日经典游',
    destination: '北京',
    startDate: '2024-05-01',
    endDate: '2024-05-03',
    travelerCount: 2,
    budget: 2000,
    status: 'PLANNED',
    summary: '经典文化美食之旅',
    days: [
      {
        dayNumber: 1,
        travelDate: '2024-05-01',
        title: '第一天：故宫 - 天安门广场',
        items: [
          {
            attractionId: 'attr_001',
            attractionName: '故宫博物院',
            address: '北京市东城区景山前街 4 号',
            latitude: 39.9163,
            longitude: 116.3972,
            startTime: '09:00:00',
            endTime: '12:00:00',
            durationMinutes: 180,
            orderIndex: 1
          }
        ]
      }
    ]
  }
})
```

---

### 4. 查询行程列表

**接口**: `GET /api/v1/travel/itineraries`

#### ✅ 正确的调用方式
```javascript
wx.request({
  url: 'http://localhost:8081/api/v1/travel/itineraries',
  method: 'GET',
  header: {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer ' + token
  }
})
```

---

### 5. 查询行程详情

**接口**: `GET /api/v1/travel/itineraries/{itineraryId}`

#### ✅ 正确的调用方式
```javascript
const itineraryId = 'itinerary_xxxxxxxx';

wx.request({
  url: 'http://localhost:8081/api/v1/travel/itineraries/' + itineraryId,
  method: 'GET',
  header: {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer ' + token
  }
})
```

---

### 6. 删除行程

**接口**: `DELETE /api/v1/travel/itineraries/{itineraryId}`

#### ✅ 正确的调用方式
```javascript
const itineraryId = 'itinerary_xxxxxxxx';

wx.request({
  url: 'http://localhost:8081/api/v1/travel/itineraries/' + itineraryId,
  method: 'DELETE',
  header: {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer ' + token
  }
})
```

---

## 📝 通用封装建议

建议在小程序中封装一个统一的 API 请求函数：

```javascript
// utils/request.js
const BASE_URL = 'http://localhost:8081/api/v1';

/**
 * 封装请求，自动添加 token
 */
export function request(options) {
  const token = wx.getStorageSync('token') || '';
  
  return new Promise((resolve, reject) => {
    wx.request({
      ...options,
      url: BASE_URL + options.url,
      header: {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : '',
        ...options.header
      },
      success: (res) => {
        if (res.statusCode === 200 && res.data.code === 200) {
          resolve(res.data);
        } else if (res.statusCode === 403) {
          // token 无效或过期
          wx.removeStorageSync('token');
          wx.redirectTo({ url: '/pages/login/login' });
          reject(new Error('未授权'));
        } else {
          reject(new Error(res.data.message || '请求失败'));
        }
      },
      fail: (err) => {
        reject(err);
      }
    });
  });
}

/**
 * GET 请求
 */
export function get(url, params = {}) {
  return request({ url, method: 'GET', data: params });
}

/**
 * POST 请求
 */
export function post(url, data = {}) {
  return request({ url, method: 'POST', data });
}

/**
 * DELETE 请求
 */
export function del(url, params = {}) {
  return request({ url, method: 'DELETE', data: params });
}
```

### 使用示例

```javascript
// pages/plan/plan.js
import { post, get } from '../../utils/request';

// 生成行程规划
async createPlan() {
  try {
    const response = await post('/travel/plan', {
      destination: this.data.destination,
      startDate: this.data.startDate,
      endDate: this.data.endDate,
      travelerCount: this.data.travelerCount,
      preferences: this.data.preferences
    });
    
    console.log('任务已创建:', response.data.taskId);
    this.setData({ taskId: response.data.taskId });
    
    // 轮询查询状态
    this.queryPlanStatus(response.data.taskId);
  } catch (error) {
    wx.showToast({
      title: error.message || '创建失败',
      icon: 'none'
    });
  }
}

// 查询规划状态
async queryPlanStatus(taskId) {
  try {
    const response = await get(`/travel/plan/${taskId}`);
    
    if (response.data.status === 'COMPLETED') {
      // 规划完成
      this.setData({ itinerary: response.data.itinerary });
    } else {
      // 继续轮询
      setTimeout(() => this.queryPlanStatus(taskId), 2000);
    }
  } catch (error) {
    console.error('查询状态失败:', error);
  }
}
```

---

## 🔧 后端已修复内容

1. ✅ 添加了 URL 规范化过滤器，自动处理双斜杠问题
2. ✅ 在 SecurityConfig 中明确配置了 `/api/v1/travel/**` 需要认证
3. ✅ JWT 认证逻辑正常工作，只要 token 正确放置在 Header 中

---

## ⚠️ 重要提醒

1. **Token 存储**：将登录后的 token 存储在 `localStorage` 中
   ```javascript
   wx.setStorageSync('token', 'your_token_here');
   ```

2. **Token 格式**：Header 中的 Authorization 值必须是 `Bearer <token>` 格式

3. **请求方法**：
   - 创建/提交数据 → 使用 `POST`
   - 查询数据 → 使用 `GET`
   - 删除数据 → 使用 `DELETE`
   - 更新数据 → 使用 `PUT` 或 `PATCH`

4. **Content-Type**：永远是 `'application/json'`，不要放在 URL 中

---

## 🎯 测试步骤

1. 先登录获取 token
2. 将 token 保存到 localStorage
3. 使用上述正确的方式调用接口
4. 观察后端日志，确认请求被正确处理

现在请按照正确的方式修改你的小程序代码！🚀
