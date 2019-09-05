package org.sagebionetworks.repo.manager.oauth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.After;
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
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.auth.AuthenticationDAO;
import org.sagebionetworks.repo.model.auth.OAuthClientDao;
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
	private static final String REDIRECT_URI = "https://foo.bar.com/user/login/synapse/login";
	
	@Mock
	private StackEncrypter stackEncrypter;

	@Mock
	private OAuthClientDao oauthClientDao;

	@Mock
	private AuthenticationDAO authDao;

	@Mock
	private UserProfileManager userProfileManager;

	@Mock
	private TeamDAO teamDAO;
	
	@Mock
	private SimpleHttpClient httpClient;

	@Mock
	private SimpleHttpResponse response;

	@Captor
	private ArgumentCaptor<SimpleHttpRequest> simpleHttpRequest;
	
	@InjectMocks
	private OpenIDConnectManagerImpl openIDConnectManagerImpl;
	
	@Before
	public void setUp() throws Exception {
		when(response.getStatusCode()).thenReturn(200);
		when(response.getContent()).thenReturn("[\"firstRedirUri\",\"secondRedirUri\"]");

		when(httpClient.get((SimpleHttpRequest)any())).thenReturn(response);
	}

	
	private static final String USER_ID = "101";
	private static final String CLIENT_NAME = "some client";
	private static final String CLIENT_URI = "https://client.uri.com/index.html";
	private static final String POLICY_URI = "https://client.uri.com/policy.html";
	private static final String TOS_URI = "https://client.uri.com/termsOfService.html";
	private static final List<String> REDIRCT_URIS = Collections.singletonList("https://client.com/redir");
	private static final String SECTOR_IDENTIFIER_URI_STRING = "https://client.uri.com/path/to/json/file";
	
	private static final URI SECTOR_IDENTIFIER_URI;
	static {
		try {
			SECTOR_IDENTIFIER_URI = new URI(SECTOR_IDENTIFIER_URI_STRING);
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}


	private static OAuthClient createOAuthClient(String userId) {
		OAuthClient result = new OAuthClient();
		result.setClient_name(CLIENT_NAME);
		result.setEtag(UUID.randomUUID().toString());
		result.setCreatedBy(userId);
		result.setClient_uri(CLIENT_URI);
		result.setCreatedOn(new Date(System.currentTimeMillis()));
		result.setModifiedOn(new Date(System.currentTimeMillis()));
		result.setPolicy_uri(POLICY_URI);
		result.setRedirect_uris(REDIRCT_URIS);
		result.setTos_uri(TOS_URI);
		result.setUserinfo_signed_response_alg(OIDCSigningAlgorithm.RS256);
		result.setVerified(false);
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
		List<String> result = openIDConnectManagerImpl.readSectorIdentifierFile(SECTOR_IDENTIFIER_URI);
		
		verify(httpClient).get(simpleHttpRequest.capture());
		assertEquals(SECTOR_IDENTIFIER_URI_STRING, simpleHttpRequest.getValue().getUri());
		
		assertEquals(ImmutableList.of("firstRedirUri", "secondRedirUri"), result);
	}

	@Test
	public void testReadSectorIdentifierFileHttpRequestFails() throws Exception {
		when(response.getStatusCode()).thenReturn(400);

		try {
			// method under test
			openIDConnectManagerImpl.readSectorIdentifierFile(SECTOR_IDENTIFIER_URI);
			fail("Exception expected");
		} catch (ServiceUnavailableException e) {
			// as expected
		}
		
		verify(httpClient).get(simpleHttpRequest.capture());
		assertEquals(SECTOR_IDENTIFIER_URI_STRING, simpleHttpRequest.getValue().getUri());
		
		// try throwing IOException
		when(httpClient.get((SimpleHttpRequest)any())).thenThrow(new IOException());
		
		try {
			// method under test
			openIDConnectManagerImpl.readSectorIdentifierFile(SECTOR_IDENTIFIER_URI);
			fail("Exception expected");
		} catch (ServiceUnavailableException e) {
			// as expected
		}
		
		verify(httpClient, times(2)).get(simpleHttpRequest.capture());
		assertEquals(SECTOR_IDENTIFIER_URI_STRING, simpleHttpRequest.getValue().getUri());
		
	}

	@Test
	public void testReadSectorIdentifierBadFileContent() throws Exception {
		when(response.getContent()).thenReturn("{\"foo\":\"bar\"}");
		
		try {
			// method under test
			openIDConnectManagerImpl.readSectorIdentifierFile(SECTOR_IDENTIFIER_URI);
			fail("Exception expected");
		} catch (IllegalArgumentException e) {
			// as expected
		}
		
		verify(httpClient).get(simpleHttpRequest.capture());
		assertEquals(SECTOR_IDENTIFIER_URI_STRING, simpleHttpRequest.getValue().getUri());
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
	public void testValidateAuthenticationRequest() {
		OAuthClient client = new OAuthClient();
		client.setRedirect_uris(Collections.singletonList(REDIRECT_URI));

		OIDCAuthorizationRequest authorizationRequest = new OIDCAuthorizationRequest();
		authorizationRequest.setRedirectUri(REDIRECT_URI);
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
