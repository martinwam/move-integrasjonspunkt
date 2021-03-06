package no.difi.meldingsutveksling.auth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import org.springframework.http.*;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.CertificateEncodingException;
import java.time.OffsetDateTime;
import java.util.*;

import static no.difi.meldingsutveksling.DateTimeUtil.DEFAULT_ZONE_ID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OidcTokenClient {

    private static final String CLIENT_ID_PREFIX = "MOVE_IP_";

    private static final String SCOPE_DPO = "move/dpo.read";
    private static final String SCOPE_DPE = "move/dpe.read";
    private static final String SCOPE_DPV = "move/dpv.read";
    private static final String SCOPE_DPF = "move/dpf.read";
    private static final List<String> SCOPES_DPI = Arrays.asList("move/dpi.read",
            "global/kontaktinformasjon.read",
            "global/sikkerdigitalpost.read",
            "global/varslingsstatus.read",
            "global/sertifikat.read",
            "global/navn.read",
            "global/postadresse.read");

    private final IntegrasjonspunktProperties props;

    @Retryable(value = HttpClientErrorException.class, maxAttempts = -1,
            backoff = @Backoff(delay = 5000, maxDelay = 1000*60*60, multiplier = 3))
    public IdportenOidcTokenResponse fetchToken() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setErrorHandler(new OidcErrorHandler());

        LinkedMultiValueMap<String, String> attrMap = new LinkedMultiValueMap<>();
        attrMap.add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
        attrMap.add("assertion", generateJWT());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> httpEntity = new HttpEntity<>(attrMap, headers);

        FormHttpMessageConverter formHttpMessageConverter = new FormHttpMessageConverter();
        restTemplate.getMessageConverters().add(formHttpMessageConverter);

        URI accessTokenUri;
        try {
            accessTokenUri = props.getOidc().getUrl().toURI();
        } catch (URISyntaxException e) {
            log.error("Error converting property to URI", e);
            throw new RuntimeException(e);
        }

        ResponseEntity<IdportenOidcTokenResponse> response = restTemplate.exchange(accessTokenUri, HttpMethod.POST,
                httpEntity, IdportenOidcTokenResponse.class);
        log.info("Response: {}", response.toString());

        return response.getBody();
    }

    public String generateJWT() {
        IntegrasjonspunktNokkel nokkel = new IntegrasjonspunktNokkel(props.getOidc().getKeystore());

        List<Base64> certChain = new ArrayList<>();
        try {
            certChain.add(Base64.encode(nokkel.getX509Certificate().getEncoded()));
        } catch (CertificateEncodingException e) {
            log.error("Could not get encoded certificate", e);
            throw new RuntimeException(e);
        }

        JWSHeader jwsHeader = new JWSHeader.Builder(JWSAlgorithm.RS256).x509CertChain(certChain).build();

        String clientId = props.getOidc().getClientId();
        if (!StringUtils.hasText(clientId)) {
            clientId = CLIENT_ID_PREFIX + props.getOrg().getNumber();
        }
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .audience(props.getOidc().getAudience())
                .issuer(clientId)
                .claim("scope", getCurrentScopes())
                .jwtID(UUID.randomUUID().toString())
                .issueTime(Date.from(OffsetDateTime.now(DEFAULT_ZONE_ID).toInstant()))
                .expirationTime(Date.from(OffsetDateTime.now(DEFAULT_ZONE_ID).toInstant().plusSeconds(120)))
                .build();

        RSASSASigner signer = new RSASSASigner(nokkel.loadPrivateKey());

        if (nokkel.shouldLockProvider()) {
            signer.getJCAContext().setProvider(nokkel.getKeyStore().getProvider());
        }

        SignedJWT signedJWT = new SignedJWT(jwsHeader, claims);
        try {
            signedJWT.sign(signer);
        } catch (JOSEException e) {
            log.error("Error occured during signing of JWT", e);
        }

        String serializedJwt = signedJWT.serialize();
        log.info("SerializedJWT: {}", serializedJwt);

        return serializedJwt;
    }

    public String getCurrentScopes() {

        ArrayList<String> scopeList = new ArrayList<>();
        if (props.getFeature().isEnableDPO()) {
            scopeList.add(SCOPE_DPO);
        }
        if (props.getFeature().isEnableDPE()) {
            scopeList.add(SCOPE_DPE);
        }
        if (props.getFeature().isEnableDPV()) {
            scopeList.add(SCOPE_DPV);
        }
        if (props.getFeature().isEnableDPF()) {
            scopeList.add(SCOPE_DPF);
        }
        if (props.getFeature().isEnableDPI()) {
            scopeList.addAll(SCOPES_DPI);
        }

        return scopeList.stream().reduce((a, b) -> a + " " + b).orElse("");
    }

}
