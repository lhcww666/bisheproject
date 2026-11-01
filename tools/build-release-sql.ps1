[CmdletBinding()]
param(
    [string]$OutputPath = "sql/release/01-bishe_schema_full.sql"
)

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$outputFile = Join-Path $repoRoot $OutputPath
$releaseDir = Split-Path -Parent $outputFile

function Optimize-RegionInserts {
    param(
        [string]$Content,
        [string]$RelativePath
    )

    if ($RelativePath -ne "api/sql/region-mysql.sql") {
        return $Content
    }

    $lines = $Content -split "\r?\n"
    $optimized = [System.Text.StringBuilder]::new()
    $batch = [System.Collections.Generic.List[string]]::new()
    $transactionStarted = $false
    $stats = @{ OriginalRows = 0; OptimizedRows = 0 }

    function Flush-RegionBatch {
        if ($batch.Count -eq 0) {
            return
        }
        [void]$optimized.AppendLine("INSERT INTO ``biz_region`` VALUES")
        for ($index = 0; $index -lt $batch.Count; $index++) {
            $suffix = if ($index -lt ($batch.Count - 1)) { "," } else { ";" }
            [void]$optimized.AppendLine("  $($batch[$index])$suffix")
        }
        $stats.OptimizedRows += $batch.Count
        $batch.Clear()
    }

    foreach ($line in $lines) {
        $match = [regex]::Match($line, '^INSERT INTO `biz_region` VALUES (.+);$')
        if ($match.Success) {
            if (-not $transactionStarted) {
                [void]$optimized.AppendLine("-- Optimized from individual region inserts")
                [void]$optimized.AppendLine("START TRANSACTION;")
                $transactionStarted = $true
            }
            $stats.OriginalRows++
            $batch.Add($match.Groups[1].Value)
            if ($batch.Count -ge 500) {
                Flush-RegionBatch
            }
        }
        else {
            Flush-RegionBatch
            [void]$optimized.AppendLine($line)
        }
    }
    Flush-RegionBatch
    if ($transactionStarted) {
        [void]$optimized.AppendLine("COMMIT;")
    }

    if ($stats.OriginalRows -ne $stats.OptimizedRows) {
        throw "Region row count changed during optimization: source=$($stats.OriginalRows), optimized=$($stats.OptimizedRows)"
    }
    return $optimized.ToString()
}

$sourceFiles = @(
    "api/sql/ruoyi-mysql.sql",
    "api/sql/magic-api-mysql.sql",
    "api/sql/region-mysql.sql",
    "sql/module_core.sql",
    "sql/module/03-field-label-alignment.sql",
    "sql/module/04-module-audit-columns.sql",
    "sql/module/01-module-dict.sql",
    "sql/module/02-gen-table-config.sql",
    "sql/module/05-module-seed-data.sql",
    "sql/module/roomMenu.sql",
    "sql/module/deviceMenu.sql",
    "sql/module/reservationDeviceMenu.sql",
    "sql/module/reservationMenu.sql",
    "sql/module/reservationReviewMenu.sql",
    "sql/module/safetyExamRecordMenu.sql",
    "sql/module/creditRecordMenu.sql",
    "sql/module/00-module-menu-directory.sql",
    "sql/module/06-reservation-biz-buttons.sql",
    "sql/module/07-reservation-review-button.sql",
    "sql/module/08-role-menu-binding.sql",
    "sql/module/10-room-access.sql",
    "sql/module/11-class-representative.sql",
    "sql/module/12-demo-class-data.sql",
    "sql/module/13-reservation-audit.sql",
    "sql/module/14-menu-data-maintenance.sql",
    "sql/module/15-reservation-page-structure.sql",
    "sql/module/16-module-list-components.sql",
    "sql/module/17-module-page-convergence.sql",
    "sql/module/20-reservation-lifecycle.sql",
    "sql/module/21-credit-violation.sql",
    "sql/module/22-safety-exam.sql",
    "sql/module/23-stats-dashboard.sql",
    "sql/module/24-agent.sql",
    "sql/module/25-agent-menu-rename.sql",
    "sql/module/26-agent-rag.sql",
    "sql/module/27-agent-reservation-draft.sql",
    "sql/module/28-agent-audit.sql",
    "sql/module/29-agent-eval.sql",
    "sql/module/30-agent-knowledge-expansion.sql",
    "sql/module/31-agent-eval-heldout.sql",
    "sql/module/32-agent-eligibility-tool.sql",
    "sql/module/33-agent-current-room.sql",
    "sql/module/34-device-expansion.sql",
    "sql/module/35-menu-navigation.sql",
    "sql/module/36-menu-icons.sql",
    "sql/module/37-trim-hjl-test-role.sql",
    "sql/module/38-agent-knowledge-vector-expansion.sql",
    "sql/module/39-agent-tool-message-link.sql",
    "sql/module/40-agent-knowledge-management.sql",
    "sql/module/41-agent-platform-help.sql",
    "sql/module/42-material.sql"
)

