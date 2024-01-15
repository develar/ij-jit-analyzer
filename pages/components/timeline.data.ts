import { openDb } from "./db"
import { RowData } from "duckdb"

// please note - duration maybe `0` even for successful tasks (not clear why - or because too fast and recorded with the same stamp, or due to another reason);
// that's why we do not use `where duration != 0`
export default {
  async load() {
    const db = await openDb()
    // 1000 - nanoseconds to microseconds, 100 - bucket by 100 ms
    // language=GenericSQL
    const compilation = groupData(await db.all(`
      select run, floor(start / 100000) * 100 as time, cast(count() as int) as count
      from tasks
      where failure is null or failure != 'stale task'
      group by run, time
      order by run, time
    `))

    // language=GenericSQL
    const successfulCompilation = groupData(await db.all(`
      select run, floor(start / 100000) * 100 as time, cast(count() as int) as count
      from tasks
      where failure is null
      group by run, time
      order by run, time
    `))

    // language=GenericSQL
    const unsuccessfulCompilation = groupData(await db.all(`
      select run, floor(start / 100000) * 100 as time, cast(count() as int) as count
      from tasks
      where failure is not null
      group by run, time
      order by run, time
    `))

    // language=GenericSQL
    const staleCompilation = groupData(await db.all(`
      select run, floor(start / 100000) * 100 as time, cast(count() as int) as count
      from tasks
      where failure = 'stale task'
      group by run, time
      order by run, time
    `))

    await db.close()
    return {
      compilation,
      staleCompilation,
      successfulCompilation,
      unsuccessfulCompilation,
    }
  }
}

function groupData(compilation: RowData[]) {
  // Map not supported (data serialized using JSON)
  const map: { [key: string]: Array<any>; } = {}
  for (const {run, time, count} of compilation) {
    let existing = map[run]
    if (existing === undefined) {
      existing = []
      map[run] = existing
    }
    existing.push([time, count])
  }

  const result = []
  for (const [run, values] of Object.entries(map)) {
    result.push([run, values])
  }
  return result
}