$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$proxyJar = Join-Path $root "proxyti.jar"
if (-not (Test-Path $proxyJar)) {
    & (Join-Path $root "build.ps1")
}

$jdk = Get-ChildItem "C:\Program Files\Java\jdk-*" -Directory | Sort-Object Name -Descending | Select-Object -First 1
if (-not $jdk) { throw "A full JDK installation was not found under C:\Program Files\Java" }

Remove-Item -Recurse -Force (Join-Path $PSScriptRoot "out") -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force (Join-Path $PSScriptRoot "out") | Out-Null

$sources = Get-ChildItem -Recurse -Filter *.java (Join-Path $PSScriptRoot "src") | ForEach-Object { $_.FullName }
& (Join-Path $jdk.FullName "bin\javac.exe") -encoding UTF-8 -cp $proxyJar -d (Join-Path $PSScriptRoot "out") $sources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Copy-Item (Join-Path $PSScriptRoot "plugin.properties") (Join-Path $PSScriptRoot "out\plugin.properties")
& (Join-Path $jdk.FullName "bin\jar.exe") --create --file (Join-Path $PSScriptRoot "motd-plugin.jar") -C (Join-Path $PSScriptRoot "out") .
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "Built examples/motd-plugin/motd-plugin.jar"
Write-Host "Copy it into the proxy's plugins directory to enable it."