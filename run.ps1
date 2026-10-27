param([switch] $Console)
$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    New-Item -ItemType Directory -Force out | Out-Null
    $sources = @(Get-ChildItem -Recurse src/main/java -Filter *.java |
        ForEach-Object { $_.FullName })
    & javac --release 21 -encoding UTF-8 -d out $sources
    if ($LASTEXITCODE -ne 0) { throw 'Java compilation failed.' }
    $arguments = @('-cp', 'out', 'contrast.Main')
    if ($Console) { $arguments += '--console' }
    & java @arguments
    if ($LASTEXITCODE -ne 0) { throw 'Application failed.' }
} finally {
    Pop-Location
}
