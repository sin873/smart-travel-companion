# 智能伴旅 - Windows 启动指南

## 📋 前置条件

确保已安装以下软件：

- [ ] JDK 17+
- [ ] Maven 3.8+
- [ ] Docker Desktop for Windows
- [ ] Node.js 18+ (前端开发需要)
- [ ] pnpm 8+ (前端开发需要)

---

## 🚀 快速启动（3步）

### 第一步：启动基础设施服务

```powershell
# 进入项目目录
cd C:\Users\sin\IdeaProjects\PaiSmart-main\PaiSmart-main

# 使用Docker Compose启动所有服务
docker-compose -f docs/docker-compose-windows.yaml up -d
```

**等待所有服务启动完成（约2-3分钟）**

检查服务状态：
```powershell
docker-compose -f docs/docker-compose-windows.yaml ps
```

服务端口映射：
| 服务 | 端口 | 说明 |
|------|------|------|
| MySQL | 3306 | 数据库 |
| Redis | 6379 | 缓存 |
| MinIO | 9000/9001 | 文件存储(9001是控制台) |
| Kafka | 9092 | 消息队列 |
| Elasticsearch | 9201 | 搜索引擎 |

### 第二步：初始化数据库

```powershell
# 方式一：使用MySQL客户端执行
mysql -u root -p1234xyzzz < create-database.sql

# 方式二：使用Docker执行
docker exec -i pai_smart_mysql mysql -u root -p1234xyzzz paismart < create-database.sql
```

### 第三步：启动后端应用

```powershell
# 设置Windows环境变量（可选）
$env:SPRING_PROFILES_ACTIVE="windows"

# 使用Maven启动（推荐，支持热部署）
mvn spring-boot:run -Dspring-boot.run.profiles=windows

# 或者先打包再运行
mvn clean package -DskipTests
java -jar target/SmartPAI-0.0.1-SNAPSHOT.jar --spring.profiles.active=windows
```

**启动成功标志：**
```
Started SmartPaiApplication in X.XXX seconds
```

访问后端：http://localhost:8081

---

## 🔧 热部署配置（开发必备）

### IDEA配置（推荐）

1. **启用自动编译**
   - File → Settings → Build, Execution, Deployment → Compiler
   - 勾选 ✅ Build project automatically

2. **启用运行时自动编译**
   - 按 `Ctrl + Shift + Alt + /` → Registry
   - 勾选 ✅ `compiler.automake.allow.when.app.running`

3. **配置DevTools**
   - pom.xml已添加devtools依赖
   - application-windows.yml已配置devtools

4. **启动应用**
   - 运行 `SmartPaiApplication.java` 的 main 方法
   - 选择 `Edit Configurations`
   - 设置 `Active profiles: windows`

**热部署效果：**
- 修改Java代码 → 自动重启（约3-5秒）
- 修改静态资源 → 自动刷新
- 修改配置文件 → 需要手动重启

### Maven命令行热部署

```powershell
# 直接用Maven运行，已支持热部署
mvn spring-boot:run -Dspring-boot.run.profiles=windows
```

---

## 📊 服务验证检查清单

### 检查MySQL
```powershell
docker exec pai_smart_mysql mysql -u root -p1234xyzzz -e "SHOW DATABASES;"
```
应该看到 `paismart` 数据库

### 检查Redis
```powershell
docker exec pai_smart_redis redis-cli -a 123456 ping
```
应该返回 `PONG`

### 检查MinIO
浏览器访问：http://localhost:9001
- 账号：`minioadmin`
- 密码：`minioadmin`

### 检查Kafka
```powershell
docker exec pai_smart_kafka kafka-topics.sh --list --bootstrap-server localhost:9092
```

### 检查Elasticsearch
浏览器访问：http://localhost:9201
应该返回JSON格式的集群信息

---

## 🎨 前端启动（可选）

```powershell
cd frontend
pnpm install
pnpm run dev
```

访问前端：http://localhost:5173

---

## 🔍 常见问题

### 问题1：端口被占用

**解决方案：**
```powershell
# 查看端口占用
netstat -ano | findstr "3306"
netstat -ano | findstr "6379"
netstat -ano | findstr "9000"
netstat -ano | findstr "9092"
netstat -ano | findstr "9201"
netstat -ano | findstr "8081"

# 停止占用端口的进程
taskkill /PID <进程ID> /F
```

### 问题2：Docker服务启动失败

**解决方案：**
```powershell
# 查看服务日志
docker-compose -f docs/docker-compose-windows.yaml logs mysql
docker-compose -f docs/docker-compose-windows.yaml logs redis

# 重启所有服务
docker-compose -f docs/docker-compose-windows.yaml restart

# 完全重置（删除数据）
docker-compose -f docs/docker-compose-windows.yaml down -v
docker-compose -f docs/docker-compose-windows.yaml up -d
```

### 问题3：连接数据库失败

**检查：**
1. MySQL容器是否启动：`docker ps | findstr mysql`
2. 密码是否正确：`1234xyzzz`
3. 数据库是否存在：手动连接MySQL确认

### 问题4：热部署不生效

**IDEA用户检查：**
1. ✅ File → Settings → Compiler → Build project automatically
2. ✅ Registry → compiler.automake.allow.when.app.running
3. ✅ 使用Debug模式运行（更快）

---

## 🛑 停止服务

```powershell
# 停止后端（Ctrl+C）

# 停止Docker服务（保留数据）
docker-compose -f docs/docker-compose-windows.yaml stop

# 停止并删除容器（保留数据卷）
docker-compose -f docs/docker-compose-windows.yaml down

# 完全清理（删除所有数据）
docker-compose -f docs/docker-compose-windows.yaml down -v
```

---

## 📝 开发提示

1. **使用Windows配置文件**：确保启动时profile设置为 `windows`
2. **数据库密码**：默认是 `1234xyzzz`，如需修改同步修改配置
3. **Redis密码**：默认是 `123456`
4. **Elasticsearch端口**：Windows下使用 `9201`，避免冲突
5. **日志查看**：IDEA控制台或 `logs/` 目录

---

**祝你开发顺利！🎉**
