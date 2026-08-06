@echo off
setlocal
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\gradle.ps1" %*
exit /b %ERRORLEVEL%
