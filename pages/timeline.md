#  How is JIT distributed over time?

<script setup>

import { data } from "./components/timeline.data"
import LineChart from "./components/LineChart.vue"

</script>

Compilation tasks are grouped by 100ms buckets based on the start time.

For the sake of chart readability, only one run per scenario is displayed by default. You can adjust this setting using the legend.

## How are non-stale tasks distributed?

<LineChart :data="data.compilation" x="time" y="count" series="run"/>

## How are successful tasks distributed?

See [about wasted compilation time](./duration#how-much-of-compilation-time-is-wasted).
<LineChart :data="data.successfulCompilation" x="time" y="count" series="run" :scatter="true"/>

## How are unsuccessful tasks distributed?

<LineChart :data="data.unsuccessfulCompilation" x="time" y="count" series="run" :scatter="true"/>

## How are stale tasks distributed?

<LineChart :data="data.staleCompilation" x="time" y="count" series="run" :scatter="true"/>