@echo off
setlocal
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0.mvnw.ps1" %*
exit /b %errorlevel%
