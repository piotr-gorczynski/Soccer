Set-Location -Path "C:\Users\Dom\StudioProjects\Soccer"

# Save current branch name
$currentBranch = git rev-parse --abbrev-ref HEAD
Write-Host "🔍 Current branch: $currentBranch"

# Fetch latest remote updates
git fetch origin

# Sync local test and prod with origin
git checkout test
git pull origin test

git checkout prod
git pull origin prod

# List commits in test not in prod
Write-Host "`n📝 Commits in 'test' not in 'prod':"
git log prod..test --oneline

# Merge test into prod
Write-Host "`n🔀 Merging 'test' into 'prod'..."
git merge test

# Push updated prod branch
Write-Host "`n🚀 Pushing 'prod' to remote..."
git push origin prod

# Switch back to original branch
Write-Host "`n🔁 Switching back to original branch '$currentBranch'..."
git checkout $currentBranch

Write-Host "`n✅ Done."
