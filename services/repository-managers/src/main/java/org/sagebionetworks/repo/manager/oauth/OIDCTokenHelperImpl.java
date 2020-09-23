package org.sagebionetworks.repo.manager.oauth;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.KeyPairUtil;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.auth.AccessTokenRecord;
import org.sagebionetworks.repo.model.auth.JSONWebTokenHelper;
import org.sagebionetworks.repo.model.auth.TokenType;
import org.sagebionetworks.repo.model.oauth.JsonWebKeySet;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequestDetails;
import org.sagebionetworks.repo.web.OAuthErrorCode;
import org.sagebionetworks.repo.web.OAuthUnauthenticatedException;
import org.sagebionetworks.util.Clock;
import org.sagebionetworks.util.EnumKeyedJsonMapUtil;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

public class OIDCTokenHelperImpl implements InitializingBean, OIDCTokenHelper {
	private static final String NONCE = "nonce";
	// the time window during which the client will consider the returned claims to be valid
	private static final long ID_TOKEN_EXPIRATION_TIME_SECONDS = 60L; // a minute
	
	private String oidcSignatureKeyId;
	private PrivateKey oidcSignaturePrivateKey;
	private JsonWebKeySet jsonWebKeySet;

	@Autowired
	private StackConfiguration stackConfiguration;

	@Autowired
	private Clock clock;
	
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
			Date authTime,
			String tokenId,
			Map<OIDCClaimName,Object> userInfo) {
		
		ClaimsWithAuthTime claims = ClaimsWithAuthTime.newClaims();
		
		for (OIDCClaimName claimName: userInfo.keySet()) {
			claims.put(claimName.name(), userInfo.get(claimName));
		}
		
		claims.setAuthTime(authTime)
			.setIssuer(issuer)
			.setAudience(oauthClientId)
			.setExpiration(new Date(now+ID_TOKEN_EXPIRATION_TIME_SECONDS*1000L))
			.setNotBefore(new Date(now))
			.setIssuedAt(new Date(now))
			.setId(tokenId)
			.setSubject(subject);

		claims.put(OIDCClaimName.token_type.name(), TokenType.OIDC_ID_TOKEN);

		if (nonce!=null) claims.put(NONCE, nonce);

		return createSignedJWT(claims);
	}
	
	@Override
	public String createOIDCaccessToken(
			String issuer,
			String subject, 
			String oauthClientId,
			long now,
			long expirationTimeSeconds,
			Date authTime,
			String refreshTokenId,
			String accessTokenId,
			List<OAuthScope> scopes,
			Map<OIDCClaimName, OIDCClaimsRequestDetails> oidcClaims) {
		
		ClaimsWithAuthTime claims = ClaimsWithAuthTime.newClaims();
		
		ClaimsJsonUtil.addAccessClaims(scopes, oidcClaims, claims);
		
		claims.setAuthTime(authTime)
			.setIssuer(issuer)
			.setAudience(oauthClientId)
			.setExpiration(new Date(now+expirationTimeSeconds*1000L))
			.setNotBefore(new Date(now))
			.setIssuedAt(new Date(now))
			.setId(accessTokenId)
			.setSubject(subject);

		claims.put(OIDCClaimName.token_type.name(), TokenType.OIDC_ACCESS_TOKEN);

		if (refreshTokenId!=null) claims.put(OIDCClaimName.refresh_token_id.name(), refreshTokenId);
		return createSignedJWT(claims);
	}

	@Override
	public String createPersonalAccessToken(String issuer, AccessTokenRecord record) {
		ClaimsWithAuthTime claims = ClaimsWithAuthTime.newClaims();

		ClaimsJsonUtil.addAccessClaims(record.getScopes(), EnumKeyedJsonMapUtil.convertKeysToEnums(record.getUserInfoClaims(), OIDCClaimName.class), claims);

		claims.put(OIDCClaimName.token_type.name(), TokenType.PERSONAL_ACCESS_TOKEN);

		claims.setIssuer(issuer)
				.setAudience(AuthorizationConstants.SYNAPSE_OAUTH_CLIENT_ID)
				.setNotBefore(record.getCreatedOn())
				.setIssuedAt(record.getCreatedOn())
				.setId(record.getId())
				.setSubject(record.getUserId());

		return createSignedJWT(claims);
	}

	@Override
	public String createTotalAccessToken(Long principalId) {
		String issuer = null; // doesn't matter -- it's only important to the client (which will never see this token, used internally)
		String subject = principalId.toString(); // we don't encrypt the subject
		String oauthClientId = ""+AuthorizationConstants.SYNAPSE_OAUTH_CLIENT_ID;
		String tokenId = UUID.randomUUID().toString();
		long expirationInSeconds = 60L; // it's for internal use only, just has to last the duration of the current request
		List<OAuthScope> allScopes = Arrays.asList(OAuthScope.values());  // everything!
		return createOIDCaccessToken(issuer, subject, oauthClientId, clock.currentTimeMillis(), expirationInSeconds, null,
				null, tokenId, allScopes, Collections.EMPTY_MAP);
	}

	
	@Override
	public Jwt<JwsHeader,Claims> parseJWT(String token) {
		Jwt<JwsHeader, Claims> parsed;
		// If the server receives an expired token, it should throw an OAuthUnauthenticatedException
		try {
			parsed = JSONWebTokenHelper.parseJWT(token, jsonWebKeySet);
		} catch (ExpiredJwtException e) {
			throw new OAuthUnauthenticatedException(OAuthErrorCode.invalid_token, "The token has expired.");
		}
		return parsed;
	}
	
	@Override
	public void validateJWT(String token) {
		try {
			JSONWebTokenHelper.parseJWT(token, jsonWebKeySet);
		} catch (ExpiredJwtException e) {
			throw new OAuthUnauthenticatedException(OAuthErrorCode.invalid_token, "The token has expired.");
		}
	}
}
