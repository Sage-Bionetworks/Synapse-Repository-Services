package org.sagebionetworks.repo.manager.oauth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
public class OpenIDConnectManagerImplTest {
	private static final String USER_ID = "101";
	private static final Long USER_ID_LONG = Long.parseLong(USER_ID);
	private static final String CLIENT_NAME = "some client";
	private static final String CLIENT_URI = "https://client.uri.com/index.html";
	private static final String POLICY_URI = "https://client.uri.com/policy.html";
	private static final String TOS_URI = "https://client.uri.com/termsOfService.html";
	private static final List<String> REDIRCT_URIS = Collections.singletonList("https://client.com/redir");
	private static final String SECTOR_IDENTIFIER_URI_STRING = "https://client.uri.com/path/to/json/file";
	private static final List<String> REDIR_URI_LIST = ImmutableList.of("https://host1.com/redir1", "https://host2.com/redir2");

	
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
	public void testValidateOAuthClientForCreateOrUpdate() {
		// happy case
		{
			OAuthClient oauthClient = createOAuthClient(USER_ID);
			OpenIDConnectManagerImpl.validateOAuthClientForCreateOrUpdate(oauthClient);
			
			// sector identifier uri is not required but can be set
			oauthClient.setSector_identifier_uri(SECTOR_IDENTIFIER_URI_STRING);
			OpenIDConnectManagerImpl.validateOAuthClientForCreateOrUpdate(oauthClient);
		}

		// missing name
		{
			OAuthClient oauthClient = createOAuthClient(USER_ID);
			oauthClient.setClient_name(null);
			try {
				OpenIDConnectManagerImpl.validateOAuthClientForCreateOrUpdate(oauthClient);
				fail("exception expected");
			} catch (IllegalArgumentException e) {
				// as expected
			}
		}

		// missing redirect URIs
		{
			OAuthClient oauthClient = createOAuthClient(USER_ID);
			oauthClient.setRedirect_uris(null);
			try {
				OpenIDConnectManagerImpl.validateOAuthClientForCreateOrUpdate(oauthClient);
				fail("exception expected");
			} catch (IllegalArgumentException e) {
				// as expected
			}
			oauthClient.setRedirect_uris(Collections.EMPTY_LIST);
			try {
				OpenIDConnectManagerImpl.validateOAuthClientForCreateOrUpdate(oauthClient);
				fail("exception expected");
			} catch (IllegalArgumentException e) {
				// as expected
			}
		}
		
		// invalid redir URI
		{
			OAuthClient oauthClient = createOAuthClient(USER_ID);
			oauthClient.setRedirect_uris(ImmutableList.of("https://client.com/redir", "not-a-valid-uri"));
			try {
				OpenIDConnectManagerImpl.validateOAuthClientForCreateOrUpdate(oauthClient);
				fail("exception expected");
			} catch (IllegalArgumentException e) {
				// as expected
			}
		}

		// invalid sector identifier uri
		{
			OAuthClient oauthClient = createOAuthClient(USER_ID);
			oauthClient.setSector_identifier_uri("not-a-valid-uri");
			try {
				OpenIDConnectManagerImpl.validateOAuthClientForCreateOrUpdate(oauthClient);
				fail("exception expected");
			} catch (IllegalArgumentException e) {
				// as expected
			}
		}

	}
	
	@Test
	public void testReadSectorIdentifierFileHappyCase() throws Exception {
		// method under test
		List<String> result = openIDConnectManagerImpl.readSectorIdentifierFile(sector_identifier_uri);
		
		verify(mockHttpClient).get(simpleHttpRequestCaptor.capture());
		assertEquals(SECTOR_IDENTIFIER_URI_STRING, simpleHttpRequestCaptor.getValue().getUri());
		
		assertEquals(REDIR_URI_LIST, result);
	}

