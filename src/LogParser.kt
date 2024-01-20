@file:Suppress("ReplaceJavaStaticMethodWithKotlinAnalog")

import org.codehaus.stax2.XMLStreamReader2
import org.duckdb.DuckDBAppender
import org.duckdb.DuckDBConnection
import java.nio.file.Files
import java.nio.file.Path
import java.sql.DriverManager
import javax.xml.stream.XMLStreamConstants
import javax.xml.stream.XMLStreamConstants.START_ELEMENT
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.relativeTo
import kotlin.io.path.walk

fun main() {
  LogParser().parseLine()
}

private class TaskTable(private val appender: DuckDBAppender) {
  fun beginRow(id: Int, method: String, kind: String?, level: Int) {
    appender.beginRow()

    appender.append(id)

    val spaceIndex = method.indexOf(' ')
    assert(spaceIndex > 0)
    appender.append(method.substring(0, spaceIndex))
    appender.append(method.substring(spaceIndex + 1))

    appender.append(kind)
    appender.append(level)
  }

  fun endRow(runId: Int, failure: String?, start: Long, duration: Long, nativeMethodSize: Int, inlinedMethodSize: Int, threadId: Int) {
    appender.append(runId)
    appender.append(failure)

    appender.append(start)
    appender.append(duration)

    appender.append(nativeMethodSize)
    appender.append(inlinedMethodSize)

    appender.append(threadId)

    appender.endRow()
  }
}

private data class Field(@JvmField val name: String, @JvmField val type: String)

private const val taskTableName = "tasks"

private fun tableSql(name: String, fields: List<Field>): String {
  return "create table $name (${fields.joinToString(separator = ", ") { "${it.name.lowercase()} ${it.type}" }})"
}

private val factory = { name: String, type: String -> Field(name, type) }
private val runIdField = factory("runId", "UINTEGER not null")

// no need to use a separate table as DuckDB stores repetitive strings efficiently (see https://duckdb.org/2022/10/28/lightweight-compression.html),
// but we need to store metadata for run
private val runFields: List<Field> = listOf(
  // use runId instead of id to be able to use the simpler USING syntax
  runIdField,
  factory("name", "VARCHAR not null"),
  // in milliseconds
  factory("start", "UBIGINT not null"),
  factory("args", "VARCHAR not null"),
)

// https://wiki.openjdk.org/display/HotSpot/LogCompilation+overview
class LogParser {
  private val runCountProvider = HashMap<String, Int>()

  @OptIn(ExperimentalPathApi::class)
  fun parseLine() {
    val dbFile = Path.of("log.duckdb").toAbsolutePath()
    println("Write to $dbFile")
    Files.deleteIfExists(dbFile)
    val connection = DriverManager.getConnection("jdbc:duckdb:$dbFile") as DuckDBConnection
    //val connection = DriverManager.getConnection("jdbc:duckdb:") as DuckDBConnection
    connection.use {
      val threadIdField = factory("threadId", "UINTEGER not null")

      val taskFields: List<Field> = listOf(
        factory("id", "UINTEGER not null"),
        factory("class", "VARCHAR not null"),
        factory("method", "VARCHAR not null"),
        factory("kind", "VARCHAR"),
        factory("level", "UTINYINT"),

        runIdField,
        factory("failure", "VARCHAR"),

        // in microseconds
        factory("start", "UBIGINT not null"),
        factory("duration", "UINTEGER not null"),

        factory("nativeMethodSize", "UINTEGER"),
        factory("inlinedMethodSize", "UINTEGER"),

        threadIdField,
      )

      val threadFields: List<Field> = listOf(
        threadIdField,
        runIdField,
        factory("name", "VARCHAR not null"),
        // in microseconds
        factory("start", "UBIGINT not null"),
      )

      connection.createStatement().use {
        it.executeUpdate(tableSql(taskTableName, taskFields))
        it.executeUpdate(tableSql("threads", threadFields))
        it.executeUpdate(tableSql("runs", runFields))
        it.executeUpdate(tableSql("code_cache", codeCacheFields))
      }

      val taskAppender = connection.createAppender(DuckDBConnection.DEFAULT_SCHEMA, taskTableName)
      val threadAppender = connection.createAppender(DuckDBConnection.DEFAULT_SCHEMA, "threads")
      val runAppender = connection.createAppender(DuckDBConnection.DEFAULT_SCHEMA, "runs")
      val codeCacheAppender = connection.createAppender(DuckDBConnection.DEFAULT_SCHEMA, "code_cache")
      try {
        val taskTable = TaskTable(taskAppender)
        val logDir = Path.of("logs").toAbsolutePath()
        val logFiles = logDir.walk().toList().sortedBy { Files.getLastModifiedTime(it) }

        connection.autoCommit = false
        var runId = 0
        for (logFile in logFiles) {
          val id = logFile.relativeTo(logDir).toString()
          if (!id.endsWith(".log")) {
            continue
          }

          val reader = createXmlReader(inputStream = Files.newInputStream(logFile), publicId = id)
          try {
            read(
              runId = runId++,
              reader = reader,
              task = taskTable,
              runCountProvider = runCountProvider,
              threadAppender = threadAppender,
              runAppender = runAppender,
              codeCacheAppender = codeCacheAppender,
            )
          }
          finally {
            reader.closeCompletely()
          }
        }
      }
      finally {
        taskAppender.close()
        threadAppender.close()
        runAppender.close()
        codeCacheAppender.close()
      }
    }
  }
}

