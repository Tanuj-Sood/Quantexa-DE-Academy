$ErrorActionPreference = 'Stop'

if (-not $env:SPARK_HOME) {
    throw 'SPARK_HOME is not set. Set SPARK_HOME to your Spark installation and re-run.'
}

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
Set-Location $projectRoot

Write-Host 'Building jar with Gradle...' -ForegroundColor Cyan
gradle clean jar

$jarPath = Get-ChildItem -Path (Join-Path $projectRoot 'build\libs') -Filter '*.jar' |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1 -ExpandProperty FullName

if (-not $jarPath) {
    throw 'No jar found in build\libs after gradle jar.'
}

Write-Host "Using jar: $jarPath" -ForegroundColor Cyan

$sparkSubmit = Join-Path $env:SPARK_HOME 'bin\spark-submit.cmd'
if (-not (Test-Path $sparkSubmit)) {
    throw "spark-submit not found at $sparkSubmit"
}

$outputBasePath = Join-Path $projectRoot 'src\main\resources'

$classes = @(
    'com.quantexa.assessments.accounts.AccountAssessment',
    'com.quantexa.assessments.customerAddresses.CustomerAddress',
    'com.quantexa.assessments.scoringModel.ScoringModel'
)

foreach ($className in $classes) {
    Write-Host "Running $className" -ForegroundColor Yellow
    & $sparkSubmit --class $className --master local[*] --driver-java-options "-Dqde.output.base.path=$outputBasePath" $jarPath
}

Write-Host 'Done. Parquet outputs are under src\main\resources\' -ForegroundColor Green
