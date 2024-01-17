import { naturalStringSort, groupData, openDb, databaseFilePath } from "./components/db"
import { RowData } from "duckdb"
import { Database } from "duckdb-async"
import { defineLoader } from "vitepress"

export default defineLoader({
  watch: [databaseFilePath],

  async load() {
    const db = await openDb()
    // language=GenericSQL
    const nonStale = await execute(db, `
      select runs.name as run, coalesce(failure, kind, cast(level as varchar)) as category, round(sum(duration) / 1000) as duration
      from tasks join runs using (runId)
      where duration > 0 and (failure is null or failure != 'stale task')
      group by run, category
      order by run, category
    `)

    // language=GenericSQL
    const nonStaleSize = await execute(db, `
      select runs.name as run, coalesce(failure, kind, cast(level as varchar)) as category, cast(sum(nativeMethodSize) as int) as size
      from tasks join runs using (runId)
      where failure is null and kind is null
      group by run, category
      order by run, category
    `)

    // language=GenericSQL
    const failedTaskDuration = await execute(db, `
      select runs.name as run, concat(failure, ' (level ', cast(level as varchar), ')') as category, round(sum(duration) / 1000) as duration
      from tasks join runs using (runId)
      where duration > 0 and failure != 'stale task' and failure is not null
      group by run, category
      order by run, category
    `)

    // language=GenericSQL
    const staleTasks = groupData(await db.all(`
        select runs.name as run, cast(count() as int) as count
        from tasks join runs using (runId)
        where failure == 'stale task'
        group by run
        order by run
      `), true)

    // language=GenericSQL
    const codeCache = groupData(await db.all(`
      select runs.name as run, floor(time / 100000) * 100 as t, max(used) as used
      from code_cache join runs using (runId)
      group by run, t
      order by run, t
    `))

    await db.close()
    return {
      nonStale,
      nonStaleSize,
      failedTaskDuration,
      staleTasks,
      codeCache,
    }
  }
})

// language=GenericSQL
async function execute(db: Database, sql: string) {
  return convertToDatasetFormat(await db.all(sql))
}

function convertToDatasetFormat(rows: Array<RowData>) {
  const map = new Map()
  const subCategorySet = new Set<string>()
  for (const row of rows) {
    const values = Object.values(row)
    const category = values[0]
    let subMap = map.get(category)
    if (subMap === undefined) {
      subMap = new Map()
      map.set(category, subMap)
    }

    const subCategory = values[1]
    let valueList = subMap.get(subCategory)
    if (valueList === undefined) {
      valueList = []
      subMap.set(subCategory, valueList)
      subCategorySet.add(subCategory)
    }
    valueList.push(values[2])
  }

  const subCategories = Array.from(subCategorySet)
  subCategories.sort(naturalStringSort)
  const result = []
  result.push(["category", ...subCategories, "total"])
  for (const category of Array.from(map.keys()).sort(naturalStringSort)) {
    const subMap = map.get(category)
    const row = [category]
    for (const subCategory of subCategories) {
      const items = subMap.get(subCategory)
      if (items === undefined) {
        row.push(undefined)
      }
      else {
        row.push(...items)
      }
    }

    row.push(0)
    result.push(row)
  }

  return result
}