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
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.OAuthClientDao;
import org.sagebionetworks.repo.model.auth.SectorIdentifier;
import org.sagebionetworks.repo.model.oauth.OAuthClient;
import org.sagebionetworks.repo.model.oauth.OAuthClientIdAndSecret;
import org.sagebionetworks.repo.model.oauth.OIDCSigningAlgorithm;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.sagebionetworks.securitytools.PBKDF2Utils;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClient;
import org.sagebionetworks.simpleHttpClient.SimpleHttpRequest;
import org.sagebionetworks.simpleHttpClient.SimpleHttpResponse;

import com.google.common.collect.ImmutableList;

@RunWith(MockitoJUnitRunner.class)
public class OAuthClientManagerImplUnitTest {
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
	private OAuthClientDao mockOauthClientDao;

	@Mock
	private SimpleHttpClient mockHttpClient;

	@Mock
	private SimpleHttpResponse mockHttpResponse;
	
	@Mock
	private AuthorizationManager mockAuthManager;

	@Captor
	private ArgumentCaptor<SimpleHttpRequest> simpleHttpRequestCaptor;
	
	@Captor
	private ArgumentCaptor<SectorIdentifier> sectorIdentifierCaptor;
	
	@Captor
	private ArgumentCaptor<OAuthClient> oauthClientCaptor;
	
	@InjectMocks
	private OAuthClientManagerImpl oauthClientManagerImpl;
	
	private UserInfo userInfo;
	private UserInfo anonymousUserInfo;
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
	public void testGetURI() throws Exception {
		// method under test
		OAuthClientManagerImpl.getUri(SECTOR_IDENTIFIER_URI_STRING);
		
		try {
			// method under test
			OAuthClientManagerImpl.getUri("not #$%^ valid");
			fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			// as expected
		}
	}


	@Test
	public void testValidateOAuthClientForCreateOrUpdate() {
		// happy case
		{
			OAuthClient oauthClient = createOAuthClient(USER_ID);
			OAuthClientManagerImpl.validateOAuthClientForCreateOrUpdate(oauthClient);
			
			// sector identifier uri is not required but can be set
			oauthClient.setSector_identifier_uri(SECTOR_IDENTIFIER_URI_STRING);
			OAuthClientManagerImpl.validateOAuthClientForCreateOrUpdate(oauthClient);
		}

		// missing name
		{
			OAuthClient oauthClient = createOAuthClient(USER_ID);
			oauthClient.setClient_name(null);
			try {
				OAuthClientManagerImpl.validateOAuthClientForCreateOrUpdate(oauthClient);
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
				OAuthClientManagerImpl.validateOAuthClientForCreateOrUpdate(oauthClient);
				fail("exception expected");
			} catch (IllegalArgumentException e) {
				// as expected
			}
			oauthClient.setRedirect_uris(Collections.EMPTY_LIST);
			try {
				OAuthClientManagerImpl.validateOAuthClientForCreateOrUpdate(oauthClient);
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
				OAuthClientManagerImpl.validateOAuthClientForCreateOrUpdate(oauthClient);
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
				OAuthClientManagerImpl.validateOAuthClientForCreateOrUpdate(oauthClient);
				fail("exception expected");
			} catch (IllegalArgumentException e) {
				// as expected
			}
		}

	}
	
	@Test
	public void testReadSectorIdentifierFileHappyCase() throws Exception {
		// method under test
		List<String> result = oauthClientManagerImpl.readSectorIdentifierFile(sector_identifier_uri);
		
		verify(mockHttpClient).get(simpleHttpRequestCaptor.capture());
		assertEquals(SECTOR_IDENTIFIER_URI_STRING, simpleHttpRequestCaptor.getValue().getUri());
		
		assertEquals(REDIR_URI_LIST, result);
	}

