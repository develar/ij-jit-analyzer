<template>
  <div ref="chartContainer" :style="{height: '350px'}"></div>
</template>
<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from "vue"

import { DatasetComponent, GridComponent, LegendComponent, TooltipComponent } from "echarts/components"
import { BarChart } from "echarts/charts"
import { CanvasRenderer } from "echarts/renderers"
import { EChartsType, init, use } from "echarts/core"
import { useData } from "vitepress"
import { getFormatter } from "./format"
import { useResizeObserver } from "./useResizeObserver"

use([TooltipComponent, DatasetComponent, LegendComponent, GridComponent, BarChart, CanvasRenderer])

const props = withDefaults(defineProps<{
  data: object
  stack?: boolean,
  yFormat?: "time" | "bytes",
}>(), {
  stack: false,
})

const {isDark} = useData()

const chartContainer = ref(null)

let chart: EChartsType | null = null

useResizeObserver(chartContainer, () => {
  chart?.resize()
})

onMounted(() => {
  const container = chartContainer.value as HTMLElement
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
  if (props.stack === true) {
    return initStackedChart(container, isDark)
  }
  else {
    return initNonStackedChart(container, isDark)
  }
}

function initNonStackedChart(container: HTMLElement, isDark: boolean): EChartsType {
  const data = props.data as Array<[string, Array<number>]>
  const chart = init(container, isDark ? "dark" : null)

  // remove suffix ` (N)`
  const groups = [...new Set(data.map(it => it[0].replace(/ \(\d+\)$/, "")))]
  const yFormatter = getFormatter(props.yFormat)
  const seriesData = groups.map(group => {
    const d = []
    for (const item of data) {
      const name = item[0]
      if (name.startsWith(group)) {
        d.push([name, ...item[1]])
      }
    }

    return {
      name: group,
      data: d,
      type: "bar",
      label: {
        show: true,
        position: "top",
        formatter: yFormatter == null ? undefined : params => yFormatter(params.value[1])
      },
    }
  })

  const option = {
    tooltip: {
      axisPointer: {
        type: "shadow"
      },
    },
    legend: {},
    xAxis: {
      type: "category",
    },
    yAxis: {
      type: "value",
      axisLabel: {
        formatter: yFormatter,
      }
    },
    series: seriesData,
  }
  // console.log({option, data})
  chart.setOption(option)
  return chart
}

function initStackedChart<V>(container: HTMLElement, isDark: boolean): EChartsType {
  const data = props.data
  const categories: Array<string> = data[0].slice(1)

  const yFormatter = getFormatter(props.yFormat)

  const seriesData: any = categories.map((category) => {
    let formatter: (params) => string
    if (category === "total") {
      formatter = (params) => yFormatter(params.data.slice(1).reduce((a, b) => a + ((b != null) ? b : 0), 0))
    }
    else {
      formatter = (params) => yFormatter(params.data[params.seriesIndex + 1])
    }

    return {
      name: category,
      type: "bar",
      stack: "total",
      label: {
        // show: true,
        position: category === "total" ? "top" : "inside",
        formatter: formatter,
      },
    }
  })

  const option = {
    dataset: {
      source: data,
    },
    tooltip: {
      trigger: "axis",
      axisPointer: {
        type: "shadow"
      },
      // override default to skip series without value
      formatter: params => {
        if (params.length === 0) {
          return ""
        }

        let tooltipMarkup = `<div style="line-height: 1.5">${params[0].name}`
        for (const param of params) {
          if (param.seriesName === "total") {
            continue
          }

          const value = param.data[param.seriesIndex + 1]
          if (value != null) {
            const formattedValue = yFormatter == null ? value : yFormatter(value)
            tooltipMarkup += `
            <div>${param.marker}<span
              style="margin-left:2px">${param.seriesName}</span><span
              style="float: right; margin-left:20px; font-weight: 900">${formattedValue}</span>
              <div style="clear:both"></div>
            </div>`
          }
        }
        tooltipMarkup += "</div>"
        return tooltipMarkup
      }
    },
    legend: {
      // explicitly set to not include `total` series
      data: categories.slice(0, categories.length - 1),
    },
    grid: {
      left: "5%",
      right: "5%",
      bottom: "5%",
      containLabel: true
    },
    xAxis: {
      type: "value",
      axisLabel: {
        formatter: yFormatter,
      }
    },
    yAxis: {
      type: "category",
      inverse: true,
    },
    series: seriesData
  }

  const chart = init(container, isDark ? "dark" : null)
  chart.setOption(option)
  // console.log({option, data})
  return chart
}
</script>
