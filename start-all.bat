@echo off
chcp 936 >nul
title 公寓租房系统Demo - 一键启动

set ROOT=%~dp0
rem 便携版 MySQL 目录（如安装在其他位置请修改此行）
set MYSQL_HOME=D:\AI-code\mysql-8.0.29

echo ==============================================
echo      公寓租房系统Demo - 一键启动
echo ==============================================
echo.

rem ---- 1. 启动便携版 MySQL（3306 端口） ----
if not exist "%MYSQL_HOME%\bin\mysqld.exe" (
    echo [警告] 未找到便携版 MySQL：%MYSQL_HOME%
    echo        后端将无法启动，请安装 MySQL 或修改脚本中的 MYSQL_HOME。
    echo.
) else (
    netstat -an | findstr ":3306" | findstr "LISTENING" >nul
    if errorlevel 1 (
        echo [启动] 便携版 MySQL 8.0.29 ...
        start "mysql-8.0.29" /D "%MYSQL_HOME%\bin" cmd /k "mysqld --defaults-file=%MYSQL_HOME%\my.ini --console"
        echo 等待 MySQL 就绪...
        for /l %%i in (1,1,15) do (
            netstat -an | findstr ":3306" | findstr "LISTENING" >nul && goto mysql_ready
            timeout /t 2 /nobreak >nul
        )
        echo [错误] MySQL 启动超时，请查看 mysql 窗口日志后重试！
        pause
        exit /b 1
    ) else (
        echo [OK] MySQL 已在运行
    )
)
:mysql_ready
echo [OK] MySQL 已就绪
echo.

rem ---- 2. 启动后端 rental-server（端口 8080） ----
if exist "%ROOT%rental-server\mvnw.cmd" (
    echo [启动] 后端 rental-server ^(端口 8080^) ...
    start "rental-server" /D "%ROOT%rental-server" cmd /k "mvnw.cmd spring-boot:run"
) else (
    where mvn >nul 2>nul
    if not errorlevel 1 (
        echo [启动] 后端 rental-server ^(端口 8080^) ...
        start "rental-server" /D "%ROOT%rental-server" cmd /k "mvn spring-boot:run"
    ) else (
        echo [警告] 未找到 Maven，请用 IDEA 打开 rental-server 手动运行后端！
    )
)

rem ---- 3. 启动三端前端 ----
if exist "%ROOT%rental-app\node_modules" (
    start "rental-app-用户端" /D "%ROOT%rental-app" cmd /k "npm run dev"
) else (
    start "rental-app-用户端" /D "%ROOT%rental-app" cmd /k "npm install && npm run dev"
)

if exist "%ROOT%rental-admin\node_modules" (
    start "rental-admin-管理端" /D "%ROOT%rental-admin" cmd /k "npm run dev"
) else (
    start "rental-admin-管理端" /D "%ROOT%rental-admin" cmd /k "npm install && npm run dev"
)

if exist "%ROOT%rental-bi\node_modules" (
    start "rental-bi-BI看板" /D "%ROOT%rental-bi" cmd /k "npm run dev"
) else (
    start "rental-bi-BI看板" /D "%ROOT%rental-bi" cmd /k "npm install && npm run dev"
)

rem ---- 4. 等待服务启动后打开浏览器 ----
echo.
echo 正在等待服务启动...
timeout /t 8 /nobreak >nul
start http://localhost:5173
start http://localhost:5174
start http://localhost:5175

echo.
echo ==============================================
echo   启动完成！
echo.
echo   用户端    http://localhost:5173   (user / user123)
echo   管理端    http://localhost:5174   (admin / admin123)
echo   BI看板    http://localhost:5175   (admin / admin123)
echo   接口文档  http://localhost:8080/doc.html
echo.
echo   提示：后端首次启动需下载依赖并自动建表，
echo   请等待 rental-server 窗口出现 Started 后再刷新页面。
echo ==============================================
pause
