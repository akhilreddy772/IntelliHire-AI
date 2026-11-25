@echo off
REM ── IntelliHire AI — Java Desktop App Launcher ──────────────────────────
REM Run this from the IntelliHire-AI root (or double-click it in Apps\).
cd /d %~dp0
echo Starting IntelliHire AI Java Desktop App...
echo Login credentials:  srm / ap   OR   recruiter / recruiter123
echo.
java -cp ".;sqlite-jdbc-3.49.1.0.jar" JavaApp
pause
