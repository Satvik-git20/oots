# OOTS — compile and run the Smart Traffic Monitoring prototype.
# Usage:  .\scripts\run.ps1 [cycles]   (default 3)
param([int]$Cycles = 3)

$root = Split-Path -Parent $PSScriptRoot
Write-Host "[1/2] Compiling with javac -Xlint:all ..." -ForegroundColor Cyan
& javac -Xlint:all -d (Join-Path $root "out") (Join-Path $root "SmartTrafficSystem.java")
if ($LASTEXITCODE -ne 0) { Write-Error "Compilation failed."; exit 1 }
Write-Host "[2/2] Running SmartTrafficSystem with cycles = $Cycles ..." -ForegroundColor Cyan
& java -cp (Join-Path $root "out") SmartTrafficSystem $Cycles
