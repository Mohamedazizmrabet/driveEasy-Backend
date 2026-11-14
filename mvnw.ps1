param([Parameter(ValueFromRemainingArguments=$true)][string[]]$MavenArgs)
$ErrorActionPreference = 'Stop'
$version = '3.9.16'
$archiveName = "apache-maven-$version-bin.zip"
$baseUri = "https://archive.apache.org/dist/maven/maven-3/$version/binaries"
$installRoot = Join-Path $PSScriptRoot ".mvn\dists\apache-maven-$version"
$mavenHome = Join-Path $installRoot "apache-maven-$version"
$maven = Join-Path $mavenHome 'bin\mvn.cmd'
if (-not (Test-Path $maven)) {
    New-Item -ItemType Directory -Force -Path $installRoot | Out-Null
    $archive = Join-Path $installRoot $archiveName
    $checksumFile = "$archive.sha512"
    try {
        Invoke-WebRequest -Uri "$baseUri/$archiveName" -OutFile $archive
        Invoke-WebRequest -Uri "$baseUri/$archiveName.sha512" -OutFile $checksumFile
        $expected = ((Get-Content -Raw $checksumFile).Trim() -split '\s+')[0].ToLowerInvariant()
        $actual = (Get-FileHash -Algorithm SHA512 -LiteralPath $archive).Hash.ToLowerInvariant()
        if ($actual -ne $expected) { throw 'Maven archive SHA-512 did not match the Apache checksum.' }
        Expand-Archive -LiteralPath $archive -DestinationPath $installRoot -Force
    } finally {
        Remove-Item -LiteralPath $archive,$checksumFile -Force -ErrorAction SilentlyContinue
    }
}
& $maven @MavenArgs
exit $LASTEXITCODE
