$ErrorActionPreference = "Stop"

Remove-Item -Recurse -Force out -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force out | Out-Null

$sources = Get-ChildItem -Recurse -Filter *.java src | ForEach-Object { $_.FullName }
$javaHome = Get-ChildItem "C:\Program Files\Java\jdk-*" -Directory | Sort-Object Name -Descending | Select-Object -First 1
if (-not $javaHome) { throw "A full JDK installation was not found under C:\Program Files\Java" }
& (Join-Path $javaHome.FullName "bin\javac.exe") -encoding UTF-8 -Xlint:all -d out $sources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& (Join-Path $javaHome.FullName "bin\jar.exe") --create --file proxyti.jar --main-class proxyti.Main -C out .
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "Built proxyti.jar"
