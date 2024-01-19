#  How is JIT distributed over time?

<script setup>

import { data } from "./timeline.data"
import SplitLineChart from "./components/SplitLineChart.vue"

// separate experiment about 240 vs 512 cache size
const patterns = [
  {pattern: '^21\\.b\\d+ rcs512 \\(\\d+\\)$', not: true},
  {pattern: '^21\\.b\\d+ (rcs512|rcs240) \\(\\d+\\)$', title: "240 vs 512 code cache size"},
]

</script>

Compilation tasks are organized into 100 ms buckets, based on both their start time and duration, with the count of tasks indicated on the y-axis.

Each 100 ms buckets can contain many tasks. 
Where each task starts and how long it lasts is tracked.

For example, if `foo` and `bar` start in the first 100 ms, we count two tasks. 
If `foo` ends but `bar` continues, the next 100 ms counts one task.

For the sake of chart readability, only one run per scenario is displayed by default. You can adjust this setting using the legend.

## How are non-stale tasks distributed?

<SplitLineChart :data="data.compilation" :patterns="patterns"/>

## How are successful tasks distributed?

See [about wasted compilation time](./duration#how-much-of-compilation-time-is-wasted).
<SplitLineChart :data="data.successfulCompilation" :patterns="patterns"/>

## How are unsuccessful tasks distributed?

<SplitLineChart :data="data.unsuccessfulCompilation" :patterns="patterns"/>

## How are stale tasks distributed?

<SplitLineChart :data="data.staleCompilation" :patterns="patterns"/>