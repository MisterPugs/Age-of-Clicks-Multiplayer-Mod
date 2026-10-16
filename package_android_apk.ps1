$ErrorActionPreference = "Stop"

$buildDir = "C:\Users\kakas\AppData\Local\Temp\cgs_modloader_build"
$apkBuild = "C:\Users\kakas\AppData\Local\Temp\android_apk_build"
$tools = "C:\Users\kakas\AppData\Local\Temp\android_tools"
$gameJar = "D:\SteamLibrary\steamapps\common\Age of Clicks\app\desktop-0.63.jar"
$binDir = Join-Path $buildDir "bin"
$jarTool = "C:\Program Files\Java\jdk-17\bin\jar.exe"
$javaTool = "C:\Program Files\Java\jdk-17\bin\java.exe"
$jarsigner = "C:\Program Files\Java\jdk-17\bin\jarsigner.exe"
$aapt2 = Join-Path $tools "aapt2.exe"
$androidJar = Join-Path $tools "android.jar"
$r8Jar = Join-Path $tools "r8.jar"

Write-Host "--- 1. PREPARING MERGED CLASSES ---"
$mergedClasses = Join-Path $apkBuild "merged_classes"
if (Test-Path $mergedClasses) { Remove-Item -Recurse -Force $mergedClasses }
New-Item -ItemType Directory -Force -Path $mergedClasses | Out-Null

Set-Location $mergedClasses
Write-Host "Extracting desktop jar classes..."
& $jarTool -xf $gameJar com ru box2dLight javazoom org

Write-Host "Removing desktop-only classes..."
if (Test-Path "$mergedClasses\com\badlogic\gdx\backends\lwjgl3") {
    Remove-Item -Recurse -Force "$mergedClasses\com\badlogic\gdx\backends\lwjgl3"
}
Get-ChildItem -Path "$mergedClasses\ru\ius\david\cgs" -Filter "Desktop*.class" | Remove-Item -Force

Write-Host "Extracting gdx-backend-android..."
& $jarTool -xf "$tools\gdx-backend-android.jar"

Write-Host "Overwriting with compiled modloader & multiplayer classes from bin using robocopy..."
& robocopy $binDir $mergedClasses /E /IS /IT | Out-Null

Write-Host "Verifying MainMenuScreen.class in merged_classes..."
$mmFile = Get-Item "$mergedClasses\ru\ius\david\cgs\MainMenuScreen.class"
Write-Host ("MainMenuScreen.class size: {0}, last modified: {1}" -f $mmFile.Length, $mmFile.LastWriteTime)

Write-Host "--- 2. PACKAGING MERGED JAR ---"
$mergedJar = Join-Path $apkBuild "merged_app.jar"
if (Test-Path $mergedJar) { Remove-Item -Force $mergedJar }
Set-Location $mergedClasses
& $jarTool -cf $mergedJar .
Write-Host ("Merged JAR created: {0:N0} bytes" -f (Get-Item $mergedJar).Length)

Write-Host "--- 3. RUNNING D8 COMPILATION ---"
$dexDir = Join-Path $apkBuild "dex_output"
if (Test-Path $dexDir) { Remove-Item -Recurse -Force $dexDir }
New-Item -ItemType Directory -Force -Path $dexDir | Out-Null

& $javaTool -Xmx4096m -cp $r8Jar com.android.tools.r8.D8 --release --min-api 21 --lib $androidJar --output $dexDir $mergedJar

Write-Host "D8 completed. Resulting DEX files:"
Get-ChildItem $dexDir | Format-Table -Property Name, Length

Write-Host "--- 4. LINKING BASE APK WITH AAPT2 ---"
$compiledRes = Join-Path $apkBuild "compiled_res.flata"
$baseApk = Join-Path $apkBuild "Age_of_Clicks_Multiplayer_v1.2.0.apk"
if (Test-Path $baseApk) { Remove-Item -Force $baseApk }
$manifest = Join-Path $apkBuild "AndroidManifest.xml"
& $aapt2 link -I $androidJar --manifest $manifest -o $baseApk $compiledRes --auto-add-overlay

Write-Host "Base APK linked: $(Get-Item $baseApk).Length bytes"

Write-Host "--- 5. INJECTING DEX, NATIVE LIBS & ASSETS ---"
Copy-Item (Join-Path $dexDir "classes.dex") (Join-Path $apkBuild "classes.dex") -Force
Copy-Item (Join-Path $dexDir "classes2.dex") (Join-Path $apkBuild "classes2.dex") -Force

Set-Location $apkBuild
& $jarTool -uf $baseApk classes.dex classes2.dex lib assets

Write-Host ("Full APK assembled! Size: {0:N2} MB" -f ((Get-Item $baseApk).Length / 1MB))

Write-Host "--- 6. SIGNING APK ---"
$keystore = Join-Path $apkBuild "debug.keystore"
& $jarsigner -sigalg SHA256withRSA -digestalg SHA-256 -keystore $keystore -storepass android -keypass android $baseApk androiddebugkey

Write-Host "--- 7. VERIFYING SIGNATURE ---"
& $jarsigner -verify $baseApk

Write-Host "--- BUILD FINISHED SUCCESSFULLY! ---"
