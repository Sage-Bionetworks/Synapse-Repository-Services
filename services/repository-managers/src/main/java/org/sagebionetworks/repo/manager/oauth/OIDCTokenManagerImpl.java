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
import org.sagebionetworks.repo.model.SessionIdThreadLocal;
import org.sagebionetworks.repo.model.auth.AccessTokenRecord;
import org.sagebionetworks.repo.model.auth.JSONWebTokenHelper;
import org.sagebionetworks.repo.model.auth.TokenType;
import org.sagebionetworks.repo.model.dbo.auth.OAuthAccessTokenDao;
import org.sagebionetworks.repo.model.dbo.auth.OIDCAccessTokenData;
import org.sagebionetworks.repo.model.oauth.JsonWebKeySet;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequestDetails;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.Clock;
import org.sagebionetworks.util.EnumKeyedJsonMapUtil;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

@Service
public class OIDCTokenManagerImpl implements InitializingBean, OIDCTokenManager {
	
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

	@Autowired
	private OAuthAccessTokenDao accessTokenDao;
	
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
		return Jwts.builder()
			.setClaims(claims)
			.setHeaderParam(Header.TYPE, Header.JWT_TYPE)
			.setHeaderParam(JwsHeader.KEY_ID, oidcSignatureKeyId)
			.signWith(oidcSignaturePrivateKey, SignatureAlgorithm.RS256)
			.compact();
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
	
	String createOIDCaccessToken(
			Long userId,
			String issuer,
			String subject, 
			String oauthClientId,
			long now,
			long expirationTimeSeconds,
			Date authTime,
			String refreshTokenId,
			String accessTokenId,
			List<OAuthScope> scopes,
			Map<OIDCClaimName, OIDCClaimsRequestDetails> oidcClaims,
			boolean persistToken) {
		
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

		if (refreshTokenId!=null) {
			claims.put(OIDCClaimName.refresh_token_id.name(), refreshTokenId);
		}
		
		if (persistToken) {
			accessTokenDao.storeAccessTokenRecord(new OIDCAccessTokenData()
				.setTokenId(accessTokenId)
				.setPrincipalId(userId)
				.setClientId(Long.valueOf(oauthClientId))
				.setCreatedOn(claims.getIssuedAt())
				.setExpiresOn(claims.getExpiration())
				.setRefreshTokenId(refreshTokenId != null ? Long.valueOf(refreshTokenId) : null)
				.setSessionId(SessionIdThreadLocal.getThreadsSessionId().orElse(null))
			);
		}
		
		return createSignedJWT(claims);
	}
	
	@Override
	@WriteTransaction
	public String createOIDCaccessToken(
			Long userId,
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
		
		boolean persistToken = true;
		
		return createOIDCaccessToken(userId, issuer, subject, oauthClientId, now, expirationTimeSeconds, authTime, refreshTokenId, accessTokenId, scopes, oidcClaims, persistToken);
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
	public String createInternalTotalAccessToken(Long principalId) {
		String issuer = null; // doesn't matter -- it's only important to the client (which will never see this token, used internally)
		String subject = principalId.toString(); // we don't encrypt the subject
		String oauthClientId = ""+AuthorizationConstants.SYNAPSE_OAUTH_CLIENT_ID;
		String tokenId = UUID.randomUUID().toString();
		long expirationInSeconds = 60L; // it's for internal use only, just has to last the duration of the current request
		List<OAuthScope> allScopes = Arrays.asList(OAuthScope.values());  // everything!
		// This is a token used internally created ad-hoc and not returned to the user
		boolean persistToken = false;
		return createOIDCaccessToken(principalId, issuer, subject, oauthClientId, clock.currentTimeMillis(), expirationInSeconds, null,
				null, tokenId, allScopes, Collections.emptyMap(), persistToken);
	}

	@Override
	@WriteTransaction
	public String createClientTotalAccessToken(final Long principalId, final String issuer) {
		String subject = principalId.toString(); // we don't encrypt the subject
		String oauthClientId = ""+AuthorizationConstants.SYNAPSE_OAUTH_CLIENT_ID;
		String tokenId = UUID.randomUUID().toString();
		List<OAuthScope> allScopes = Arrays.asList(OAuthScope.values());  // everything!
		long expirationInSeconds = AuthorizationConstants.ACCESS_TOKEN_EXPIRATION_TIME_SECONDS;
		return createOIDCaccessToken(principalId, issuer, subject, oauthClientId, clock.currentTimeMillis(), expirationInSeconds, null,
				null, tokenId, allScopes, Collections.emptyMap());
	}
	
	@Override
	public Jwt<JwsHeader,Claims> parseJWT(String token) {
		return JSONWebTokenHelper.parseJWT(token, jsonWebKeySet);
	}
	
	@Override
	public void validateJWT(String token) {
		JSONWebTokenHelper.parseJWT(token, jsonWebKeySet);
	}
	
	@Override
	public boolean isOIDCAccessTokenExists(String tokenId) {
		return accessTokenDao.isAccessTokenRecordExists(tokenId);
	}
	
	@Override
	@WriteTransaction
	public void revokeOIDCAccessTokens(Long principalId) {
		accessTokenDao.deleteAccessTokenRecords(principalId);
	}

	@Override
	@WriteTransaction
	public void revokeOIDCAccessToken(String token) {
		Claims tokenClaims = parseJWT(token).getBody();
		
		String tokenId = tokenClaims.getId();
		
		accessTokenDao.deleteAccessTokenRecord(tokenId);
	}
	
	
}