$missingFiles = @()
foreach ($relativePath in $sourceFiles) {
    $absolutePath = Join-Path $repoRoot $relativePath
    if (-not (Test-Path -LiteralPath $absolutePath -PathType Leaf)) {
        $missingFiles += $relativePath
    }
}
if ($missingFiles.Count -gt 0) {
    throw "Missing SQL source files: $($missingFiles -join ', ')"
}

$builder = [System.Text.StringBuilder]::new()
[void]$builder.AppendLine("-- Campus laboratory platform full database initialization")
[void]$builder.AppendLine("-- Target database engine: MySQL 8.x")
[void]$builder.AppendLine("-- Target database name: bishe_schema")
[void]$builder.AppendLine("-- Generated by tools/build-release-sql.ps1. Do not edit manually.")
[void]$builder.AppendLine("-- WARNING: This script drops and recreates system and business tables.")
[void]$builder.AppendLine()
[void]$builder.AppendLine("SET NAMES utf8mb4;")
[void]$builder.AppendLine("SET FOREIGN_KEY_CHECKS = 0;")
[void]$builder.AppendLine("CREATE DATABASE IF NOT EXISTS ``bishe_schema`` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;")
[void]$builder.AppendLine("USE ``bishe_schema``;")
[void]$builder.AppendLine("SET FOREIGN_KEY_CHECKS = 1;")
[void]$builder.AppendLine()

foreach ($relativePath in $sourceFiles) {
    $absolutePath = Join-Path $repoRoot $relativePath
    $content = Get-Content -LiteralPath $absolutePath -Raw -Encoding UTF8
    if ($null -eq $content) {
        $content = ""
    }

    $startDelimiters = ([regex]::Matches($content, '(?m)^DELIMITER\s+\$\$\s*$')).Count
    $endDelimiters = ([regex]::Matches($content, '(?m)^DELIMITER\s+;\s*$')).Count
    if ($startDelimiters -ne $endDelimiters) {
        throw "DELIMITER mismatch in ${relativePath}: block=$startDelimiters, reset=$endDelimiters"
    }
    $content = Optimize-RegionInserts -Content $content -RelativePath $relativePath

    [void]$builder.AppendLine("-- >>> BEGIN: $relativePath")
    [void]$builder.AppendLine($content.TrimEnd())
    [void]$builder.AppendLine()
    [void]$builder.AppendLine("-- <<< END: $relativePath")
    [void]$builder.AppendLine()
}

[void]$builder.AppendLine("-- Key data checks after initialization")
[void]$builder.AppendLine("SELECT 'sys_user' AS table_name, COUNT(*) AS row_count FROM sys_user")
[void]$builder.AppendLine("UNION ALL SELECT 'sys_menu', COUNT(*) FROM sys_menu")
[void]$builder.AppendLine("UNION ALL SELECT 'room', COUNT(*) FROM room")
[void]$builder.AppendLine("UNION ALL SELECT 'device', COUNT(*) FROM device")
[void]$builder.AppendLine("UNION ALL SELECT 'reservation', COUNT(*) FROM reservation")
[void]$builder.AppendLine("UNION ALL SELECT 'material', COUNT(*) FROM material")
[void]$builder.AppendLine("UNION ALL SELECT 'knowledge_document', COUNT(*) FROM knowledge_document")
[void]$builder.AppendLine("UNION ALL SELECT 'agent_eval_case', COUNT(*) FROM agent_eval_case;")
[void]$builder.AppendLine()
[void]$builder.AppendLine("SET FOREIGN_KEY_CHECKS = 1;")

New-Item -ItemType Directory -Path $releaseDir -Force | Out-Null
$utf8NoBom = [System.Text.UTF8Encoding]::new($false)
[System.IO.File]::WriteAllText($outputFile, $builder.ToString(), $utf8NoBom)

$hash = Get-FileHash -LiteralPath $outputFile -Algorithm SHA256
$hashFile = Join-Path $releaseDir "SHA256SUMS.txt"
$hashLine = "$($hash.Hash.ToLowerInvariant())  $([System.IO.Path]::GetFileName($outputFile))" +
    [Environment]::NewLine
[System.IO.File]::WriteAllText($hashFile, $hashLine, $utf8NoBom)

$file = Get-Item -LiteralPath $outputFile
Write-Host "Generated: $($file.FullName)"
Write-Host "Size: $([math]::Round($file.Length / 1MB, 2)) MB"
Write-Output $hash.Hash.ToLowerInvariant()
