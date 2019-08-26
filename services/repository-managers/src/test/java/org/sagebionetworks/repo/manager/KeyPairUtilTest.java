package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.security.KeyPair;
import java.security.cert.X509Certificate;

import org.junit.Test;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.StackConfigurationSingleton;

public class KeyPairUtilTest {
	private static final StackConfiguration STACK_CONFIG = StackConfigurationSingleton.singleton();

	@Test
	public void testReadX509CertificateFromPEM() throws Exception {
		// just make sure a valid result can be extracted
		KeyPairUtil.getX509CertificateFromPEM(STACK_CONFIG.getDockerAuthorizationCertificate());
	}
	
	@Test
	public void testReadPrivateKeyFromPEM() throws Exception {
		// just make sure a valid result can be extracted
		KeyPairUtil.getPrivateKeyFromPEM(STACK_CONFIG.getDockerAuthorizationPrivateKey(), "EC");
	}
	
	@Test
	public void testGetRSAKeyPairFromPEM() throws Exception {
		String pemEncodedPrivateKey=STACK_CONFIG.getOIDCSignatureRSAPrivateKeys().get(0);
		KeyPair keyPair = KeyPairUtil.getRSAKeyPairFromPrivateKey(pemEncodedPrivateKey);
		assertNotNull(keyPair.getPrivate());
		assertNotNull(keyPair.getPublic());
	}

	@Test
	public void testComputeKeyId() throws Exception {
		X509Certificate certificate = KeyPairUtil.getX509CertificateFromPEM(STACK_CONFIG.getDockerAuthorizationCertificate());
		String keyId = KeyPairUtil.computeKeyId(certificate.getPublicKey());
		String expectedKeyId = "FWOZ:6JNY:OUZ5:BHLA:YWJI:PKL4:G6QR:XCMK:3BU4:EIXW:L3Q7:VMIR";
		assertEquals(expectedKeyId, keyId);
	}
}
