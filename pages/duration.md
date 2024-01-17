# Spent Time

<script setup>

import { data } from "./duration.data"
import BarChart from "./components/BarChart.vue"

</script>

## How much time consumed for JIT compilation?

<BarChart :data="data.duration" x="run" y="duration" series="category" y-format="time"/>

## How much of compilation time is wasted?

A compilation task may fail due to various reasons. This means that the result of the compilation is not utilized, resulting in wasted CPU resources.

<BarChart :data="data.failedTaskDuration" x="run" y="duration" series="category" y-format="time"/>

## How many stale tasks?

Tasks aren't run right away. They go into a queue to run later and can be cancelled. The chart above shows this.

This chart shows tasks that were never started.

<BarChart :data="data.staleTasks" x="run" y="count"/>

