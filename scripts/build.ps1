param([string]$JavaHome=$env:JAVA_HOME,[string]$MavenRepository)
$ErrorActionPreference='Stop'
$projectRoot=Split-Path $PSScriptRoot -Parent
if ($JavaHome) { $env:JAVA_HOME=$JavaHome }
if (!$MavenRepository) { $MavenRepository=Join-Path $projectRoot '.tools/m2' }
$env:MAVEN_USER_HOME=Join-Path $projectRoot '.tools/maven'
$previousTemp=$env:TEMP
$previousTmp=$env:TMP
$buildTemp=Join-Path $projectRoot '.tools/tmp'
New-Item -ItemType Directory -Path $buildTemp -Force | Out-Null
$env:TEMP=$buildTemp
$env:TMP=$buildTemp
Push-Location $projectRoot
try {
    & ./mvnw.cmd -B "-Dmaven.repo.local=$MavenRepository" verify
    if ($LASTEXITCODE -ne 0) { throw 'Java build or tests failed.' }
    Push-Location frontend
    try {
        & npm.cmd ci --cache (Join-Path $projectRoot '.tools/npm')
        if ($LASTEXITCODE -ne 0) { throw 'Frontend dependency installation failed.' }
        & npm.cmd run build
        if ($LASTEXITCODE -ne 0) { throw 'Frontend build failed.' }
    } finally { Pop-Location }
} finally { Pop-Location; $env:TEMP=$previousTemp; $env:TMP=$previousTmp }
