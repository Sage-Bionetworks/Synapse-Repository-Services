package org.sagebionetworks.repo.manager;

import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.StackConfigurationSingleton;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

public class DockerTokenUtil {
	private static final String ISSUER = "www.synapse.org";

	private static final long DOCKER_AUTHORIZATION_EXPIRATION_SECS = 120L; // two minutes
	private static final String ACCESS = "access";

	public static final String DOCKER_AUTHORIZATION_PUBLIC_KEY_ID;
	private static final PrivateKey DOCKER_AUTHORIZATION_PRIVATE_KEY;

	// Eliptic Curve key is required by the JSON Web Token signing library
	private static final String KEY_GENERATION_ALGORITHM = "EC";
	
	static {
		Security.removeProvider("SunEC");
		Security.removeProvider("EC");
		Security.addProvider(new BouncyCastleProvider());
		StackConfiguration stackConfig = StackConfigurationSingleton.singleton();
		DOCKER_AUTHORIZATION_PRIVATE_KEY = KeyPairUtil.getPrivateKeyFromPEM(stackConfig.getDockerAuthorizationPrivateKey(), KEY_GENERATION_ALGORITHM);
		X509Certificate certificate = KeyPairUtil.getX509CertificateFromPEM(stackConfig.getDockerAuthorizationCertificate());
		DOCKER_AUTHORIZATION_PUBLIC_KEY_ID = KeyPairUtil.computeKeyId(certificate.getPublicKey());
	}

	// This implements the specification: https://docs.docker.com/registry/spec/auth/jwt/
	public static String createDockerAuthorizationToken(String user, 
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
				.setExpiration(new Date(now+DOCKER_AUTHORIZATION_EXPIRATION_SECS*1000L))
				.setNotBefore(new Date(now-DOCKER_AUTHORIZATION_EXPIRATION_SECS*1000L))
				.setIssuedAt(new Date(now))
				.setId(uuid)
				.setSubject(user);
		claims.put(ACCESS, access);

		String s = Jwts.builder().setClaims(claims).
				setHeaderParam(Header.TYPE, Header.JWT_TYPE).
				setHeaderParam("kid", DOCKER_AUTHORIZATION_PUBLIC_KEY_ID).
				signWith(SignatureAlgorithm.ES256, DOCKER_AUTHORIZATION_PRIVATE_KEY).compact();

		return s;

	}
}
