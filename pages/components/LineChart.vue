<template>
  <div ref="chartContainer" :style="{height: '350px'}"></div>
</template>
<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from "vue"

import { GridComponent, LegendComponent, TooltipComponent } from "echarts/components"
import { LineChart } from "echarts/charts"
import { CanvasRenderer } from "echarts/renderers"
import { EChartsType, init, use } from "echarts/core"
import { useData } from "vitepress"

use([TooltipComponent, LegendComponent, GridComponent, LineChart, CanvasRenderer])

const props = withDefaults(defineProps<{
  data: object
  endLabel?: boolean
}>(), {
  endLabel: true,
})

const chartContainer = ref(null)

const numberFormatter = new Intl.NumberFormat()

let resizeObserver: ResizeObserver

function formatValue(value: number) {
  return formatBarLabel(value) + " ms"
}

// without unit for brevity
function formatBarLabel(value: number): string {
  return numberFormatter.format(value)
}

let chart: EChartsType | null = null

onMounted(() => {
  const container = chartContainer.value as HTMLElement
  const {isDark} = useData()

  chart = initChart(container, isDark.value)
  resizeObserver = new ResizeObserver(() => {
    chart?.resize()
  })

  watch(isDark, isDark => {
    chart?.dispose()
    chart = initChart(chartContainer.value as HTMLElement, isDark)
  })
  resizeObserver.observe(container)
})
onBeforeUnmount(() => {
  const container = chartContainer.value
  // noinspection JSIncompatibleTypesComparison
  if (resizeObserver !== undefined && container != null) {
    resizeObserver.unobserve(container)
  }

  chart?.dispose()
  chart = null
})

function initChart<V>(container: HTMLElement, isDark: boolean): EChartsType {
  const data = props.data as Array<[string, Array<any>]>

  const legend: object = {}
  if (data.some(it => it[0].match(/ \(\d+\)$/))) {
    const legendState = {}
    for (const it of data) {
      legendState[it[0]] = it[0].endsWith(" (1)")
    }
    legend.selected = legendState
  }

  const option = {
    xAxis: {
      type: "value",
    },
    tooltip: {
      trigger: "axis",
    },
    yAxis: {},
    legend,
    series: data.map(item => {
      return {
        name: item[0],
        type: "line",
        data: item[1],
        sampling: "lttb",
        showSymbol: false,
        endLabel: {
          show: props.endLabel,
          formatter: "{a}",
        },
      }
    }),
  }

  const chart = init(container, isDark ? "dark" : null)
  chart.setOption(option)
  // console.log({option, data})
  return chart
}
</script>
