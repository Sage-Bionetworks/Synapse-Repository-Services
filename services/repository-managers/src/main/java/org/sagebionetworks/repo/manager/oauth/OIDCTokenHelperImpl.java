package org.sagebionetworks.repo.manager.oauth;

import java.math.BigInteger;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
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

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;

public class OIDCTokenHelperImpl implements InitializingBean, OIDCTokenHelper {
	private static final String NONCE = "nonce";
	// the time window during which the client will consider the returned claims to be valid
	private static final long ID_TOKEN_EXPIRATION_TIME_SECONDS = 3600*24L; // a day
	private static final long ACCESS_TOKEN_EXPIRATION_TIME_SECONDS = 3600*24L; // a day
	private static final String KEY_USE_SIGNATURE = "SIGNATURE";
	
	private String oidcSignatureKeyId;
	private PrivateKey oidcSignaturePrivateKey;
	private JsonWebKeySet jsonWebKeySet;

	@Autowired
	private StackConfiguration stackConfiguration;
	
	@Override
	public void afterPropertiesSet() {
		jsonWebKeySet = new JsonWebKeySet();
		List<JsonWebKey> publicKeys = new ArrayList<JsonWebKey>();
		jsonWebKeySet.setKeys(publicKeys);
		for (String s: stackConfiguration.getOIDCSignatureRSAPrivateKeys()) {
			KeyPair keyPair = KeyPairUtil.getRSAKeyPairFromPrivateKey(s);
			String kid = KeyPairUtil.computeKeyId(keyPair.getPublic());
			// grab the first one to use when signing
			if (oidcSignaturePrivateKey==null) {
				oidcSignaturePrivateKey=keyPair.getPrivate();
				oidcSignatureKeyId = kid;
			}
			RSAPublicKey rsaPublicKey = (RSAPublicKey)keyPair.getPublic();
			JsonWebKeyRSA rsaKey = new JsonWebKeyRSA();
			// these would be set for all algorithms
			rsaKey.setKty(SignatureAlgorithm.RS256.name());
			rsaKey.setUse(KEY_USE_SIGNATURE);
			rsaKey.setKid(kid);
			// these are specific to the RSA algorithm
			rsaKey.setE(rsaPublicKey.getPublicExponent().toString());
			rsaKey.setN(rsaPublicKey.getModulus().toString());
			publicKeys.add(rsaKey);
		}
	}

	@Override
	public JsonWebKeySet getJSONWebKeySet() {
		return jsonWebKeySet;
	}

	private String createSignedJWT(Claims claims) {
		return Jwts.builder().setClaims(claims).
		setHeaderParam(Header.TYPE, Header.JWT_TYPE).
		setHeaderParam("kid", oidcSignatureKeyId).
		signWith(SignatureAlgorithm.RS256, oidcSignaturePrivateKey).compact();
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
	
	public static RSAPublicKey getRSAPublicKeyForJsonWebKeyRSA(JsonWebKeyRSA jwkRsa) {
		BigInteger modulus = new BigInteger(jwkRsa.getN());
		BigInteger publicExponent = new BigInteger(jwkRsa.getE());
		RSAPublicKeySpec keySpec = new RSAPublicKeySpec(modulus, publicExponent);
		try {
			KeyFactory kf = KeyFactory.getInstance("RSA");
			return (RSAPublicKey)kf.generatePublic(keySpec);
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			throw new RuntimeException(e);
		} 
	}
	
	@Override
	public Jwt<JwsHeader,Claims> validateJWTSignature(String token) {
		// This is a little awkward:  We first have to parse the token to
		// find the key Id, then, once we map the key Id to the signing key,
		// we parse again, setting the matching public key for verification
		String[] pieces = token.split("\\.");
		if (pieces.length!=3) throw new IllegalArgumentException("Expected three pieces but found "+pieces.length);
		String unsignedToken = pieces[0]+"."+pieces[1]+".";
		JsonWebKey matchingKey=null;
		{
			Jwt<Header,Claims> unsignedJwt = Jwts.parser().parseClaimsJwt(unsignedToken);
			for (JsonWebKey jwk : jsonWebKeySet.getKeys()) {
				if (jwk.getKid().equals(unsignedJwt.getHeader().get("kid"))) {
					matchingKey = jwk;
				}
			}
			if (matchingKey==null) {
				return null;
			}
		}
		Jwt<JwsHeader,Claims> result = null;
		try {
			Key rsaPublicKey = getRSAPublicKeyForJsonWebKeyRSA((JsonWebKeyRSA)matchingKey);
			result = Jwts.parser().setSigningKey(rsaPublicKey).parse(token);
		} catch (SignatureException e) {
			return null;
		}
		
		Claims claims = result.getBody();
		if (System.currentTimeMillis()>claims.getExpiration().getTime()) {
			return null;
		}

		return result;
	}
	
}
