package org.sagebionetworks.repo.manager.oauth;

import java.security.Key;
import java.security.KeyPair;
import java.security.PrivateKey;
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
	private static final long ID_TOKEN_EXPIRATION_TIME_SECONDS = 60L; // a minute
	private static final long ACCESS_TOKEN_EXPIRATION_TIME_SECONDS = 3600*24L; // a day
	
	private String oidcSignatureKeyId;
	private PrivateKey oidcSignaturePrivateKey;
	private JsonWebKeySet jsonWebKeySet;

	@Autowired
	private StackConfiguration stackConfiguration;
	
	@Override
	public void afterPropertiesSet() {
		List<String> pemEncodedRsaPrivateKeys = stackConfiguration.getOIDCSignatureRSAPrivateKeys();
		this.jsonWebKeySet = KeyPairUtil.getJSONWebKeySetForPEMEncodedRsaKeys(pemEncodedRsaPrivateKeys);
		
		// grab the latest private key to be used for signing
		KeyPair keyPair = KeyPairUtil.getRSAKeyPairFromPrivateKey(pemEncodedRsaPrivateKeys.get(pemEncodedRsaPrivateKeys.size()-1));
		this.oidcSignaturePrivateKey=keyPair.getPrivate();
		this.oidcSignatureKeyId = KeyPairUtil.computeKeyId(keyPair.getPublic());
	}

	@Override
	public JsonWebKeySet getJSONWebKeySet() {
		return jsonWebKeySet;
	}

	private String createSignedJWT(Claims claims) {
		return Jwts.builder().setClaims(claims).
			setHeaderParam(Header.TYPE, Header.JWT_TYPE).
			setHeaderParam(JwsHeader.KEY_ID, oidcSignatureKeyId).
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
	
	@Override
	public Jwt<JwsHeader,Claims> parseJWT(String token) {
		// This is a little awkward:  We first have to parse the token to
		// find the key Id, then, once we map the key Id to the signing key,
		// we parse again, setting the matching public key for verification
		String[] pieces = token.split("\\.");
		if (pieces.length!=3) throw new IllegalArgumentException("Expected three sections of the token but found "+pieces.length);
		String unsignedToken = pieces[0]+"."+pieces[1]+".";
		JsonWebKey matchingKey=null;
		{
			Jwt<Header,Claims> unsignedJwt = Jwts.parser().parseClaimsJwt(unsignedToken);
			String keyId = (String)unsignedJwt.getHeader().get(JwsHeader.KEY_ID);
			for (JsonWebKey jwk : jsonWebKeySet.getKeys()) {
				if (jwk.getKid().equals(keyId)) {
					matchingKey = jwk;
				}
			}
			if (matchingKey==null) {
				throw new IllegalArgumentException("Could not find token key, "+keyId+" in the list of available public keys.");
			}
		}
		Jwt<JwsHeader,Claims> result = null;
		try {
			Key rsaPublicKey = KeyPairUtil.getRSAPublicKeyForJsonWebKeyRSA((JsonWebKeyRSA)matchingKey);
			result = Jwts.parser().setSigningKey(rsaPublicKey).parse(token);
		} catch (SignatureException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
		
		Claims claims = result.getBody();
		if (System.currentTimeMillis()>claims.getExpiration().getTime()) {
			throw new IllegalArgumentException("Token has expired.");
		}

		return result;
	}
	
	@Override
	public void validateJWT(String token) {
		parseJWT(token);
	}
}
