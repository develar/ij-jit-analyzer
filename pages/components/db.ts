import { Database, OPEN_READONLY } from "duckdb-async"
import { RowData } from "duckdb"

export function openDb(): Promise<Database> {
  return Database.create("./log.duckdb", OPEN_READONLY)
}

export function groupData(rows: RowData[]) {
  // Map not supported (data serialized using JSON)
  const map: { [key: string]: Array<any>; } = {}
  for (const row of rows) {
    const values = Object.values(row)
    const groupKey = values[0]
    let existing = map[groupKey]
    if (existing === undefined) {
      existing = []
      map[groupKey] = existing
    }
    existing.push(values.slice(1))
  }

  const result = []
  for (const [key, values] of Object.entries(map)) {
    result.push([key, values])
  }
  return result
}