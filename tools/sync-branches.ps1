$ErrorActionPreference = 'Stop'

function Assert-CleanState {
  $porcelain = git status --porcelain
  if ($porcelain) { throw "Working tree not clean. Commit/stash first." }
  if (git rev-parse -q --verify MERGE_HEAD 2>$null) { throw "A merge is in progress. Finish or abort it first." }
  $rebasing = (Test-Path .git/rebase-merge) -or (Test-Path .git/rebase-apply)
  if ($rebasing) { throw "A rebase is in progress. Finish or abort it first." }
}

function Update-Submodules {
  git submodule sync --recursive | Out-Null
  git submodule update --init --recursive | Out-Null
}

function Assert-SubmodulePresent {
  param([string]$branch)
  $ls = (git ls-files -s secrets 2>$null)
  if (-not $ls -or -not ($ls -match '^160000\s+[0-9a-f]{40}\s+0\s+secrets')) {
    throw "Branch '$branch' does not track 'secrets' as a submodule (gitlink). Fix by making it a submodule on '$branch'."
  }
}

function Pull-FF([string]$remoteBranch) {
  git pull --ff-only origin $remoteBranch | Out-Null
}

function Switch-And-Prep([string]$branch) {
  git switch $branch | Out-Null
  Pull-FF $branch
  Update-Submodules
  Assert-SubmodulePresent $branch
}

function Sync-Branches {
  param(
    [Parameter(Mandatory=$true)][string]$Source,
    [Parameter(Mandatory=$true)][string]$Target
  )

  $originalBranch = (git rev-parse --abbrev-ref HEAD).Trim()
  Write-Host "🔍 Current branch: $originalBranch"

  try {
    Assert-CleanState
    git fetch --all --prune | Out-Null

    Switch-And-Prep $Source
    Switch-And-Prep $Target

    Write-Host "`n📝 Commits in '$Source' not in '$Target':"
    git log --oneline "$Target..$Source"

    Write-Host "`n🔀 Merging '$Source' into '$Target'..."
    git merge --no-edit $Source

    $unmerged = git diff --name-only --diff-filter=U
    if ($unmerged) { throw "Merge produced conflicts. Resolve them and re-run. Conflicted paths:`n$unmerged" }

    Write-Host "`n🚀 Pushing '$Target' to remote..."
    git push origin $Target

    Write-Host "`n✅ Done."
  }
  catch {
    Write-Host "💥 ERROR: $($_.Exception.Message)"
  }
  finally {
    try {
      if (((git rev-parse --abbrev-ref HEAD).Trim()) -ne $originalBranch) {
        Write-Host "`n🔁 Switching back to original branch '$originalBranch'..."
        git switch $originalBranch | Out-Null
      }
    } catch {}
  }
}
