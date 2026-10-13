param([int]$Port = 18080)
$ErrorActionPreference = 'Stop'
$backendRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$java = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin\java.exe' } else { (Get-Command java).Source }
$jar = Join-Path $backendRoot 'target\backend-0.0.1-SNAPSHOT.jar'
if (-not (Test-Path -LiteralPath $jar)) { throw 'Build the backend jar before running this smoke check.' }
if (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue) { throw "Port $Port is already occupied." }
$runRoot = Join-Path $backendRoot ('target\runtime-smoke\' + [Guid]::NewGuid().ToString())
New-Item -ItemType Directory -Path $runRoot -Force | Out-Null
$database = (Join-Path $runRoot 'pandora').Replace('\','/')
$previousDbUrl = $env:PANDORA_DB_URL
$previousBootstrap = $env:PANDORA_BOOTSTRAP_TOKEN
$previousDbUser = $env:PANDORA_DB_USER
$previousDbPassword = $env:PANDORA_DB_PASSWORD
$env:PANDORA_DB_URL = "jdbc:h2:file:$database;DB_CLOSE_ON_EXIT=FALSE;WRITE_DELAY=0"
$env:PANDORA_DB_USER = 'sa'
$env:PANDORA_DB_PASSWORD = ''
$env:PANDORA_BOOTSTRAP_TOKEN = [Guid]::NewGuid().ToString()
$password = [Guid]::NewGuid().ToString()
$server = $null
$base = "http://127.0.0.1:$Port"
$startNumber = 0
function Start-SmokeBackend {
    $script:startNumber += 1
    $script:server = Start-Process -FilePath $java -ArgumentList @('-jar',"`"$jar`"","--server.port=$Port") -WorkingDirectory $backendRoot -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $runRoot "server-$startNumber.log") -RedirectStandardError (Join-Path $runRoot "server-$startNumber.err")
    $deadline = (Get-Date).AddSeconds(45)
    do {
        if ($script:server.HasExited) { throw 'Backend process exited; inspect the smoke logs.' }
        try { if ((Invoke-RestMethod "$base/health" -TimeoutSec 2).status -eq 'UP') { return } } catch { }
        Start-Sleep -Milliseconds 250
    } while ((Get-Date) -lt $deadline)
    throw 'Backend did not become healthy within 45 seconds.'
}
function Call-Api($method,$path,$body,$token,$extraHeaders) {
    $headers = @{}
    if ($token) { $headers.Authorization = "Bearer $token" }
    if ($extraHeaders) { foreach ($entry in $extraHeaders.GetEnumerator()) { $headers[$entry.Key] = $entry.Value } }
    $parameters = @{ Method=$method; Uri="$base/api/v1$path"; Headers=$headers; ContentType='application/json'; TimeoutSec=10 }
    if ($null -ne $body) { $parameters.Body = ($body | ConvertTo-Json -Depth 20 -Compress) }
    return (Invoke-RestMethod @parameters).data
}
try {
    Start-SmokeBackend
    $domain = 'smoke-' + [Guid]::NewGuid().ToString()
    $initial = Call-Api 'POST' '/admin/enterprise-initializations' @{loginDomain=$domain;name='Runtime smoke enterprise';businessTimezone='Asia/Shanghai';adminLoginName='admin';adminPassword=$password;adminDisplayName='Smoke administrator'} $null @{'X-Bootstrap-Token'=$env:PANDORA_BOOTSTRAP_TOKEN;'Idempotency-Key'='smoke-init'}
    $admin = (Call-Api 'POST' '/auth/login' @{enterpriseDomain=$domain;loginName='admin';password=$password} $null $null).accessToken
    $employee = Call-Api 'POST' '/admin/users' @{loginName='employee';password=$password;displayName='Smoke employee';orgUnitId=$initial.rootOrgUnitId;roles=@('EMPLOYEE')} $admin $null
    $token = (Call-Api 'POST' '/auth/login' @{enterpriseDomain=$domain;loginName='employee';password=$password} $null $null).accessToken
    $today = [TimeZoneInfo]::ConvertTimeBySystemTimeZoneId([DateTimeOffset]::UtcNow,'China Standard Time').ToString('yyyy-MM-dd')
    $body = @{workDate=$today;entries=@(@{clientEntryKey='smoke-entry';content='Persist across a real process restart'});tagIds=@()}
    $journal = Call-Api 'POST' '/work-logs' $body $token @{'Idempotency-Key'='smoke-journal'}
    $retry = Call-Api 'POST' '/work-logs' $body $token @{'Idempotency-Key'='smoke-journal'}
    if ($journal.id -ne $retry.id) { throw 'Idempotent journal retry changed the ID.' }
    $null = Call-Api 'PUT' '/me/important-entries' @{expectedVersion=0;entryIds=@()} $token $null
    Stop-Process -Id $server.Id
    $server.WaitForExit()
    Start-SmokeBackend
    $restored = Call-Api 'GET' "/work-logs/$($journal.id)" $null $token $null
    $selection = Call-Api 'GET' '/me/important-entries' $null $token $null
    $dashboard = Call-Api 'GET' '/dashboard' $null $token $null
    if ($restored.entries[0].entryId -ne $journal.entries[0].entryId) { throw 'Stable entry ID did not survive restart.' }
    if (-not $selection.hasManualSelection -or $selection.entries.Count -ne 0) { throw 'Manual empty selection did not persist.' }
    if ($dashboard.workEntries.items.Count -ne 1) { throw 'Dashboard did not use persisted journal entries.' }
    $null = Call-Api 'POST' '/auth/logout' $null $token $null
    $rejected = $false
    try { $null = Call-Api 'GET' '/me' $null $token $null } catch { if ([int]$_.Exception.Response.StatusCode -eq 401) { $rejected=$true } else { throw } }
    if (-not $rejected) { throw 'A revoked session remained usable.' }
    Write-Output 'Runtime smoke passed: migration, bootstrap/login, journal idempotency, process restart persistence, stable entry IDs, manual empty selection, dashboard, session revocation.'
} finally {
    if ($server -and -not $server.HasExited) { Stop-Process -Id $server.Id; $server.WaitForExit() }
    $env:PANDORA_DB_URL = $previousDbUrl
    $env:PANDORA_BOOTSTRAP_TOKEN = $previousBootstrap
    $env:PANDORA_DB_USER = $previousDbUser
    $env:PANDORA_DB_PASSWORD = $previousDbPassword
}
