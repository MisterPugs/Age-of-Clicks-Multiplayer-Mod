$ErrorActionPreference = "Stop"
$gameDir = "d:\SteamLibrary\steamapps\common\Age of Clicks"
$zipPath = Join-Path $gameDir "Age_of_Clicks_Multiplayer_Mod.zip"
$tempDir = Join-Path $env:TEMP "aoc_mod_pack"

if (Test-Path $tempDir) { Remove-Item -Recurse -Force $tempDir }
New-Item -ItemType Directory -Path (Join-Path $tempDir "app") -Force | Out-Null
New-Item -ItemType Directory -Path (Join-Path $tempDir "mods") -Force | Out-Null

Copy-Item (Join-Path $gameDir "app\cgs-modloader.jar") (Join-Path $tempDir "app\cgs-modloader.jar")
Copy-Item (Join-Path $gameDir "app\Age of Clicks.cfg") (Join-Path $tempDir "app\Age of Clicks.cfg")
Copy-Item (Join-Path $gameDir "mods\*") (Join-Path $tempDir "mods") -Recurse -Force

$readmeText = @"
=====================================================
  Age of Clicks - Multiplayer Mod & ModLoader (v1.2.0)
=====================================================

ИНСТРУКЦИЯ ПО УСТАНОВКЕ:
1. Зайдите в Steam -> Библиотека.
2. Кликните правой кнопкой мыши по 'Age of Clicks' -> 'Управление' -> 'Просмотреть локальные файлы'.
3. Скопируйте папки 'app' и 'mods' из этого архива прямо в открывшуюся папку игры.
   (Нажмите 'Заменить файлы в папке назначения', если система спросит).
4. Запустите игру обычным способом через Steam!

НОВЫЕ ВОЗМОЖНОСТИ И ГОРЯЧИЕ КЛАВИШИ В ИГРЕ:
- Расширенное лобби сценариев — выбор официальных карт, сценариев из Steam Workshop и кастомных модов с категориями и пагинацией.
- Автодокачка Steam Workshop — если у хоста выбран сценарий из мастерской, клиенты могут нажать 'СКАЧАТЬ В STEAM' прямо в лобби!
- [F7] — Сетевой ленд-лиз: выбор любой страны мира (или только союзников), передача золота (+10k, +50k, +250k) и дивизий, полная блокировка прокликивания карты
- [F6] — Сохранение сетевого матча (хост сохраняет игру для всех, saves/mp/)
- [Enter] или [T] — Внутриигровой чат с союзниками и глобальный чат
- [Alt+ЛКМ] / [Shift+ЛКМ] — Тактические пинги по карте для союзников
- [Tab] — Сетевая таблица матча (счёт, казна, дивизии, пинг)
- [DEFCON 1] — Автоматическая ядерная тревога, сирена и таймер до удара МБР
- [Hot-Reconnect] — При вылете или перезаходе игрок сразу возвращается на свою страну
- [F5] — Мгновенная горячая перезагрузка скриптов и модов
- Авто-обновление — Игра автоматически уведомит при выходе новой версии на GitHub!
=====================================================
"@

Set-Content -Path (Join-Path $tempDir "ИНСТРУКЦИЯ.txt") -Value $readmeText -Encoding UTF8

if (Test-Path $zipPath) { Remove-Item -Force $zipPath }
Compress-Archive -Path (Join-Path $tempDir "*") -DestinationPath $zipPath -Force
Write-Host "SUCCESS: Created $zipPath"
Get-Item $zipPath | Select-Object Name, Length
