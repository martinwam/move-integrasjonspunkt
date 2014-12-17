package no.difi.meldingsutveksling.dokumentpakking.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.Test;

public class CmsUtilTest {

	@Test
	public void testDecryptCMSKeysGeneratedProgrammatically() throws Exception {
		CmsUtil util = new CmsUtil();
		KeyPair keyPair = generateKeyPair();
		Certificate certificate = generateCertificate(keyPair.getPublic(), keyPair.getPrivate());

		byte[] plaintext = "Text to be encrypted".getBytes();
		byte[] ciphertext = (new CmsUtil()).createCMS(plaintext, (X509Certificate) certificate);
		byte[] plaintextRecovered = util.decryptCMS(ciphertext, keyPair.getPrivate());

		assertThat(plaintextRecovered, is(equalTo(plaintext)));
	}

	@Test
	public void test() throws IOException, CertificateException {
		Security.addProvider(new BouncyCastleProvider());

		X509Certificate cert = null;
		PEMParser pemRd = openPEMResource("difi-cert.pem");
		Object o = null;

		while ((o = pemRd.readObject()) != null) {
			if (!(o instanceof X509CertificateHolder)) {
				throw new RuntimeException();
			} else {
				 cert =  new JcaX509CertificateConverter().setProvider( "BC" )
						  .getCertificate( (X509CertificateHolder) o );
			}
		}
		
		CmsUtil util = new CmsUtil();
		KeyPair keyPair = doOpenSslTestFile("difi-privkey.pem", RSAPrivateKey.class);

		byte[] plaintext = "Text to be encrypted".getBytes();
		byte[] ciphertext = (new CmsUtil()).createCMS(plaintext, cert);
		byte[] plaintextRecovered = util.decryptCMS(ciphertext, keyPair.getPrivate());

		assertThat(plaintextRecovered, is(equalTo(plaintext)));
	}

	public KeyPair generateKeyPair() throws NoSuchAlgorithmException {
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
		keyPairGenerator.initialize(2048);
		return keyPairGenerator.generateKeyPair();
	}

	private Certificate generateCertificate(PublicKey subjectPublicKey, PrivateKey issuerPrivateKey) throws ParseException, OperatorCreationException,
			CertificateException, IOException {
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");

		X500Name issuer = new X500Name("CN=Issuer and subject (self signed)");
		BigInteger serial = new BigInteger("100");
		Date notBefore = df.parse("2010-01-01");
		Date notAfter = df.parse("2050-01-01");
		X500Name subject = issuer;
		SubjectPublicKeyInfo publicKeyInfo = new SubjectPublicKeyInfo(ASN1Sequence.getInstance(subjectPublicKey.getEncoded()));

		X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(issuer, serial, notBefore, notAfter, subject, publicKeyInfo);

		ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(issuerPrivateKey);

		X509CertificateHolder holder = certBuilder.build(signer);

		CertificateFactory factory = CertificateFactory.getInstance("X.509");
		X509Certificate cert = (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(holder.getEncoded()));

		return cert;
	}

	

	private KeyPair doOpenSslTestFile(String fileName, Class expectedPrivKeyClass) throws IOException {
		JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
		PEMDecryptorProvider decProv = new JcePEMDecryptorProviderBuilder().setProvider("BC").build("changeit".toCharArray());

		PEMParser pr = openPEMResource(fileName);
		Object o = pr.readObject();

		if (o == null || !((o instanceof PEMKeyPair) || (o instanceof PEMEncryptedKeyPair))) {
			throw new RuntimeException();
		}

		KeyPair kp;
		if (o instanceof PEMEncryptedKeyPair)
			kp = converter.getKeyPair(((PEMEncryptedKeyPair) o).decryptKeyPair(decProv));
		else
			kp = converter.getKeyPair((PEMKeyPair) o);

		return kp;
	}

	private PEMParser openPEMResource(String fileName) {
		InputStream res = getClass().getClassLoader().getResourceAsStream(fileName);
		Reader fRd = new BufferedReader(new InputStreamReader(res));
		return new PEMParser(fRd);
	}
}