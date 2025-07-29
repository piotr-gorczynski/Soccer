Set-Location -Path "C:\Users\Dom\StudioProjects\Soccer"

# Save current branch name
$currentBranch = git rev-parse --abbrev-ref HEAD
Write-Host "🔍 Current branch: $currentBranch"

# Fetch latest remote updates
git fetch origin

# Sync local dev and test with origin
git checkout dev
git pull origin dev

git checkout test
git pull origin test

# List commits in dev not in test
Write-Host "`n📝 Commits in 'dev' not in 'test':"
git log test..dev --oneline

# Merge dev into test
Write-Host "`n🔀 Merging 'dev' into 'test'..."
git merge dev

# Push updated test branch
Write-Host "`n🚀 Pushing 'test' to remote..."
git push origin test

# Switch back to original branch
Write-Host "`n🔁 Switching back to original branch '$currentBranch'..."
git checkout $currentBranch

Write-Host "`n✅ Done."
