import { onBeforeUnmount, onMounted, Ref } from "vue"

export function useResizeObserver(elementRef: Ref<HTMLElement | null>, callback: () => void) {
  let resizeObserver: ResizeObserver | null = null
  onMounted(() => {
    const container = elementRef.value
    if (container !== undefined) {
      resizeObserver = new ResizeObserver(callback)
      resizeObserver.observe(container)
    }
  })
  onBeforeUnmount(() => {
    if (resizeObserver !== undefined && elementRef.value !== undefined) {
      resizeObserver.disconnect()
      resizeObserver = null
    }
  })
}