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

const props = defineProps({
  x: {type: String, required: true},
  y: {type: String, required: true},
  series: {type: String, required: false},
  data: {type: Object, required: true},
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
  if (props.series == undefined) {
    return initNonStackedChart(container, isDark)
  }
  else {
    return initStackedChart(container, isDark)
  }
}

function initNonStackedChart(container: HTMLElement, isDark: boolean): EChartsType {
  throw Error("unsupported")
}

function initStackedChart<V>(container: HTMLElement, isDark: boolean): EChartsType {
  const data = props.data as Array<[string, Array<any>]>

  const legendState = {}
  for (const it of data) {
    legendState[it[0]] = it[0].endsWith(" (1)")
  }

  const option = {
    xAxis: {
      type: "value",
    },
    tooltip: {
      trigger: "axis",
    },
    yAxis: {},
    legend: {
      selected: legendState,
    },
    series: data.map(item => {
      return {
        name: item[0],
        type: "line",
        data: item[1],
        sampling: "lttb",
        showSymbol: false,
        endLabel: {
          show: true,
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