	@Test
	public void testReadSectorIdentifierFileHttpRequestFails() throws Exception {
		when(mockHttpResponse.getStatusCode()).thenReturn(400);

		try {
			// method under test
			oauthClientManagerImpl.readSectorIdentifierFile(sector_identifier_uri);
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
			oauthClientManagerImpl.readSectorIdentifierFile(sector_identifier_uri);
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
			oauthClientManagerImpl.readSectorIdentifierFile(sector_identifier_uri);
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
		String sectorIdentifier = oauthClientManagerImpl.resolveSectorIdentifier(null, 
				ImmutableList.of("https://host/redir1", "https://host/redir2"));
		
		assertEquals("host", sectorIdentifier);
	}

	@Test
	public void testResolveSectorIdentifier_NoSIURI_InvalidURI() throws Exception {
		try {
			// method under test
			oauthClientManagerImpl.resolveSectorIdentifier(null, 
					ImmutableList.of("https://host/redir1", "https://host/%$#@*"));
			fail("exception expected");
		} catch (IllegalArgumentException e) {
			// as expected
		}
	}

	@Test
	public void testResolveSectorIdentifier_NoSIURI_NULL_URI() throws Exception {
		try {
			List<String> uris = new ArrayList<String>();
			uris.add("https://host/redir1");
			uris.add(null);
			// method under test
			oauthClientManagerImpl.resolveSectorIdentifier(null, uris);
			fail("exception expected");
		} catch (IllegalArgumentException e) {
			// as expected
		}
	}

	@Test
	public void testResolveSectorIdentifier_NoSIURI_DifferentHosts() throws Exception {
		try {
			// method under test
			oauthClientManagerImpl.resolveSectorIdentifier(null, 
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
			oauthClientManagerImpl.resolveSectorIdentifier(null, null);
			fail("exception expected");
		} catch (IllegalArgumentException e) {
			// as expected
		}
		try {
			// method under test
			oauthClientManagerImpl.resolveSectorIdentifier(null, Collections.EMPTY_LIST);
			fail("exception expected");
		} catch (IllegalArgumentException e) {
			// as expected
		}
	}
	
	@Test
	public void testResolveSectorIdentifier_WithSIURI_HappyCase() throws Exception {
		// method under test
		String sectorIdentifier = oauthClientManagerImpl.resolveSectorIdentifier(
				SECTOR_IDENTIFIER_URI_STRING, REDIR_URI_LIST);
		
		// the redir's are a subset of those in the file; the Sector Identifer is the host part of the URL pointing to the file
		assertEquals("client.uri.com", sectorIdentifier);
		
		
		// the registered URIs must be a *subset* of those listed in the files.  So this is OK too:
		// method under test
		sectorIdentifier = oauthClientManagerImpl.resolveSectorIdentifier(SECTOR_IDENTIFIER_URI_STRING, 
				Collections.singletonList("https://host1.com/redir1"));
		assertEquals("client.uri.com", sectorIdentifier);
	}

	@Test
	public void testResolveSectorIdentifier_WithSIURI_InvalidFileURI() throws Exception {
		try {
			// method under test
			oauthClientManagerImpl.resolveSectorIdentifier("*&#%#$$@", REDIR_URI_LIST);
			fail("exception expected");
		} catch (IllegalArgumentException e) {
			// as expected
		}

		// scheme must be https not http
		try {
			// method under test
			oauthClientManagerImpl.resolveSectorIdentifier("http://insecure.com/file", REDIR_URI_LIST);
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
			oauthClientManagerImpl.resolveSectorIdentifier(SECTOR_IDENTIFIER_URI_STRING, 
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
		assertTrue(OAuthClientManagerImpl.canCreate(userInfo));
		
		// method under test
		assertFalse(OAuthClientManagerImpl.canCreate(anonymousUserInfo));
	}

	@Test
	public void testCanAdministrate() {
		UserInfo userInfo = new UserInfo(false);
		userInfo.setId(USER_ID_LONG);
		// method under test
		assertTrue(OAuthClientManagerImpl.canAdministrate(userInfo, USER_ID));
		// method under test
		assertFalse(OAuthClientManagerImpl.canAdministrate(userInfo, "9999"));
		
		UserInfo adminUserInfo = new UserInfo(true);
		adminUserInfo.setId(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		// method under test
		assertTrue(OAuthClientManagerImpl.canAdministrate(adminUserInfo, USER_ID));
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
		OAuthClient result = oauthClientManagerImpl.createOpenIDConnectClient(userInfo, oauthClient);
		
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
		OAuthClient result = oauthClientManagerImpl.createOpenIDConnectClient(userInfo, oauthClient);
		
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
		oauthClientManagerImpl.createOpenIDConnectClient(userInfo, oauthClient);
		
		// make sure sector identifier was created
		verify(mockOauthClientDao, never()).createSectorIdentifier((SectorIdentifier)any());
	}
	
	@Test
	public void testCreateOpenIDConnectClient_Unauthorized() throws Exception {
		OAuthClient oauthClient = createOAuthClient(USER_ID);

		try {
			// method under test
			oauthClientManagerImpl.createOpenIDConnectClient(anonymousUserInfo, oauthClient);
			fail("Exception expected.");
		} catch (UnauthorizedException e) {
			//as expected
		}
	}
	
	@Test
	public void testGetOpenIDConnectClient_owner() throws Exception {
		OAuthClient oauthClient = createOAuthClient(USER_ID);
		String id = "123";
		oauthClient.setClient_id(id);
		oauthClient.setCreatedBy(USER_ID);
		oauthClient.setSector_identifier("foo.com");
		oauthClient.setEtag("some etag");
		when(mockOauthClientDao.getOAuthClient(id)).thenReturn(oauthClient);
		
		// method under test
		oauthClientManagerImpl.getOpenIDConnectClient(userInfo, id);
		
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
		oauthClient.setClient_id(id);
		oauthClient.setCreatedBy(USER_ID);
		oauthClient.setSector_identifier("foo.com");
		oauthClient.setEtag("some etag");
		when(mockOauthClientDao.getOAuthClient(id)).thenReturn(oauthClient);
		
		// method under test
		oauthClientManagerImpl.getOpenIDConnectClient(anonymousUserInfo, id);
		
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
		oauthClientManagerImpl.listOpenIDConnectClients(userInfo, nextPageToken);
		
		verify(mockOauthClientDao).listOAuthClients(nextPageToken, USER_ID_LONG);
	}
	
	// create a fully populated object, to be updated
	private static OAuthClient newCreatedOAuthClient() {
		OAuthClient oauthClient = createOAuthClient(USER_ID);
		oauthClient.setClient_id(OAUTH_CLIENT_ID);
		oauthClient.setCreatedBy(USER_ID);
		String etag = "some etag";
		oauthClient.setEtag(etag);
		String sector_identifier = "hostname.com";
		oauthClient.setSector_identifier(sector_identifier);
		oauthClient.setSector_identifier_uri(SECTOR_IDENTIFIER_URI_STRING);
		oauthClient.setVerified(true);
		return oauthClient;
	}
	
	@Test
	public void testUpdateOpenIDConnectClient_HappyCase() throws Exception {
		// 'created' simulates what's in the database already
		OAuthClient created = newCreatedOAuthClient();
		
		// 'toUpdate' is the object as retrieved and modified by the client
		OAuthClient toUpdate = newCreatedOAuthClient();
		
		// these are all the fields we can change
		toUpdate.setClient_name("some other name");
		toUpdate.setClient_uri("some other client uri");
		String newHost = "new.client.com";
		toUpdate.setRedirect_uris(Collections.singletonList("https://"+newHost+"/redir"));
		toUpdate.setPolicy_uri("some new policy URI");
		toUpdate.setTos_uri("some new TOS URI");
		toUpdate.setUserinfo_signed_response_alg(null);
		toUpdate.setSector_identifier_uri(null);
		
		// we can try to change these, but the changes will be ignored, as we will see below
		toUpdate.setCreatedBy(null);
		toUpdate.setCreatedOn(null);
		toUpdate.setModifiedOn(null);
		
		when(mockOauthClientDao.selectOAuthClientForUpdate(created.getClient_id())).thenReturn(created);
		when(mockOauthClientDao.updateOAuthClient((OAuthClient)any())).then(returnsFirstArg());	
		
		// method under test
		OAuthClient updated = oauthClientManagerImpl.updateOpenIDConnectClient(userInfo, toUpdate);
		
		assertEquals(toUpdate.getClient_id(), updated.getClient_id());
		assertEquals(toUpdate.getClient_name(), updated.getClient_name());
		assertEquals(toUpdate.getClient_uri(), updated.getClient_uri());
		assertEquals(toUpdate.getPolicy_uri(), updated.getPolicy_uri());
		assertEquals(toUpdate.getTos_uri(), updated.getTos_uri());
		assertEquals(toUpdate.getUserinfo_signed_response_alg(), updated.getUserinfo_signed_response_alg());
		assertEquals(toUpdate.getRedirect_uris(), updated.getRedirect_uris());
		assertEquals(toUpdate.getSector_identifier_uri(), updated.getSector_identifier_uri());
		assertNotNull(updated.getModifiedOn());
		assertNotEquals(toUpdate.getEtag(), updated.getEtag());
		// Note, we test that the sector identifier has been updated...
		assertEquals(newHost, updated.getSector_identifier());
		// ... and that updating it causes 'verified' to revert to 'false'
		assertFalse(updated.getVerified());
		
		// this checks that client modifications to the fields are ignored
		assertNotNull(updated.getCreatedBy());
		assertNotNull(updated.getCreatedOn());
		assertNotNull(updated.getModifiedOn());
	}
	
	@Test
	public void testUpdateOpenIDConnectClient_unauthorized() throws Exception {
		// 'created' simulates what's in the database already
		OAuthClient created = newCreatedOAuthClient();
		created.setCreatedBy(userInfo.getId().toString());
		when(mockOauthClientDao.selectOAuthClientForUpdate(created.getClient_id())).thenReturn(created);
		
		// 'toUpdate' is the object as retrieved and modified by the client
		OAuthClient toUpdate = newCreatedOAuthClient();

		try {
			// method under test
			oauthClientManagerImpl.updateOpenIDConnectClient(anonymousUserInfo, toUpdate);
			fail("UnauthorizedException expected");
		} catch (UnauthorizedException e) {
			// as expected
		}
	}
	
	@Test
	public void testUpdateOpenIDConnectClient_etagMismatch() throws Exception {
		// 'created' simulates what's in the database already
		OAuthClient created = newCreatedOAuthClient();
		created.setCreatedBy(userInfo.getId().toString());
		when(mockOauthClientDao.selectOAuthClientForUpdate(created.getClient_id())).thenReturn(created);
		
		// 'toUpdate' is the object as retrieved and modified by the client
		OAuthClient toUpdate = newCreatedOAuthClient();
		toUpdate.setEtag("mismatched etag");

		try {
			// method under test
			oauthClientManagerImpl.updateOpenIDConnectClient(userInfo, toUpdate);
			fail("ConflictingUpdateException expected");
		} catch (ConflictingUpdateException e) {
			// as expected
		}
	}
	
	@Test
	public void testDeleteOpenIDConnectClient() {
		// method under test
		oauthClientManagerImpl.deleteOpenIDConnectClient(userInfo, OAUTH_CLIENT_ID);
		verify(mockOauthClientDao).deleteOAuthClient(OAUTH_CLIENT_ID);
	}
	
	@Test
	public void testDeleteOpenIDConnectClient_Unauthorized() {
		when(mockOauthClientDao.getOAuthClientCreator(OAUTH_CLIENT_ID)).thenReturn("202");

		try {
			// method under test
			oauthClientManagerImpl.deleteOpenIDConnectClient(userInfo, OAUTH_CLIENT_ID);
			fail("UnauthorizesException expected");
		} catch (UnauthorizedException e) {
			// as expected
		}
		verify(mockOauthClientDao, never()).deleteOAuthClient(OAUTH_CLIENT_ID);
	}
	
	@Test
	public void testCreateClientSecret() {
		
		// method under test
		OAuthClientIdAndSecret idAndSecret = oauthClientManagerImpl.createClientSecret(userInfo, OAUTH_CLIENT_ID);

		verify(mockOauthClientDao).setOAuthClientSecretHash(eq(OAUTH_CLIENT_ID), anyString(), anyString());
		assertEquals(OAUTH_CLIENT_ID, idAndSecret.getClient_id());
		assertNotNull(idAndSecret.getClient_secret());
	}

	@Test
	public void testCreateClientSecret_unauthorized() {
		try {
			// method under test
			oauthClientManagerImpl.createClientSecret(anonymousUserInfo, OAUTH_CLIENT_ID);
			fail("UnauthorizedException expected");
		} catch (UnauthorizedException e) {
			// as expected
		}

		verify(mockOauthClientDao, never()).setOAuthClientSecretHash(eq(OAUTH_CLIENT_ID), anyString(), anyString());
	}
	
	private static final String CLIENT_SECRET = "some secret";
	
	@Test
	public void testValidateClientCredentials_HappyCase() {
		String secretHash = PBKDF2Utils.hashPassword(CLIENT_SECRET, null);
		byte[] clientSalt = PBKDF2Utils.extractSalt(secretHash);
		when(mockOauthClientDao.getSecretSalt(OAUTH_CLIENT_ID)).thenReturn(clientSalt);
		when(mockOauthClientDao.checkOAuthClientSecretHash(OAUTH_CLIENT_ID, secretHash)).thenReturn(true);
		
		OAuthClientIdAndSecret clientIdAndSecret = new OAuthClientIdAndSecret();
		clientIdAndSecret.setClient_id(OAUTH_CLIENT_ID);
		clientIdAndSecret.setClient_secret(CLIENT_SECRET);
		
		// method under test
		assertTrue(oauthClientManagerImpl.validateClientCredentials(clientIdAndSecret));
		
		verify(mockOauthClientDao).getSecretSalt(OAUTH_CLIENT_ID);
		verify(mockOauthClientDao).checkOAuthClientSecretHash(OAUTH_CLIENT_ID, secretHash);
	}
	
	@Test
	public void testValidateClientCredentials_missingCredentials() {
		String secretHash = PBKDF2Utils.hashPassword(CLIENT_SECRET, null);
		byte[] clientSalt = PBKDF2Utils.extractSalt(secretHash);
		when(mockOauthClientDao.getSecretSalt(OAUTH_CLIENT_ID)).thenReturn(clientSalt);
		when(mockOauthClientDao.checkOAuthClientSecretHash(OAUTH_CLIENT_ID, secretHash)).thenReturn(true);
		
		OAuthClientIdAndSecret clientIdAndSecret = new OAuthClientIdAndSecret();
		clientIdAndSecret.setClient_id(null);
		clientIdAndSecret.setClient_secret(null);
		
		// method under test
		assertFalse(oauthClientManagerImpl.validateClientCredentials(clientIdAndSecret));
		
		verify(mockOauthClientDao, never()).getSecretSalt(anyString());
		verify(mockOauthClientDao, never()).checkOAuthClientSecretHash(anyString(), anyString());
	}
	
	@Test
	public void testValidateClientCredentials_badClientId() {
		String wrongClientId = "wrong id";
		when(mockOauthClientDao.getSecretSalt(wrongClientId)).thenThrow(new NotFoundException());

		OAuthClientIdAndSecret clientIdAndSecret = new OAuthClientIdAndSecret();
		clientIdAndSecret.setClient_id(wrongClientId);
		clientIdAndSecret.setClient_secret(CLIENT_SECRET);
		
		// method under test
		assertFalse(oauthClientManagerImpl.validateClientCredentials(clientIdAndSecret));
		
		verify(mockOauthClientDao).getSecretSalt(wrongClientId);
		verify(mockOauthClientDao, never()).checkOAuthClientSecretHash(anyString(), anyString());
	}
	
	@Test
	public void testValidateClientCredentials_BadSecret() {
		String secretHash = PBKDF2Utils.hashPassword(CLIENT_SECRET, null);
		byte[] clientSalt = PBKDF2Utils.extractSalt(secretHash);
		when(mockOauthClientDao.getSecretSalt(OAUTH_CLIENT_ID)).thenReturn(clientSalt);
		
		String wrongSecretHash = PBKDF2Utils.hashPassword("Wrong secret", clientSalt);

		when(mockOauthClientDao.checkOAuthClientSecretHash(OAUTH_CLIENT_ID, wrongSecretHash)).thenReturn(false);
		
		OAuthClientIdAndSecret clientIdAndSecret = new OAuthClientIdAndSecret();
		clientIdAndSecret.setClient_id(OAUTH_CLIENT_ID);
		clientIdAndSecret.setClient_secret("Wrong secret");
		
		// method under test
		assertFalse(oauthClientManagerImpl.validateClientCredentials(clientIdAndSecret));
		
		verify(mockOauthClientDao).getSecretSalt(OAUTH_CLIENT_ID);
		verify(mockOauthClientDao).checkOAuthClientSecretHash(OAUTH_CLIENT_ID, wrongSecretHash);
	}
	
	@Test
	public void testUpdateOpenIDConnectClientVerifiedStatusWithEmptyUser() {
		UserInfo userInfo = null;
		boolean verifiedStatus = true;
		String clientId = OAUTH_CLIENT_ID;
		
		IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			// Method under test
			oauthClientManagerImpl.updateOpenIDConnectClientVerifiedStatus(userInfo, clientId, verifiedStatus);
		});
		
		assertEquals("User info is required.", ex.getMessage());
	}
	
	@Test
	public void testUpdateOpenIDConnectClientVerifiedStatusWithWrongClientId() {
		boolean verifiedStatus = true;
		
		IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			// Method under test
			oauthClientManagerImpl.updateOpenIDConnectClientVerifiedStatus(userInfo, "", verifiedStatus);
		});
		
		assertEquals("Client ID is required and must not be the empty string.", ex.getMessage());
		
		ex = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			// Method under test
			oauthClientManagerImpl.updateOpenIDConnectClientVerifiedStatus(userInfo, "    ", verifiedStatus);
		});
		
