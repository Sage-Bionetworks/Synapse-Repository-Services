package org.sagebionetworks.repo.manager;

import java.io.ByteArrayInputStream;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;

import org.apache.commons.codec.binary.Base32;
import org.apache.commons.net.util.Base64;

public class JWTUtil {
	public static X509Certificate getX509CertificateFromPEM(String pem) {
		try {
			byte[] content = Base64.decodeBase64(pem);
			CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
			X509Certificate certificate = (X509Certificate)certFactory.generateCertificate(new ByteArrayInputStream(content));
			if (certificate.getPublicKey()==null) throw new RuntimeException();
			return certificate;
		} catch (CertificateException e) {
			throw new RuntimeException(e);			
		}
	}

	public static PrivateKey getPrivateKeyFromPEM(String pem, String keyGenerationAlgorithm) {
		try {
			byte[] content = Base64.decodeBase64(pem);
			PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(content);
			KeyFactory factory = KeyFactory.getInstance(keyGenerationAlgorithm);
			return factory.generatePrivate(privKeySpec);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	// from https://botbot.me/freenode/cryptography-dev/2015-12-04/?page=1
	// SPKI DER SHA-256 hash, strip of the last two bytes, base32 encode it and then add a : every four chars.
	public static String computeKeyId(PublicKey publicKey) {
		try {
			if (publicKey==null) throw new RuntimeException();
			// http://stackoverflow.com/questions/3103652/hash-string-via-sha-256-in-java
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(publicKey.getEncoded());
			byte[] digest = md.digest();
			// use bytes 0->digest.length-2
			Base32 base32 = new Base32();
			byte[] skipLastTwoBytes = new byte[digest.length-2];
			System.arraycopy(digest, 0, skipLastTwoBytes, 0, skipLastTwoBytes.length);
			String base32Encoded = base32.encodeAsString(skipLastTwoBytes);
			StringBuilder sb = new StringBuilder();
			if ((base32Encoded.length() % 4)!=0) 
				throw new IllegalStateException("Expected string length to be a multiple of 4 but found "+base32Encoded);
			boolean firsttime = true;
			for (int i=0; i<base32Encoded.length(); i+=4) {
				if (firsttime) firsttime=false; else sb.append(":");
				sb.append(base32Encoded.substring(i, i+4));
			}
			return sb.toString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
