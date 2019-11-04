package no.difi.meldingsutveksling.domain.arkivmelding

import no.arkivverket.standarder.noark5.arkivmelding.Arkivmelding
import no.arkivverket.standarder.noark5.metadatakatalog.Korrespondanseparttype
import no.difi.meldingsutveksling.DateTimeUtil
import no.difi.meldingsutveksling.core.BestEduConverter
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException
import no.difi.meldingsutveksling.noarkexchange.PutMessageRequestWrapper
import no.difi.meldingsutveksling.util.logger
import no.difi.meldingsutveksling.util.notNullOrEmpty
import org.springframework.stereotype.Component
import javax.xml.datatype.XMLGregorianCalendar

@Component
object ArkivmeldingFactoryKt {
    val log = logger()

    private fun String.toXmlGregorianCalendar() : XMLGregorianCalendar? = DateTimeUtil.toXMLGregorianCalendar(this)

    fun from(putMessage: PutMessageRequestWrapper) : Arkivmelding {
        val mt = BestEduConverter.payloadAsMeldingType(putMessage.payload)
        if (mt.noarksak == null) {
            throw MeldingsUtvekslingRuntimeException("No Noarksak in MeldingType for message ${putMessage.conversationId}, aborting conversion")
        }
        if (mt.journpost == null) {
            throw MeldingsUtvekslingRuntimeException("No Journpost in MeldingType for message ${putMessage.conversationId}, aborting conversion")
        }

        val amOf = no.arkivverket.standarder.noark5.arkivmelding.ObjectFactory()
        val arkivmelding = amOf.createArkivmelding()

        val sm = amOf.createSaksmappe()
        with (mt.noarksak) {
            sm.saksaar = saSaar?.toBigInteger()
            sm.sakssekvensnummer = saSeknr?.toBigInteger()
            sm.saksansvarlig = saAnsvinit
            sm.administrativEnhet = saAdmkort
            sm.offentligTittel = saOfftittel
            sm.systemID = saId
            sm.saksdato = saDato?.toXmlGregorianCalendar()
            sm.tittel = saTittel
            sm.saksstatus = SaksstatusMapper.getArkivmeldingType(saStatus)
            saArkdel?.let { sm.referanseArkivdel.add(it) }
            sm.journalenhet = saJenhet
        }

        val jp = amOf.createJournalpost()
        with (mt.journpost) {
            jp.systemID = jpId
            jp.tittel = jpInnhold
            jp.journalaar = jpJaar?.toBigInteger()
            jp.forfallsdato = jpForfdato?.toXmlGregorianCalendar()
            jp.journalsekvensnummer = jpSeknr?.toBigInteger()
            jp.journalpostnummer = jpJpostnr?.toBigInteger()
            jp.journalposttype = JournalposttypeMapper.getArkivmeldingType(jpNdoktype)
            jp.journalstatus = JournalstatusMapper.getArkivmeldingType(jpStatus)
            jp.referanseArkivdel = jpArkdel
            jp.antallVedlegg = jpAntved?.toBigInteger()
            jp.offentligTittel = jpOffinnhold
            jp.journaldato = jpJdato?.toXmlGregorianCalendar()
            jp.dokumentetsDato = jpDokdato?.toXmlGregorianCalendar()

            val skjerming = amOf.createSkjerming()
            skjerming.skjermingshjemmel = jpUoff
            sm.skjerming = skjerming
        }

        mt.journpost.avsmot.filterNotNull().forEach {
            val kp = amOf.createKorrespondansepart()
            kp.korrespondansepartNavn = it.amNavn
            kp.administrativEnhet = it.amAdmkort
            kp.saksbehandler = it.amSbhinit
            it.amAdresse?.let { a -> kp.postadresse.add(a) }
            kp.postnummer = it.amPostnr
            kp.poststed = it.amPoststed
            kp.land = it.amUtland

            kp.korrespondanseparttype = when (it.amIhtype) {
                "0" -> Korrespondanseparttype.AVSENDER
                "1" -> Korrespondanseparttype.MOTTAKER
                else -> null
            }
            jp.korrespondansepart.add(kp)

            val avs = amOf.createAvskrivning()
            it.amAvskm.notNullOrEmpty { am -> avs.avskrivningsmaate = AvskrivningsmaateMapper.getArkivmeldingType(am) }
            avs.referanseAvskrivesAvJournalpost = it.amAvsavdok
            avs.avskrivningsdato = it.amAvsavdok?.toXmlGregorianCalendar()
            jp.avskrivning.add(avs)
        }

        mt.journpost.dokument.filterNotNull().forEach {
            val db = amOf.createDokumentbeskrivelse()
            db.tittel = it.dbTittel
            db.dokumentnummer = it.dlRnr?.toBigInteger()
            db.tilknyttetRegistreringSom = TilknyttetRegistreringSomMapper.getArkivmeldingType(it.dlType)

            val dobj = amOf.createDokumentobjekt()
            dobj.referanseDokumentfil = it.veFilnavn
            dobj.variantformat = VariantformatMapper.getArkivmeldingType(it.veVariant)
            db.dokumentobjekt.add(dobj)
            jp.dokumentbeskrivelseAndDokumentobjekt.add(db)
        }

        sm.basisregistrering.add(jp)
        arkivmelding.mappe.add(sm)
        return arkivmelding
    }
}