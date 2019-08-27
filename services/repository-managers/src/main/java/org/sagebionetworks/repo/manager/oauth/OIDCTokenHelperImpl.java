package org.sagebionetworks.repo.manager.oauth;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.KeyPairUtil;
import org.sagebionetworks.repo.model.oauth.JsonWebKey;
import org.sagebionetworks.repo.model.oauth.JsonWebKeyRSA;
import org.sagebionetworks.repo.model.oauth.JsonWebKeySet;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequestDetails;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

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

public class OIDCTokenHelperImpl implements InitializingBean, OIDCTokenHelper {
	private static final String NONCE = "nonce";
	// the time window during which the client will consider the returned claims to be valid
	private static final long ID_TOKEN_EXPIRATION_TIME_SECONDS = 3600*24L; // a day
	private static final long ACCESS_TOKEN_EXPIRATION_TIME_SECONDS = 3600*24L; // a day
	
	private String oidcSignatureKeyId;
	private PrivateKey oidcSignaturePrivateKey;
	private List<JWK> jsonWebKeySet;

	@Autowired
	private StackConfiguration stackConfiguration;
	
	@Override
	public void afterPropertiesSet() {
		jsonWebKeySet = new ArrayList<JWK>();
		PrivateKey signingPrivateKey = null;
		String signingKeyId = null;
		for (String s: stackConfiguration.getOIDCSignatureRSAPrivateKeys()) {
			KeyPair keyPair = KeyPairUtil.getRSAKeyPairFromPrivateKey(s);
			String kid = KeyPairUtil.computeKeyId(keyPair.getPublic());
			// grab the first one to use when signing
			if (signingPrivateKey==null) {
				signingPrivateKey=keyPair.getPrivate();
				signingKeyId = kid;
			}
			if (keyPair.getPublic() instanceof ECPublicKey) {
				Curve curve = Curve.forECParameterSpec(((ECPublicKey)keyPair.getPublic()).getParams());
				JWK jwk = new ECKey.Builder(curve, (ECPublicKey)keyPair.getPublic())
						.algorithm(JWSAlgorithm.ES256)
						.keyUse(KeyUse.SIGNATURE)
						.keyID(kid)
						.build();
				jsonWebKeySet.add(jwk);
			} else if (keyPair.getPublic() instanceof RSAPublicKey) {
				JWK jwk = new RSAKey.Builder((RSAPublicKey)keyPair.getPublic())
						.algorithm(JWSAlgorithm.RS256)
						.keyUse(KeyUse.SIGNATURE)
						.keyID(kid)
						.build();
				jsonWebKeySet.add(jwk);
			} else {
				throw new RuntimeException(keyPair.getPublic().getClass()+" not supported.");
			}
		}
		oidcSignaturePrivateKey = signingPrivateKey;
		oidcSignatureKeyId = signingKeyId;
	}

