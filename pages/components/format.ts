import prettyBytes from "pretty-bytes"
import prettyMilliseconds from "pretty-ms"

export function getFormatter(format: "bytes" | "time" | undefined) {
  if (format === "bytes") {
    const options = {binary: true, maximumFractionDigits: 0}
    return (value: number | string) => prettyBytes(value as number, options)
  }
  else if (format === "time") {
    const options = {formatSubMilliseconds: true}
    return it => prettyMilliseconds(it as number, options)
  }
  else {
    return undefined
  }
}
