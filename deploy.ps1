<#
    GameCommunityCreator 一键部署（Windows）

    用法：双击同目录的 deploy.bat（或在本目录执行 deploy.bat）。
    它做四件事：
      1) 检查 Docker 是否装好、引擎是否在跑（Windows 上最常见的问题是缺 WSL2）
      2) 首次运行时生成 .env：随机数据库口令 + 随机令牌密钥，并自动避让被占用的端口
      3) 构建镜像并启动 MySQL + 后端 + nginx
      4) 等三个容器 healthy 后打印访问地址与种子账号

    本机不需要安装 JDK / Maven / Node / MySQL / nginx。

    ⚠ 编码：本文件必须是 **UTF-8 with BOM**。Windows PowerShell 5.1 默认按 ANSI 读脚本，
      没有 BOM 中文就会变成乱码、脚本直接语法报错。用编辑器改过之后若 BOM 丢了，
      不用手工修：deploy.bat 每次运行都会检查并自动补回 BOM。
      同目录的 deploy.bat 则必须保持纯 ASCII（cmd 按字节读批处理文件）。
#>
[CmdletBinding()]
param()

Set-Location -LiteralPath $PSScriptRoot

$script:DcExe = $null
$script:DcArgs = @()

function Write-Head($text) { Write-Host $text -ForegroundColor Cyan }
function Write-Ok($text) { Write-Host "[OK] $text" -ForegroundColor Green }
function Write-Info($text) { Write-Host "[*] $text" -ForegroundColor Gray }
function Write-Warn($text) { Write-Host "[!] $text" -ForegroundColor Yellow }

function Stop-WithMessage {
    param([string[]]$Lines)
    Write-Host ''
    $Lines | ForEach-Object { Write-Host $_ }
    Write-Host ''
    Write-Host '部署未完成：按上面的提示处理后再运行一次本脚本。' -ForegroundColor Yellow
}

function Invoke-Dc {
    param([string[]]$Arguments, [switch]$Quiet)
    $all = @($script:DcArgs + $Arguments)
    if ($Quiet) {
        & $script:DcExe @all *> $null
    } else {
        & $script:DcExe @all
    }
    # 刻意不 return 退出码：return 会和命令的 stdout 混进同一个输出流，
    # 调用方拿到的是「一堆输出行 + 退出码」的数组，拿它判断成功失败必然误判。
    # 退出码请照常读自动变量 $LASTEXITCODE。
}

function Exit-WithPause {
    param([int]$Code)
    Write-Host ''
    Read-Host '按回车键关闭窗口'
    exit $Code
}

Write-Head '============================================================'
Write-Head '  GameCommunityCreator 一键部署'
Write-Head '============================================================'
Write-Host '  本机不需要安装 JDK / Maven / Node / MySQL / nginx，只要有 Docker。'
Write-Head '============================================================'
Write-Host ''

# ==================== 1. Docker ====================
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Stop-WithMessage @(
        '[X] 没找到 docker 命令。',
        '    请先安装 Docker Desktop：https://www.docker.com/products/docker-desktop/',
        '    装完请重新打开一个窗口再运行本脚本（否则 PATH 还是旧的）。'
    )
    Exit-WithPause 1
}

# 注意：引擎信息要用 docker info，不是 docker compose info（后者不存在）
& docker info *> $null
if ($LASTEXITCODE -ne 0) {
    Stop-WithMessage @(
        '[X] Docker 引擎没有运行。',
        '    1) 打开 Docker Desktop，等它的状态变成 running；',
        '    2) 如果它一直起不来，或 docker info 的 Server 一段是空的，',
        '       说明 Windows 上缺 WSL2：用【管理员】终端执行  wsl --install',
        '       然后重启系统，再回来运行本脚本。'
    )
    Exit-WithPause 1
}
Write-Ok 'Docker 可用'

# ==================== 2. compose 命令 ====================
$script:DcExe = 'docker'
$script:DcArgs = @('compose')
Invoke-Dc -Arguments @('version') -Quiet | Out-Null
if ($LASTEXITCODE -ne 0) {
    $script:DcExe = 'docker-compose'
    $script:DcArgs = @()
    Invoke-Dc -Arguments @('version') -Quiet | Out-Null
    if ($LASTEXITCODE -ne 0) {
        Stop-WithMessage @(
            '[X] 找不到 docker compose。',
            '    请升级 Docker Desktop 到较新版本（它自带 compose 插件）。'
        )
        Exit-WithPause 1
    }
}
Write-Ok ("使用命令：" + $script:DcExe + ' ' + ($script:DcArgs -join ' ')).Trim()

# ==================== 3. .env ====================
$envPath = Join-Path $PSScriptRoot '.env'
$generated = $false

