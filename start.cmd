@echo off
setlocal
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0start.ps1"
set "code=%errorlevel%"
endlocal & exit /b %code%
