package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.sagebionetworks.repo.model.oauth.JsonWebKey;
import org.sagebionetworks.repo.model.oauth.JsonWebKeyRSA;
import org.sagebionetworks.repo.model.oauth.JsonWebKeySet;

public class KeyPairUtilTest {
	
	private static final String DEV_DOCKER_AUTHORIZATION_PRIVATE_KEY = 
		"MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQg38HpyCOGkOiq2iJB"+
		"C7uEN8wWv/NV0jYF/ihpQJ4ng36hRANCAAQ5e3PRzLYweP78cDPDwPMc6XhuTaKg"+
		"KtmDEsqS13DmBXt8c65rt3owdJ7JCxHJYxON91Eg9sBQhT2K8yyOf+PB";

	private static final String DEV_DOCKER_AUTHORIZATION_CERTIFICATE_PEM = 
		"MIIC+zCCAqGgAwIBAgIJAI+Kok0VfNbJMAkGByqGSM49BAEwgYgxCzAJBgNVBAYT" + 
		"AlVTMQswCQYDVQQIEwJXQTEQMA4GA1UEBxMHU2VhdHRsZTENMAsGA1UEChMEU2Fn" + 
		"ZTELMAkGA1UECxMCUEwxGDAWBgNVBAMTD3d3dy5zeW5hcHNlLm9yZzEkMCIGCSqG" + 
		"SIb3DQEJARYVcGxhdGZvcm1Ac2FnZWJhc2Uub3JnMCAXDTE2MDYxMjE2NTU0M1oY" + 
		"DzIxMTYwNTE5MTY1NTQzWjCBiDELMAkGA1UEBhMCVVMxCzAJBgNVBAgTAldBMRAw" + 
		"DgYDVQQHEwdTZWF0dGxlMQ0wCwYDVQQKEwRTYWdlMQswCQYDVQQLEwJQTDEYMBYG" + 
		"A1UEAxMPd3d3LnN5bmFwc2Uub3JnMSQwIgYJKoZIhvcNAQkBFhVwbGF0Zm9ybUBz" + 
		"YWdlYmFzZS5vcmcwWTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAAQ5e3PRzLYweP78" + 
		"cDPDwPMc6XhuTaKgKtmDEsqS13DmBXt8c65rt3owdJ7JCxHJYxON91Eg9sBQhT2K" + 
		"8yyOf+PBo4HwMIHtMB0GA1UdDgQWBBQXzi7VyAjmc2nucRtLQaAkGL7RWTCBvQYD" + 
		"VR0jBIG1MIGygBQXzi7VyAjmc2nucRtLQaAkGL7RWaGBjqSBizCBiDELMAkGA1UE" + 
		"BhMCVVMxCzAJBgNVBAgTAldBMRAwDgYDVQQHEwdTZWF0dGxlMQ0wCwYDVQQKEwRT" + 
		"YWdlMQswCQYDVQQLEwJQTDEYMBYGA1UEAxMPd3d3LnN5bmFwc2Uub3JnMSQwIgYJ" + 
		"KoZIhvcNAQkBFhVwbGF0Zm9ybUBzYWdlYmFzZS5vcmeCCQCPiqJNFXzWyTAMBgNV" + 
		"HRMEBTADAQH/MAkGByqGSM49BAEDSQAwRgIhAI5mLcT6D++3oTbjdNhKs6SI3ijO" + 
		"mKf6Xe++KzE8JJknAiEAq17eb0ZG0Eh/3hKk+9FREUAZ2iub3HPjwD3QPA+bL1c=";
	
