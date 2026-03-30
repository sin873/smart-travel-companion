# 旅游规划模块 - API接口契约

> ⚠️ **重要**: 本文档在T+0.5小时后冻结，所有窗口必须严格按照此契约开发！

---

## 📋 变更记录

| 版本 | 时间 | 修改人 | 变更内容 |
|------|------|--------|---------|
| v1.0 | T+0 | 窗口1 | 初始版本 |

---

## 🔗 统一响应格式

**所有接口必须遵循此格式！**

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1714567890123
}
```

**字段说明**:
- `code`: 200=成功，其他=失败
- `message`: 成功时为"success"，失败时为错误信息
- `data`: 响应数据，失败时可为null
- `timestamp`: Unix时间戳（毫秒）

---

## 🎯 接口清单

### 1. 生成行程

**接口**: `POST /api/v1/travel/plan`

**描述**: 提交行程需求，触发生成任务

**Request Body**:
```json
{
  "destination": "北京",
  "startDate": "2024-05-01",
  "endDate": "2024-05-03",
  "travelerCount": 2,
  "budget": 2000,
  "preferences": {
    "pace": "moderate",
    "interests": ["historical", "food"],
    "avoidCrowds": false
  }
}
```

**Request字段说明**:
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| destination | String | 是 | 目的地，如"北京" |
| startDate | String | 是 | 开始日期，格式YYYY-MM-DD |
| endDate | String | 是 | 结束日期，格式YYYY-MM-DD |
| travelerCount | Integer | 是 | 出行人数，默认2 |
| budget | Decimal | 否 | 预算，单位元 |
| preferences.pace | String | 否 | 节奏：relaxed/moderate/intensive |
| preferences.interests | String[] | 否 | 兴趣：historical/natural/cultural/shopping/food |
| preferences.avoidCrowds | Boolean | 否 | 是否避开人群 |

**Response**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "taskId": "task_abc123xyz",
    "status": "QUEUED",
    "estimatedTime": 15
  },
  "timestamp": 1714567890123
}
```

**Response字段说明**:
| 字段 | 类型 | 说明 |
|------|------|------|
| taskId | String | 任务ID，用于后续查询状态 |
| status | String | 任务状态：QUEUED/PROCESSING/COMPLETED/FAILED |
| estimatedTime | Integer | 预计耗时，单位秒 |

---

### 2. 查询规划状态

**接口**: `GET /api/v1/travel/plan/{taskId}`

**描述**: 查询行程生成任务的进度和结果

**Path参数**:
| 参数 | 类型 | 说明 |
|------|------|------|
| taskId | String | 任务ID |

**Response (QUEUED/PROCESSING状态)**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "taskId": "task_abc123xyz",
    "status": "PROCESSING",
    "progress": 65,
    "message": "正在规划路线...",
    "itinerary": null
  },
  "timestamp": 1714567890123
}
```

**Response (COMPLETED状态)**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "taskId": "task_abc123xyz",
    "status": "COMPLETED",
    "progress": 100,
    "message": "行程规划完成！",
    "itinerary": {
      "itineraryId": "temp_xyz789",
      "title": "北京3日经典游",
      "destination": "北京",
      "startDate": "2024-05-01",
      "endDate": "2024-05-03",
      "travelerCount": 2,
      "budget": 2000,
      "status": "PLANNED",
      "days": [
        {
          "dayId": "day_1",
          "dayNumber": 1,
          "date": "2024-05-01",
          "title": "故宫深度游",
          "notes": "第一天主要游览故宫区域",
          "attractions": [
            {
              "itemId": "item_1",
              "attractionId": "gugong",
              "attractionName": "故宫博物院",
              "address": "北京市东城区景山前街4号",
              "latitude": 39.9163,
              "longitude": 116.3972,
              "startTime": "09:00:00",
              "endTime": "12:00:00",
              "durationMinutes": 180,
              "notes": "建议游览3小时，提前网上预约"
            }
          ]
        }
      ]
    }
  },
  "timestamp": 1714567890123
}
```

**Response (FAILED状态)**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "taskId": "task_abc123xyz",
    "status": "FAILED",
    "progress": 0,
    "message": "规划失败：暂无该目的地数据",
    "itinerary": null
  },
  "timestamp": 1714567890123
}
```

**status枚举**:
- `QUEUED`: 排队中
- `PROCESSING`: 处理中
- `COMPLETED`: 已完成
- `FAILED`: 失败

**progress字段**: 0-100的整数

---

### 3. 保存行程

**接口**: `POST /api/v1/travel/itineraries`

**描述**: 将生成的行程保存到数据库

**Request Body**:
```json
{
  "itineraryId": "temp_xyz789",
  "title": "北京3日经典游",
  "destination": "北京",
  "startDate": "2024-05-01",
  "endDate": "2024-05-03",
  "travelerCount": 2,
  "budget": 2000,
  "status": "PLANNED",
  "days": [
    {
      "dayId": "day_1",
      "dayNumber": 1,
      "date": "2024-05-01",
      "title": "故宫深度游",
      "notes": "第一天主要游览故宫区域",
      "attractions": [
        {
          "itemId": "item_1",
          "attractionId": "gugong",
          "attractionName": "故宫博物院",
          "address": "北京市东城区景山前街4号",
          "latitude": 39.9163,
          "longitude": 116.3972,
          "startTime": "09:00:00",
          "endTime": "12:00:00",
          "durationMinutes": 180,
          "notes": "建议游览3小时，提前网上预约"
        }
      ]
    }
  ]
}
```

**Response**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "itineraryId": "itin_saved_123"
  },
  "timestamp": 1714567890123
}
```

