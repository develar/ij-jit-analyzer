import { Database, OPEN_READONLY } from "duckdb-async"

export default {
  async load() {
    const db = await Database.create("../log.db", OPEN_READONLY)
    const data = await db.all(`
        select run, coalesce(failure, kind, cast(level as varchar)) as category, round(sum(duration) / 1000) as duration
        from task
        where duration > 0
        group by run, category
        order by run, category
    `)
    await db.close()
    return data
  }
}
