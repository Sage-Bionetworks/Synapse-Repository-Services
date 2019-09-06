package org.sagebionetworks.repo.manager.oauth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.StackEncrypter;
import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthenticationDAO;
import org.sagebionetworks.repo.model.auth.OAuthClientDao;
import org.sagebionetworks.repo.model.auth.SectorIdentifier;
import org.sagebionetworks.repo.model.oauth.OAuthClient;
import org.sagebionetworks.repo.model.oauth.OAuthClientIdAndSecret;
import org.sagebionetworks.repo.model.oauth.OAuthResponseType;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.sagebionetworks.repo.model.oauth.OIDCAuthorizationRequest;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequestDetails;
import org.sagebionetworks.repo.model.oauth.OIDCSigningAlgorithm;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClient;
import org.sagebionetworks.simpleHttpClient.SimpleHttpRequest;
import org.sagebionetworks.simpleHttpClient.SimpleHttpResponse;

import com.google.common.collect.ImmutableList;

@RunWith(MockitoJUnitRunner.class)
public class OpenIDConnectManagerImplUnitTest {
	private static final String USER_ID = "101";
	private static final Long USER_ID_LONG = Long.parseLong(USER_ID);
	private static final String CLIENT_NAME = "some client";
	private static final String CLIENT_URI = "https://client.uri.com/index.html";
	private static final String POLICY_URI = "https://client.uri.com/policy.html";
	private static final String TOS_URI = "https://client.uri.com/termsOfService.html";
	private static final List<String> REDIRCT_URIS = Collections.singletonList("https://client.com/redir");
	private static final String SECTOR_IDENTIFIER_URI_STRING = "https://client.uri.com/path/to/json/file";
	private static final List<String> REDIR_URI_LIST = ImmutableList.of("https://host1.com/redir1", "https://host2.com/redir2");
	private static final String OAUTH_CLIENT_ID = "123";

	
	@Mock
	private StackEncrypter mockStackEncrypter;

	@Mock
	private OAuthClientDao mockOauthClientDao;

	@Mock
	private AuthenticationDAO mockAuthDao;

	@Mock
	private UserProfileManager mockUserProfileManager;

	@Mock
	private TeamDAO mockTeamDAO;
	
	@Mock
	private SimpleHttpClient mockHttpClient;

	@Mock
	private SimpleHttpResponse mockHttpResponse;

	@Captor
	private ArgumentCaptor<SimpleHttpRequest> simpleHttpRequestCaptor;
	
	@Captor
	private ArgumentCaptor<SectorIdentifier> sectorIdentifierCaptor;
	
	@InjectMocks
	private OpenIDConnectManagerImpl openIDConnectManagerImpl;
	
	private UserInfo userInfo;
	UserInfo anonymousUserInfo;
	private  URI sector_identifier_uri;
	
	@Before
	public void setUp() throws Exception {
		userInfo = new UserInfo(false);
		userInfo.setId(USER_ID_LONG);

		anonymousUserInfo = new UserInfo(false);
		anonymousUserInfo.setId(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());

		sector_identifier_uri = new URI(SECTOR_IDENTIFIER_URI_STRING);

		when(mockHttpResponse.getStatusCode()).thenReturn(200);
		when(mockHttpResponse.getContent()).thenReturn("[\"https://host1.com/redir1\",\"https://host2.com/redir2\"]");

		when(mockHttpClient.get((SimpleHttpRequest)any())).thenReturn(mockHttpResponse);

		when(mockOauthClientDao.createOAuthClient((OAuthClient)any())).then(returnsFirstArg());	
		when(mockOauthClientDao.doesSectorIdentifierExistForURI(anyString())).thenReturn(false);
		when(mockOauthClientDao.getOAuthClientCreator(OAUTH_CLIENT_ID)).thenReturn(USER_ID);
		
	}
	
