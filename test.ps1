param([string] $JunitJar)

$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    if (-not $JunitJar -and (Get-Command mvn -ErrorAction SilentlyContinue)) {
        & mvn test
        if ($LASTEXITCODE -ne 0) { throw 'Maven tests failed.' }
        return
    }

    if (-not $JunitJar) {
        $toolsDirectory = Join-Path $PSScriptRoot '.tools'
        $JunitJar = Join-Path $toolsDirectory 'junit-platform-console-standalone-1.11.4.jar'
        if (-not (Test-Path -LiteralPath $JunitJar)) {
            New-Item -ItemType Directory -Force $toolsDirectory | Out-Null
            $downloadUrl = 'https://repo.maven.apache.org/maven2/org/junit/platform/' +
                'junit-platform-console-standalone/1.11.4/' +
                'junit-platform-console-standalone-1.11.4.jar'
            Write-Host 'Downloading the JUnit test runner...'
            Invoke-WebRequest -Uri $downloadUrl -OutFile $JunitJar
        }
    }

    $jarPath = (Resolve-Path -LiteralPath $JunitJar).Path
    New-Item -ItemType Directory -Force out/main, out/test | Out-Null
    $mainSources = @(Get-ChildItem -Recurse src/main/java -Filter *.java |
        ForEach-Object { $_.FullName })
    $testSources = @(Get-ChildItem -Recurse src/test/java -Filter *.java |
        ForEach-Object { $_.FullName })
    & javac --release 21 -encoding UTF-8 -d out/main $mainSources
    if ($LASTEXITCODE -ne 0) { throw 'Application compilation failed.' }
    & javac --release 21 -encoding UTF-8 -cp "out/main;$jarPath" -d out/test $testSources
    if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed.' }
    & java -jar $jarPath execute --class-path 'out/main;out/test' --scan-class-path --fail-if-no-tests
    if ($LASTEXITCODE -ne 0) { throw 'JUnit tests failed.' }
} finally {
    Pop-Location
}
