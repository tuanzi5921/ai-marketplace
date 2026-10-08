@echo off
chcp 65001 >nul
echo ========================================
echo   AI Marketplace - Git 同步脚本
echo ========================================
echo.

cd /d "%~dp0"

echo [1/4] 检查 git 状态...
git status --short
echo.

echo [2/4] 添加所有改动...
git add -A
echo.

echo [3/4] 提交更改...
git commit -m "feat: 修复登录bug, 合并前后台, 完善业务流程" -m "- 修复 AuthService.createUser 中 wecomUserid 唯一约束冲突导致登录失败" -m "- 修复 JwtService 角色解析未 trim 的问题" -m "- 新增 USER 普通用户角色, 完善权限矩阵" -m "- 合并前后台为统一前端, 实现基于角色的路由和导航" -m "- 实现完整业务流程: 提交/审核/评分/排行榜" -m "- 新增 StorageService 本地文件存储" -m "- 修复前端 TypeScript 类型错误"
echo.

if errorlevel 1 (
    echo.
    echo [!] 提交失败，请检查上面的错误信息
    pause
    exit /b 1
)

echo [4/4] 推送到 GitHub (origin/main)...
git push origin main
echo.

if errorlevel 1 (
    echo.
    echo [!] 推送失败，请检查网络连接和 GitHub 权限
    echo     提示: 如果需要用户名密码，请使用 GitHub Personal Access Token
    pause
    exit /b 1
)

echo.
echo ========================================
echo   ✓ 同步完成！
echo ========================================
pause
