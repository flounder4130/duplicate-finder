package finder.parsing

import finder.DuplicateFinderOptions
import java.io.StringReader
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamConstants

class Tag(val name: String) {
    val contentBuilder = StringBuilder()
}

private const val NESTED_TAG_PLACEHOLDER = "</>"

class XmlParser(
    options: DuplicateFinderOptions,
    val skipTags: List<String> = emptyList(),
) : ContentParser(options) {
    override fun parse(content: String): List<Element> {
        val xmlStreamReader = XMLInputFactory.newInstance().apply {
            setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false)
            setProperty(XMLInputFactory.SUPPORT_DTD, false)
        }.createXMLStreamReader(StringReader(content))

        val elements = mutableListOf<Element>()
        val stack = ArrayDeque<Tag>()

        while (xmlStreamReader.hasNext()) {
            when (xmlStreamReader.next()) {
                XMLStreamConstants.START_ELEMENT -> {
                    val tag = Tag(xmlStreamReader.localName)
                    if (!(options.inlineNested || stack.isEmpty())) {
                        stack.last().contentBuilder.append(NESTED_TAG_PLACEHOLDER)
                    }
                    stack.addLast(tag)
                }

                XMLStreamConstants.CHARACTERS -> {
                    if (options.inlineNested) {
                        stack.forEach { it.contentBuilder.append(xmlStreamReader.text) }
                    } else {
                        stack.last().contentBuilder.append(xmlStreamReader.text)
                    }
                }

                XMLStreamConstants.END_ELEMENT -> {
                    val tag = stack.removeLast()
                    if (tag.name == xmlStreamReader.localName
                        && tag.contentBuilder.toString().replace(NESTED_TAG_PLACEHOLDER, "").isNotBlank()
                        && tag.name !in skipTags
                    ) {
                        val tagContent = tag.contentBuilder.toString().trim()
                        elements.add(
                            Element(
                                content = tagContent,
                                lineNumber = xmlStreamReader.location.lineNumber,
                                type = "xml_${tag.name}",
                            )
                        )
                    }
                }
            }
        }

        return elements
    }
}