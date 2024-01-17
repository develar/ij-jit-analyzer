import { Database, OPEN_READONLY } from "duckdb-async"
import { RowData } from "duckdb"

export const databaseFilePath = "./log.duckdb"

export function openDb(): Promise<Database> {
  return Database.create(databaseFilePath, OPEN_READONLY)
}

export function groupData(rows: RowData[], flatValues: boolean = false) {
  const map = new Map<string, Array<any>>()
  for (const row of rows) {
    const values = Object.values(row)
    const groupKey = values[0]
    let existing = map.get(groupKey)
    if (existing === undefined) {
      existing = []
      map.set(groupKey, existing)
    }

    if (flatValues) {
      existing.push(...values.slice(1))
    }
    else {
      existing.push(values.slice(1))
    }
  }

  const result = []
  // @ts-ignore
  for (const [key, values] of map) {
    result.push([key, values])
  }
  return result
}

export function naturalStringSort(a: string, b: string) {
  return a.localeCompare(b, undefined, {numeric: true, sensitivity: "base"})
}
