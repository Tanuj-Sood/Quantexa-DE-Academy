# Local setup and ETL run scripts

This folder contains helper scripts for running this assignment locally.

## Windows install script

Run PowerShell as Administrator and execute:

```powershell
Set-ExecutionPolicy Bypass -Scope Process -Force
.\scripts\windows\install-qde-prereqs.ps1
```

The script installs:

- Temurin JDK 8
- Gradle
- 7zip
- Apache Spark 2.4.8 (Hadoop 2.7)

It also sets machine-level `JAVA_HOME`, `SPARK_HOME`, and updates `Path`.

## Build and run all assessments (Windows)

From `SparkTestProject`:

```powershell
.\scripts\etl\run_assessments_local.ps1
```

This will:

1. Build a jar (`gradle clean jar`)
2. Run `AccountAssessment`
3. Run `CustomerAddress`
4. Run `ScoringModel`

The runners pass `-Dqde.output.base.path=<absolute project src/main/resources path>` so reads/writes are stable regardless of your current terminal folder.

## Build and run all assessments (bash)

```bash
chmod +x ./scripts/etl/run_assessments_local.sh
./scripts/etl/run_assessments_local.sh
```

## Output locations

The jobs write parquet outputs to:

- `src/main/resources/customerAccountOutputDS.parquet`
- `src/main/resources/customerDocument.parquet`

The scoring job reads the `customerDocument.parquet` file and prints the BVI-linked customer count.


## Windows troubleshooting

If you see `Path does not exist` with a malformed path like `D:Quantexa...`, use the provided PowerShell runner.
It normalizes the base path to forward slashes before passing `-Dqde.output.base.path` to Spark.

If you see `Failed to locate the winutils binary`, run the install script again as Administrator to install and set `HADOOP_HOME`/`winutils.exe`:

```powershell
.\scripts\windows\install-qde-prereqs.ps1
```
