$psi = New-Object System.Diagnostics.ProcessStartInfo
$psi.FileName = "C:\Program Files\Java\jdk-17\bin\java.exe"
$psi.Arguments = "-cp app\cgs-modloader.jar;app\desktop-0.63.jar cgs.modloader.ModLauncher"
$psi.WorkingDirectory = "D:\SteamLibrary\steamapps\common\Age of Clicks"
$psi.RedirectStandardOutput = $true
$psi.RedirectStandardError = $true
$psi.UseShellExecute = $false
$proc = [System.Diagnostics.Process]::Start($psi)
Start-Sleep -Seconds 3
if (-not $proc.HasExited) { $proc.Kill() }
$stdout = $proc.StandardOutput.ReadToEnd()
$stderr = $proc.StandardError.ReadToEnd()
Write-Host "=== STDOUT ==="
Write-Host $stdout
Write-Host "=== STDERR ==="
Write-Host $stderr
if (Test-Path "cgs.pid") { Remove-Item "cgs.pid" -Force }