	@Test
	public void testReadSectorIdentifierFileHttpRequestFails() throws Exception {
		when(mockHttpResponse.getStatusCode()).thenReturn(400);

		try {
			// method under test
			openIDConnectManagerImpl.readSectorIdentifierFile(sector_identifier_uri);
			fail("Exception expected");
		} catch (ServiceUnavailableException e) {
			// as expected
		}
		
		verify(mockHttpClient).get(simpleHttpRequestCaptor.capture());
		assertEquals(SECTOR_IDENTIFIER_URI_STRING, simpleHttpRequestCaptor.getValue().getUri());
		
		// try throwing IOException
		when(mockHttpClient.get((SimpleHttpRequest)any())).thenThrow(new IOException());
		
		try {
			// method under test
			openIDConnectManagerImpl.readSectorIdentifierFile(sector_identifier_uri);
			fail("Exception expected");
		} catch (ServiceUnavailableException e) {
			// as expected
		}
		
		verify(mockHttpClient, times(2)).get(simpleHttpRequestCaptor.capture());
		assertEquals(SECTOR_IDENTIFIER_URI_STRING, simpleHttpRequestCaptor.getValue().getUri());
		
	}

	@Test
	public void testReadSectorIdentifierBadFileContent() throws Exception {
		when(mockHttpResponse.getContent()).thenReturn("{\"foo\":\"bar\"}");
		
		try {
			// method under test
			openIDConnectManagerImpl.readSectorIdentifierFile(sector_identifier_uri);
			fail("Exception expected");
		} catch (IllegalArgumentException e) {
			// as expected
		}
		
		verify(mockHttpClient).get(simpleHttpRequestCaptor.capture());
		assertEquals(SECTOR_IDENTIFIER_URI_STRING, simpleHttpRequestCaptor.getValue().getUri());
	}
	
	@Test
	public void testResolveSectorIdentifier_NoSIURI_HappyCase() throws Exception {
		// method under test
		String sectorIdentifier = openIDConnectManagerImpl.resolveSectorIdentifier(null, 
				ImmutableList.of("https://host/redir1", "https://host/redir2"));
		
		assertEquals("host", sectorIdentifier);
	}

	@Test
	public void testResolveSectorIdentifier_NoSIURI_InvalidURI() throws Exception {
		try {
			// method under test
			openIDConnectManagerImpl.resolveSectorIdentifier(null, 
					ImmutableList.of("https://host/redir1", "https://host/%$#@*"));
			fail("exception expected");
		} catch (IllegalArgumentException e) {
			// as expected
		}
	}

	@Test
	public void testResolveSectorIdentifier_NoSIURI_DifferentHosts() throws Exception {
		try {
			// method under test
			openIDConnectManagerImpl.resolveSectorIdentifier(null, 
					ImmutableList.of("https://host/redir1", "https://host2/redir1"));
			fail("exception expected");
		} catch (IllegalArgumentException e) {
			// as expected
		}
	}

	@Test
	public void testResolveSectorIdentifier_NoSIURI_NoRedirURIs() throws Exception {
		try {
			// method under test
			openIDConnectManagerImpl.resolveSectorIdentifier(null, null);
			fail("exception expected");
		} catch (IllegalArgumentException e) {
			// as expected
		}
		try {
			// method under test
			openIDConnectManagerImpl.resolveSectorIdentifier(null, Collections.EMPTY_LIST);
			fail("exception expected");
		} catch (IllegalArgumentException e) {
			// as expected
		}
	}
	
	@Test
	public void testResolveSectorIdentifier_WithSIURI_HappyCase() throws Exception {
		// method under test
		String sectorIdentifier = openIDConnectManagerImpl.resolveSectorIdentifier(
				SECTOR_IDENTIFIER_URI_STRING, REDIR_URI_LIST);
		
		// the redir's are a subset of those in the file; the Sector Identifer is the host part of the URL pointing to the file
		assertEquals("client.uri.com", sectorIdentifier);
		
		
		// the registered URIs must be a *subset* of those listed in the files.  So this is OK too:
		// method under test
		sectorIdentifier = openIDConnectManagerImpl.resolveSectorIdentifier(SECTOR_IDENTIFIER_URI_STRING, 
				Collections.singletonList("https://host1.com/redir1"));
		assertEquals("client.uri.com", sectorIdentifier);
	}

