@echo off
powershell -NoProfile -Command "try { Invoke-RestMethod http://localhost:8080/api/public/health | ConvertTo-Json } catch { Write-Host 'El backend no responde. Inicie INICIAR_BACKEND.bat primero.' -ForegroundColor Red; exit 1 }"
pause
