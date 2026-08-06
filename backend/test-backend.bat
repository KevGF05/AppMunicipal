@echo off
setlocal
cd /d "%~dp0"
call mvnw.cmd test
endlocal
