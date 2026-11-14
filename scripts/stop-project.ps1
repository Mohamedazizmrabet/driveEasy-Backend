$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '..')
docker compose down
Write-Host 'Zipkin container stopped. Stop each Spring Boot process with Ctrl+C in its terminal.'
