package no.difi.meldingsutveksling.noarkexchange

import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException
import no.difi.meldingsutveksling.noarkexchange.schema.AppReceiptType
import no.difi.meldingsutveksling.util.logger
import org.springframework.xml.transform.StringSource
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import java.io.StringReader
import javax.xml.XMLConstants
import javax.xml.bind.JAXBContext
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.stream.XMLInputFactory
import javax.xml.xpath.XPathFactory

class PayloadUtilKt {
    companion object {
        val log = logger()
        private const val APP_RECEIPT_INDICATOR = "AppReceipt"
        private const val PAYLOAD_UNKNOWN_TYPE = "Payload is of unknown type cannot determine what type of message it is"

        @JvmStatic
        fun isAppReceipt(payload: Any): Boolean {
            return when (val p = payload) {
                is AppReceiptType -> true
                is String -> p.contains(APP_RECEIPT_INDICATOR)
                is Node -> p.firstChild?.textContent?.contains(APP_RECEIPT_INDICATOR) ?: false
                else -> false
            }
        }

        @JvmStatic
        fun payloadAsString(payload: Any): String {
            return when (val p = payload) {
                is String -> p
                is Node -> p.firstChild?.textContent ?: throw MeldingsUtvekslingRuntimeException("Child not accessible")
                else -> throw MeldingsUtvekslingRuntimeException("Could not get payload as String")
            }
        }

        @JvmStatic
        fun isEmpty(payload: Any): Boolean {
            return when (val p = payload) {
                is String -> p.isBlank()
                is Node -> !p.hasChildNodes()
                else -> throw MeldingsUtvekslingRuntimeException(PAYLOAD_UNKNOWN_TYPE)
            }
        }

        @JvmStatic
        fun getAppReceiptType(payload: Any): AppReceiptType {
            val jaxbContext = JAXBContext.newInstance("no.difi.meldingsutveksling.noarkexchange.schema")
            val unmarshaller = jaxbContext.createUnmarshaller()
            return unmarshaller.unmarshal(StringSource(payloadAsString(payload)), AppReceiptType::class.java).value
        }

        @JvmStatic
        @Throws(PayloadException::class)
        fun queryPayload(payload: Any, xpathInput: String): String {
            val xpath = XPathFactory.newInstance().newXPath()
            val xpathExpr = xpath.compile(xpathInput)

            val docFactory = DocumentBuilderFactory.newInstance()
            docFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
            val parsedDoc = docFactory.newDocumentBuilder()
                    .parse(ByteArrayInputStream(getDoc(payload).toByteArray()))
            return xpathExpr.evaluate(parsedDoc)
        }

        @JvmStatic
        fun parsePayloadForDocuments(payload: Any): List<NoarkDocument> {
            val docs = mutableListOf<NoarkDocument>()

            val xmlInputFactory = XMLInputFactory.newInstance()
            xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false)
            val eventReader = xmlInputFactory.createXMLEventReader(StringReader(getDoc(payload)))

            var noarkDoc: NoarkDocument? = null
            while (eventReader.hasNext()) {
                val event = eventReader.nextEvent()
                if (event.isStartElement) {
                    when (event.asStartElement().name.localPart) {
                        "dokument" -> noarkDoc = NoarkDocument()
                        "veFilnavn" -> noarkDoc?.filename = eventReader.nextEvent().asCharacters().data
                        "veMimeType" -> noarkDoc?.contentType = eventReader.nextEvent().asCharacters().data
                        "dbTittel" -> noarkDoc?.title = eventReader.nextEvent().asCharacters().data
                        "base64" -> noarkDoc?.content = eventReader.nextEvent().asCharacters().data.toByteArray()
                    }
                }
                if (event.isEndElement) {
                    if ("dokument" == event.asEndElement().name.localPart && noarkDoc != null) {
                        docs.add(noarkDoc)
                    }
                }
            }
            return docs
        }

        private fun getDoc(payload: Any): String {
            return when (val p = payload) {
                is String -> p
                is Node -> p.firstChild.textContent.trim()
                else -> throw MeldingsUtvekslingRuntimeException("Unknown payload")
            }

        }

        @JvmStatic
        fun queryJpId(payload: Any): String {
            return queryPayload(payload, "/Melding/journpost/jpId")
        }
    }
}