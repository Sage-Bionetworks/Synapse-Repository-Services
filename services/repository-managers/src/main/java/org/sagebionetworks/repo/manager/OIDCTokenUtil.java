package org.sagebionetworks.repo.manager;

import java.security.KeyPair;
import java.security.PublicKey;
import java.security.Security;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.json.JSONObject;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.StackConfigurationSingleton;

import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

public class OIDCTokenUtil {
	private static final String ISSUER = "https://repo-prod.prod.sagebase.org/auth/v1"; // TODO  Is this redundant with a string provided elsewhere? Should it be passed in?

	// the time window during which the client will consider the returned claims to be valid
	private static final long OIDC_CLAIMS_EXPIRATION_TIME_SECONDS = 60L; // a minute

	private static final List<KeyPair> OIDC_SIGNATURE_KEY_PAIRS;

	static {
		Security.removeProvider("SunEC");
		Security.removeProvider("EC");
		Security.addProvider(new BouncyCastleProvider());

		StackConfiguration stackConfig = StackConfigurationSingleton.singleton();

		OIDC_SIGNATURE_KEY_PAIRS = new ArrayList<KeyPair>();
		for (String s: stackConfig.getOIDCSignaturePrivateKeys()) {
			KeyPair keyPair = JWTUtil.getRSAKeyPairFromPEM(s, "RSA");
			OIDC_SIGNATURE_KEY_PAIRS.add(keyPair);
		}
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

		KeyPair keyPair = OIDC_SIGNATURE_KEY_PAIRS.get(0);
		String kid = JWTUtil.computeKeyId(keyPair.getPublic());
		String s = Jwts.builder().setClaims(claims).
				setHeaderParam(Header.TYPE, Header.JWT_TYPE).
				setHeaderParam("kid", kid).
				signWith(SignatureAlgorithm.RS256, keyPair.getPrivate()).compact();

		return s;
	}
	
	// Note:  Call .toJSONString() on each JWK to get a JSON formatted JSON Web Key
	public static List<JWK> extractJSONWebKeySet() throws Exception {
		List<JWK> result = new ArrayList<JWK>();
		for (KeyPair keyPair : OIDC_SIGNATURE_KEY_PAIRS) {
			String kid = JWTUtil.computeKeyId(keyPair.getPublic());
			if (keyPair.getPublic() instanceof ECPublicKey) {
				Curve curve = Curve.forECParameterSpec(((ECPublicKey)keyPair.getPublic()).getParams());
				JWK jwk = new ECKey.Builder(curve, (ECPublicKey)keyPair.getPublic())
						.privateKey((ECPrivateKey)keyPair.getPrivate())
						.keyUse(KeyUse.SIGNATURE)
						.keyID(kid)
						.build();
				result.add(jwk);
			} else if (keyPair.getPublic() instanceof RSAPublicKey) {
				JWK jwk = new RSAKey.Builder((RSAPublicKey)keyPair.getPublic())
						.privateKey((RSAPrivateKey)keyPair.getPrivate())
						.keyUse(KeyUse.SIGNATURE)
						.keyID(kid)
						.build();
				result.add(jwk);
			} else {
				throw new RuntimeException(keyPair.getPublic()+" not supported.");
			}
		}
		return result;
	}


}
