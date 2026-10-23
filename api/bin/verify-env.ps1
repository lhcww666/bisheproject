param(
    [switch]$SkipResourceCopy
)

$ErrorActionPreference = 'Stop'
$ApiRoot = Split-Path -Parent $PSScriptRoot
$ProjectRoot = Split-Path -Parent $ApiRoot
$UiRoot = Join-Path $ProjectRoot 'ui'
$hasError = $false

function Write-Result {
    param(
        [string]$Name,
        [bool]$Ok,
        [string]$Detail,
        [switch]$WarningOnly
    )

    if ($Ok) {
        Write-Host "[PASS] $Name - $Detail"
    }
    elseif ($WarningOnly) {
        Write-Host "[WARN] $Name - $Detail"
    }
    else {
        Write-Host "[FAIL] $Name - $Detail"
        $script:hasError = $true
    }
}

function Test-LocalPort {
    param([int]$Port)
    try {
        return (Test-NetConnection -ComputerName 127.0.0.1 -Port $Port -WarningAction SilentlyContinue).TcpTestSucceeded
    }
    catch {
        return $false
    }
}

Write-Host 'Lab platform environment check'
Write-Host "Project: $ProjectRoot"
Write-Host ''

Write-Result 'Java' ([bool](Get-Command java -ErrorAction SilentlyContinue)) 'command available or JDK must be added to PATH'
Write-Result 'Maven' ([bool](Get-Command mvn -ErrorAction SilentlyContinue)) 'command available'
Write-Result 'Node.js' ([bool](Get-Command node -ErrorAction SilentlyContinue)) 'command available'
Write-Result 'npm' ([bool](Get-Command npm -ErrorAction SilentlyContinue)) 'command available'

$devConfig = Join-Path $ApiRoot 'ruoyi-admin\src\main\resources\application-devmy.yml'
$localConfig = Join-Path $ApiRoot 'ruoyi-admin\application-local.yml'
Write-Result 'Backend config' (Test-Path -LiteralPath $devConfig) $devConfig
Write-Result 'Frontend dependencies' (Test-Path -LiteralPath (Join-Path $UiRoot 'node_modules')) (Join-Path $UiRoot 'node_modules')

$mysqlReady = Test-LocalPort 3306
$redisReady = Test-LocalPort 6379
Write-Result 'MySQL 3306' $mysqlReady '127.0.0.1:3306 must accept connections'
Write-Result 'Redis 6379' $redisReady '127.0.0.1:6379 must accept connections'

$hasModelKey = -not [string]::IsNullOrWhiteSpace($env:DEEPSEEK_API_KEY)
if (-not $hasModelKey -and (Test-Path -LiteralPath $localConfig)) {
    $configText = Get-Content -LiteralPath $localConfig -Raw -ErrorAction SilentlyContinue
    $hasModelKey = $configText -match 'deepseek-api-key:\s*["'']?sk-[^"''\s]+'
}
Write-Result 'DeepSeek API key' $hasModelKey 'configured in environment or ignored application-local.yml; value is not printed' -WarningOnly

$backendListening = Test-LocalPort 8087
$frontendListening = Test-LocalPort 8090
Write-Result 'Backend 8087' $backendListening 'running' -WarningOnly
Write-Result 'Frontend 8090' $frontendListening 'running' -WarningOnly

if (-not $SkipResourceCopy) {
    Write-Host ''
    Write-Host 'Copying Spring Boot resources...'
    Push-Location $ApiRoot
    try {
        & mvn -q -pl ruoyi-admin -am process-resources
        if ($LASTEXITCODE -ne 0) {
            throw "Maven process-resources exited with code $LASTEXITCODE"
        }
    }
    finally {
        Pop-Location
    }
}

$resourceChecks = @(
    'ruoyi-admin\target\classes\application.yml',
    'ruoyi-admin\target\classes\mybatis\mybatis-config.xml',
    'ruoyi-admin\target\classes\i18n\messages.properties',
    'ruoyi-biz\target\classes\mapper\module\RoomMapper.xml'
)
foreach ($relativePath in $resourceChecks) {
    $fullPath = Join-Path $ApiRoot $relativePath
    Write-Result "Resource $relativePath" (Test-Path -LiteralPath $fullPath) $fullPath
}

Write-Host ''
if ($hasError) {
    Write-Host 'Environment check failed. Fix FAIL items before starting the application.'
    exit 1
}

Write-Host 'Environment check completed. Start backend and frontend with the commands in AGENTS.md.'
