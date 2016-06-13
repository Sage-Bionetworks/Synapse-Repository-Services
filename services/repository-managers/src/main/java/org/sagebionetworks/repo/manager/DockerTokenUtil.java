package org.sagebionetworks.repo.manager;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.io.ByteArrayInputStream;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;
import java.util.List;

import org.apache.commons.codec.binary.Base32;
import org.apache.commons.net.util.Base64;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.sagebionetworks.StackConfiguration;

public class DockerTokenUtil {

	private static final String ISSUER = "www.synapse.org";
	private static final long TIME_WINDOW_SEC = 1200; // twenty minutes
	private static final String ACCESS = "access";

	private static final String PUBLIC_KEY_ID;
	private static final PrivateKey PRIVATE_KEY;
	
	static {
		PRIVATE_KEY = readPrivateKey();
		X509Certificate certificate = readCertificate();
		PUBLIC_KEY_ID = computeKeyId(certificate.getPublicKey());
	}

	public static String createToken(String userName, String type, 
			String registry, String repository, List<String> actions, long now, String uuid) {

		JSONArray access = new JSONArray();
		JSONObject accessEntry = new JSONObject();

		access.add(accessEntry);
		accessEntry.put("type", type);
		accessEntry.put("name", repository);
		JSONArray actionArray = new JSONArray();
		for (String action : actions) actionArray.add(action);
		accessEntry.put("actions", actionArray);

		Claims claims = Jwts.claims()
				.setIssuer(ISSUER)
				.setAudience(registry)
				.setExpiration(new Date(now+TIME_WINDOW_SEC*1000L))
				.setNotBefore(new Date(now-TIME_WINDOW_SEC*1000L))
				.setIssuedAt(new Date(now))
				.setId(uuid)
				.setSubject(userName);
		claims.put(ACCESS, access);
		
		String s = Jwts.builder().setClaims(claims).
				setHeaderParam(Header.TYPE, Header.JWT_TYPE).
				setHeaderParam("kid", PUBLIC_KEY_ID).
				signWith(SignatureAlgorithm.ES256, PRIVATE_KEY).compact();

		// The signature created by the Jwts library is wrong:
		// The signature must be in P1363 format, NOT ASN.1, 
		// which is what the code underlying 'compact()' generates when signing.
		// Below we regenerate it.
		try {
			String[] pieces = s.split("\\.");
			if (pieces.length!=3) throw new RuntimeException("Expected 3 pieces but found "+pieces.length);
			// the first two pieces are the message to sign.  The third piece is the (incorrect) signature.

			Base64 base64 = new Base64();
			// what are the original signature bytes?
			byte[] originalSigBytes = base64.decode(pieces[2]);

			int lengthOfRemaining = originalSigBytes[1]; // # of bytes, from originalSigBytes[2] to end
			if (lengthOfRemaining>originalSigBytes.length-2) throw
				new IllegalStateException("Expected <="+(originalSigBytes.length-2)+" but found "+lengthOfRemaining);
			int shouldBe2 = originalSigBytes[2];
			if (shouldBe2!=2) throw new IllegalStateException("Exected 2 but found "+shouldBe2);
			int lengthOfVR = originalSigBytes[3]; // should be 32
			if (lengthOfVR!=32 && lengthOfVR!=33) 
				throw new IllegalStateException("Exected 32 or 33 but found "+lengthOfVR);
			// VR goes from originalSigBytes[4], for lengthOfVR bytes
			// for Java, you can simply perform new BigInteger(1, byte[] r).toByteArray() as the default Java encoding of a BigInteger is identical to the ASN.1 encoding of INTEGER
			shouldBe2 = originalSigBytes[4+lengthOfVR];
			if (shouldBe2!=2) 
				throw new IllegalStateException("Exected 2 but found "+shouldBe2);
			int lengthOfVS = originalSigBytes[5+lengthOfVR];
			if (lengthOfVS!=32 && lengthOfVS!=33) 
				throw new IllegalStateException("Exected 32 or 33 but found "+lengthOfVS);
			// VS starts at originalSigBytes[6+lengthOfVR] and goes for lengthOfVS bytes
			// originalSigBytes.length should be >= 6+lengthOfVR+lengthOfVS
			if (lengthOfVS>originalSigBytes.length-6-lengthOfVR) throw
				new IllegalStateException("Expected <="+(originalSigBytes.length-6-lengthOfVR)+" but found "+lengthOfVS);

			byte[] p1363Signature = new byte[64];

			// r and s each occupy half the array
			// Remove padding bytes
			int numVRBytes = lengthOfVR > 32 ? 32 : lengthOfVR;
			System.arraycopy(originalSigBytes, 4+(lengthOfVR > 32 ? 1 : 0), 
					p1363Signature, 0, numVRBytes);

			int numVSBytes = lengthOfVS > 32 ? 32 : lengthOfVS;
			if (numVRBytes+numVSBytes!=p1363Signature.length)
				throw new IllegalStateException("Source bytes number: "+(numVRBytes+numVSBytes)
						+" but destination array has length "+p1363Signature.length);
			System.arraycopy(originalSigBytes, 6+lengthOfVR+(lengthOfVS > 32 ? 1 : 0), 
					p1363Signature, numVRBytes, numVSBytes);

			String base64Encoded = Base64.encodeBase64URLSafeString(p1363Signature);
			while (base64Encoded.endsWith("=")) 
				base64Encoded = base64Encoded.substring(0, base64Encoded.length()-1);

			s = pieces[0]+"."+pieces[1]+"."+base64Encoded;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return s;

	}
	
	public static final String KEY_GENERATION_ALGORITHM = "EC";

	private static PrivateKey readPrivateKey() {
		try {
			KeyFactory factory = KeyFactory.getInstance(KEY_GENERATION_ALGORITHM);
			byte[] content = Base64.decodeBase64(StackConfiguration.getDockerAuthorizationPrivateKey());
			PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(content);
			return factory.generatePrivate(privKeySpec);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static X509Certificate readCertificate() {
		try {
			byte[] content = Base64.decodeBase64(StackConfiguration.getDockerAuthorizationCertificate());
			CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
			X509Certificate certificate = (X509Certificate)certFactory.generateCertificate(new ByteArrayInputStream(content));
			if (certificate.getPublicKey()==null) throw new RuntimeException();
			return certificate;
		} catch (CertificateException e) {
			throw new RuntimeException(e);			
		}
	}

	// from https://botbot.me/freenode/cryptography-dev/2015-12-04/?page=1
	// SPKI DER SHA-256 hash, strip of the last two bytes, base32 encode it and then add a : every four chars.
	private static String computeKeyId(PublicKey publicKey) {
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