private val taskChildTags = java.util.Set.of("task_done", "failure", "code_cache")

internal class VmInfo(@JvmField var maxCodeCacheSize: Int = -1)

private fun read(
  runId: Int,
  reader: XMLStreamReader2,
  task: TaskTable,
  runCountProvider: MutableMap<String, Int>,
  threadAppender: DuckDBAppender,
  runAppender: DuckDBAppender,
  codeCacheAppender: DuckDBAppender,
) {
  var vmVersion = ""
  var vmStart = -1L

  var threadId = -1
  var threadName = ""

  val info = VmInfo()

  while (reader.hasNext()) {
    val state = reader.next()
    when (state) {
      START_ELEMENT -> {
        when (reader.localName) {
          "hotspot_log" -> {
            for (i in 0..<reader.attributeCount) {
              when (reader.getAttributeLocalName(i)) {
                "time_ms" -> vmStart = reader.getAttributeAsLong(i)
              }
            }
          }

          "vm_version" -> {
            val vmVersionDom = readXmlAsModel(reader, reader.localName, java.util.Set.of("release"))
            vmVersion = vmVersionDom.getChild("release")!!.content!!.trim()
          }

          "args" -> {
            assert(reader.next() == XMLStreamConstants.CHARACTERS)
            val args = reader.text
            val run = computeRunName(args = args, runCountProvider = runCountProvider, info = info, vmVersion = vmVersion)

            assert(vmStart != -1L)
            runAppender.beginRow()
            runAppender.append(runId)
            runAppender.append(run)
            runAppender.append(vmStart)
            runAppender.append(args)
            runAppender.endRow()
          }

          "start_compile_thread" -> {
            var start = -1L
            for (i in 0..<reader.attributeCount) {
              when (reader.getAttributeLocalName(i)) {
                "name" -> threadName = reader.getAttributeValue(i)
                "thread" -> threadId = reader.getAttributeAsInt(i)
                "stamp" -> start = convertSecondsToMicroseconds(reader.getAttributeAsDouble(i))
              }
            }

            // maybe not distinct, that's ok, do not complicate
            threadAppender.beginRow()
            threadAppender.append(threadId)
            threadAppender.append(runId)
            threadAppender.append(threadName)
            threadAppender.append(start)
            threadAppender.endRow()
          }

          "properties" -> {
            //val properties = Properties()
            //properties.load(StringReader(reader.elementText))
            //run = properties.getProperty("java.vm.version") + " " + time
            reader.skipElement()
          }

          "task" -> {
            val start = readTaskAttributes(reader = reader, task = task)

            val dom = readXmlAsModel(reader, "task", taskChildTags)
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
            assert(threadId >= 0)
            task.endRow(
              runId = runId,
              failure = failure,
              start = start,
              duration = duration,
              nativeMethodSize = nativeMethodSize,
              inlinedMethodSize = inlinedMethodSize,
              threadId = threadId,
            )

            dom.getChild("code_cache")?.let {
              val maxCodeCacheSize = info.maxCodeCacheSize
              assert(maxCodeCacheSize != -1)
              writeCodeCache(dom = it, codeCacheAppender = codeCacheAppender, runId = runId, end = end, maxCodeCacheSize = maxCodeCacheSize)
            }
          }
        }
      }
    }
  }
}

private val codeCacheFields: List<Field> = listOf(
  runIdField,
  // in microseconds
  factory("time", "UBIGINT not null"),

  factory("blobs", "UINTEGER not null"),
  factory("methods", "UINTEGER not null"),
  factory("adapters", "UINTEGER not null"),

  factory("used", "UINTEGER not null"),
)

private fun writeCodeCache(dom: XmlElement, codeCacheAppender: DuckDBAppender, runId: Int, end: Long, maxCodeCacheSize: Int) {
  codeCacheAppender.beginRow()

  codeCacheAppender.append(runId)
  codeCacheAppender.append(end)

  codeCacheAppender.append(dom.getAttributeValue("total_blobs")!!.toInt())
  @Suppress("SpellCheckingInspection")
  codeCacheAppender.append(dom.getAttributeValue("nmethods")!!.toInt())
  codeCacheAppender.append(dom.getAttributeValue("adapters")!!.toInt())

  codeCacheAppender.append(maxCodeCacheSize - dom.getAttributeValue("free_code_cache")!!.toInt())

  codeCacheAppender.endRow()
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

  assert(startTime >= -0)
  task.beginRow(id = compileId, method = method!!, kind = kind, level = level)
  return startTime
}