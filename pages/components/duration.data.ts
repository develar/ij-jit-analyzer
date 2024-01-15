import { openDb } from "./db"

export default {
  async load() {
    const db = await openDb()
    // language=GenericSQL
    const duration = await db.all(`
      select run, coalesce(failure, kind, cast(level as varchar)) as category, round(sum(duration) / 1000) as duration
      from tasks
      where duration > 0 and (failure is null or failure != 'stale task')
      group by run, category
      order by run, category
    `)

    // language=GenericSQL
    const failedTaskDuration = await db.all(`
      select run, concat(failure, ' (level ', cast(level as varchar), ')') as category, round(sum(duration) / 1000) as duration
      from tasks
      where duration > 0 and failure != 'stale task' and failure is not null
      group by run, category
      order by run, category
    `)

    // language=GenericSQL
    const staleTasks = await db.all(`
      select run, cast(count() as int) as count
      from tasks
      where failure == 'stale task'
      group by run
      order by run
    `)

    await db.close()
    return {
      duration,
      failedTaskDuration,
      staleTasks,
    }
  }
}