if (Test-Path -LiteralPath $envPath) {
    Write-Ok '.env 已存在，沿用现有配置（想改口令或端口就直接编辑它）'
} else {
    Write-Info '首次运行：生成 .env（随机数据库口令 + 随机令牌密钥）'

    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    $dbBytes = New-Object byte[] 18
    $jwtBytes = New-Object byte[] 32
    $rng.GetBytes($dbBytes)
    $rng.GetBytes($jwtBytes)
    $dbPassword = (($dbBytes | ForEach-Object { $_.ToString('x2') }) -join '')
    $jwtSecret = (($jwtBytes | ForEach-Object { $_.ToString('x2') }) -join '')

    # 后端调试端口默认 9331；被占用时（例如本机正跑着开发用的后端）自动换成 19331。
    # 这个映射只是方便本机调试用，对外访问走的是 nginx，换掉不影响功能。
    $backendPort = 9331
    $probe = New-Object Net.Sockets.TcpClient
    try {
        $probe.Connect('127.0.0.1', 9331)
        $backendPort = 19331
        Write-Info '检测到本机 9331 已被占用，后端调试端口改用 19331'
    } catch {
        # 连不上说明端口空着，用默认值
    } finally {
        $probe.Dispose()
    }

    $lines = @(
        '# 由 deploy.bat 生成，可自由修改；改完重新运行 deploy.bat 即可生效',
        "DB_ROOT_PASSWORD=$dbPassword",
        "JWT_SECRET=$jwtSecret",
        '',
        '# 对外端口：用户端 / 管理端',
        'USER_PORT=80',
        'ADMIN_PORT=8081',
        '',
        '# 后端接口只绑本机，仅供调试；与其它服务冲突时改这里',
        "BACKEND_PORT=$backendPort",
        '',
        '# 后端 JVM 内存，小内存机器可以调小',
        'JAVA_OPTS=-Xms256m -Xmx512m'
    )
    # 必须写 UTF-8 且不带 BOM：带 BOM 的话第一个键会变成「BOM+DB_ROOT_PASSWORD」，Compose 读不到
    [System.IO.File]::WriteAllLines($envPath, $lines, (New-Object System.Text.UTF8Encoding($false)))
    $generated = $true
    Write-Ok '.env 已生成（口令与密钥都是随机的，写在文件里，需要时直接打开看）'
}

# 从 .env 里读出对外端口，好把访问地址打对
$userPort = 80
$adminPort = 8081
foreach ($line in Get-Content -LiteralPath $envPath) {
    if ($line -match '^\s*USER_PORT\s*=\s*(\d+)') { $userPort = [int]$Matches[1] }
    if ($line -match '^\s*ADMIN_PORT\s*=\s*(\d+)') { $adminPort = [int]$Matches[1] }
}
$userUrl = if ($userPort -eq 80) { 'http://localhost' } else { "http://localhost:$userPort" }
$adminUrl = "http://localhost:$adminPort"

# ==================== 4. 构建并启动 ====================
Write-Host ''
Write-Info '构建镜像并启动三个容器……'
Write-Host '     首次运行要下载 Maven 与 npm 依赖，几分钟到十几分钟都正常，请不要关闭本窗口。'
Write-Host ''
Invoke-Dc -Arguments @('up', '-d', '--build')
if ($LASTEXITCODE -ne 0) {
    Stop-WithMessage @(
        '[X] 启动失败。常见原因与处理：',
        '    - 端口被占：编辑 .env 里的 USER_PORT / ADMIN_PORT / BACKEND_PORT 换一个；',
        '    - 拉镜像超时（auth.docker.io 之类）：属于网络抖动，直接再运行一次本脚本；',
        '    - 想看详细日志：在项目目录执行  docker compose logs --tail=50'
    )
    Exit-WithPause 1
}

# ==================== 5. 等三个容器 healthy ====================
Write-Host ''
Write-Info '等三个容器就绪（最多 5 分钟）……'
$healthy = 0
for ($i = 0; $i -lt 60; $i++) {
    # 别用 docker compose ps --filter health=healthy：compose 不认 health 这个过滤键
    # （直接报 unknown filter health），只能按 --format 取出来自己数
    $healths = & $script:DcExe @($script:DcArgs + @('ps', '--format', '{{.Health}}')) 2>$null
    $healthy = @($healths | Where-Object { "$_".Trim() -eq 'healthy' }).Count
    if ($healthy -ge 3) { break }
    Start-Sleep -Seconds 5
}

if ($healthy -lt 3) {
    Write-Warn '等待超时：容器还没全部就绪（数据库首次初始化要建 28 张表，慢一两分钟是正常的）。'
    Write-Host '     用这两条命令看原因：'
    Write-Host '       docker compose ps'
    Write-Host '       docker compose logs --tail=50 backend'
    Write-Host '     如果只是还在初始化，等一会儿再运行一次本脚本即可。'
    Write-Host '     注意：如果你删掉过 .env 让脚本重新生成，那么数据库卷里还是旧口令，后端会连不上；'
    Write-Host '           这种情况执行 docker compose down -v 让数据库按新口令重新初始化（数据会清空）。'
    Exit-WithPause 1
}

# ==================== 完成 ====================
Invoke-Dc -Arguments @('ps') | Out-Null
Write-Host ''
Write-Head '============================================================'
Write-Head '  部署完成'
Write-Head '============================================================'
Write-Host ''
Write-Host "  用户端（社区本身）  $userUrl"
Write-Host "  管理端（后台）      $adminUrl"
Write-Host ''
Write-Host '  管理端账号：admin / admin123      （登录后第一件事：改密码）'
Write-Host '  玩家账号：  player1 / member123'
Write-Host ''
Write-Host '  还差最后一步：打开管理端 -> 「站点配置」-> 「创建社区」'
Write-Host '  填社区名称与游戏名称，首页/攻略/板块/帖子/公告/个人主页就建好了。'
Write-Host '  （首次打开用户端是空白的，这是正常的）'
Write-Host ''
Write-Host '  常用命令：'
Write-Host '    docker compose ps                  看容器状态'
Write-Host '    docker compose logs -f backend     跟后端日志'
Write-Host '    docker compose down                停掉（数据保留）'
Write-Host '    docker compose down -v             彻底重来（连数据一起删）'
Write-Head '============================================================'
if ($generated) {
    Write-Host ''
    Write-Info "本次生成的配置在 $envPath（里面是随机口令，别提交到版本库）"
}
Exit-WithPause 0
