package org.sagebionetworks.repo.manager;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.Date;
import java.util.Iterator;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.json.JSONObject;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.StackConfigurationSingleton;

import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyUse;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

public class OIDCTokenUtil {
	private static final String ISSUER = "https://repo-prod.prod.sagebase.org/auth/v1"; // TODO  Is this redundant with a string provided elsewhere? Should it be passed in?

	// the time window during which the client will consider the returned claims to be valid
	private static final long OIDC_CLAIMS_EXPIRATION_TIME_SECONDS = 24*3600L; // a day

	private static final String OIDC_SIGNATURE_PUBLIC_KEY_ID;
	private static final PrivateKey OIDC_SIGNATURE_PRIVATE_KEY;
	private static final PublicKey OIDC_SIGNATURE_PUBLIC_KEY;

	// OIDC allows for a variety of signature algorithms.  We'd like to help Python clients by using one of these:
	// https://pyjwt.readthedocs.io/en/latest/algorithms.html
	public static final String OIDC_SIGNATURE_KEY_GENERATION_ALGORITHM = "EC";
	
	static {
		Security.removeProvider("SunEC");
		Security.removeProvider("EC");
		Security.addProvider(new BouncyCastleProvider());	
		StackConfiguration stackConfig = StackConfigurationSingleton.singleton();
		OIDC_SIGNATURE_PRIVATE_KEY = JWTUtil.getPrivateKeyFromPEM(stackConfig.getOIDCSignaturePrivateKey(), OIDC_SIGNATURE_KEY_GENERATION_ALGORITHM);
		X509Certificate certificate = JWTUtil.getX509CertificateFromPEM(stackConfig.getOIDCSignatureCertificate());
		OIDC_SIGNATURE_PUBLIC_KEY = certificate.getPublicKey();
		OIDC_SIGNATURE_PUBLIC_KEY_ID = JWTUtil.computeKeyId(OIDC_SIGNATURE_PUBLIC_KEY);
	}

	public static String createOIDCidToken(String user, 
			String oauthClientId,
			long now, 
			String nonce, 
			Date auth_time,
			String uuid,
			JSONObject userClaims) {
		
		Claims claims = Jwts.claims();
		
		for (Iterator<String> it = userClaims.keys(); it.hasNext();) {
			String key = it.next();
			claims.put(key, userClaims.get(key));
		}
		
		claims.setIssuer(ISSUER)
			.setAudience(oauthClientId)
			.setExpiration(new Date(now+OIDC_CLAIMS_EXPIRATION_TIME_SECONDS*1000L))
			.setNotBefore(new Date(now))
			.setIssuedAt(new Date(now))
			.setId(uuid)
			.setSubject(user);
		
		if (nonce!=null) claims.put("nonce", nonce);
		if (auth_time!=null) claims.put("auth_time", auth_time.getTime()/1000L);

		String s = Jwts.builder().setClaims(claims).
				setHeaderParam(Header.TYPE, Header.JWT_TYPE).
				setHeaderParam("kid", OIDC_SIGNATURE_PUBLIC_KEY_ID).
				signWith(SignatureAlgorithm.ES256, OIDC_SIGNATURE_PRIVATE_KEY).compact();

		return s;
	}
	
	// Note:  Call .toJSONString() on the result to get a JSON formatted JSON Web Key
	public static JWK extractJSONWebKey() throws Exception {
		Curve curve = Curve.forECParameterSpec(((ECPublicKey)OIDC_SIGNATURE_PUBLIC_KEY).getParams());
		JWK jwk = new ECKey.Builder(curve, (ECPublicKey)OIDC_SIGNATURE_PUBLIC_KEY)
			    .privateKey((ECPrivateKey)OIDC_SIGNATURE_PRIVATE_KEY)
			    .keyUse(KeyUse.SIGNATURE)
			    .keyID(OIDC_SIGNATURE_PUBLIC_KEY_ID)
			    .build();
		return jwk;
	}


}