	private static final String DEV_OIDC_SIGNATURE_RSA_PRIVATE_KEY_PEM =
		"MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDinbEq1zwFsfSA" + 
		"FCajV9qrk2Qok9EiF/jlp/bRknCMEgj8j5oYiDxmnFXvnrSHJVk1NUOo4ON8m3WR" + 
		"MCLpN7t0ygIKvx30L7xdVlnvoldv/lYx87KqGEVadEnl/lGEaGRxpdXkiwgJem9l" + 
		"Ht7V8TquJiIcqQviwCc1ZKjtKtSbZtcpA0MxT/YNATf7GBWWD5KH39qL6/hHrCIa" + 
		"BYCqbNQt594/UZEgxRfB6sKyb2744Jshiq41Y25TJc4gNUq69EBMkaQrC9V67as6" + 
		"TJG2GJhfmAPsXgr7Lbyk5/kTsC8YXAasAzZnpA+ljLNv6pIdpyizx9XNrz13DFrY" + 
		"RK8lbHkrAgMBAAECggEANIuT9PcLN9bXdos1mlJYpcf7RV1g9KLSV43msRlfd1sH" + 
		"Mmiptl6AgtplIraN7Xg/gxLiqVnb5Zy2Wf/rWGBP2visGInQDDq1Vn8bQ3FFDPbQ" + 
		"TazQFJikHCEysV2S0TzTbXaibee+6VO2WKAb00en75Fv/21DEES10q+Qa82ulomG" + 
		"THIy7RitkdhkKPv16Lz7EQFcrdbZieldppijm15EK+wAKl8TAShIyZaAC8QY61F1" + 
		"hqqLApigI1GL7knP4jnii6sF7ykEpHb5huf3RI18DGwtUZOWLqimHgvmfn3p5NLT" + 
		"d/Xo56SOazhTBlvkNlUQccIQDzA8M2pjLcxxnMLcoQKBgQD0eRY63R3ZJh72OLCm" + 
		"C1iRS8NnQmdC+iR3zw7J5jvOkHSDctI1Zf6X+D4iyPLr1+tjnLy5YgTDP9PUJi9h" + 
		"3FRbnGkPD7I5q+19cvV5bQvxn36+kKZunfgLQYYe1Zk/6BbItOli5fzyQK3QTSp8" + 
		"eIKdQ02VQiFNV4UESbrb00UWNQKBgQDtTQ/hun6eYCVW9n3TmTi5+WRBb1R4X3kd" + 
		"/tifYhzy2Eab2pgQ3BN9INENlBsPb8J1+86rWwY2KVO9vHhctK47ytEn1kv//KCo" + 
		"ugo870dEZuAqX9ACNtG/Ba52IkCSjcpTJRo3R3lTLASV7vLZs4q5AyKNWCJD49kr" + 
		"KC9nNpW93wKBgQDRkYtQ4oPXxin8gBROAqPlycC0H+RNMglY+xJ+WPMj3AlFNYSl" + 
		"ac2ZkKATSZeUPP/34ECX2kKi7XA1CJbNmQZnkektlBMABTYMuCNd9/CpLESGL5G8" + 
		"eYZMf9rtS8WXVulRHGSE9wqi0HcvfTbShKvTDALR1GKf3kqUpm+cSbuLkQKBgCfS" + 
		"hNXGrDT7wYhkeR0nW2OqPG7WtgA1VWf5OnUUy/Lc5IyHFHnP1N1swmha8GeYw7N0" + 
		"/Gu5LMOuD8WJeVFlaM/T62GaDsr4pCVsgwdSyEzsTrYNuiSE+pHp7Csa+GcfsFJf" + 
		"qZSZQ/z3KBXZMZvjC2ac5hF+NtHZzLn3Vm0ltd9VAoGBAJcs9PBjJ9XqAHo6x88g" + 
		"7KxK1mfXRrVoxMLZD2njx4xWTK6OWOAECdWBIPbsstT57BVUeSa5wAVy/SkgfZ3E" + 
		"MjjcxwFQoYuOR/cgYAFyXY/jMXb6kKSdc02437hTXjTotvkgAJfO7gxVy8s3ipI7" + 
		"pzvMOgD+6P3bCeoBuyKg2xhY";

	@Test
	public void testReadX509CertificateFromPEM() throws Exception {
		// just make sure a valid result can be extracted
		KeyPairUtil.getX509CertificateFromPEM(DEV_DOCKER_AUTHORIZATION_CERTIFICATE_PEM);
	}
	
	@Test
	public void testReadPrivateKeyFromPEM() throws Exception {
		// just make sure a valid result can be extracted
		KeyPairUtil.getPrivateKeyFromPEM(DEV_DOCKER_AUTHORIZATION_PRIVATE_KEY, "EC");
	}
	
	@Test
	public void testGetRSAKeyPairFromPEM() throws Exception {
		// just make sure we can extract a private/public key pair
		KeyPair keyPair = KeyPairUtil.getRSAKeyPairFromPrivateKey(DEV_OIDC_SIGNATURE_RSA_PRIVATE_KEY_PEM);
		assertNotNull(keyPair.getPrivate());
		assertNotNull(keyPair.getPublic());
	}

	@Test
	public void testComputeKeyId() throws Exception {
		X509Certificate certificate = KeyPairUtil.getX509CertificateFromPEM(DEV_DOCKER_AUTHORIZATION_CERTIFICATE_PEM);
		String keyId = KeyPairUtil.computeKeyId(certificate.getPublicKey());
		String expectedKeyId = "FWOZ:6JNY:OUZ5:BHLA:YWJI:PKL4:G6QR:XCMK:3BU4:EIXW:L3Q7:VMIR";
		assertEquals(expectedKeyId, keyId);
	}
	
	@Test
	public void testGetJSONWebKeySetForPEMEncodedRsaKeys() throws Exception {
		List<String> devPrivateKeys = Collections.singletonList(DEV_OIDC_SIGNATURE_RSA_PRIVATE_KEY_PEM);
		
		// method under test
		JsonWebKeySet jwks = KeyPairUtil.getJSONWebKeySetForPEMEncodedRsaKeys(devPrivateKeys);
		
		assertNotNull(jwks.getKeys());
		List<JsonWebKey> keys = jwks.getKeys();
		assertEquals(1, keys.size());
		assertTrue(keys.get(0) instanceof JsonWebKeyRSA);
		JsonWebKeyRSA key = (JsonWebKeyRSA)keys.get(0);
		assertEquals("FPQF:TYN3:DMDM:URKQ:BRS4:BX2W:5VSW:3HXA:4D7Z:KOTS:EI26:GPJ6", key.getKid());
		assertEquals("RSA", key.getKty());
		assertEquals("sig", key.getUse());
		new BigInteger(Base64.getUrlDecoder().decode(key.getE())); // make sure its a Base64 encoded integer string
		new BigInteger(Base64.getUrlDecoder().decode(key.getN())); // make sure its a Base64 encoded integer string	
	}
}