---

### 4. 行程列表

**接口**: `GET /api/v1/travel/itineraries`

**描述**: 获取当前用户的行程列表

**Response**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 5,
    "items": [
      {
        "itineraryId": "itin_saved_123",
        "title": "北京3日经典游",
        "destination": "北京",
        "startDate": "2024-05-01",
        "endDate": "2024-05-03",
        "travelerCount": 2,
        "budget": 2000,
        "status": "PLANNED",
        "createdAt": "2024-04-25T10:30:00"
      }
    ]
  },
  "timestamp": 1714567890123
}
```

**status枚举**:
- `DRAFT`: 草稿
- `PLANNED`: 已规划
- `COMPLETED`: 已完成

---

### 5. 行程详情

**接口**: `GET /api/v1/travel/itineraries/{itineraryId}`

**描述**: 获取单个行程的完整详情

**Path参数**:
| 参数 | 类型 | 说明 |
|------|------|------|
| itineraryId | String | 行程ID |

**Response**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "itineraryId": "itin_saved_123",
    "title": "北京3日经典游",
    "destination": "北京",
    "startDate": "2024-05-01",
    "endDate": "2024-05-03",
    "travelerCount": 2,
    "budget": 2000,
    "status": "PLANNED",
    "days": [
      {
        "dayId": "day_1",
        "dayNumber": 1,
        "date": "2024-05-01",
        "title": "故宫深度游",
        "notes": "第一天主要游览故宫区域",
        "attractions": [
          {
            "itemId": "item_1",
            "attractionId": "gugong",
            "attractionName": "故宫博物院",
            "address": "北京市东城区景山前街4号",
            "latitude": 39.9163,
            "longitude": 116.3972,
            "startTime": "09:00:00",
            "endTime": "12:00:00",
            "durationMinutes": 180,
            "notes": "建议游览3小时，提前网上预约"
          }
        ]
      }
    ]
  },
  "timestamp": 1714567890123
}
```

---

### 6. 删除行程

**接口**: `DELETE /api/v1/travel/itineraries/{itineraryId}`

**描述**: 删除指定的行程

**Path参数**:
| 参数 | 类型 | 说明 |
|------|------|------|
| itineraryId | String | 行程ID |

**Response**:
```json
{
  "code": 200,
  "message": "success",
  "data": null,
  "timestamp": 1714567890123
}
```

---

## 📊 数据模型定义

### ItineraryDTO（行程主表）

```typescript
interface ItineraryDTO {
  itineraryId?: string;        // 行程ID（保存后有）
  title: string;               // 行程标题
  destination: string;         // 目的地
  startDate: string;           // 开始日期 (YYYY-MM-DD)
  endDate: string;             // 结束日期 (YYYY-MM-DD)
  travelerCount: number;       // 出行人数
  budget?: number;             // 预算
  status?: string;             // 状态: DRAFT/PLANNED/COMPLETED
  days: ItineraryDayDTO[];     // 每日行程列表
  createdAt?: string;          // 创建时间（列表页返回）
}
```

### ItineraryDayDTO（每日行程）

```typescript
interface ItineraryDayDTO {
  dayId?: string;              // 每日行程ID
  dayNumber: number;           // 第几天 (1,2,3...)
  date: string;                // 日期 (YYYY-MM-DD)
  title?: string;              // 当日主题
  notes?: string;              // 备注
  attractions: ItineraryAttractionDTO[];  // 景点列表
}
```

### ItineraryAttractionDTO（行程景点）

```typescript
interface ItineraryAttractionDTO {
  itemId?: string;             // 行程项ID
  attractionId?: string;       // 景点ID
  attractionName: string;      // 景点名称
  address?: string;            // 地址
  latitude?: number;           // 纬度
  longitude?: number;          // 经度
  startTime?: string;          // 开始时间 (HH:mm:ss)
  endTime?: string;            // 结束时间 (HH:mm:ss)
  durationMinutes?: number;    // 预计时长（分钟）
  notes?: string;              // 备注
}
```

---

## 🔐 认证说明

**认证方式**: 复用现有JWT认证

**Header**:
```
Authorization: Bearer <your_jwt_token>
```

**简化策略**: 6小时版本中，如果没有token，可以使用固定的演示用户ID: `demo_user_001`

---

## ⚠️ 注意事项

1. **日期格式**: 所有日期必须使用 `YYYY-MM-DD` 格式
2. **时间格式**: 所有时间必须使用 `HH:mm:ss` 格式
3. **经纬度**: 使用十进制格式，保留7位小数
4. **ID生成**: 所有ID建议使用 `前缀_随机字符串` 格式
5. **进度更新**: `PROCESSING`状态时，建议每2-3秒更新一次进度
6. **Mock数据**: 窗口3必须内置北京、上海、杭州、成都的Mock景点数据

---

**此文档T+0.5小时后冻结，如有问题请及时沟通！**