	@Test
	public void testResolveSectorIdentifier_WithSIURI_InvalidFileURI() throws Exception {
		try {
			// method under test
			openIDConnectManagerImpl.resolveSectorIdentifier("*&#%#$$@", REDIR_URI_LIST);
			fail("exception expected");
		} catch (IllegalArgumentException e) {
			// as expected
		}

		// scheme must be https not http
		try {
			// method under test
			openIDConnectManagerImpl.resolveSectorIdentifier("http://insecure.com/file", REDIR_URI_LIST);
			fail("exception expected");
		} catch (IllegalArgumentException e) {
			// as expected
		}
	}
	
	@Test
	public void testResolveSectorIdentifier_WithSIURI_RedirMismatch() throws Exception {
		// trying to use a redirect uri that's not in the file
		try {
			// method under test
			openIDConnectManagerImpl.resolveSectorIdentifier(SECTOR_IDENTIFIER_URI_STRING, 
					Collections.singletonList("https://SomeOtherHost/redir1"));
			fail("exception expected");
		} catch (IllegalArgumentException e) {
			// as expected
		}
	}

	@Test
	public void testCanCreate() {
		UserInfo userInfo = new UserInfo(false);
		userInfo.setId(USER_ID_LONG);
		// method under test
		assertTrue(OpenIDConnectManagerImpl.canCreate(userInfo));
		
		// method under test
		assertFalse(OpenIDConnectManagerImpl.canCreate(anonymousUserInfo));
	}

