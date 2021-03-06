package no.difi.meldingsutveksling.nextmove;

import com.google.common.collect.Maps;
import io.swagger.annotations.ApiModel;
import lombok.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Map;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@XmlRootElement(name = "digital", namespace = "urn:no:difi:profile:digitalpost:ver1.0")
@ApiModel(value = "digital", parent = BusinessMessage.class)
public class DpiDigitalMessage extends BusinessMessage<DpiDigitalMessage> {

    private String avsenderId;
    private String fakturaReferanse;
    @NotNull
    private String tittel;
    @NotNull
    private String spraak;

    @NotNull
    @Valid
    private DigitalPostInfo digitalPostInfo;

    private DpiNotification varsler;

    private Map<String, String> metadataFiler = Maps.newHashMap();
}
