$ErrorActionPreference = "Stop"
if (-not (Test-Path proxyti.jar)) { & "$PSScriptRoot\build.ps1" }
java -jar proxyti.jar $(if ($args.Count -gt 0) { $args[0] } else { "config.properties" })
