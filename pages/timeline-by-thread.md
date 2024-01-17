<script setup>

import { data } from "./timeline-thread.data"
import LineChart from "./components/LineChart.vue" 

</script>

# Timeline of Non-Stale Task Counts by Thread

See [how we calculate data](/timeline#how-is-jit-distributed-over-time).

Each run has its own chart for easier reading.

<template v-for="it in data.threads">

<h2 :id="it[0].replace(/ /g, '_').toLowerCase()">{{ it[0] }}</h2>
  
<LineChart :data="it[1]" :endLabel="false"/>

</template>