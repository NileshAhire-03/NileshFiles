$ErrorActionPreference = 'Stop'
$envPath = Join-Path $PSScriptRoot '.env'
if (Test-Path -LiteralPath $envPath) {
    Get-Content -LiteralPath $envPath | ForEach-Object {
        if ($_ -match '^\s*([^#=]+)=(.*)$') {
            [Environment]::SetEnvironmentVariable($Matches[1].Trim(), $Matches[2].Trim().Trim('"').Trim("'"), 'Process')
        }
    }
}
if (-not $env:DB_PASSWORD) {
    throw 'Set DB_PASSWORD in .env or your environment before starting the backend.'
}
Push-Location (Join-Path $PSScriptRoot 'backend')
try {
    # A short temp path avoids the Windows Unix-domain socket path limit.
    $runtimeTemp = Join-Path $PSScriptRoot '..\.runtime\tmp'
    New-Item -ItemType Directory -Path $runtimeTemp -Force | Out-Null
    $runtimeTemp = (Resolve-Path -LiteralPath $runtimeTemp).Path
    & mvn.cmd spring-boot:run "-Dspring-boot.run.jvmArguments=-Djava.io.tmpdir=$runtimeTemp -Djdk.net.unixdomain.tmpdir=$runtimeTemp"
    if ($LASTEXITCODE -ne 0) { throw "Backend exited with code $LASTEXITCODE" }
} finally {
    Pop-Location
}
