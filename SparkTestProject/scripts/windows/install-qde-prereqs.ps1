$ErrorActionPreference = 'Stop'

function Write-Step {
    param([string]$Message)
    Write-Host "`n=== $Message ===" -ForegroundColor Cyan
}

function Add-ToMachinePathIfMissing {
    param([string]$PathToAdd)

    $currentMachinePath = [System.Environment]::GetEnvironmentVariable('Path', 'Machine')
    if ($currentMachinePath -notlike "*$PathToAdd*") {
        [System.Environment]::SetEnvironmentVariable(
            'Path',
            "$currentMachinePath;$PathToAdd",
            'Machine'
        )
    }
}

Write-Step "Checking for winget"
if (-not (Get-Command winget -ErrorAction SilentlyContinue)) {
    throw "winget is required for this script. Install App Installer from Microsoft Store and run again."
}

Write-Step "Installing Temurin JDK 8"
winget install --id EclipseAdoptium.Temurin.8.JDK --exact --accept-source-agreements --accept-package-agreements

Write-Step "Installing 7zip"
winget install --id 7zip.7zip --exact --accept-source-agreements --accept-package-agreements

Write-Step "Installing Gradle"
winget install --id Gradle.Gradle --exact --accept-source-agreements --accept-package-agreements

Write-Step "Installing Apache Spark 2.4.8 (Hadoop 2.7)"
$toolsDir = 'C:\tools'
$sparkZipPath = Join-Path $env:TEMP 'spark-2.4.8-bin-hadoop2.7.tgz'
$sparkTgzExtractDir = Join-Path $env:TEMP 'spark_extract'
$sparkInstallDir = Join-Path $toolsDir 'spark-2.4.8-bin-hadoop2.7'

New-Item -Path $toolsDir -ItemType Directory -Force | Out-Null
if (Test-Path $sparkTgzExtractDir) {
    Remove-Item -Path $sparkTgzExtractDir -Recurse -Force
}
New-Item -Path $sparkTgzExtractDir -ItemType Directory -Force | Out-Null

Invoke-WebRequest -Uri 'https://archive.apache.org/dist/spark/spark-2.4.8/spark-2.4.8-bin-hadoop2.7.tgz' -OutFile $sparkZipPath

& 'C:\Program Files\7-Zip\7z.exe' x $sparkZipPath "-o$sparkTgzExtractDir" -y | Out-Null
$innerTar = Join-Path $sparkTgzExtractDir 'spark-2.4.8-bin-hadoop2.7.tar'
& 'C:\Program Files\7-Zip\7z.exe' x $innerTar "-o$toolsDir" -y | Out-Null

Write-Step "Setting machine environment variables"
$javaHome = (Get-ChildItem 'C:\Program Files\Eclipse Adoptium' -Directory | Sort-Object Name -Descending | Select-Object -First 1).FullName
if (-not $javaHome) {
    throw 'JDK install not found under C:\Program Files\Eclipse Adoptium'
}

[System.Environment]::SetEnvironmentVariable('JAVA_HOME', $javaHome, 'Machine')
[System.Environment]::SetEnvironmentVariable('SPARK_HOME', $sparkInstallDir, 'Machine')

Add-ToMachinePathIfMissing "$javaHome\bin"
Add-ToMachinePathIfMissing "$sparkInstallDir\bin"

Write-Step "Installation complete"
Write-Host 'Open a new PowerShell window, then verify:' -ForegroundColor Green
Write-Host '  java -version'
Write-Host '  gradle -v'
Write-Host '  spark-submit --version'
