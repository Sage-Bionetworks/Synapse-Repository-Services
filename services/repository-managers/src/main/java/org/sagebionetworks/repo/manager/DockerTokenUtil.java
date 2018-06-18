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
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.codec.binary.Base32;
import org.apache.commons.net.util.Base64;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.repo.model.docker.RegistryEventAction;

public class DockerTokenUtil {

	private static final String ISSUER = "www.synapse.org";
	private static final long TIME_WINDOW_SEC = 120L; // two minutes
	private static final String ACCESS = "access";

	public static final String PUBLIC_KEY_ID;
	private static final PrivateKey PRIVATE_KEY;

	// Eliptic Curve key is required by the Json Web Token signing library
	public static final String KEY_GENERATION_ALGORITHM = "EC";
	

	static {
		Security.removeProvider("SunEC");
		Security.removeProvider("EC");
		Security.addProvider(new BouncyCastleProvider());	
		PRIVATE_KEY = readPrivateKey();
		X509Certificate certificate = readCertificate();
		PUBLIC_KEY_ID = computeKeyId(certificate.getPublicKey());
	}

	// This implements the specification: https://docs.docker.com/registry/spec/auth/jwt/
	public static String createToken(String user, 
			String registry, List<DockerScopePermission> accessPermissions, long now, String uuid) {

		JSONArray access = new JSONArray();
		
		for(DockerScopePermission permission : accessPermissions){
			JSONObject accessEntry = new JSONObject();
			access.add(accessEntry);
			
			accessEntry.put("type", permission.getScopeType());
			accessEntry.put("name", permission.getRepositoryPath());

			JSONArray actionArray = new JSONArray();
			//put set into a list and sort for deterministic ordering of actions
			List<String> actions = new ArrayList<String>(permission.getPermittedActions());
			Collections.sort(actions);
			for (String action : actions) actionArray.add(action);
			accessEntry.put("actions", actionArray);
		}
		
		Claims claims = Jwts.claims()
				.setIssuer(ISSUER)
				.setAudience(registry)
				.setExpiration(new Date(now+TIME_WINDOW_SEC*1000L))
				.setNotBefore(new Date(now-TIME_WINDOW_SEC*1000L))
				.setIssuedAt(new Date(now))
				.setId(uuid)
				.setSubject(user);
		claims.put(ACCESS, access);

		String s = Jwts.builder().setClaims(claims).
				setHeaderParam(Header.TYPE, Header.JWT_TYPE).
				setHeaderParam("kid", PUBLIC_KEY_ID).
				signWith(SignatureAlgorithm.ES256, PRIVATE_KEY).compact();

		return s;

	}

	private static PrivateKey readPrivateKey() {
		try {
			KeyFactory factory = KeyFactory.getInstance(KEY_GENERATION_ALGORITHM);
			byte[] content = Base64.decodeBase64(StackConfigurationSingleton.singleton().getDockerAuthorizationPrivateKey());
			PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(content);
			return factory.generatePrivate(privKeySpec);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static X509Certificate readCertificate() {
		try {
			byte[] content = Base64.decodeBase64(StackConfigurationSingleton.singleton().getDockerAuthorizationCertificate());
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