	@Override
	public JsonWebKeySet getJSONWebKeySet() {
		JsonWebKeySet result = new JsonWebKeySet();
		List<JsonWebKey> keys = new ArrayList<JsonWebKey>();
		result.setKeys(keys);
		for (JWK jwk : jsonWebKeySet) {
			if (JWSAlgorithm.RS256.equals(jwk.getAlgorithm())) {
				JsonWebKeyRSA rsaKey = new JsonWebKeyRSA();
				keys.add(rsaKey);
				// these would be set for all algorithms
				rsaKey.setKty(jwk.getKeyType().getValue());
				rsaKey.setUse(jwk.getKeyUse().toString());
				rsaKey.setKid(jwk.getKeyID());
				// these are specific to the RSA algorithm
				RSAKey jwkRsa = (RSAKey)jwk;
//				if (jwkRsa.getPrivateExponent()!=null) rsaKey.setD(jwkRsa.getPrivateExponent().toString());
//				if (jwkRsa.getFirstFactorCRTExponent()!=null) rsaKey.setDp(jwkRsa.getFirstFactorCRTExponent().toString());
//				if (jwkRsa.getSecondFactorCRTExponent()!=null) rsaKey.setDq(jwkRsa.getSecondFactorCRTExponent().toString());
				if (jwkRsa.getPublicExponent()!=null) rsaKey.setE(jwkRsa.getPublicExponent().toString());
				if (jwkRsa.getModulus()!=null) rsaKey.setN(jwkRsa.getModulus().toString());
//				if (jwkRsa.getFirstPrimeFactor()!=null) rsaKey.setP(jwkRsa.getFirstPrimeFactor().toString());
//				if (jwkRsa.getSecondPrimeFactor()!=null) rsaKey.setQ(jwkRsa.getSecondPrimeFactor().toString());
//				if (jwkRsa.getFirstCRTCoefficient()!=null) rsaKey.setQi(jwkRsa.getFirstCRTCoefficient().toString());
			} else {
				// in the future we can add mappings for algorithms other than RSA
				// MUST TAKE CARE to publish just the public side of the key
				throw new RuntimeException("Unsupported: "+jwk.getAlgorithm());
			}
		}
		return result;
	}

	@Override
	public String createSignedJWT(Claims claims) {
		return Jwts.builder().setClaims(claims).
		setHeaderParam(Header.TYPE, Header.JWT_TYPE).
		setHeaderParam("kid", oidcSignatureKeyId).
		signWith(SignatureAlgorithm.RS256, oidcSignaturePrivateKey).compact();
	}
	
	@Override
	public boolean validateSignedJWT(String token) {
		boolean verified = false;
		try {
			SignedJWT signedJWT = (SignedJWT)JWTParser.parse(token);
			JWK matchingJwk = null;
			for (JWK jwk : jsonWebKeySet) {
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
		} catch (ParseException | JOSEException e) {
			throw new RuntimeException(e);
		}
		return verified;
	}
	
	@Override
	public String createOIDCIdToken(
			String issuer,
			String subject, 
			String oauthClientId,
			long now, 
			String nonce, 
			Long authTimeSeconds,
			String tokenId,
			Map<OIDCClaimName,String> userInfo) {
		
		Claims claims = Jwts.claims();
		
		for (OIDCClaimName claimName: userInfo.keySet()) {
			claims.put(claimName.name(), userInfo.get(claimName));
		}
		
		claims.setIssuer(issuer)
			.setAudience(oauthClientId)
			.setExpiration(new Date(now+ID_TOKEN_EXPIRATION_TIME_SECONDS*1000L))
			.setNotBefore(new Date(now))
			.setIssuedAt(new Date(now))
			.setId(tokenId)
			.setSubject(subject);
		
		if (nonce!=null) claims.put(NONCE, nonce);
		
		if (authTimeSeconds!=null) claims.put(OIDCClaimName.auth_time.name(), authTimeSeconds);

		return createSignedJWT(claims);
	}
	
	@Override
	public String createOIDCaccessToken(
			String issuer,
			String subject, 
			String oauthClientId,
			long now, 
			Long authTimeSeconds,
			String tokenId,
			List<OAuthScope> scopes,
			Map<OIDCClaimName, OIDCClaimsRequestDetails> oidcClaims) {
		
		Claims claims = Jwts.claims();
		
		ClaimsJsonUtil.addAccessClaims(scopes, oidcClaims, claims);
		
		claims.setIssuer(issuer)
			.setAudience(oauthClientId)
			.setExpiration(new Date(now+ACCESS_TOKEN_EXPIRATION_TIME_SECONDS*1000L))
			.setNotBefore(new Date(now))
			.setIssuedAt(new Date(now))
			.setId(tokenId)
			.setSubject(subject);
		
		if (authTimeSeconds!=null) claims.put(OIDCClaimName.auth_time.name(), authTimeSeconds);

		return createSignedJWT(claims);
	}
}
