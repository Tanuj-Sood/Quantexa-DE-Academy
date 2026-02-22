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

$defaultHadoopHome = 'C:\tools\hadoop'
if (-not $env:HADOOP_HOME -and (Test-Path $defaultHadoopHome)) {
    $env:HADOOP_HOME = $defaultHadoopHome
}

$outputBasePathWindows = Join-Path $projectRoot 'src\main\resources'
if (-not (Test-Path (Join-Path $outputBasePathWindows 'customer_data.csv'))) {
    throw "customer_data.csv not found under $outputBasePathWindows"
}

# Spark/Hadoop on Windows handles forward slashes more reliably when passed through JVM system properties.
$outputBasePathForJvm = $outputBasePathWindows -replace '\\', '/'
$env:QDE_OUTPUT_BASE_PATH = $outputBasePathForJvm

$driverJavaOptions = "-Dqde.output.base.path=$outputBasePathForJvm"
if ($env:HADOOP_HOME) {
    $hadoopHomeForJvm = $env:HADOOP_HOME -replace '\\', '/'
    $driverJavaOptions = "$driverJavaOptions -Dhadoop.home.dir=$hadoopHomeForJvm"
}

$classes = @(
    'com.quantexa.assessments.accounts.AccountAssessment',
    'com.quantexa.assessments.customerAddresses.CustomerAddress',
    'com.quantexa.assessments.scoringModel.ScoringModel'
)

foreach ($className in $classes) {
    Write-Host "Running $className" -ForegroundColor Yellow
    & $sparkSubmit --class $className --master local[*] --driver-java-options $driverJavaOptions $jarPath
}

Write-Host "Done. Parquet outputs are under $outputBasePathWindows" -ForegroundColor Green
