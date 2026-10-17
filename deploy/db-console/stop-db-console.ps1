$ErrorActionPreference = 'Stop'
$pidFile = Join-Path $PSScriptRoot 'tunnel.json'
if (!(Test-Path -LiteralPath $pidFile)) { Write-Output '没有记录到本工具启动的隧道。'; return }
$saved = Get-Content -Raw -LiteralPath $pidFile | ConvertFrom-Json
$processInfo = Get-CimInstance Win32_Process -Filter "ProcessId = $($saved.processId)" -ErrorAction SilentlyContinue
if ($processInfo) {
    if ($processInfo.Name -ne 'ssh.exe' -or !$processInfo.CommandLine.Contains($saved.keyPath)) {
        throw '记录的 PID 已被其他进程使用，未停止该进程。'
    }
    Stop-Process -Id $saved.processId
}
Remove-Item -LiteralPath $pidFile
Write-Output '数据库查看隧道已关闭。'
