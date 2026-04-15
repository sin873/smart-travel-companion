@echo off
chcp 65001 >nul
echo ========================================
echo 启动开发环境服务
echo ========================================
echo.

set DEVELOP_DIR=D:\develop
set PROJECT_DIR=%~dp0

echo [1/5] 启动 Redis...
start "Redis Server" cmd /k "cd /d %DEVELOP_DIR%\redis && .\redis-server.exe redis.windows.conf"
timeout /t 2 /nobreak >nul

echo [2/5] 启动 MinIO...
start "MinIO" cmd /k "cd /d %DEVELOP_DIR%\minio-2025-04-22-win-x64 && .\minio.exe server D:\develop\minio-data --console-address :9001"
timeout /t 2 /nobreak >nul

echo [3/5] 启动 Kafka...
start "Kafka" cmd /k "cd /d %DEVELOP_DIR%\kafka_2.13-3.9.0 && .\bin\windows\kafka-server-start.bat .\config\kraft\server.properties"
timeout /t 2 /nobreak >nul

echo [4/5] 启动 Elasticsearch...
start "Elasticsearch" cmd /k "cd /d %DEVELOP_DIR%\elasticsearch-8.10.0-windows-x86_64\elasticsearch-8.10.0 && .\bin\elasticsearch.bat"
timeout /t 2 /nobreak >nul

echo [5/5] 启动知识库前端 Web...
start "Frontend Web" cmd /k "cd /d %PROJECT_DIR%frontend && pnpm run dev"
timeout /t 2 /nobreak >nul

echo.
echo ========================================
echo 所有服务正在启动中...
echo ========================================
echo.
echo 服务端口信息:
echo   - Redis:          6379
echo   - MinIO API:      9000
echo   - MinIO Console:  9001
echo   - Kafka:          9092
echo   - Elasticsearch:  9200
echo   - Frontend Web:   由 Vite 启动后输出
echo.
echo 提示: 每个服务都在独立窗口运行
echo       关闭窗口即可停止对应服务
echo ========================================
echo.
pause
