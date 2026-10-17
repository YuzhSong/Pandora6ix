param([switch]$NoBrowser)
$ErrorActionPreference = 'Stop'
$config = Get-Content -Raw -LiteralPath (Join-Path $PSScriptRoot 'access.json') | ConvertFrom-Json
$pidFile = Join-Path $PSScriptRoot 'tunnel.json'
$url = "http://127.0.0.1:$($config.localPort)/"
function Test-Console {
    try { return (Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 2).Content -match 'H2 Console' }
    catch { return $false }
}
$running = $false
if (Test-Path -LiteralPath $pidFile) {
    $saved = Get-Content -Raw -LiteralPath $pidFile | ConvertFrom-Json
    $processInfo = Get-CimInstance Win32_Process -Filter "ProcessId = $($saved.processId)" -ErrorAction SilentlyContinue
    $running = $processInfo -and $processInfo.Name -eq 'ssh.exe' -and $processInfo.CommandLine.Contains($config.keyPath)
    if ($running -and !(Test-Console)) { throw '隧道进程存在，但控制台不可用；请先运行停止脚本，再重试。' }
}
if (!$running) {
    $listener = Get-NetTCPConnection -LocalPort $config.localPort -State Listen -ErrorAction SilentlyContinue
    if ($listener) { throw '本机控制台端口已被其他程序占用。' }
    $arguments = @('-N', '-T', '-L', "127.0.0.1:$($config.localPort):127.0.0.1:18082",
        '-i', ('"' + $config.keyPath + '"'), '-o', 'IdentitiesOnly=yes', '-o', 'BatchMode=yes',
        '-o', 'StrictHostKeyChecking=yes', '-o', ('UserKnownHostsFile="' + $config.knownHostsPath + '"'),
        '-o', 'ExitOnForwardFailure=yes', '-o', 'ServerAliveInterval=30', '-o', 'ServerAliveCountMax=3',
        "$($config.sshUser)@$($config.host)")
    $tunnel = Start-Process -FilePath 'ssh.exe' -ArgumentList $arguments -WindowStyle Hidden -PassThru `
        -RedirectStandardError (Join-Path $PSScriptRoot 'tunnel-error.log')
    @{ processId = $tunnel.Id; keyPath = $config.keyPath; url = $url } | ConvertTo-Json | Set-Content -LiteralPath $pidFile -Encoding UTF8
    for ($attempt = 0; $attempt -lt 30; $attempt++) {
        if ($tunnel.HasExited) { throw 'SSH 隧道启动失败，请查看同目录 tunnel-error.log。' }
        if (Test-Console) { $running = $true; break }
        Start-Sleep -Milliseconds 300
    }
    if (!$running) {
        Stop-Process -Id $tunnel.Id -ErrorAction SilentlyContinue
        throw '控制台连接超时，请查看隧道日志或云端服务状态。'
    }
}
Write-Output "数据库只读入口：$url"
Write-Output "账号与密码见：$(Join-Path $PSScriptRoot '访问说明.md')"
if (!$NoBrowser) {
    $landing = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 10
    $session = [regex]::Match($landing.Content, 'jsessionid=([a-zA-Z0-9]+)').Groups[1].Value
    if (!$session) { throw '控制台没有返回有效会话。' }
    $login = Invoke-WebRequest -Uri ($url + 'login.do?jsessionid=' + $session) -UseBasicParsing -Method Post -TimeoutSec 15 -Body @{
        language = 'en'; driver = 'org.h2.Driver'; url = $config.jdbcUrl
        user = $config.dbUser; password = $config.dbPassword
    }
    if ($login.Content -notmatch 'frame.jsp|query.jsp') { throw '只读账号登录失败，请查看访问说明。' }
    Start-Process ($url + 'frame.jsp?jsessionid=' + $session)
}
