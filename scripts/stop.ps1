$ErrorActionPreference = 'Stop'
$stateFile = Join-Path (Split-Path $PSScriptRoot -Parent) '.run/processes.json'
if (!(Test-Path -LiteralPath $stateFile)) { Write-Output 'No saved project processes.'; return }
$ownedProcesses = Get-Content -LiteralPath $stateFile -Raw | ConvertFrom-Json
foreach ($owned in $ownedProcesses) {
    $current = Get-Process -Id $owned.id -ErrorAction SilentlyContinue
    if ($current -and $current.StartTime.ToUniversalTime().ToString('o') -eq $owned.started) {
        Stop-Process -Id $owned.id
        Write-Output "Stopped $($owned.name)."
    }
}
Remove-Item -LiteralPath $stateFile
