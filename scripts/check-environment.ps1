$ErrorActionPreference = 'Continue'
Write-Host 'DriveEasy environment check' -ForegroundColor Cyan
Write-Host "OS: $([System.Runtime.InteropServices.RuntimeInformation]::OSDescription)"
foreach ($tool in @('java','node','npm','git','docker','ng','postman')) {
    $command = Get-Command $tool -ErrorAction SilentlyContinue
    if (-not $command) {
        Write-Host "${tool}: missing or not on PATH" -ForegroundColor Yellow
        continue
    }
    Write-Host "${tool}: $($command.Source)"
    switch ($tool) {
        'java' { & java -version }
        'node' { & node --version }
        'npm' { & npm.cmd --version }
        'git' { & git --version }
        'docker' { & docker --version; & docker info --format 'Engine {{.ServerVersion}}' 2>$null }
        'ng' { Write-Host 'Angular CLI found; run ng version to inspect it.' }
        'postman' { & postman --version }
    }
}
Write-Host 'Maven builds use ./mvnw.cmd once the wrapper bootstrap is available.'
