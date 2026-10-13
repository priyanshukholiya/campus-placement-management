param([string]$JavaHome = $env:JAVA_HOME,[switch]$Wait,[switch]$Smoke)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$runRoot = Join-Path $projectRoot '.run'
New-Item -ItemType Directory -Force -Path $runRoot | Out-Null
if (!$JavaHome) {
    $installedJdk = Get-ChildItem -LiteralPath "$env:ProgramFiles/Java" -Directory -ErrorAction SilentlyContinue |
        Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName 'bin/java.exe') } |
        Sort-Object Name -Descending | Select-Object -First 1
    if ($installedJdk) { $JavaHome = $installedJdk.FullName }
}
$javaBin = if ($JavaHome) { Join-Path $JavaHome 'bin/java.exe' } else { (Get-Command java -ErrorAction Stop).Source }
$nodeBin = (Get-Command node -ErrorAction Stop).Source
$services = @('student-service','recruitment-service','application-service','notification-service','api-gateway')
foreach ($service in $services) {
    if (!(Test-Path -LiteralPath (Join-Path $projectRoot "$service/target/$service-0.1.0.jar"))) { throw "Build the project first: missing $service JAR." }
}
if (!(Test-Path -LiteralPath (Join-Path $projectRoot 'frontend/node_modules/vite/bin/vite.js'))) { throw 'Install frontend dependencies first: cd frontend; npm ci' }
foreach ($port in @(8080,8081,8082,8083,8084,5173)) {
    $probe = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback,$port)
    try { $probe.Start() }
    catch { throw "Cannot bind port $port. Stop the existing process or use scripts/stop.ps1 for an earlier run." }
    finally { $probe.Stop() }
}
$ownedProcesses = @()
try {
    foreach ($service in $services) {
        $jarFile = Join-Path $projectRoot "$service/target/$service-0.1.0.jar"
        $p = Start-Process -FilePath $javaBin -ArgumentList @('-Xms64m','-Xmx256m','-jar',('"' + $jarFile + '"')) -WorkingDirectory $projectRoot -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $runRoot "$service.log") -RedirectStandardError (Join-Path $runRoot "$service.err.log")
        $ownedProcesses += @{ name=$service; id=$p.Id; started=$p.StartTime.ToUniversalTime().ToString('o') }
    }
    $viteScript = Join-Path $projectRoot 'frontend/node_modules/vite/bin/vite.js'
    $p = Start-Process -FilePath $nodeBin -ArgumentList @(('"' + $viteScript + '"'),'--host','127.0.0.1') -WorkingDirectory (Join-Path $projectRoot 'frontend') -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $runRoot 'frontend.log') -RedirectStandardError (Join-Path $runRoot 'frontend.err.log')
    $ownedProcesses += @{ name='frontend'; id=$p.Id; started=$p.StartTime.ToUniversalTime().ToString('o') }
    $ownedProcesses | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $runRoot 'processes.json') -Encoding utf8
    Write-Output 'Waiting for services to become ready...'
    foreach ($port in @(8081,8082,8083,8084,8080,5173)) {
        $path = if ($port -eq 5173) { '/' } else { '/actuator/health' }
        $ready = $false
        $deadline = (Get-Date).AddSeconds(90)
        while ((Get-Date) -lt $deadline) {
            foreach ($owned in $ownedProcesses) {
                if (!(Get-Process -Id $owned.id -ErrorAction SilentlyContinue)) { throw "$($owned.name) exited. Check its .run/ log." }
            }
            try {
                $response = Invoke-WebRequest -Uri "http://127.0.0.1:$port$path" -UseBasicParsing -TimeoutSec 2
                if ($response.StatusCode -eq 200) { $ready = $true; break }
            } catch { Start-Sleep -Milliseconds 500 }
        }
        if (!$ready) { throw "Port $port did not become ready. Check .run/ logs." }
        Write-Output "Ready: $port"
    }
    if ($Smoke) {
        & $nodeBin (Join-Path $PSScriptRoot 'smoke.mjs')
        if ($LASTEXITCODE -ne 0) { throw 'Cross-service smoke tests failed.' }
    }
    Write-Output 'Services ready. Open http://127.0.0.1:5173.'
    Write-Output 'Demo accounts: student, student2, officer, recruiter. Password: placement-demo (unless overridden with DEMO_PASSWORD).'
    Write-Output 'Logs: .run/. Stop only this project with scripts/stop.ps1.'
    if ($Wait) { Wait-Process -Id @($ownedProcesses | ForEach-Object { $_.id }) }
} catch {
    foreach ($owned in $ownedProcesses) {
        $current = Get-Process -Id $owned.id -ErrorAction SilentlyContinue
        if ($current -and $current.StartTime.ToUniversalTime().ToString('o') -eq $owned.started) { Stop-Process -Id $owned.id }
    }
    throw
}
