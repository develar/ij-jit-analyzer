import { openDb } from "./components/db"
import { RowData } from "duckdb"

// please note - duration maybe `0` even for successful tasks (not clear why - or because too fast and recorded with the same stamp, or due to another reason);
// that's why we do not use `where duration != 0`
export default {
  async load() {
    const db = await openDb()
    // 1000 - microseconds to milliseconds, 100 - bucket by 100 ms
    // language=GenericSQL
    const compilation = groupData(await db.all(`
      with recursive intervals as (select min(start) as st, min(start) + 100000 as et
                                   from tasks
                                   union all
                                   select st + 100000, et + 100000
                                   from intervals,
                                        (select max(start + duration) as max_time from tasks) as maxi
                                   where et < max_time)
      select runs.name as run, st / 1000 as time, cast(count() as int) as count
      from intervals
        left join tasks
      on tasks.start < intervals.et and (tasks.start + tasks.duration) > intervals.st
        join runs using (runId)
      where failure is null or failure != 'stale task'
      group by run, time
      order by run, time
    `))

    // language=GenericSQL
    const successfulCompilation = groupData(await db.all(`
      with recursive intervals as (select min(start) as st, min(start) + 100000 as et
                                   from tasks
                                   union all
                                   select st + 100000, et + 100000
                                   from intervals,
                                        (select max(start + duration) as max_time from tasks) as maxi
                                   where et < max_time)
      select runs.name as run, st / 1000 as time, cast(count() as int) as count
      from intervals
        left join tasks
      on tasks.start < intervals.et and (tasks.start + tasks.duration) > intervals.st
        join runs using (runId)
      where failure is null
      group by run, time
      order by run, time
    `))

    // language=GenericSQL
    const unsuccessfulCompilation = groupData(await db.all(`
      with recursive intervals as (select min(start) as st, min(start) + 100000 as et
                                   from tasks
                                   union all
                                   select st + 100000, et + 100000
                                   from intervals,
                                        (select max(start + duration) as max_time from tasks) as maxi
                                   where et < max_time)
      select runs.name as run, st / 1000 as time, cast(count() as int) as count
      from intervals
        left join tasks
      on tasks.start < intervals.et and (tasks.start + tasks.duration) > intervals.st
        join runs using (runId)
      where failure is not null
      group by run, time
      order by run, time
    `))

    // language=GenericSQL
    const staleCompilation = groupData(await db.all(`
      with recursive intervals as (select min(start) as st, min(start) + 100000 as et
                                   from tasks
                                   union all
                                   select st + 100000, et + 100000
                                   from intervals,
                                        (select max(start + duration) as max_time from tasks) as maxi
                                   where et < max_time)
      select runs.name as run, st / 1000 as time, cast(count() as int) as count
      from intervals
        left join tasks
      on tasks.start < intervals.et and (tasks.start + tasks.duration) > intervals.st
        join runs using (runId)
      where failure = 'stale task'
      group by run, time
      order by run, time
    `))

    // language=GenericSQL
    const threads = groupData(await db.all(`
      with recursive intervals as (select min(start) as st, min(start) + 100000 as et
                                   from tasks
                                   union all
                                   select st + 100000, et + 100000
                                   from intervals,
                                        (select max(start + duration) as max_time from tasks) as maxi
                                   where et < max_time)
      select runs.name as run, threads.name as thread, st / 1000 as time, cast(count() as int) as count
      from intervals
        left join tasks
      on tasks.start < intervals.et and (tasks.start + tasks.duration) > intervals.st
        join runs using (runId)
        join threads using (runId, threadId)
      where failure is null or failure != 'stale task'
      group by run, thread, time
      order by run, thread, time
    `))
    groupAndSortThreads(threads)

    await db.close()
    return {
      compilation,
      staleCompilation,
      successfulCompilation,
      unsuccessfulCompilation,
      threads,
    }
  }
}

function groupData(rows: RowData[]) {
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

function groupAndSortThreads(threads: any[]) {
  for (let i = 0; i < threads.length; i++) {
    const thread = threads[i]
    const v = groupData(thread[1])
    for (const vElement of v) {
      vElement[0] = vElement[0].replace(" CompilerThread", " ")
    }

    // natural sort
    v.sort((a, b) => a[0].localeCompare(b[0], undefined, {numeric: true, sensitivity: "base"}))
    thread[1] = v
  }
}