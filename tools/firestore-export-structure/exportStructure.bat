@echo off
echo Exporting Firestore structure...
node --no-deprecation "%~dp0index.js" %1 %2
echo Done.
pause
