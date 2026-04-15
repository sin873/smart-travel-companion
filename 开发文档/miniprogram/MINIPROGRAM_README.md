# 智能伴旅小程序 - 快速入门指南

## 📱 项目概述

智能伴旅小程序是一个基于AI Agent的个性化行程规划应用,通过大模型和RAG知识库为用户提供智能的旅行行程定制服务。

## 🗂️ 项目结构

```
PaiSmart-main/
├── miniprogram/                    # 小程序前端
│   ├── pages/                      # 页面
│   │   ├── index/                  # 首页
│   │   ├── login/                  # 登录页
│   │   ├── plan/                   # 行程定制页
│   │   ├── planning/               # 规划进度页
│   │   ├── itinerary/              # 行程展示页
│   │   └── my-itineraries/         # 我的行程页
│   ├── utils/                      # 工具类
│   │   ├── api.js                  # API请求封装
│   │   └── auth.js                 # 认证工具
│   ├── app.js                      # 小程序入口
│   ├── app.json                    # 小程序配置
│   └── project.config.json         # 项目配置
├── src/main/java/com/yizhaoqi/smartpai/
│   ├── entity/miniprogram/         # 小程序实体类
│   ├── repository/miniprogram/     # 小程序数据访问
│   ├── service/miniprogram/        # 小程序业务逻辑
│   │   ├── TravelAgentService.java # AI行程规划Agent
│   │   └── MiniprogramAuthService.java # 小程序认证
│   └── controller/miniprogram/     # 小程序API控制器
├── 开发文档/
│   └── 小程序开发文档.md           # 详细开发文档
└── create-miniprogram-database.sql # 数据库扩展脚本
```

## 🚀 快速开始

### 1. 数据库初始化

首先执行数据库扩展脚本:

```bash
mysql -u root -p < create-miniprogram-database.sql
```

### 2. 后端配置

确保application.yml中添加微信小程序配置(可选):

```yaml
wechat:
  miniprogram:
    appId: your_app_id
    appSecret: your_app_secret
```

### 3. 启动后端服务

```bash
mvn spring-boot:run
```

### 4. 打开小程序

使用微信开发者工具打开`miniprogram`目录,即可开始开发和调试。

## 🔑 核心功能

### 1. AI行程规划Agent

`TravelAgentService` 是核心的AI规划服务,主要功能:

- **景点检索**: 基于目的地检索相关景点信息
- **智能筛选**: 根据用户偏好筛选景点
- **路线优化**: 基于地理位置优化每日行程
- **时间安排**: 智能分配各景点时间
- **异步处理**: 支持异步规划,实时进度反馈

### 2. 小程序页面

| 页面 | 功能 |
|------|------|
| index | 首页,快捷入口,热门目的地 |
| login | 微信登录,游客模式 |
| plan | 行程定制表单,用户需求收集 |
| planning | 规划进度展示,步骤指示器 |
| itinerary | 行程详情,时间轴展示 |
| my-itineraries | 我的行程列表,统计信息 |

## 📡 API接口

### 小程序专用API (prefix: `/api/v1/miniprogram`)

#### 认证相关
- `POST /auth/login` - 微信小程序登录

#### 行程规划相关
- `POST /itinerary/plan` - 创建行程规划任务
- `GET /itinerary/plan/{taskId}` - 查询规划任务状态
- `POST /itinerary` - 保存行程
- `GET /itinerary` - 获取行程列表
- `GET /itinerary/{itineraryId}` - 获取行程详情

## 🎨 技术栈

### 后端
- **框架**: Spring Boot 3.4.2
- **数据库**: MySQL 8.0
- **ORM**: Spring Data JPA
- **AI集成**: DeepSeek API (可扩展)

### 小程序前端
- **框架**: 微信小程序原生
- **样式**: 自定义渐变主题
- **状态管理**: 小程序全局App + Storage

## 🔧 配置说明

### 小程序baseUrl配置

在 `miniprogram/app.js` 中修改后端地址:

```javascript
globalData: {
  baseUrl: 'http://localhost:8080/api/v1/miniprogram'
}
```

### 开发环境配置

在微信开发者工具中:
1. 打开"详情" -> "本地设置"
2. 勾选"不校验合法域名、web-view(业务域名)、TLS版本以及HTTPS证书"

## 📝 开发说明

### 添加新的景点数据源

目前使用模拟数据,实际项目中可以:

1. 集成RAG知识库检索
2. 连接第三方景点API
3. 从数据库加载景点信息

修改 `TravelAgentService.java` 中的 `searchAttractions` 方法:

```java
private List<AttractionInfo> searchAttractions(String destination) {
    // TODO: 从RAG知识库或API检索真实景点数据
    return yourRealDataSource(destination);
}
```

### 扩展AI能力

可以集成更多AI能力:

- 自然语言理解用户需求
- 智能推荐算法
- 多目标优化
- 实时路况集成

## 📚 相关文档

- [小程序开发文档.md](小程序开发文档.md) - 详细API和架构文档
- [智能伴旅开发文档.md](智能伴旅开发文档.md) - 整体项目文档

## 🤝 贡献指南

1. 从 `feature/miniprogram-agent` 分支创建新分支
2. 提交更改
3. 创建 Pull Request

## 📄 License

本项目基于派聪明项目扩展开发。

---

**祝你旅途愉快! ✈️🌍**
