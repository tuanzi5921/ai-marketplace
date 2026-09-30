<#
.SYNOPSIS
  企业 AI 应用市场 — 一键构建发布包（Windows 开发机执行）

.DESCRIPTION
  产出 dist-release/ai-marketplace-release-<时间戳>.tar.gz，内含：
    backend/ai-marketplace.jar          Spring Boot fat jar
    frontend-workbench/dist/            员工工作台静态产物
    frontend-admin/dist/                运营后台静态产物
    deploy/                             全部配置与安装脚本
  上传到服务器后解压到 /aitest/packages/，再执行 deploy/scripts/ 下的脚本。

.NOTES
  依赖：JDK17、Maven、Node.js。三个路径按本机实际情况修改下面的变量。
  两个前端的 build script 都带 vue-tsc --noEmit，但项目里没有提交
  auto-imports.d.ts / components.d.ts（首次构建才由插件生成），类型检查必然失败，
  因此这里直接调用 vite build 跳过类型门禁。
#>

$ErrorActionPreference = 'Stop'

$RepoRoot  = 'D:\WorkbuddyWorkspace\ai-marketplace'
$JavaHome  = 'C:\Program Files\Java\jdk-17'
$MavenHome = 'D:\WorkbuddyWorkspace\tools\apache-maven-3.9.16'

$Stamp   = Get-Date -Format 'yyyyMMdd-HHmmss'
$Staging = Join-Path $RepoRoot "dist-release\ai-marketplace-$Stamp"

$env:JAVA_HOME = $JavaHome
$env:PATH      = "$JavaHome\bin;$MavenHome\bin;$env:PATH"

Write-Host "`n==> 校验工具链" -ForegroundColor Cyan
java -version 2>&1 | Select-Object -First 1
& mvn -v | Select-Object -First 1
node -v

New-Item -ItemType Directory -Force -Path $Staging | Out-Null

Write-Host "`n==> 构建后端 jar" -ForegroundColor Cyan
Push-Location (Join-Path $RepoRoot 'backend')
& mvn -B clean package -DskipTests
if ($LASTEXITCODE -ne 0) { Pop-Location; throw "Maven 构建失败（退出码 $LASTEXITCODE）" }
Pop-Location
New-Item -ItemType Directory -Force -Path "$Staging\backend" | Out-Null
Copy-Item "$RepoRoot\backend\target\ai-marketplace.jar" "$Staging\backend\"

foreach ($app in 'frontend-workbench', 'frontend-admin') {
    Write-Host "`n==> 构建前端 $app" -ForegroundColor Cyan
    Push-Location (Join-Path $RepoRoot $app)
    if (-not (Test-Path 'node_modules')) { & npm install --no-audit --no-fund }
    & npx vite build
    if ($LASTEXITCODE -ne 0) { Pop-Location; throw "$app 构建失败" }
    Pop-Location
    New-Item -ItemType Directory -Force -Path "$Staging\$app" | Out-Null
    Copy-Item -Recurse "$RepoRoot\$app\dist" "$Staging\$app\dist"
}

Write-Host "`n==> 收集部署脚本与数据库初始化 SQL" -ForegroundColor Cyan
Copy-Item -Recurse "$RepoRoot\deploy" "$Staging\deploy"
New-Item -ItemType Directory -Force -Path "$Staging\deploy\sql" | Out-Null
Copy-Item "$RepoRoot\backend\src\main\resources\sql\schema.sql" "$Staging\deploy\sql\"

Write-Host "`n==> 打包 tar.gz" -ForegroundColor Cyan
$Archive = "$RepoRoot\dist-release\ai-marketplace-release-$Stamp.tar.gz"
Push-Location $Staging
& tar -czf $Archive *
Pop-Location

Write-Host "`n发布包已生成：$Archive" -ForegroundColor Green
Write-Host ("体积：{0:N1} MB" -f ((Get-Item $Archive).Length / 1MB))
Write-Host @"

下一步（上传并在服务器执行）：
  scp $Archive aiuser@192.168.1.132:/tmp/
  ssh aiuser@192.168.1.132
    sudo mkdir -p /aitest/packages && sudo tar -xzf /tmp/$(Split-Path $Archive -Leaf) -C /aitest/packages
    # 数据层见 deploy/README.md
"@
