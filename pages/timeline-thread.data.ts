import { naturalStringSort, groupData, openDb, databaseFilePath } from "./components/db"
import { defineLoader } from "vitepress"

// please note - duration maybe `0` even for successful tasks (not clear why - or because too fast and recorded with the same stamp, or due to another reason);
// that's why we do not use `where duration != 0`
export default defineLoader({
  watch: [databaseFilePath],

  async load() {
    const db = await openDb()
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

    // we compute size by end time (as it is a time when a task is finished and native method is produced)
    // language=GenericSQL
    const size = groupData(await db.all(`
      select runs.name as run, threads.name as thread, floor((tasks.start + tasks.duration) / 100000) * 100 as time, cast(sum(nativeMethodSize) as int) as size
      from tasks
        join runs using (runId)
        join threads using (runId, threadId)
      where failure is null or failure != 'stale task'
      group by run, thread, time
      order by run, thread, time
    `))
    groupAndSortThreads(size)

    await db.close()
    return {
      threads, size,
    }
  }
})

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