import com.fasterxml.aalto.`in`.ByteSourceBootstrapper
import com.fasterxml.aalto.`in`.ReaderConfig
import com.fasterxml.aalto.stax.StreamReaderImpl
import org.codehaus.stax2.XMLInputFactory2
import org.codehaus.stax2.XMLStreamReader2
import java.io.InputStream
import java.util.*
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamConstants
import javax.xml.stream.XMLStreamException

// https://wiki.openjdk.org/display/HotSpot/LogCompilation+overview
private fun createXmlConfig(): ReaderConfig {
  val config = ReaderConfig()
  config.doAutoCloseInput(true)
  config.setProperty(XMLInputFactory.SUPPORT_DTD, false)
  config.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false)
  config.setProperty(XMLInputFactory2.P_INTERN_NAMES, false)
  config.setProperty(XMLInputFactory2.P_INTERN_NS_URIS, false)
  config.doPreserveLocation(false)
  config.setProperty(XMLInputFactory2.P_AUTO_CLOSE_INPUT, true)
  config.setXmlEncoding("UTF-8")
  config.doCoalesceText(true)
  config.doParseLazily(true)
  return config
}

fun createXmlReader(inputStream: InputStream, publicId: String): StreamReaderImpl {
  return StreamReaderImpl.construct(ByteSourceBootstrapper.construct(createXmlConfig().createNonShared(publicId, null, "UTF-8"), inputStream))
}

data class XmlElement(
  @JvmField val name: String,
  @JvmField val attributes: Map<String, String>,
  @JvmField val children: List<XmlElement>,
  @JvmField val content: String?,
) {
  fun count(name: String): Int = children.count { it.name == name }

  fun getAttributeValue(name: String): String? = attributes.get(name)

  fun getAttributeValue(name: String, defaultValue: String?): String? = attributes.get(name) ?: defaultValue

  fun getChild(name: String): XmlElement? = children.firstOrNull { it.name == name }

  // should not be used - uncomment for migration
  //fun getChildren(name: String): List<XmlElement> = children.filter { it.name == name }

  fun children(name: String): Sequence<XmlElement> = children.asSequence().filter { it.name == name }
}

fun readXmlAsModel(reader: XMLStreamReader2, rootName: String?): XmlElement {
  val fragment = XmlElementBuilder(name = rootName ?: "", attributes = readAttributes(reader = reader))
  var current = fragment
  val stack = ArrayDeque<XmlElementBuilder>()
  val elementPool = ArrayDeque<XmlElementBuilder>()
  var depth = 1
  while (reader.hasNext()) {
    when (reader.next()) {
      XMLStreamConstants.START_ELEMENT -> {
        val name = reader.localName
        val attributes = readAttributes(reader)
        if (reader.isEmptyElement) {
          current.children.add(XmlElement(name = name,
                                          attributes = attributes,
                                          children = Collections.emptyList(),
                                          content = null))
          reader.skipElement()
          continue
        }

        var child = elementPool.pollLast()
        if (child == null) {
          child = XmlElementBuilder(name = name, attributes = attributes)
        }
        else {
          child.name = name
          child.attributes = attributes
        }
        stack.addLast(current)
        current = child
        depth++
      }
      XMLStreamConstants.END_ELEMENT -> {
        val children: List<XmlElement>
        if (current.children.isEmpty()) {
          children = Collections.emptyList()
        }
        else {
          @Suppress("ReplaceJavaStaticMethodWithKotlinAnalog")
          children = Arrays.asList(*current.children.toArray(arrayOfNulls<XmlElement>(current.children.size)))
          current.children.clear()
        }

        val result = XmlElement(name = current.name, attributes = current.attributes, children = children, content = current.content)
        current.content = null
        elementPool.addLast(current)

        depth--
        if (depth == 0) {
          return result
        }

        current = stack.removeLast()
        current.children.add(result)
      }
      XMLStreamConstants.CDATA -> {
        if (current.content == null) {
          current.content = reader.text
        }
        else {
          current.content += reader.text
        }
      }
      XMLStreamConstants.CHARACTERS -> {
        if (!reader.isWhiteSpace) {
          if (current.content == null) {
            current.content = reader.text
          }
          else {
            current.content += reader.text
          }
        }
      }
      XMLStreamConstants.SPACE, XMLStreamConstants.ENTITY_REFERENCE, XMLStreamConstants.COMMENT, XMLStreamConstants.PROCESSING_INSTRUCTION -> {
      }
      else -> throw XMLStreamException("Unexpected XMLStream event ${reader.eventType}", reader.location)
    }
  }

  throw XMLStreamException("Unexpected end of input: ${reader.eventType}", reader.location)
}

private class XmlElementBuilder(@JvmField var name: String, @JvmField var attributes: Map<String, String>) {
  @JvmField var content: String? = null
  @JvmField val children: ArrayList<XmlElement> = ArrayList()
}

fun readAttributes(reader: XMLStreamReader2): Map<String, String> {
  when (val attributeCount = reader.attributeCount) {
    0 -> return Collections.emptyMap()
    1 -> {
      val name = reader.getAttributeLocalName(0)
      return Collections.singletonMap(name, reader.getAttributeValue(0))
    }
    else -> {
      // Map.of cannot be used here - in core-impl only Java 8 is allowed
      val result = HashMap<String, String>(attributeCount)
      var i = 0
      while (i < attributeCount) {
        val name = reader.getAttributeLocalName(i)
        result.put(name, reader.getAttributeValue(i))
        i++
      }
      return result
    }
  }
}