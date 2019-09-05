package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.List;

import org.junit.Test;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.repo.model.oauth.JsonWebKey;
import org.sagebionetworks.repo.model.oauth.JsonWebKeyRSA;
import org.sagebionetworks.repo.model.oauth.JsonWebKeySet;

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
		// just make sure we can extract a private/public key pair
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
	
	@Test
	public void testGetJSONWebKeySetForPEMEncodedRsaKeys() throws Exception {
		List<String> devPrivateKeys = STACK_CONFIG.getOIDCSignatureRSAPrivateKeys();
		
		// method under test
		JsonWebKeySet jwks = KeyPairUtil.getJSONWebKeySetForPEMEncodedRsaKeys(devPrivateKeys);
		
		assertNotNull(jwks.getKeys());
		List<JsonWebKey> keys = jwks.getKeys();
		assertEquals(1, keys.size());
		assertTrue(keys.get(0) instanceof JsonWebKeyRSA);
		JsonWebKeyRSA key = (JsonWebKeyRSA)keys.get(0);
		assertEquals("FPQF:TYN3:DMDM:URKQ:BRS4:BX2W:5VSW:3HXA:4D7Z:KOTS:EI26:GPJ6", key.getKid());
		assertEquals("RS256", key.getKty());
		assertEquals("SIGNATURE", key.getUse());
		assertNotNull(key.getE());
		assertNotNull(key.getN());		
	}
	
	@Test
	public void getRSAPublicKeyForJsonWebKeyRSA() throws Exception {
		List<String> devPrivateKeys = STACK_CONFIG.getOIDCSignatureRSAPrivateKeys();
		JsonWebKeySet jwks = KeyPairUtil.getJSONWebKeySetForPEMEncodedRsaKeys(devPrivateKeys);
		JsonWebKeyRSA key = (JsonWebKeyRSA)jwks.getKeys().get(0);
		
		// method under test
		RSAPublicKey rsaPublicKey = KeyPairUtil.getRSAPublicKeyForJsonWebKeyRSA(key);
		
		assertEquals("RSA", rsaPublicKey.getAlgorithm());
		assertEquals(key.getN(), rsaPublicKey.getModulus().toString());
		assertEquals(key.getE(), rsaPublicKey.getPublicExponent().toString());
	}
}
