private val codeCacheSizeRegex = Regex("-XX:ReservedCodeCacheSize=(\\d+)m")
private val compilerCountRegex = Regex("-XX:CICompilerCount=(\\d+)")
private val tieredCompilationRegex = Regex("-XX:([-+])TieredCompilation")

private val regex = Regex("""(\d{2})[^b]+(b[^.]+).*""")

internal fun computeRunName(args: String, runCountProvider: MutableMap<String, Int>, info: VmInfo, vmVersion: String): String {
  // shorten the version to avoid long labels
  val parsedVersion = regex.matchEntire(vmVersion)!!.groups
  val result = StringBuilder()
  result.append(parsedVersion.get(1)!!.value)
  result.append('.')
  result.append(parsedVersion.get(2)!!.value)

  compilerCountRegex.find(args)?.let {
    result.append(" cc${it.groups[1]!!.value}")
  }

  val tieredCompilationMatch = tieredCompilationRegex.find(args)
  val isTieredCompilation = tieredCompilationMatch == null || tieredCompilationMatch.groups.get(1)!!.value == "+"
  if (isTieredCompilation) {
    result.append(" tc")
  }

  val codeCacheSizeMatch = codeCacheSizeRegex.find(args)
  if (codeCacheSizeMatch == null) {
    // The default maximum code cache size is 240 MB; if you disable tiered compilation with the option -XX:-TieredCompilation, then the default size is 48 MB
    val sizeInMb = if (isTieredCompilation) 240 else 48
    info.maxCodeCacheSize = sizeInMb * 1024 * 1024
    result.append(" rcs$sizeInMb")
  }
  else {
    val sizeInMb = codeCacheSizeMatch.groups.get(1)!!.value
    info.maxCodeCacheSize = sizeInMb.toInt() * 1024 * 1024
    result.append(" rcs$sizeInMb")
  }

  val number = runCountProvider.compute(result.toString()) { _, value -> (value ?: 0) + 1 }
  result.append(" ($number)")
  return result.toString()
}