package org.sagebionetworks.repo.manager;

import java.security.KeyPair;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.json.JSONObject;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.StackConfigurationSingleton;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.SignedJWT;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

public class OIDCTokenUtil {
	private static final String ISSUER = "https://repo-prod.prod.sagebase.org/auth/v1"; // TODO  Is this redundant with a string provided elsewhere? Should it be passed in?

	// the time window during which the client will consider the returned claims to be valid
	private static final long OIDC_CLAIMS_EXPIRATION_TIME_SECONDS = 60L; // a minute

	private static final List<KeyPair> OIDC_SIGNATURE_KEY_PAIRS;
	
	private static List<JWK> JSON_WEB_KEY_SET;

	static {
		StackConfiguration stackConfig = StackConfigurationSingleton.singleton();

		OIDC_SIGNATURE_KEY_PAIRS = new ArrayList<KeyPair>();
		JSON_WEB_KEY_SET = new ArrayList<JWK>();
		for (String s: stackConfig.getOIDCSignatureRSAPrivateKeys()) {
			KeyPair keyPair = JWTUtil.getRSAKeyPairFromPEM(s);
			OIDC_SIGNATURE_KEY_PAIRS.add(keyPair);
			String kid = JWTUtil.computeKeyId(keyPair.getPublic());
			if (keyPair.getPublic() instanceof ECPublicKey) {
				Curve curve = Curve.forECParameterSpec(((ECPublicKey)keyPair.getPublic()).getParams());
				JWK jwk = new ECKey.Builder(curve, (ECPublicKey)keyPair.getPublic())
						.privateKey((ECPrivateKey)keyPair.getPrivate())
						.algorithm(JWSAlgorithm.ES256)
						.keyUse(KeyUse.SIGNATURE)
						.keyID(kid)
						.build();
				JSON_WEB_KEY_SET.add(jwk);
			
			} else if (keyPair.getPublic() instanceof RSAPublicKey) {
				JWK jwk = new RSAKey.Builder((RSAPublicKey)keyPair.getPublic())
						.privateKey((RSAPrivateKey)keyPair.getPrivate())
						.algorithm(JWSAlgorithm.RS256)
						.keyUse(KeyUse.SIGNATURE)
						.keyID(kid)
						.build();
				JSON_WEB_KEY_SET.add(jwk);
			} else {
				throw new RuntimeException(keyPair.getPublic()+" not supported.");
			}
		}
	}

	public static String createOIDCidToken(
			String subject, 
			String oauthClientId,
			long now, 
			String nonce, 
			Date auth_time,
			String uuidtokenId,
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
			.setId(uuidtokenId)
			.setSubject(subject);
		
		if (nonce!=null) claims.put("nonce", nonce);
		if (auth_time!=null) claims.put("auth_time", auth_time.getTime()/1000L);

		KeyPair keyPair = OIDC_SIGNATURE_KEY_PAIRS.get(0);
		String kid = JWTUtil.computeKeyId(keyPair.getPublic());
		String result = Jwts.builder().setClaims(claims).
			setHeaderParam(Header.TYPE, Header.JWT_TYPE).
			setHeaderParam("kid", kid).
			signWith(SignatureAlgorithm.RS256, keyPair.getPrivate()).compact();

		return result;
	}
	
	public static boolean validateToken(String token) {
		boolean verified = false;
		try {
			SignedJWT signedJWT = (SignedJWT)JWTParser.parse(token);
			JWK matchingJwk = null;
			for (JWK jwk : JSON_WEB_KEY_SET) {
				if (jwk.getKeyID().equals(signedJWT.getHeader().getKeyID())) {
					matchingJwk = jwk;
				}
			}
			if (matchingJwk!=null && matchingJwk instanceof RSAKey) {
				JWSVerifier verifier = new RSASSAVerifier((RSAKey)matchingJwk);
				verified = signedJWT.verify(verifier);
			}
			
			if (System.currentTimeMillis()>signedJWT.getJWTClaimsSet().getExpirationTime().getTime()) {
				verified=false;
			}
			// We check issuer (though the validation of the signature should be enough to know it came from us
			if (!signedJWT.getJWTClaimsSet().getIssuer().equals(ISSUER)) {
				verified=false;
			}

		} catch (ParseException | JOSEException e) {
			throw new RuntimeException(e);
		}
		return verified;
	}
	
		public static String createOIDCaccessToken(
				String subject, 
				String oauthClientId,
				long now, 
				Date auth_time,
				String uuidtokenId,
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
				.setId(uuidtokenId)
				.setSubject(subject);
			
			if (auth_time!=null) claims.put("auth_time", auth_time.getTime()/1000L);

			KeyPair keyPair = OIDC_SIGNATURE_KEY_PAIRS.get(0);
			String kid = JWTUtil.computeKeyId(keyPair.getPublic());
			String result = Jwts.builder().setClaims(claims).
				setHeaderParam(Header.TYPE, Header.JWT_TYPE).
				setHeaderParam("kid", kid).
				signWith(SignatureAlgorithm.RS256, keyPair.getPrivate()).compact();

			return result;
		}
	
	public static List<JWK> getJSONWebKeySet() {
		return JSON_WEB_KEY_SET;
	}


}
