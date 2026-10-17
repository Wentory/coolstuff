@echo off
setlocal
title Coolstuff - Minecraft Client
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\launch-client.ps1" %*
if errorlevel 1 (
    echo.
    echo Client launch failed. See the error above.
    pause
)
endlocal