		assertEquals("Client ID is required and must not be a blank string.", ex.getMessage());
	}
	
	@Test
	public void testUpdateOpenIDConnectClientVerifiedStatusWithWrongCredentials() {
		boolean verifiedStatus = true;
		String clientId = OAUTH_CLIENT_ID;
		
		when(mockAuthManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(false);
		
		Assertions.assertThrows(UnauthorizedException.class, () -> {
			// Method under test
			oauthClientManagerImpl.updateOpenIDConnectClientVerifiedStatus(userInfo, clientId, verifiedStatus);
		});
		
		verify(mockAuthManager).isACTTeamMemberOrAdmin(userInfo);
		verifyZeroInteractions(mockOauthClientDao);
		
	}
	
	@Test
	public void testUpdateOpenIDConnectClientVerifiedStatusNoChange() {
		OAuthClient mockClient = newCreatedOAuthClient();
		
		String clientId = OAUTH_CLIENT_ID;
		boolean verifiedStatus = true;
		
		mockClient.setVerified(verifiedStatus);
		
		when(mockAuthManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(true);
		when(mockOauthClientDao.selectOAuthClientForUpdate(clientId)).thenReturn(mockClient);
		when(mockOauthClientDao.updateOAuthClient((OAuthClient)any())).then(returnsFirstArg());	
		
		// Method under test
		oauthClientManagerImpl.updateOpenIDConnectClientVerifiedStatus(userInfo, clientId, verifiedStatus);
		
		verify(mockAuthManager).isACTTeamMemberOrAdmin(userInfo);
		verify(mockOauthClientDao).selectOAuthClientForUpdate(clientId);
		verify(mockOauthClientDao, times(0)).updateOAuthClient(any());
		
	}
	
	@Test
	public void testUpdateOpenIDConnectClientVerifiedStatus() {
		OAuthClient originalClient = newCreatedOAuthClient();
		
		String clientId = OAUTH_CLIENT_ID;
		
		Date originalModifiedOn = Date.from(Instant.now().minusSeconds(60));
		String originalEtag = "Some Etag";
		boolean originalVerifiedStatus = false;
		
		originalClient.setModifiedOn(originalModifiedOn);
		originalClient.setEtag(originalEtag);
		originalClient.setVerified(originalVerifiedStatus);
		
		when(mockAuthManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(true);
		when(mockOauthClientDao.selectOAuthClientForUpdate(clientId)).thenReturn(originalClient);
		when(mockOauthClientDao.updateOAuthClient((OAuthClient)any())).then(returnsFirstArg());	
		
		// Method under test
		oauthClientManagerImpl.updateOpenIDConnectClientVerifiedStatus(userInfo, clientId, !originalVerifiedStatus);
		
		verify(mockAuthManager).isACTTeamMemberOrAdmin(userInfo);
		verify(mockOauthClientDao).selectOAuthClientForUpdate(clientId);
		verify(mockOauthClientDao).updateOAuthClient(oauthClientCaptor.capture());
		
		OAuthClient updated = oauthClientCaptor.getValue();
		
		assertNotEquals(originalModifiedOn, updated.getModifiedOn());
		assertNotEquals(originalEtag, updated.getEtag());
		assertNotEquals(originalVerifiedStatus, updated.getVerified());
		
	}
}
