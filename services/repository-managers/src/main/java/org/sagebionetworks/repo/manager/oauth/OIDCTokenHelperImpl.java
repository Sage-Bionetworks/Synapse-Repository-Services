package org.sagebionetworks.repo.manager.oauth;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
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
import com.nimbusds.jose.jwk.KeyOperation;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.Base64URL;
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
			RSAPublicKey publicKey = (RSAPublicKey)keyPair.getPublic();

			RSAKey jwkRsa = new RSAKey.Builder(publicKey)
					.algorithm(JWSAlgorithm.RS256)
					.keyUse(KeyUse.SIGNATURE)
					.keyID(kid)
					.build();
			
			JsonWebKeyRSA rsaKey = new JsonWebKeyRSA();
			// these would be set for all algorithms
			rsaKey.setKty(jwkRsa.getKeyType().getValue());
			rsaKey.setUse(jwkRsa.getKeyUse().toString());
			rsaKey.setKid(jwkRsa.getKeyID());
			// these are specific to the RSA algorithm
			if (jwkRsa.getPublicExponent()!=null) rsaKey.setE(jwkRsa.getPublicExponent().toString());
			if (jwkRsa.getModulus()!=null) rsaKey.setN(jwkRsa.getModulus().toString());
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
	
	@Override
	public boolean validateSignedJWT(String token) {
		boolean verified = false;
		try {
			SignedJWT signedJWT = (SignedJWT)JWTParser.parse(token);
			JsonWebKeyRSA matchingJwk = null;
			for (JsonWebKey jwk : jsonWebKeySet.getKeys()) {
				if (jwk.getKid().equals(signedJWT.getHeader().getKeyID())) {
					matchingJwk = (JsonWebKeyRSA)jwk;
				}
			}
			if (matchingJwk!=null) {
			    RSAKey rsaKey = new RSAKey(new Base64URL(matchingJwk.getN()), new Base64URL(matchingJwk.getE()),
					      KeyUse.SIGNATURE, Collections.singleton(KeyOperation.VERIFY), null, 
					      matchingJwk.getKid(), null, null, null, null, null);
				JWSVerifier verifier = new RSASSAVerifier(rsaKey);
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
	
}
