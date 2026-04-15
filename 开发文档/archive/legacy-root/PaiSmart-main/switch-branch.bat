@echo off
chcp 65001 >nul
echo ========================================
echo 切换到小程序开发分支
echo ========================================
echo.

cd /d "%~dp0"

echo [1/4] 检查当前分支...
git branch --show-current
echo.

echo [2/4] 尝试切换到 main 或 master 分支...
git checkout main >nul 2>&1
if %errorlevel% neq 0 (
    echo main 分支不存在，尝试 master...
    git checkout master >nul 2>&1
    if %errorlevel% neq 0 (
        echo 主分支切换失败，保持在当前分支
    ) else (
        echo 已切换到 master 分支
        git pull origin master
    )
) else (
    echo 已切换到 main 分支
    git pull origin main
)
echo.

echo [3/4] 检查是否已有小程序分支...
git branch -a | findstr /i "miniprogram mini-program 小程序"
if %errorlevel% equ 0 (
    echo.
    echo 发现已有的小程序分支！
    set /p choice="是否切换到现有分支？(Y/N): "
    if /i "%choice%"=="Y" (
        for /f "tokens=*" %%a in ('git branch -a ^| findstr /i "miniprogram"') do (
            set "branch=%%a"
            goto :found
        )
    ) else (
        goto :create
    )
)

:create
echo.
echo [4/4] 创建新的小程序分支...
git checkout -b feature/miniprogram-agent
if %errorlevel% equ 0 (
    echo.
    echo ✓ 成功创建并切换到 feature/miniprogram-agent 分支
    echo.
    echo 推送到远程仓库...
    git push -u origin feature/miniprogram-agent
    if %errorlevel% equ 0 (
        echo ✓ 推送成功！
    ) else (
        echo ⚠ 推送失败，但本地分支已创建
    )
) else (
    echo ✗ 创建分支失败
)
goto :end

:found
set "branch=%branch: =%"
set "branch=%branch:*=%"
echo 切换到分支：%branch%
git checkout %branch%
if %errorlevel% equ 0 (
    echo ✓ 切换成功！
) else (
    echo ✗ 切换失败
)

:end
echo.
echo ========================================
echo 当前分支：
git branch --show-current
echo ========================================
echo.
pause
