package org.sagebionetworks.repo.manager.oauth;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_ACCESS_TOKEN_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_ACCESS_TOKEN_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_OAUTH_ACCESS_TOKEN;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.manager.KeyPairUtil;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.SessionIdThreadLocal;
import org.sagebionetworks.repo.model.auth.AccessTokenRecord;
import org.sagebionetworks.repo.model.auth.JSONWebTokenHelper;
import org.sagebionetworks.repo.model.auth.TokenType;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOOAuthAccessToken;
import org.sagebionetworks.repo.model.oauth.JsonWebKeySet;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequestDetails;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.Clock;
import org.sagebionetworks.util.EnumKeyedJsonMapUtil;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import io.jsonwebtoken.Claims;
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
	
	@Autowired
	private IdGenerator idGenerator;
	
	@Autowired
	private DBOBasicDao basicDao;
	
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
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
			DBOOAuthAccessToken token = new DBOOAuthAccessToken();
			
			token.setId(idGenerator.generateNewId(IdType.OAUTH_ACCESS_TOKEN_ID));
			token.setTokenId(accessTokenId);
			token.setPrincipalId(userId);
			token.setClientId(Long.parseLong(oauthClientId));
			token.setCreatedOn(claims.getIssuedAt());
			token.setExpiresOn(claims.getExpiration());			
			token.setSessionId(SessionIdThreadLocal.getThreadsSessionId().orElse(null));
			
			if (refreshTokenId != null) {
				token.setRefreshTokenId(Long.valueOf(refreshTokenId));
			}
			
			basicDao.createNew(token);
			
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
				null, tokenId, allScopes, Collections.EMPTY_MAP, persistToken);
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
				null, tokenId, allScopes, Collections.EMPTY_MAP);
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
	public boolean isValidOIDCAccessToken(String tokenId) {
		String sql = "SELECT COUNT(*) FROM " + TABLE_OAUTH_ACCESS_TOKEN + " WHERE " + COL_OAUTH_ACCESS_TOKEN_ID + "=?";
		
		return jdbcTemplate.queryForObject(sql, Long.class, tokenId) > 0;
	}
	
	@Override
	@WriteTransaction
	public void invalidateOIDCAccessTokens(Long userId) {
		String sql = "DELETE FROM " + TABLE_OAUTH_ACCESS_TOKEN + " WHERE " + COL_OAUTH_ACCESS_TOKEN_PRINCIPAL_ID + "=?";
		
		jdbcTemplate.update(sql);
	}
}