	private static OAuthClient createOAuthClient(String userId) {
		OAuthClient result = new OAuthClient();
		result.setClient_name(CLIENT_NAME);
		result.setClient_uri(CLIENT_URI);
		result.setCreatedOn(new Date(System.currentTimeMillis()));
		result.setModifiedOn(new Date(System.currentTimeMillis()));
		result.setPolicy_uri(POLICY_URI);
		result.setRedirect_uris(REDIRCT_URIS);
		result.setTos_uri(TOS_URI);
		result.setUserinfo_signed_response_alg(OIDCSigningAlgorithm.RS256);
		return result;
	}

	
	@Test
	public void testValidateAuthenticationRequest() {
		OAuthClient client = new OAuthClient();
		client.setRedirect_uris(Collections.singletonList(REDIRCT_URIS.get(0)));

		OIDCAuthorizationRequest authorizationRequest = new OIDCAuthorizationRequest();
		authorizationRequest.setRedirectUri(REDIRCT_URIS.get(0));
		authorizationRequest.setResponseType(OAuthResponseType.code);
		
		// method under test
		OpenIDConnectManagerImpl.validateAuthenticationRequest(authorizationRequest, client);
		
		authorizationRequest.setRedirectUri("some invalid uri");
		
		try {
			OpenIDConnectManagerImpl.validateAuthenticationRequest(authorizationRequest, client);
			fail("Exception expected.");
		} catch (IllegalArgumentException e) {
			// as expected
		}
	}

	
	@Test
	public void testParseScopeString() throws Exception {
		// method under test (happy case)
		List<OAuthScope> scopes = OpenIDConnectManagerImpl.parseScopeString("openid openid");
		List<OAuthScope> expected = new ArrayList<OAuthScope>();
		expected.add(OAuthScope.openid);
		expected.add(OAuthScope.openid);
		assertEquals(expected, scopes);
		
		try {
			// method under test
			OpenIDConnectManagerImpl.parseScopeString("openid foo");
			fail("IllegalArgumentException expected.");
		} catch (IllegalArgumentException e) {
			// as expected
		}
		
		// what if url encoded?
		// method under test
		scopes = OpenIDConnectManagerImpl.parseScopeString("openid+openid");
		assertEquals(expected, scopes);
		
		// method under test
		scopes = OpenIDConnectManagerImpl.parseScopeString("openid%20openid");
		assertEquals(expected, scopes);
		
	}
	
	@Test
	public void testGetClaimsMapFromClaimsRequestParam() throws Exception {
		String claimsString = "{\"somekey\":{\"team\":{\"values\":[\"101\"]},\"given_name\":null,\"family_name\":{\"essential\":true,\"value\":\"foo\"}}}";
		Map<OIDCClaimName,OIDCClaimsRequestDetails> map = OpenIDConnectManagerImpl.getClaimsMapFromClaimsRequestParam(claimsString, "somekey");
		{
			assertTrue(map.containsKey(OIDCClaimName.team));
			OIDCClaimsRequestDetails details = map.get(OIDCClaimName.team);
			OIDCClaimsRequestDetails expectedDetails = new OIDCClaimsRequestDetails();
			expectedDetails.setValues(Collections.singletonList("101"));
			assertEquals(expectedDetails, details);
		}
		{
			assertTrue(map.containsKey(OIDCClaimName.given_name));
			assertNull(map.get(OIDCClaimName.given_name));
		}
		{
			assertTrue(map.containsKey(OIDCClaimName.family_name));
			OIDCClaimsRequestDetails details = map.get(OIDCClaimName.family_name);
			OIDCClaimsRequestDetails expectedDetails = new OIDCClaimsRequestDetails();
			expectedDetails.setEssential(true);
			expectedDetails.setValue("foo");
			assertEquals(expectedDetails, details);
		}
		// what if key is omitted?
		claimsString = "{\"somekey\":{\"team\":{\"values\":[\"101\"]},\"given_name\":null,\"family_name\":{\"essential\":true,\"value\":\"foo\"}}}";
		map = OpenIDConnectManagerImpl.getClaimsMapFromClaimsRequestParam(claimsString, "some other key");
		assertTrue(map.isEmpty());
	}

}
