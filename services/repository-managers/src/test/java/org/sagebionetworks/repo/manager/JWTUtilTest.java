package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;

import java.security.cert.X509Certificate;

import org.junit.Test;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.StackConfigurationSingleton;

public class JWTUtilTest {
	private static final StackConfiguration STACK_CONFIG = StackConfigurationSingleton.singleton();

	@Test
	public void testReadX509CertificateFromPEM() throws Exception {
		// just make sure a valid result can be extracted
		JWTUtil.getX509CertificateFromPEM(STACK_CONFIG.getDockerAuthorizationCertificate());
	}
	
	@Test
	public void testReadPrivateKeyFromPEM() throws Exception {
		// just make sure a valid result can be extracted
		JWTUtil.getPrivateKeyFromPEM(STACK_CONFIG.getDockerAuthorizationPrivateKey(), "EC");
	}
	
	@Test
	public void testKeyId() throws Exception {
		X509Certificate certificate = JWTUtil.getX509CertificateFromPEM(STACK_CONFIG.getDockerAuthorizationCertificate());
		String keyId = JWTUtil.computeKeyId(certificate.getPublicKey());
		String expectedKeyId = "FWOZ:6JNY:OUZ5:BHLA:YWJI:PKL4:G6QR:XCMK:3BU4:EIXW:L3Q7:VMIR";
		assertEquals(expectedKeyId, keyId);
	}

}
