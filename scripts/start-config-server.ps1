$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '..')
./mvnw.cmd -pl config-server spring-boot:run
