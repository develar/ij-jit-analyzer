<script setup>

import { data } from "./timeline.data"
import LineChart from "./components/LineChart.vue" 
import { onMounted } from "vue" 

import { useData } from 'vitepress'

const { page } = useData()

  onMounted({
    page.headers.push({
      level: 2,
      title: "Header Title",
      slug: "header-id"
    })
  })

</script>

# Non-stale task distribution over time by thread

See [how we calculate data](/timeline#how-is-jit-distributed-over-time).

Each run has its own chart for easier reading.

<template v-for="it in data.threads">
<h2>{{ it[0] }}</h2>
  
<LineChart :data="it[1]" :endLabel="false"/>
</template>