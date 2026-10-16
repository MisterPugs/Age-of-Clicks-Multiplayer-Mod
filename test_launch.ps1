$proc = Start-Process -FilePath "C:\Program Files\Java\jdk-17\bin\java.exe" -ArgumentList "-cp", "app\cgs-modloader.jar;app\desktop-0.63.jar", "cgs.modloader.ModLauncher" -PassThru -WorkingDirectory "D:\SteamLibrary\steamapps\common\Age of Clicks"
Start-Sleep -Seconds 4
if (-not $proc.HasExited) {
    Stop-Process -Id $proc.Id -Force
    Write-Host "Process ran successfully and was stopped after 4s."
} else {
    Write-Host ("Process exited early with exit code: " + $proc.ExitCode)
}
if (Test-Path "cgs.pid") { Remove-Item "cgs.pid" -Force }
