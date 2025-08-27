if (-not (Test-Path (Join-Path (Get-Location) '.gitmodules'))) {
    Write-Error "Run this script from the repository root (Soccer) with 'tools\\sync-test-to-prod.ps1'."
    exit 1
}

. "$PSScriptRoot/sync-branches.ps1"
Sync-Branches -Source 'test' -Target 'prod'
