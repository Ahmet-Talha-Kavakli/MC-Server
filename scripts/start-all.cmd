@echo off
REM Master script - launches proxy + hub + bedwars in separate windows
echo.
echo Launching ReinaCraft network...
echo.

start "" cmd /c "%~dp0..\network\hub\start.cmd"
timeout /t 4 /nobreak >nul

start "" cmd /c "%~dp0..\network\bedwars\start.cmd"
timeout /t 4 /nobreak >nul

start "" cmd /c "%~dp0..\network\proxy\start.cmd"

echo.
echo All three servers launched in separate windows.
echo Connect to 127.0.0.1:25565 once all are ready.
echo.
pause
