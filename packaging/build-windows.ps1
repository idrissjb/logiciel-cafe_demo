# Construit l'application Windows autonome (dist\Cafe Manager\Cafe Manager.exe) et le zip a publier.
# Usage (depuis la racine du projet) :  powershell -ExecutionPolicy Bypass -File packaging\build-windows.ps1
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$name = 'Cafe Manager'
$version = '1.0.0'
$dist = Join-Path $root 'dist'
$zip = Join-Path $dist "CafeManager-$version-windows.zip"

if (-not (Test-Path 'packaging\cafe.ico')) { & powershell -ExecutionPolicy Bypass -File packaging\make-icon.ps1 }

Write-Host '1/3 Compilation Maven...'
mvn -q clean package
if ($LASTEXITCODE -ne 0) { throw 'Echec de mvn package' }

Write-Host '2/3 jpackage (Java + JavaFX embarques)...'
Copy-Item "target\cafe-manager-$version.jar" target\libs
if (Test-Path $dist) { Remove-Item $dist -Recurse -Force }
New-Item -ItemType Directory $dist | Out-Null
jpackage --type app-image `
    --name $name `
    --app-version $version `
    --vendor 'Cafe Manager' `
    --description 'Caisse pour cafe / cafeteria' `
    --input target\libs `
    --main-jar "cafe-manager-$version.jar" `
    --main-class com.cafemanager.Launcher `
    --icon packaging\cafe.ico `
    --java-options '-Dfile.encoding=UTF-8' `
    --dest $dist
if ($LASTEXITCODE -ne 0) { throw 'Echec de jpackage' }

Write-Host '3/3 Creation du zip...'
Copy-Item packaging\LEIAME.txt (Join-Path $dist $name)
Compress-Archive -Path (Join-Path $dist $name) -DestinationPath $zip -Force
Write-Host "Termine : $zip"
