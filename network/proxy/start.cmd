@echo off
title ReinaCraft - Velocity Proxy
cd /d "%~dp0"
call "%~dp0..\..\scripts\env.cmd"

echo.
echo ============================================
echo   ReinaCraft - Velocity Proxy Starting...
echo ============================================
echo.

"%JAVA%" ^
  -Xms512M -Xmx1G ^
  -XX:+UseG1GC ^
  -XX:G1HeapRegionSize=4M ^
  -XX:+UnlockExperimentalVMOptions ^
  -XX:+ParallelRefProcEnabled ^
  -XX:+AlwaysPreTouch ^
  -XX:MaxInlineLevel=15 ^
  -jar velocity.jar

echo.
echo Velocity stopped. Press any key to close.
pause >nul
