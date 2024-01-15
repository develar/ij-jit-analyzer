---
title: JIT insights
---

## How much time consumed for JIT compilation?

```sql duration_by_category
select run, coalesce(failure, kind, cast(level as varchar)) as category, round(sum(duration) / 1000) as duration
from tasks
where duration > 0 and category != 'stale task'
group by run, category
order by run, category
```

<BarChart 
  data={duration_by_category} 
  x=run 
  y=duration 
  series=category
  labels=true
  labelFmt=duration
  printEchartsConfig=true
  echartsOptions={{
    xAxis: {
      inverse: true,
    },
  }}
/>