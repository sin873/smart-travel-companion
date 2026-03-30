# 派聪明（PaiSmart）项目指南

## 项目概述

派聪明（PaiSmart）是一个企业级的 AI 知识库管理系统，采用检索增强生成（RAG）技术，提供智能文档处理和检索能力。

### 核心技术栈

**后端：**
- 框架：Spring Boot 3.4.2 (Java 17)
- 数据库：MySQL 8.0
- ORM：Spring Data JPA
- 缓存：Redis
- 搜索引擎：Elasticsearch 8.10.0
- 消息队列：Apache Kafka
- 文件存储：MinIO
- 文档解析：Apache Tika
- 安全认证：Spring Security + JWT
- AI 集成：DeepSeek API/本地 Ollama + 豆包 Embedding
- 实时通信：WebSocket
- 依赖管理：Maven
- 响应式编程：WebFlux

**前端：**
- 框架：Vue 3 + TypeScript
- 构建工具：Vite
- UI 组件：Naive UI
- 状态管理：Pinia
- 路由：Vue Router
- 样式：UnoCSS + SCSS
- 图标：Iconify
- 包管理：pnpm

## 项目结构

### 后端结构
```
src/main/java/com/yizhaoqi/smartpai/
├── SmartPaiApplication.java      # 主应用程序入口
├── client/                        # 外部 API 客户端
├── config/                        # 配置类
├── consumer/                      # Kafka 消费者
├── controller/                    # REST API 端点
├── entity/                        # 数据实体
├── exception/                     # 自定义异常
├── handler/                       # WebSocket 处理器
├── model/                         # 领域模型
├── repository/                    # 数据访问层
├── service/                       # 业务逻辑
├── test/                          # 测试代码
└── utils/                         # 工具类
```

### 前端结构
```
frontend/
├── packages/           # 可重用模块
├── public/             # 静态资源
├── src/                # 主应用程序代码
│   ├── assets/         # SVG 图标，图片
│   ├── components/     # Vue 组件
│   ├── layouts/        # 页面布局
│   ├── router/         # 路由配置
│   ├── service/        # API 集成
│   ├── store/          # 状态管理
│   ├── views/          # 页面组件
│   └── ...             # 其他工具和配置
└── ...                 # 构建配置文件
```

## 构建与运行

### 前置环境要求

- Java 17
- Maven 3.8.6 或更高版本
- Node.js 18.20.0 或更高版本
- pnpm 8.7.0 或更高版本
- MySQL 8.0
- Elasticsearch 8.10.0
- MinIO 8.5.12
- Kafka 3.2.1
- Redis 7.0.11
- Docker（可选，用于运行 Redis、MinIO、Elasticsearch 和 Kafka 等服务）

### 后端运行

使用 Maven 构建和运行 Spring Boot 应用：

```bash
# 编译项目
mvn clean compile

# 运行应用
mvn spring-boot:run

# 打包
mvn clean package
```

### 前端运行

```bash
# 进入前端项目目录
cd frontend

# 安装依赖
pnpm install

# 启动开发服务器（测试环境）
pnpm run dev

# 启动开发服务器（生产环境）
pnpm run dev:prod

# 构建生产版本
pnpm run build

# 构建测试版本
pnpm run build:test

# 代码检查和修复
pnpm run lint

# 类型检查
pnpm run typecheck
```

## 开发规范

### ⚠️ 重要：编码方式

**绝对不要修改现有代码文件的编码方式！**

- 保持所有源文件原有的编码格式
- 不要随意转换文件的换行符（LF/CRLF）
- 保持现有文件的字符编码不变

### 代码风格

- **后端**：遵循 Spring Boot 和 Java 标准开发规范，使用 Lombok 简化代码
- **前端**：使用 ESLint 进行代码检查，遵循 Vue 3 和 TypeScript 最佳实践

### 提交规范

项目配置了 Git hooks 来确保代码质量：

```bash
# 使用中文提交信息
pnpm commit

# 使用英文提交信息
pnpm commit:en
```

### 测试

- **后端**：使用 Spring Boot Test 框架
- **前端**：TODO - 查看前端项目中的测试配置

## 核心功能

1. **知识库管理**：文档上传与解析，支持文件分片上传和断点续传，标签组织管理
2. **AI 驱动的 RAG 实现**：文档语义分块、向量存储、语义搜索和基于文档的 AI 响应
3. **企业级多租户**：通过组织标签支持多租户架构
4. **实时通信**：基于 WebSocket 的实时聊天交互

## 配置文件

关键配置文件位置：
- 后端配置：`src/main/resources/application*.yml`
- 前端环境配置：`frontend/.env`, `frontend/.env.test`, `frontend/.env.prod`
- 数据库初始化：`create-database.sql`
- Docker 配置：`docs/docker-compose.yaml`

## 更多信息

- 项目教程：https://paicoding.com/column/10/1
- 如何写到简历上：https://paicoding.com/column/10/2
