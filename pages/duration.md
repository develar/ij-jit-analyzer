---
toc: false
---

# Spent Time

<script setup>

import { data } from "./duration.data"
import BarChart from "./components/BarChart.vue"
import LineChart from "./components/LineChart.vue"

const nonStaleDefault = data.nonStale.filter((item, index) => {
return index === 0 || !item[0].includes("rcs240")
 })

</script>

## How much time consumed for JIT compilation?

<BarChart :data="nonStaleDefault" :stack="true" y-format="time"/>

### Native Method Size

[OSR](https://stackoverflow.com/a/9105846) (On Stack Replacement) is not included here. 

<BarChart :data="data.nonStaleSize" :stack="true" y-format="bytes"/>

### Code Cache

<LineChart :data="data.codeCache" y-format="bytes"/>

## How much of compilation time is wasted?

A compilation task may fail due to various reasons. This means that the result of the compilation is not utilized, resulting in wasted CPU resources.

<BarChart :data="data.failedTaskDuration" :stack="true" y-format="time"/>

## How many stale tasks?

Tasks aren't run right away. They go into a queue to run later and can be cancelled. The chart above shows this.

This chart shows tasks that were never started.

<BarChart :data="data.staleTasks"/>