	@Test
	public void testCanAdministrate() {
		UserInfo userInfo = new UserInfo(false);
		userInfo.setId(USER_ID_LONG);
		// method under test
		assertTrue(OpenIDConnectManagerImpl.canAdministrate(userInfo, USER_ID));
		// method under test
		assertFalse(OpenIDConnectManagerImpl.canAdministrate(userInfo, "9999"));
		
		UserInfo adminUserInfo = new UserInfo(true);
		adminUserInfo.setId(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		// method under test
		assertTrue(OpenIDConnectManagerImpl.canAdministrate(adminUserInfo, USER_ID));
	}
	
	@Test
	public void testCreateOpenIDConnectClient() throws Exception {
		OAuthClient oauthClient = createOAuthClient(USER_ID);
		
		// later we test these fields are filled in.  So let's ensure they are not pre-filled.
		assertNull(oauthClient.getCreatedBy());
		assertNull(oauthClient.getEtag());
		assertNull(oauthClient.getVerified());
		assertNull(oauthClient.getSector_identifier());
		assertNull(oauthClient.getSector_identifier_uri());
		
		// method under test
		OAuthClient result = openIDConnectManagerImpl.createOpenIDConnectClient(userInfo, oauthClient);
		
		assertEquals(USER_ID, result.getCreatedBy());
		assertNotNull(result.getEtag());
		assertFalse(result.getVerified());
		assertEquals("client.com", result.getSector_identifier());
		
		// make sure sector identifier was created
		verify(mockOauthClientDao).createSectorIdentifier(sectorIdentifierCaptor.capture());
		
		SectorIdentifier si = sectorIdentifierCaptor.getValue();
		assertEquals(USER_ID_LONG, si.getCreatedBy());
		assertNotNull(si.getCreatedOn());
		assertNotNull(si.getSecret());
		assertEquals("client.com", si.getSectorIdentifierUri());
	}
	
	@Test
	public void testCreateOpenIDConnectClient_WithSectorIdentifierURI() throws Exception {
		OAuthClient oauthClient = createOAuthClient(USER_ID);
		oauthClient.setRedirect_uris(Collections.singletonList("https://host1.com/redir1"));
		oauthClient.setSector_identifier_uri(SECTOR_IDENTIFIER_URI_STRING);
		
		// method under test
		OAuthClient result = openIDConnectManagerImpl.createOpenIDConnectClient(userInfo, oauthClient);
		
		assertEquals("client.uri.com", result.getSector_identifier());
		
		// make sure sector identifier was created
		verify(mockOauthClientDao).createSectorIdentifier(sectorIdentifierCaptor.capture());
		SectorIdentifier si = sectorIdentifierCaptor.getValue();
		assertEquals("client.uri.com", si.getSectorIdentifierUri());
	}
	

	@Test
	public void testCreateOpenIDConnectClient_SIAlreadyExists() throws Exception {
		OAuthClient oauthClient = createOAuthClient(USER_ID);
		
		when(mockOauthClientDao.doesSectorIdentifierExistForURI(anyString())).thenReturn(true);

		// method under test
		openIDConnectManagerImpl.createOpenIDConnectClient(userInfo, oauthClient);
		
		// make sure sector identifier was created
		verify(mockOauthClientDao, never()).createSectorIdentifier((SectorIdentifier)any());
	}
	
	@Test
	public void testCreateOpenIDConnectClient_Unauthorized() throws Exception {
		OAuthClient oauthClient = createOAuthClient(USER_ID);

		try {
			// method under test
			openIDConnectManagerImpl.createOpenIDConnectClient(anonymousUserInfo, oauthClient);
			fail("Exception expected.");
		} catch (UnauthorizedException e) {
			//as expected
		}
	}
	
	@Test
	public void testGetOpenIDConnectClient_owner() throws Exception {
		OAuthClient oauthClient = createOAuthClient(USER_ID);
		String id = "123";
		oauthClient.setClientId(id);
		oauthClient.setCreatedBy(USER_ID);
		oauthClient.setSector_identifier("foo.com");
		oauthClient.setEtag("some etag");
		when(mockOauthClientDao.getOAuthClient(id)).thenReturn(oauthClient);
		
		// method under test
		openIDConnectManagerImpl.getOpenIDConnectClient(userInfo, id);
		
		verify(mockOauthClientDao).getOAuthClient(id);
		
		// verify not scrubbed of private info
		assertNotNull(oauthClient.getCreatedBy());
		assertNotNull(oauthClient.getRedirect_uris());
		assertNotNull(oauthClient.getSector_identifier());
		assertNotNull(oauthClient.getCreatedOn());
		assertNotNull(oauthClient.getModifiedOn());
		assertNotNull(oauthClient.getEtag());
	}
	
	@Test
	public void testGetOpenIDConnectClient_not_owner() throws Exception {
		OAuthClient oauthClient = createOAuthClient(USER_ID);
		String id = "123";
		oauthClient.setClientId(id);
		oauthClient.setCreatedBy(USER_ID);
		oauthClient.setSector_identifier("foo.com");
		oauthClient.setEtag("some etag");
		when(mockOauthClientDao.getOAuthClient(id)).thenReturn(oauthClient);
		
		// method under test
		openIDConnectManagerImpl.getOpenIDConnectClient(anonymousUserInfo, id);
		
		verify(mockOauthClientDao).getOAuthClient(id);
		
		// verify IS scrubbed of private info
		assertNull(oauthClient.getCreatedBy());
		assertNull(oauthClient.getRedirect_uris());
		assertNull(oauthClient.getSector_identifier());
		assertNull(oauthClient.getCreatedOn());
		assertNull(oauthClient.getModifiedOn());
		assertNull(oauthClient.getEtag());
	}
	
	@Test
	public void testListOpenIDConnectClients() throws Exception {
		String nextPageToken = "some token";
		
		// method under test
		openIDConnectManagerImpl.listOpenIDConnectClients(userInfo, nextPageToken);
		
		verify(mockOauthClientDao).listOAuthClients(nextPageToken, USER_ID_LONG);
	}
	
	@Test
	public void testUpdateOpenIDConnectClient() throws Exception {
		OAuthClient toUpdate = createOAuthClient(USER_ID);
		
		// method under test
		OAuthClient updated = openIDConnectManagerImpl.updateOpenIDConnectClient(userInfo, toUpdate);
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
