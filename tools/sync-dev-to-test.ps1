if (-not (Test-Path (Join-Path (Get-Location) '.gitmodules'))) {
    Write-Error "Run this script from the repository root (Soccer) with 'tools\\sync-dev-to-test.ps1'."
    exit 1
}

. "$PSScriptRoot/sync-branches.ps1"
Sync-Branches -Source 'dev' -Target 'test'
