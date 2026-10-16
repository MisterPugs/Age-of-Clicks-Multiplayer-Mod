$ErrorActionPreference = "Stop"

$gameDir = "D:\SteamLibrary\steamapps\common\Age of Clicks"
$java = "C:\Program Files\Java\jdk-17\bin\java.exe"
$cp = "app\cgs-modloader.jar;app\desktop-0.63.jar"
$libPath = "runtime\bin;runtime\bin\server"

$hostLog = Join-Path $gameDir "test_host.log"
$hostErr = Join-Path $gameDir "test_host_err.log"
$specLog = Join-Path $gameDir "test_spectator.log"
$specErr = Join-Path $gameDir "test_spectator_err.log"

Remove-Item -Force -ErrorAction SilentlyContinue $hostLog, $hostErr, $specLog, $specErr

Write-Host "Cleaning up previous game processes..."
Get-Process -Name "java", "Age of Clicks" -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 1

Write-Host ">>> LAUNCHING CLIENT 1 (HOST)..."
$hostProc = Start-Process -FilePath $java -ArgumentList @(
    "-Xmx2048m",
    "-Dcgs.allowMultiple=true",
    "-Dcgs.dontRedirectLog=true",
    "-Dcgs.test.role=host",
    "-Djava.library.path=$libPath",
    "-cp", $cp,
    "cgs.modloader.ModLauncher"
) -WorkingDirectory $gameDir -PassThru -RedirectStandardOutput $hostLog -RedirectStandardError $hostErr

Write-Host ("Client 1 started with PID: {0}" -f $hostProc.Id)
Start-Sleep -Seconds 4

Write-Host ">>> LAUNCHING CLIENT 2 (SPECTATOR)..."
$specProc = Start-Process -FilePath $java -ArgumentList @(
    "-Xmx2048m",
    "-Dcgs.allowMultiple=true",
    "-Dcgs.dontRedirectLog=true",
    "-Dcgs.test.role=spectator",
    "-Djava.library.path=$libPath",
    "-cp", $cp,
    "cgs.modloader.ModLauncher"
) -WorkingDirectory $gameDir -PassThru -RedirectStandardOutput $specLog -RedirectStandardError $specErr

Write-Host ("Client 2 started with PID: {0}" -f $specProc.Id)

$hostVerified = $false
$specVerified = $false

$stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
while ($stopwatch.Elapsed.TotalSeconds -lt 60) {
    Start-Sleep -Seconds 2

    if (Test-Path $hostLog) {
        $hostContent = Get-Content $hostLog -Raw -ErrorAction SilentlyContinue
        if ($hostContent -and $hostContent -match "All actions dispatched") {
            if (-not $hostVerified) {
                Write-Host "[OK] Client 1 (Host) verified all directives dispatched!" -ForegroundColor Green
                $hostVerified = $true
            }
        }
    }

    if (Test-Path $specLog) {
        $specContent = Get-Content $specLog -Raw -ErrorAction SilentlyContinue
        if ($specContent -and $specContent -match "Audit complete") {
            if (-not $specVerified) {
                Write-Host "[OK] Client 2 (Spectator) verified network sync audit!" -ForegroundColor Green
                $specVerified = $true
            }
        }
    }

    if ($hostVerified -and $specVerified) {
        Write-Host "==================================================" -ForegroundColor Cyan
        Write-Host "  BOTH CLIENTS VERIFIED AND AUDIT COMPLETED!" -ForegroundColor Green
        Write-Host "==================================================" -ForegroundColor Cyan
        break
    }
}

Start-Sleep -Seconds 2

Write-Host "=== HOST LOG (tail 40 lines) ==="
if (Test-Path $hostLog) {
    Get-Content $hostLog -Tail 40
}

Write-Host "=== SPECTATOR LOG (tail 40 lines) ==="
if (Test-Path $specLog) {
    Get-Content $specLog -Tail 40
}

Write-Host "Terminating test instances..."
try { $hostProc.Kill() } catch {}
try { $specProc.Kill() } catch {}
Get-Process -Name "java" -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue

if ($hostVerified -and $specVerified) {
    Write-Host "TEST RESULT: ALL TESTS PASSED!" -ForegroundColor Green
    exit 0
} else {
    Write-Host "TEST RESULT: TIMEOUT OR PARTIAL VERIFICATION" -ForegroundColor Yellow
    exit 1
}
