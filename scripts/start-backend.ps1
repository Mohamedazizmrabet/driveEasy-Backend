param([Parameter(Mandatory=$true)][ValidateSet('vehicule-service','client-service','location-service','api-gateway')][string]$Service)
$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '..')
./mvnw.cmd -pl $Service spring-boot:run
