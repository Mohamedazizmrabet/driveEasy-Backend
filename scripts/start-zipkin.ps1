$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '..')
docker compose up -d zipkin
Write-Host 'Zipkin UI: http://localhost:9411'
