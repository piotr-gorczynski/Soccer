@echo off
echo Exporting Firestore structure...
node --no-deprecation index.js %1 %2
echo Done.
pause
