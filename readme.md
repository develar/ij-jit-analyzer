## Converting JVM Compilation Logs to Database
The front-end utilizes `log.duckdb`. To put this into operation:
1. Place the log files in the `logs` directory.
2. Open the project in IntelliJ IDEA and run the `Generate log.duckdb` configuration.

## Run UI
1. Install [bun](https://bun.sh/docs/installation#installing).
2. Run the command `bun install`.
3. Run the command `bun dev`.
4. Open `http://localhost:5173/` in your web browser.

## Development
When writing SQL queries or setting up ECharts, consider using [JetBrains AI](https://www.jetbrains.com/ai/).
JetBrains AI supports both DuckDB and ECharts, so it can provide assistance from general support to specific syntax requirements.