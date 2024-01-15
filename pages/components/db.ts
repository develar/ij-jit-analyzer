import { Database, OPEN_READONLY } from "duckdb-async"

export function openDb(): Promise<Database> {
  return Database.create("./log.duckdb", OPEN_READONLY)
}