## Questions

### How much time consumed for JIT compilation?

Duration for a compilation task is computed as `task.task_done.stamp` - `task.stamp`.

## Download Metabase

Open [mb.sh](mb.sh) and run `curl` command (assume that you have [Shell Script](https://plugins.jetbrains.com/plugin/13122-shell-script) plugin enabled).

## Update DB

Open http://localhost:3000/admin/databases/2 and trigger re-scan for scheme and/or field values.

// https://github.com/juba/pyobsplot