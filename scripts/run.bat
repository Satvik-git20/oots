@echo off
REM Use: run.bat [cycles]   (default 3)
setlocal
set ROOT=%~dp0..
javac -Xlint:all -d "%ROOT%\out" "%ROOT%\SmartTrafficSystem.java"
if errorlevel 1 (
    echo [error] Compilation failed.
    exit /b 1
)
java -cp "%ROOT%\out" SmartTrafficSystem %1
