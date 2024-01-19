<template>
  <template v-for="(item, index) in dataList" :key="index">
    <h3 style="padding: 0" v-if="item.title != null" :id="item.title.replace(/ /g, '_').toLowerCase()">{{item.title}}</h3>
    <LineChart :data="item.data" :endLabel="endLabel" :yFormat="yFormat"/>
  </template>
</template>
<script setup lang="ts">

import LineChart from "./LineChart.vue"
import { computed } from "vue"

const props = withDefaults(defineProps<{
  data: object
  endLabel?: boolean,
  yFormat?: "bytes",
  patterns: Array<{ not: boolean, pattern: string, title?: string }>,
}>(), {
  endLabel: true,
})

const dataList = computed(() => {
  const result = []
  for (const pattern of props.patterns) {
    const not = pattern.not === true
    const regExp = RegExp(pattern.pattern)
    result.push({
      data: props.data.filter(item => {
        return regExp.test(item[0]) !== not
      }),
      title: pattern.title,
    })
  }

  return result
})

</script>