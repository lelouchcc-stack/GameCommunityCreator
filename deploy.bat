@echo off
REM GameCommunityCreator one-click deploy (Windows launcher).
REM
REM This file must stay PURE ASCII: cmd.exe reads batch files byte by byte, and
REM multi-byte UTF-8 characters swallow the line ending, which corrupts the script.
REM All localized messages live in deploy.ps1, which is UTF-8 WITH BOM so that
REM Windows PowerShell 5.1 (which defaults to ANSI) reads it correctly.
REM
REM The block below re-adds the BOM if some editor stripped it, then runs the script.
cd /d "%~dp0"
set "PS1=%~dp0deploy.ps1"
powershell -NoProfile -ExecutionPolicy Bypass -Command "$p='%PS1%'; $b=[IO.File]::ReadAllBytes($p); if (-not ($b.Length -ge 3 -and $b[0] -eq 0xEF -and $b[1] -eq 0xBB -and $b[2] -eq 0xBF)) { [IO.File]::WriteAllBytes($p, [byte[]](0xEF,0xBB,0xBF) + $b); Write-Host '[i] deploy.ps1 lost its UTF-8 BOM; restored it.' }"
powershell -NoProfile -ExecutionPolicy Bypass -File "%PS1%"
exit /b %ERRORLEVEL%
