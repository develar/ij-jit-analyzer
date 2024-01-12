select run, kind, level, failure, round(sum(duration) / 1000) as duration, count() as count
from task
group by run, kind, failure, level
order by run, kind, level, failure;


select run, coalesce(failure, kind, cast(level as varchar)) as category, round(sum(duration) / 1000) as duration
from task
where duration > 0
group by run, category
order by run, category
