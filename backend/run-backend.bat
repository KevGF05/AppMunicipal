@echo off
setlocal
cd /d "%~dp0"
call mvnw.cmd spring-boot:run
endlocal
