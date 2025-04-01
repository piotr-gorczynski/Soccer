# Replace android.support.* imports with androidx.* equivalents

$sourceFiles = Get-ChildItem -Recurse -Filter *.java -Path ".\mobile\app\src\main\java"

foreach ($file in $sourceFiles) {
    (Get-Content $file.FullName) |
        ForEach-Object {
            $_ -replace 'android\.support\.v7\.app\.AppCompatActivity', 'androidx.appcompat.app.AppCompatActivity' `
               -replace 'android\.support\.v7\.app\.AlertDialog', 'androidx.appcompat.app.AlertDialog' `
               -replace 'android\.support\.v7\.widget\.Toolbar', 'androidx.appcompat.widget.Toolbar' `
               -replace 'android\.support\.annotation\.Nullable', 'androidx.annotation.Nullable' `
               -replace 'android\.support\.annotation\.LayoutRes', 'androidx.annotation.LayoutRes' `
               -replace 'android\.support\.v4\.content\.ContextCompat', 'androidx.core.content.ContextCompat'
        } | Set-Content $file.FullName
}

Write-Host "`n✅ Migration complete. You can now rebuild your project." -ForegroundColor Green
