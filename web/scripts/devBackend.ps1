$ErrorActionPreference = 'Stop'
$webRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$repoRoot = [IO.Path]::GetFullPath((Join-Path $webRoot '..'))
$sourceRoot = Join-Path $repoRoot 'backend'
$backendRevision = (git -C $repoRoot log -1 --format=%h -- backend).Trim()
if (-not $backendRevision -or $backendRevision -notmatch '^[0-9a-f]+$') { throw 'Cannot determine backend source revision.' }
$copyRoot = Join-Path $webRoot ('.integration/backend-' + $backendRevision)
$runtimeRoot = Join-Path $webRoot '.integration/local'
$java = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/java.exe' } else { (Get-Command java).Source }
$javaVersion = (& $java --version | Out-String)
if ($javaVersion -notmatch '(?:java|openjdk)\s+(\d+)' -or [int]$Matches[1] -lt 21) { throw 'Set JAVA_HOME to JDK 21 or newer before running dev:backend.' }
if (-not (Test-Path -LiteralPath $copyRoot)) {
    New-Item -ItemType Directory -Path $copyRoot -Force | Out-Null
    Copy-Item -LiteralPath (Join-Path $sourceRoot 'pom.xml'),(Join-Path $sourceRoot 'mvnw.cmd') -Destination $copyRoot
    Copy-Item -LiteralPath (Join-Path $sourceRoot 'src'),(Join-Path $sourceRoot '.mvn') -Destination $copyRoot -Recurse
}
$jar = Join-Path $copyRoot 'target/backend-0.0.1-SNAPSHOT.jar'
if (-not (Test-Path -LiteralPath $jar)) {
    Push-Location $copyRoot
    try {
        & (Join-Path $copyRoot 'mvnw.cmd') -q -DskipTests package
        if ($LASTEXITCODE -ne 0) { throw 'Backend copy build failed. Set JAVA_HOME to JDK 21 or newer.' }
    } finally { Pop-Location }
}
New-Item -ItemType Directory -Path $runtimeRoot -Force | Out-Null
Write-Host 'Local backend: http://127.0.0.1:8080. Source is unchanged; database is under web/.integration/local.'
if (-not $env:PANDORA_BOOTSTRAP_TOKEN) { Write-Host 'PANDORA_BOOTSTRAP_TOKEN is unset; first-time enterprise initialization is closed.' }
Push-Location $runtimeRoot
$previousDbUrl = $env:PANDORA_DB_URL
$previousDbUser = $env:PANDORA_DB_USER
$previousDbPassword = $env:PANDORA_DB_PASSWORD
try {
    $env:PANDORA_DB_URL = 'jdbc:h2:file:./pandora;DB_CLOSE_ON_EXIT=FALSE;WRITE_DELAY=0'
    $env:PANDORA_DB_USER = 'sa'
    $env:PANDORA_DB_PASSWORD = ''
    & $java -jar $jar --server.address=127.0.0.1 --server.port=8080
    if ($LASTEXITCODE -ne 0) { throw 'Backend process exited unsuccessfully.' }
} finally {
    $env:PANDORA_DB_URL = $previousDbUrl
    $env:PANDORA_DB_USER = $previousDbUser
    $env:PANDORA_DB_PASSWORD = $previousDbPassword
    Pop-Location
}
