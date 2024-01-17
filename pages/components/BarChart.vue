<template>
  <div ref="chartContainer" :style="{height: '350px'}"></div>
</template>
<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from "vue"

import { GridComponent, LegendComponent, TooltipComponent } from "echarts/components"
import { BarChart } from "echarts/charts"
import { CanvasRenderer } from "echarts/renderers"
import { EChartsType, init, use } from "echarts/core"
import { useData } from "vitepress"
import { getFormatter } from "./format"

use([TooltipComponent, LegendComponent, GridComponent, BarChart, CanvasRenderer])

const props = withDefaults(defineProps<{
  data: object
  x: string,
  y: string,
  series?: string,
  yFormat?: "time",
}>(), {
})

const chartContainer = ref(null)

const numberFormatter = new Intl.NumberFormat()

let resizeObserver: ResizeObserver

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
  const data = props.data
  const chart = init(container, isDark ? "dark" : null)

  // remove suffix ` (N)`
  const groups = [...new Set(data.map(it => it[props.x].replace(/ \(\d+\)$/, "")))]
  const seriesData = groups.map(group => {
    const d = []
    for (const item of data) {
      const name = item[props.x]
      if (name.startsWith(group)) {
        d.push([name, item[props.y]])
      }
    }

    return {
      name: group,
      data: d,
      type: "bar",
      label: {
        show: true,
        position: "top",
        formatter: params => formatBarLabel(params.value[1])
      },
    }
  })

  const yFormatter = getFormatter(props.yFormat)
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

  const xAxisData = [...new Set(data.map(item => item[props.x]))]

  const seriesPropertyName = props.series
  const categories = [...new Set(data.map(item => item[seriesPropertyName]))]
  // sort to ensure stable order of legend (as user data maybe sorted by several fields)
  categories.sort((a, b) => a.localeCompare(b))

  const total = new Array(xAxisData.length).fill(0)

  const seriesData = categories.map(category => {
    const values = []
    for (const item of data) {
      if (item[seriesPropertyName] === category) {
        const index = xAxisData.indexOf(item[props.x])
        const v = item[props.y]
        values[index] = v
        total[index] += v
      }
    }

    return {
      name: category,
      type: "bar",
      stack: "total",
      label: {
        show: true,
        position: "inside",
        formatter: (params) => formatBarLabel(params.value)
      },
      data: values
    }
  })

  const valueFormatter = getFormatter("time")
  const totalSeriesData = {
    name: "Total",
    type: "bar",
    stack: "total",
    label: {
      show: true,
      position: "top",
      formatter: params => valueFormatter(total[params.dataIndex]),
    },
    tooltip: {
      show: false,
    },
    // must be 0 to not draw bar
    data: total.map(_ => 0),
  }
  seriesData.push(totalSeriesData as any)

  const yFormatter = getFormatter(props.yFormat)
  const option = {
    tooltip: {
      trigger: "axis",
      axisPointer: {
        type: "shadow"
      },
      // `-` indicate missing value
      // no easy way to filter out from tooltip, but it maybe even better - tooltip height is the same for all series
      valueFormatter: it => it === undefined ? "-" : valueFormatter(it),
    },
    legend: {
      // explicitly set to not include `total` series
      data: categories,
    },
    grid: {
      left: "5%",
      right: "5%",
      bottom: "5%",
      containLabel: true
    },
    xAxis: {
      type: "category",
      data: xAxisData
    },
    yAxis: {
      type: "value",
      axisLabel: {
        formatter: yFormatter,
      }
    },
    series: seriesData
  }

  const chart = init(container, isDark ? "dark" : null)
  chart.setOption(option)
  // console.log({option, data})
  return chart
}
</script>
