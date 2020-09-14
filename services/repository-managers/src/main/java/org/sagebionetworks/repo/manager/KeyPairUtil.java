package org.sagebionetworks.repo.manager;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.List;

import java.util.Base64;
import org.apache.commons.codec.binary.Base32;
import org.sagebionetworks.repo.model.oauth.JsonWebKey;
import org.sagebionetworks.repo.model.oauth.JsonWebKeyRSA;
import org.sagebionetworks.repo.model.oauth.JsonWebKeySet;

import io.jsonwebtoken.SignatureAlgorithm;

public class KeyPairUtil {
	
	public static final String X509 = "X.509";
	
	public static final String RSA = "RSA";
	
	public static final String SHA_256 = "SHA-256";
	
	private static final String KEY_USE_SIGNATURE = "sig";

	public static X509Certificate getX509CertificateFromPEM(String pem) {
		try {
			byte[] content = Base64.getDecoder().decode(pem);
			CertificateFactory certFactory = CertificateFactory.getInstance(X509);
			X509Certificate certificate = (X509Certificate)certFactory.generateCertificate(new ByteArrayInputStream(content));
			if (certificate.getPublicKey()==null) throw new RuntimeException();
			return certificate;
		} catch (CertificateException e) {
			throw new RuntimeException(e);			
		}
	}

	public static PrivateKey getPrivateKeyFromPEM(String pem, String keyGenerationAlgorithm) {
		try {
			byte[] content = Base64.getDecoder().decode(pem);
			PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(content);
			KeyFactory factory = KeyFactory.getInstance(keyGenerationAlgorithm);
			return factory.generatePrivate(privKeySpec);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Given a pem-encoded private key return the key pair (private and public key)
	 * The translation of the private key into a public key is specific to RSA
	 * so this will not work for non-RSA keys.
	 * 
	 * @param pemEncodedPrivateKey
	 * @return
	 */
	public static KeyPair getRSAKeyPairFromPrivateKey(String pemEncodedPrivateKey) {
		try {
			byte[] content = Base64.getDecoder().decode(pemEncodedPrivateKey);
			PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(content);
			KeyFactory factory = KeyFactory.getInstance(RSA);
			PrivateKey privateKey = factory.generatePrivate(keySpec);

			RSAPrivateCrtKey privk = (RSAPrivateCrtKey)privateKey;
			RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(privk.getModulus(), privk.getPublicExponent());			
			
			PublicKey publicKey = factory.generatePublic(publicKeySpec);
			return new KeyPair(publicKey, privateKey);
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
			MessageDigest md = MessageDigest.getInstance(SHA_256);
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

	private static String bigIntToBase64URLEncoded(BigInteger i) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(i.toByteArray());
	}

	public static JsonWebKeySet getJSONWebKeySetForPEMEncodedRsaKeys(List<String> pemEncodedKeyPairs) {
		JsonWebKeySet jsonWebKeySet = new JsonWebKeySet();
		List<JsonWebKey> publicKeys = new ArrayList<JsonWebKey>();
		jsonWebKeySet.setKeys(publicKeys);
		if (pemEncodedKeyPairs==null) return jsonWebKeySet;
		for (String s : pemEncodedKeyPairs) {
			KeyPair keyPair = getRSAKeyPairFromPrivateKey(s);
			String kid = computeKeyId(keyPair.getPublic());
			RSAPublicKey rsaPublicKey = (RSAPublicKey)keyPair.getPublic();
			JsonWebKeyRSA rsaKey = new JsonWebKeyRSA();
			// these would be set for all algorithms
			rsaKey.setKty(RSA);
			rsaKey.setUse(KEY_USE_SIGNATURE);
			rsaKey.setKid(kid);
			// these are specific to the RSA algorithm
			rsaKey.setE(bigIntToBase64URLEncoded(rsaPublicKey.getPublicExponent()));
			rsaKey.setN(bigIntToBase64URLEncoded(rsaPublicKey.getModulus()));
			publicKeys.add(rsaKey);
		}
		return jsonWebKeySet;
	}

}
