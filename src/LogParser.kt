import org.codehaus.stax2.XMLStreamReader2
import org.duckdb.DuckDBAppender
import org.duckdb.DuckDBConnection
import java.nio.file.Files
import java.nio.file.Path
import java.sql.DriverManager
import javax.xml.stream.XMLStreamConstants
import javax.xml.stream.XMLStreamConstants.START_ELEMENT

fun main() {
  LogParser().parseLine()
}

private class TaskTable(private val appender: DuckDBAppender) {
  fun beginRow(id: Int, method: String?, kind: String?, level: Int) {
    appender.beginRow()

    appender.append(id)
    appender.append(method)
    appender.append(kind)
    appender.append(level)
  }

  fun endRow(run: String, failure: String?, start: Long, duration: Long, nativeMethodSize: Int, inlinedMethodSize: Int) {
    appender.append(run)
    appender.append(failure)

    appender.append(start)
    if (duration == -624373000L) {
      error("sd")
    }
    appender.append(duration)

    appender.append(nativeMethodSize)
    appender.append(inlinedMethodSize)

    appender.endRow()
  }
}

private data class Field(@JvmField val name: String, @JvmField val type: String)

private fun taskTableSql(fields: List<Field>): String {
  return "drop table if exists task; create table task (${fields.joinToString(separator = ", ") { "${it.name.lowercase()} ${it.type}" }})"
}

class LogParser {
  private val runCountProvider = HashMap<String, Int>()

  fun parseLine() {
    val dbFile = Path.of("log.db").toAbsolutePath()
    Files.deleteIfExists(dbFile)
    //val connection = DriverManager.getConnection("jdbc:sqlite:$dbFile?journal_mode=off")
    val connection = DriverManager.getConnection("jdbc:duckdb:$dbFile") as DuckDBConnection
    connection.use {
      val factory = { name: String, type: String -> Field(name, type) }
      val taskFields: List<Field> = listOf(
        factory("id", "UINTEGER"),
        factory("method", "VARCHAR"),
        factory("kind", "VARCHAR"),
        factory("level", "UTINYINT"),

        factory("run", "VARCHAR"),
        factory("failure", "VARCHAR"),

        factory("start", "UBIGINT"),
        factory("duration", "UINTEGER"),

        factory("nativeMethodSize", "UINTEGER"),
        factory("inlinedMethodSize", "UINTEGER"),
      )
      connection.createStatement().use { it.executeUpdate(taskTableSql(taskFields)) }

      val appender = connection.createAppender(DuckDBConnection.DEFAULT_SCHEMA, "task")
      appender.use {
        val taskTable = TaskTable(appender)
        val logFiles = Files.newDirectoryStream(Path.of("logs").toAbsolutePath(), "*.log").use { stream -> stream.toList().sortedBy { Files.getLastModifiedTime(it) } }

        connection.autoCommit = false
        for (logFile in logFiles) {
          val reader = createXmlReader(inputStream = Files.newInputStream(logFile), publicId = logFile.fileName.toString())
          try {
            read(reader, taskTable, runCountProvider)
          }
          finally {
            reader.closeCompletely()
          }
        }
      }
    }
  }
}

private fun read(reader: XMLStreamReader2, task: TaskTable, runCountProvider: MutableMap<String, Int>) {
  var run = ""
  while (reader.hasNext()) {
    val state = reader.next()
    when (state) {
      START_ELEMENT -> {
        when (reader.localName) {
          "hotspot_log" -> {
            //val a = readAttributes(reader)
            //time = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(Instant.ofEpochMilli(a.get("time_ms")!!.toLong()).atZone(ZoneId.systemDefault()))
          }

          "vm_version" -> {
            val vmVersion = readXmlAsModel(reader, reader.localName)
            run = vmVersion.getChild("release")!!.content!!.trim()
          }

          "args" -> {
            assert(reader.next() == XMLStreamConstants.CHARACTERS)
            val args = reader.text
            compilerCountRegex.find(args)?.let {
              run += " cc${it.groups[1]!!.value}"
            }

            val tieredCompilationMatch = tieredCompilationRegex.find(args)
            if (tieredCompilationMatch == null || tieredCompilationMatch.groups.get(1)!!.value == "+") {
              run += " tc"
            }

            run += " (" + runCountProvider.compute(run) { _, value -> (value ?: 0) + 1 } + ")"
          }

          "properties" -> {
            //val properties = Properties()
            //properties.load(StringReader(reader.elementText))
            //run = properties.getProperty("java.vm.version") + " " + time
            reader.skipElement()
          }

          "task" -> {
            val start = readTaskAttributes(reader = reader, task = task)

            val dom = readXmlAsModel(reader, "task")
            val task_done = dom.getChild("task_done")!!
            val success = task_done.getAttributeValue("success")
            val failure: String?
            var nativeMethodSize = 0
            var inlinedMethodSize = 0
            @Suppress("GrazieInspection", "SpellCheckingInspection")
            if (success == "0") {
              failure = dom.getChild("failure")!!.getAttributeValue("reason")!!
            }
            else {
              failure = null
              // if it succeeded there will be an 'nmsize' attribute which indicates the number of bytes of instruction produced by the compile
              nativeMethodSize = task_done.getAttributeValue("nmsize")!!.toInt()
              assert(nativeMethodSize >= 0)
              task_done.getAttributeValue("inlined_bytes")?.let {
                inlinedMethodSize = it.toInt()
                assert(inlinedMethodSize >= 0)
              }
            }

            val end = convertSecondsToMicroseconds(task_done.getAttributeValue("stamp")!!.toDouble())
            val duration = end - start
            assert(duration >= 0)
            task.endRow(run, failure, start, duration, nativeMethodSize, inlinedMethodSize)
          }
        }
      }
    }
  }
}

private fun convertSecondsToMicroseconds(value: Double) = (value * 1_000_000).toLong()

private fun readTaskAttributes(reader: XMLStreamReader2, task: TaskTable): Long {
  var compileId = -1
  var startTime = -1L
  var method: String? = null
  var kind: String? = null
  // if the level is not specified, it is 4 (later we can check the corresponding native method)
  var level = 4
  for (i in 0..<reader.attributeCount) {
    when (reader.getAttributeLocalName(i)) {
      "compile_id" -> compileId = reader.getAttributeAsInt(i)
      "method" -> method = reader.getAttributeValue(i)
      // convert float seconds to int nanosecond
      // The time stamp is in seconds since the start of the VM and the start time
      "stamp" -> startTime = convertSecondsToMicroseconds(reader.getAttributeAsDouble(i))
      "compile_kind" -> kind = reader.getAttributeValue(i)
      "level" -> level = reader.getAttributeAsInt(i)
    }
  }

  assert(startTime>= -0)
  task.beginRow(compileId, method, kind, level)
  return startTime
}

private val compilerCountRegex = Regex("-XX:CICompilerCount=(\\d+)")
private val tieredCompilationRegex = Regex("-XX:([-+])TieredCompilation")