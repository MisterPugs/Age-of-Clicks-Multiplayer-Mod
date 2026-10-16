$ErrorActionPreference = "Stop"

$buildDir = "C:\Users\kakas\AppData\Local\Temp\cgs_modloader_build"
$srcDir = Join-Path $buildDir "src"
$binDir = Join-Path $buildDir "bin"
$luajJar = "C:\Users\kakas\AppData\Local\Temp\luaj-jse-3.0.1.jar"
$gameJar = "D:\SteamLibrary\steamapps\common\Age of Clicks\app\desktop-0.63.jar"
$targetModloaderJar = "D:\SteamLibrary\steamapps\common\Age of Clicks\app\cgs-modloader.jar"
$targetMpJar = "D:\SteamLibrary\steamapps\common\Age of Clicks\mods\multiplayer\multiplayer-mod.jar"
$javac = "C:\Program Files\Java\jdk-17\bin\javac.exe"
$jar = "C:\Program Files\Java\jdk-17\bin\jar.exe"

if (Test-Path $binDir) { Remove-Item -Recurse -Force $binDir }
New-Item -ItemType Directory -Force -Path $binDir | Out-Null

Write-Host "Extracting LuaJ into bin..."
Set-Location $binDir
& $jar -xf $luajJar

Write-Host "Gathering Java files..."
$sources = Get-ChildItem -Path $srcDir -Recurse -Filter "*.java" | Select-Object -ExpandProperty FullName
$sourceFileList = Join-Path $buildDir "sources.txt"
$sources | Out-File -FilePath $sourceFileList -Encoding ascii

Write-Host ("Compiling {0} Java sources..." -f $sources.Count)
$cp = ("{0};{1};{2}" -f $gameJar, $luajJar, $binDir)
& $javac -encoding UTF-8 -cp $cp -d $binDir ("@" + $sourceFileList)

if ($LASTEXITCODE -ne 0) {
    Write-Error "Compilation failed!"
    exit 1
}

Write-Host "Packaging cgs-modloader.jar..."
Set-Location $binDir
& $jar -cf $targetModloaderJar cgs ru org
Write-Host ("Modloader JAR size: {0}" -f (Get-Item $targetModloaderJar).Length)

Write-Host "Packaging multiplayer-mod.jar..."
& $jar -cf $targetMpJar cgs\mods\multiplayer
Write-Host ("Multiplayer JAR size: {0}" -f (Get-Item $targetMpJar).Length)

Write-Host "BUILD SUCCESSFUL!"
