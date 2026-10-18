param(
    [ValidateSet('Start', 'Stop')][string]$Action = 'Start',
    [switch]$NoBrowser,
    [switch]$NoDialog
)
$ErrorActionPreference = 'Stop'
$logPath = Join-Path $PSScriptRoot 'launcher.log'
try {
    "$(Get-Date -Format o) $Action requested" | Add-Content -LiteralPath $logPath -Encoding UTF8
    if ($Action -eq 'Stop') {
        & (Join-Path $PSScriptRoot 'stop-db-console.ps1') | Add-Content -LiteralPath $logPath -Encoding UTF8
    } else {
        & (Join-Path $PSScriptRoot 'start-db-console.ps1') -NoBrowser:$NoBrowser | Add-Content -LiteralPath $logPath -Encoding UTF8
    }
    "$(Get-Date -Format o) $Action completed" | Add-Content -LiteralPath $logPath -Encoding UTF8
    exit 0
} catch {
    # Do not persist HTTP bodies, URLs containing session IDs or credential-bearing arguments.
    $diagnostic = '{0} {1} failed: {2}; category={3}; script={4}; line={5}' -f (
        (Get-Date -Format o), $Action, $_.Exception.GetType().FullName,
        $_.CategoryInfo.Category, $_.InvocationInfo.ScriptName, $_.InvocationInfo.ScriptLineNumber)
    $diagnostic | Add-Content -LiteralPath $logPath -Encoding UTF8
    if (!$NoDialog) {
        Add-Type -AssemblyName System.Windows.Forms
        [System.Windows.Forms.MessageBox]::Show(
            "数据库查看入口启动失败。请查看诊断日志：`n$logPath",
            'Pandora 数据库查看', 'OK', 'Error') | Out-Null
    }
    exit 1
}
