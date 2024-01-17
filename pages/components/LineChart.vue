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
import { getFormatter } from "./format"
import { useResizeObserver } from "./useResizeObserver"

use([TooltipComponent, LegendComponent, GridComponent, LineChart, CanvasRenderer])

const props = withDefaults(defineProps<{
  data: object
  endLabel?: boolean,
  yFormat?: "bytes",
}>(), {
  endLabel: true,
})

const chartContainer = ref(null)

let chart: EChartsType | null = null

useResizeObserver(chartContainer, () => {
  chart?.resize()
})

onMounted(() => {
  const container = chartContainer.value as HTMLElement
  const {isDark} = useData()

  chart = initChart(container, isDark.value)

  watch(isDark, isDark => {
    chart?.dispose()
    chart = initChart(chartContainer.value as HTMLElement, isDark)
  })
})
onBeforeUnmount(() => {
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

  const xFormatter = getFormatter("time")
  const yFormatter = getFormatter(props.yFormat)
  const option = {
    xAxis: {
      type: "value",
      axisLabel: {
        formatter: xFormatter
      },
    },
    tooltip: {
      trigger: "axis",
      axisPointer: {
        label: {
          formatter: function (params) {
            return xFormatter(params.value)
          }
        }
      },
      valueFormatter: yFormatter,
    },
    yAxis: {
      axisLabel: {
        formatter: yFormatter
      },
    },
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
