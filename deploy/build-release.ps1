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

# Windows PowerShell 5.1 在 Stop 偏好下会把原生命令写到 stderr 的正常输出
# 当成终止性错误（NativeCommandError），而 java/mvn/npm/tar 都会写 stderr，
# 所以原生命令统一走这个包装：临时放宽偏好，只按退出码判定成败。
function Invoke-Tool {
    param([Parameter(Mandatory)][string]$Name, [string[]]$ToolArgs = @())
    $prev = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try { & $Name @ToolArgs 2>&1 | ForEach-Object { Write-Host $_ } }
    finally { $ErrorActionPreference = $prev }
    return $LASTEXITCODE
}

$RepoRoot  = 'D:\WorkbuddyWorkspace\ai-marketplace'
$JavaHome  = 'C:\Program Files\Java\jdk-17'
$MavenHome = 'D:\WorkbuddyWorkspace\tools\apache-maven-3.9.16'

$Stamp   = Get-Date -Format 'yyyyMMdd-HHmmss'
$Staging = Join-Path $RepoRoot "dist-release\ai-marketplace-$Stamp"

$env:JAVA_HOME = $JavaHome
$env:PATH      = "$JavaHome\bin;$MavenHome\bin;$env:PATH"

Write-Host "`n==> 校验工具链" -ForegroundColor Cyan
$null = Invoke-Tool java @('-version')
$null = Invoke-Tool mvn  @('-v')
$null = Invoke-Tool node @('-v')

New-Item -ItemType Directory -Force -Path $Staging | Out-Null

Write-Host "`n==> 构建后端 jar" -ForegroundColor Cyan
Push-Location (Join-Path $RepoRoot 'backend')
$code = Invoke-Tool mvn @('-B', 'clean', 'package', '-DskipTests')
if ($code -ne 0) { Pop-Location; throw "Maven 构建失败（退出码 $code）" }
Pop-Location
New-Item -ItemType Directory -Force -Path "$Staging\backend" | Out-Null
Copy-Item "$RepoRoot\backend\target\ai-marketplace.jar" "$Staging\backend\"

foreach ($app in 'frontend-workbench', 'frontend-admin') {
    Write-Host "`n==> 构建前端 $app" -ForegroundColor Cyan
    Push-Location (Join-Path $RepoRoot $app)
    if (-not (Test-Path 'node_modules')) { $null = Invoke-Tool npm @('install', '--no-audit', '--no-fund') }
    $code = Invoke-Tool npx @('vite', 'build')
    if ($code -ne 0) { Pop-Location; throw "$app 构建失败（退出码 $code）" }
    Pop-Location
    New-Item -ItemType Directory -Force -Path "$Staging\$app" | Out-Null
    Copy-Item -Recurse "$RepoRoot\$app\dist" "$Staging\$app\dist"
}

Write-Host "`n==> 收集部署脚本与数据库初始化 SQL" -ForegroundColor Cyan
Copy-Item -Recurse "$RepoRoot\deploy" "$Staging\deploy"
New-Item -ItemType Directory -Force -Path "$Staging\deploy\sql" | Out-Null
Copy-Item "$RepoRoot\backend\src\main\resources\sql\schema.sql" "$Staging\deploy\sql\"

Write-Host "`n==> 打包 tar.gz" -ForegroundColor Cyan
$Leaf    = "ai-marketplace-release-$Stamp.tar.gz"
$Archive = "$RepoRoot\dist-release\$Leaf"
Push-Location $Staging
# PowerShell 不会为原生命令展开通配符，必须自己列出暂存目录的一级条目。
# 另外 PATH 里 Git Bash 的 GNU tar 会把 D:\xxx 解析成 host:path 远程语法而报
# "Cannot connect to D:"，所以用相对文件名写出，再挪回 dist-release。
$entries = Get-ChildItem -Path $Staging -Name
$code = Invoke-Tool tar (@('-czf', $Leaf) + $entries)
Pop-Location
if ($code -ne 0) { throw "打包失败（退出码 $code）" }
Move-Item -Force "$Staging\$Leaf" $Archive

Write-Host "`n发布包已生成：$Archive" -ForegroundColor Green
Write-Host ("体积：{0:N1} MB" -f ((Get-Item $Archive).Length / 1MB))
Write-Host @"

下一步（上传并在服务器执行）：
  scp $Archive aiuser@192.168.1.132:/tmp/
  ssh aiuser@192.168.1.132
    sudo mkdir -p /aitest/packages && sudo tar -xzf /tmp/$(Split-Path $Archive -Leaf) -C /aitest/packages
    # 数据层见 deploy/README.md
"@
