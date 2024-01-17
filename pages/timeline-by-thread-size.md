<script setup>

import { data } from "./timeline-thread.data"
import LineChart from "./components/LineChart.vue" 

</script>

# Non-stale task distribution over time by thread

See [how we calculate data](/timeline#how-is-jit-distributed-over-time).

Each run has its own chart for easier reading.

[//]: # (https://github.com/vuejs/vitepress/discussions/1473)
<template v-for="it in data.size">
<h2>{{ it[0] }}</h2>
  
<LineChart :data="it[1]" :endLabel="false" y-format="bytes"/>
</template>