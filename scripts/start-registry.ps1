$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '..')
./mvnw.cmd -pl service-registry spring-boot:run
