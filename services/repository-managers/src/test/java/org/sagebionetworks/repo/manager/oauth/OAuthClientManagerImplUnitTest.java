package org.sagebionetworks.repo.manager.oauth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.NotificationManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.auth.OAuthClientDao;
import org.sagebionetworks.repo.model.auth.SectorIdentifier;
import org.sagebionetworks.repo.model.dbo.dao.AccessControlListUtils;
import org.sagebionetworks.repo.model.oauth.OAuthClient;
import org.sagebionetworks.repo.model.oauth.OAuthClientIdAndSecret;
import org.sagebionetworks.repo.model.oauth.OIDCSigningAlgorithm;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.securitytools.PBKDF2Utils;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClient;
import org.sagebionetworks.simpleHttpClient.SimpleHttpRequest;
import org.sagebionetworks.simpleHttpClient.SimpleHttpResponse;

import com.google.common.collect.ImmutableList;

@ExtendWith(MockitoExtension.class)
public class OAuthClientManagerImplUnitTest {
	private static final String USER_ID = "101";
	private static final Long USER_ID_LONG = Long.parseLong(USER_ID);
	private static final String CLIENT_NAME = "some client";
	private static final String CLIENT_URI = "https://client.uri.com/index.html";
	private static final String POLICY_URI = "https://client.uri.com/policy.html";
	private static final String TOS_URI = "https://client.uri.com/termsOfService.html";
	private static final String REDIRCT_URIS_HOST = "client.com";
	private static final List<String> REDIRCT_URIS = Collections.singletonList("https://"+REDIRCT_URIS_HOST+"/redir");
	private static final List<String> REDIRCT_URIS_ALT_SUFFIX = Collections.singletonList("https://"+REDIRCT_URIS_HOST+"/new_redir");
	private static final String SECTOR_IDENTIFIER_URI_HOST = "client.uri.com";
	private static final String SECTOR_IDENTIFIER_URI_JSON_FILE_URL = "https://"+SECTOR_IDENTIFIER_URI_HOST+"/path/to/json/file";
	private static final List<String> REDIR_URI_LIST = ImmutableList.of("https://host1.com/redir1", "https://host2.com/redir2");
	private static final String OAUTH_CLIENT_ID = "123";
	private static final String OAUTH_CLIENT_ETAG = UUID.randomUUID().toString();

	@Mock
	private OAuthClientDao mockOauthClientDao;
	
	@Mock
	private AccessControlListDAO mockAclDAO;

	@Mock
	private SimpleHttpClient mockHttpClient;

	@Mock
	private SimpleHttpResponse mockHttpResponse;
	
	@Mock
	private AuthorizationManager mockAuthManager;
	
	@Mock
	private UserManager mockUserManager;
	
	@Mock
	private NotificationManager mockNotificationManager;

	@Captor
	private ArgumentCaptor<SimpleHttpRequest> simpleHttpRequestCaptor;
	
	@Captor
	private ArgumentCaptor<SectorIdentifier> sectorIdentifierCaptor;
	
	@Captor
	private ArgumentCaptor<OAuthClient> oauthClientCaptor;
	
	@Captor
	private ArgumentCaptor<AccessControlList> aclCaptor;
	
	@InjectMocks
	private OAuthClientManagerImpl oauthClientManagerImpl;
	
	private UserInfo userInfo;
	private UserInfo anonymousUserInfo;
	private  URI sector_identifier_uri;
	
	@BeforeEach
	public void setUp() throws Exception {
		userInfo = new UserInfo(false);
		userInfo.setId(USER_ID_LONG);
		userInfo.setGroups(Collections.singleton(USER_ID_LONG));

		anonymousUserInfo = new UserInfo(false);
		anonymousUserInfo.setId(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());

		sector_identifier_uri = new URI(SECTOR_IDENTIFIER_URI_JSON_FILE_URL);		
	}
	
	@Test
	public void testGetURIInvalid() throws Exception {
		// method under test
		OAuthClientManagerImpl.getUri(SECTOR_IDENTIFIER_URI_JSON_FILE_URL);
		
		String uri = "not #$%^ valid";
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			// method under test
			OAuthClientManagerImpl.getUri(uri);
		});
		
		assertEquals(uri + " is not a valid URI.", ex.getMessage());
		
	}


	@Test
	public void testValidateOAuthClientForCreateOrUpdate() {
		// happy case
		{
			OAuthClient oauthClient = createOAuthClient(USER_ID);
			OAuthClientManagerImpl.validateOAuthClientForCreateOrUpdate(oauthClient);
			
			// sector identifier uri is not required but can be set
			oauthClient.setSector_identifier_uri(SECTOR_IDENTIFIER_URI_JSON_FILE_URL);
			OAuthClientManagerImpl.validateOAuthClientForCreateOrUpdate(oauthClient);
		}

		// missing name
		{
			OAuthClient oauthClient = createOAuthClient(USER_ID);
			oauthClient.setClient_name(null);
			
			assertThrows(IllegalArgumentException.class, () -> {
				OAuthClientManagerImpl.validateOAuthClientForCreateOrUpdate(oauthClient);
			});
		}

		// missing redirect URIs
		{
			OAuthClient oauthClient = createOAuthClient(USER_ID);
			oauthClient.setRedirect_uris(null);
			
			assertThrows(IllegalArgumentException.class, () -> {
				OAuthClientManagerImpl.validateOAuthClientForCreateOrUpdate(oauthClient);
			});
			
			oauthClient.setRedirect_uris(Collections.emptyList());
			
			assertThrows(IllegalArgumentException.class, () -> {
				OAuthClientManagerImpl.validateOAuthClientForCreateOrUpdate(oauthClient);
			});
				
		}
		
		// invalid redir URI
		{
			OAuthClient oauthClient = createOAuthClient(USER_ID);
			oauthClient.setRedirect_uris(ImmutableList.of("https://client.com/redir", "not-a-valid-uri"));
			
			assertThrows(IllegalArgumentException.class, () -> {
				OAuthClientManagerImpl.validateOAuthClientForCreateOrUpdate(oauthClient);
			});
		}

		// invalid sector identifier uri
		{
			OAuthClient oauthClient = createOAuthClient(USER_ID);
			oauthClient.setSector_identifier_uri("not-a-valid-uri");
			
			assertThrows(IllegalArgumentException.class, () -> {
				OAuthClientManagerImpl.validateOAuthClientForCreateOrUpdate(oauthClient);
			});
		}

	}
	
	@Test
	public void testReadSectorIdentifierFileHappyCase() throws Exception {

		when(mockHttpResponse.getStatusCode()).thenReturn(200);
		when(mockHttpResponse.getContent()).thenReturn("[\"https://host1.com/redir1\",\"https://host2.com/redir2\"]");
		when(mockHttpClient.get((SimpleHttpRequest)any())).thenReturn(mockHttpResponse);

		// method under test
		List<String> result = oauthClientManagerImpl.readSectorIdentifierFile(sector_identifier_uri);
		
		verify(mockHttpClient).get(simpleHttpRequestCaptor.capture());
		assertEquals(SECTOR_IDENTIFIER_URI_JSON_FILE_URL, simpleHttpRequestCaptor.getValue().getUri());
		
		assertEquals(REDIR_URI_LIST, result);
	}

	@Test
	public void testReadSectorIdentifierFileHttpRequestFails() throws Exception {
		when(mockHttpResponse.getStatusCode()).thenReturn(400);
		when(mockHttpClient.get((SimpleHttpRequest)any())).thenReturn(mockHttpResponse);

		assertThrows(IllegalArgumentException.class, () -> {
			// method under test
			oauthClientManagerImpl.readSectorIdentifierFile(sector_identifier_uri);
		});
		
		verify(mockHttpClient).get(simpleHttpRequestCaptor.capture());
		assertEquals(SECTOR_IDENTIFIER_URI_JSON_FILE_URL, simpleHttpRequestCaptor.getValue().getUri());
		
		// try throwing IOException
		when(mockHttpClient.get((SimpleHttpRequest)any())).thenThrow(new IOException());
		
		assertThrows(IllegalArgumentException.class, () -> {
			// method under test
			oauthClientManagerImpl.readSectorIdentifierFile(sector_identifier_uri);
		});
		
		verify(mockHttpClient, times(2)).get(simpleHttpRequestCaptor.capture());
		assertEquals(SECTOR_IDENTIFIER_URI_JSON_FILE_URL, simpleHttpRequestCaptor.getValue().getUri());
		
	}

	@Test
	public void testReadSectorIdentifierBadFileContent() throws Exception {
		
		when(mockHttpResponse.getStatusCode()).thenReturn(200);
		when(mockHttpResponse.getContent()).thenReturn("{\"foo\":\"bar\"}");
		when(mockHttpClient.get((SimpleHttpRequest)any())).thenReturn(mockHttpResponse);
				
		assertThrows(IllegalArgumentException.class, () -> {
			// method under test
			oauthClientManagerImpl.readSectorIdentifierFile(sector_identifier_uri);
		});
		
		verify(mockHttpClient).get(simpleHttpRequestCaptor.capture());
		assertEquals(SECTOR_IDENTIFIER_URI_JSON_FILE_URL, simpleHttpRequestCaptor.getValue().getUri());
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
		assertThrows(IllegalArgumentException.class, () -> {
			// method under test
			oauthClientManagerImpl.resolveSectorIdentifier(null, 
					ImmutableList.of("https://host/redir1", "https://host/%$#@*"));
		});
	}

	@Test
	public void testResolveSectorIdentifier_NoSIURI_NULL_URI() throws Exception {	
		List<String> uris = Arrays.asList("https://host/redir1", null);
	
		assertThrows(IllegalArgumentException.class, () -> {
			// method under test
			oauthClientManagerImpl.resolveSectorIdentifier(null, uris);
		
		});
	}

	@Test
	public void testResolveSectorIdentifier_NoSIURI_DifferentHosts() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			// method under test
			oauthClientManagerImpl.resolveSectorIdentifier(null, 
					ImmutableList.of("https://host/redir1", "https://host2/redir1"));
		});
	}

	@Test
	public void testResolveSectorIdentifier_NoSIURI_NoRedirURIs() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			// method under test
			oauthClientManagerImpl.resolveSectorIdentifier(null, null);
		});
		
		assertThrows(IllegalArgumentException.class, () -> {
			// method under test
			oauthClientManagerImpl.resolveSectorIdentifier(null, Collections.emptyList());
		});
	}
	
	@Test
	public void testResolveSectorIdentifier_WithSIURI_HappyCase() throws Exception {
		
		// method under test
		String sectorIdentifier = oauthClientManagerImpl.resolveSectorIdentifier(
				SECTOR_IDENTIFIER_URI_JSON_FILE_URL, REDIR_URI_LIST);
		
		// the redir's are a subset of those in the file; the Sector Identifer is the host part of the URL pointing to the file
		assertEquals("client.uri.com", sectorIdentifier);
		
		
		// the registered URIs must be a *subset* of those listed in the files.  So this is OK too:
		// method under test
		sectorIdentifier = oauthClientManagerImpl.resolveSectorIdentifier(SECTOR_IDENTIFIER_URI_JSON_FILE_URL, 
				Collections.singletonList("https://host1.com/redir1"));
		assertEquals("client.uri.com", sectorIdentifier);
	}

	@Test
	public void testResolveSectorIdentifier_WithSIURI_InvalidFileURI() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			// method under test
			oauthClientManagerImpl.resolveSectorIdentifier("*&#%#$$@", REDIR_URI_LIST);
		});

		// scheme must be https not http
		assertThrows(IllegalArgumentException.class, () -> {
			// method under test
			oauthClientManagerImpl.resolveSectorIdentifier("http://insecure.com/file", REDIR_URI_LIST);
		});
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
	public void testCreateOpenIDConnectClient() throws Exception {

		when(mockOauthClientDao.createOAuthClient((OAuthClient)any())).then(returnsFirstArg());	
		when(mockOauthClientDao.doesSectorIdentifierExistForURI(anyString())).thenReturn(false);
		
		OAuthClient oauthClient = createOAuthClient(null);
		
		// later we test these fields are filled in.  So let's ensure they are not pre-filled.
		assertNull(oauthClient.getCreatedBy());
		assertNull(oauthClient.getEtag());
		assertNull(oauthClient.getVerified());
		assertNull(oauthClient.getSector_identifier());
		assertNull(oauthClient.getSector_identifier_uri());
		
		// mock the dao behavior of filling in the client id
		oauthClient.setClient_id(OAUTH_CLIENT_ID);
		
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
		
		verify(mockNotificationManager).sendTemplatedNotification(userInfo, "message/OAuthClientAddedNotification.html.vtl", "OAuth Client Added", 
			Map.of("clientName", CLIENT_NAME, "redirectUris", REDIRCT_URIS)
		);
		
		// make sure the ACL was created
		verify(mockAclDAO).create(aclCaptor.capture(), eq(ObjectType.OAUTH_CLIENT));
		AccessControlList acl = aclCaptor.getValue();
		
		assertEquals(OAUTH_CLIENT_ID, acl.getId());
		assertEquals(USER_ID, acl.getCreatedBy());
		assertNotNull(acl.getCreationDate());
		assertEquals(USER_ID, acl.getModifiedBy());
		assertNotNull(acl.getModifiedOn());
		assertEquals(1, acl.getResourceAccess().size());
		ResourceAccess aclEntry = acl.getResourceAccess().iterator().next();
		assertEquals(USER_ID, ""+aclEntry.getPrincipalId());
		assertEquals(AccessControlListUtils.ALLOWED_ACCESS_TYPES.get(ObjectType.OAUTH_CLIENT), aclEntry.getAccessType());
	}
	
	@Test
	public void testCreateOpenIDConnectClient_WithSectorIdentifierURI() throws Exception {
		OAuthClient oauthClient = createOAuthClient(USER_ID);
		oauthClient.setRedirect_uris(Collections.singletonList("https://host1.com/redir1"));
		oauthClient.setSector_identifier_uri(SECTOR_IDENTIFIER_URI_JSON_FILE_URL);
		
		when(mockOauthClientDao.createOAuthClient((OAuthClient)any())).then(returnsFirstArg());	
		when(mockOauthClientDao.doesSectorIdentifierExistForURI(anyString())).thenReturn(false);
		
		// method under test
		OAuthClient result = oauthClientManagerImpl.createOpenIDConnectClient(userInfo, oauthClient);
		
		assertEquals("client.uri.com", result.getSector_identifier());
		
		// make sure sector identifier was created
		verify(mockOauthClientDao).createSectorIdentifier(sectorIdentifierCaptor.capture());
		SectorIdentifier si = sectorIdentifierCaptor.getValue();
		assertEquals("client.uri.com", si.getSectorIdentifierUri());
		
		verify(mockNotificationManager).sendTemplatedNotification(userInfo, "message/OAuthClientAddedNotification.html.vtl", "OAuth Client Added", 
			Map.of("clientName", CLIENT_NAME, "redirectUris", List.of("https://host1.com/redir1"))
		);
	}
	

	@Test
	public void testCreateOpenIDConnectClient_SIAlreadyExists() throws Exception {
		OAuthClient oauthClient = createOAuthClient(USER_ID);
		
		when(mockOauthClientDao.doesSectorIdentifierExistForURI(anyString())).thenReturn(true);

		// method under test
		oauthClientManagerImpl.createOpenIDConnectClient(userInfo, oauthClient);
		
		// make sure sector identifier was created
		verify(mockOauthClientDao, never()).createSectorIdentifier((SectorIdentifier)any());

		verify(mockNotificationManager).sendTemplatedNotification(userInfo, "message/OAuthClientAddedNotification.html.vtl", "OAuth Client Added", 
			Map.of("clientName", CLIENT_NAME, "redirectUris", REDIRCT_URIS)
		);
	}
	
	@Test
	public void testCreateOpenIDConnectClient_Unauthorized() throws Exception {
		OAuthClient oauthClient = createOAuthClient(USER_ID);

		assertThrows(UnauthorizedException.class, () -> {
			// method under test
			oauthClientManagerImpl.createOpenIDConnectClient(anonymousUserInfo, oauthClient);
		});
		
		verifyZeroInteractions(mockNotificationManager);
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
		
		when(mockAuthManager.canAccess(userInfo, id, ObjectType.OAUTH_CLIENT, ACCESS_TYPE.UPDATE)).
			thenReturn(AuthorizationStatus.authorized());

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
		
		when(mockAuthManager.canAccess(anonymousUserInfo, id, ObjectType.OAUTH_CLIENT, ACCESS_TYPE.UPDATE)).
			thenReturn(AuthorizationStatus.accessDenied("not authorized"));
		
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
		
		verify(mockOauthClientDao).listOAuthClients(userInfo.getGroups(), ACCESS_TYPE.UPDATE, nextPageToken);
	}
	
	@Test
	public void testReverificationRequiredForUpdatedOpenIDConnectClient_SameUriHost() throws Exception {
		// 'created' simulates what's in the database already
		OAuthClient created = createOAuthClient(USER_ID);
		created.setClient_id(OAUTH_CLIENT_ID);
		created.setEtag(OAUTH_CLIENT_ETAG);
		created.setSector_identifier(REDIRCT_URIS_HOST);
		
		when(mockOauthClientDao.selectOAuthClientForUpdate(created.getClient_id())).thenReturn(created);
		
		when(mockAuthManager.canAccess(userInfo, OAUTH_CLIENT_ID, ObjectType.OAUTH_CLIENT, ACCESS_TYPE.UPDATE)).
			thenReturn(AuthorizationStatus.authorized());
	
		// 'toUpdate' is the object as retrieved and modified by the client
		OAuthClient toUpdate = createOAuthClient(USER_ID);
		toUpdate.setClient_id(OAUTH_CLIENT_ID);
		toUpdate.setEtag(OAUTH_CLIENT_ETAG);
		// if we just change the suffix of the REDIR URI then no reverification is needed
		toUpdate.setRedirect_uris(REDIRCT_URIS_ALT_SUFFIX);
		
		// method under test
		boolean reverificationRequired = oauthClientManagerImpl.reverificationRequiredForUpdatedOpenIDConnectClient(userInfo, toUpdate);

		assertFalse(reverificationRequired);
	}	
	
	@Test
	public void testReverificationRequiredForUpdatedOpenIDConnectClient_UriHostChanged() throws Exception {
		// 'created' simulates what's in the database already
		OAuthClient created = createOAuthClient(USER_ID);
		created.setClient_id(OAUTH_CLIENT_ID);
		created.setEtag(OAUTH_CLIENT_ETAG);
		created.setSector_identifier(REDIRCT_URIS_HOST);
		
		when(mockOauthClientDao.selectOAuthClientForUpdate(created.getClient_id())).thenReturn(created);
		
		when(mockAuthManager.canAccess(userInfo, OAUTH_CLIENT_ID, ObjectType.OAUTH_CLIENT, ACCESS_TYPE.UPDATE)).
			thenReturn(AuthorizationStatus.authorized());

		// 'toUpdate' is the object as retrieved and modified by the client
		OAuthClient toUpdate = createOAuthClient(USER_ID);
		toUpdate.setClient_id(OAUTH_CLIENT_ID);
		toUpdate.setEtag(OAUTH_CLIENT_ETAG);
		// if we change the host of the REDIR URI then reverification IS needed
		toUpdate.setRedirect_uris(Collections.singletonList("https://new.client.com/redir"));
		
		// method under test
		boolean reverificationRequired = oauthClientManagerImpl.reverificationRequiredForUpdatedOpenIDConnectClient(userInfo, toUpdate);

		assertTrue(reverificationRequired);
	}
	
	@Test
	public void testReverificationRequiredForUpdatedOpenIDConnectClient_NoClientID() throws Exception {
		// 'toUpdate' is the object as retrieved and modified by the client
		OAuthClient toUpdate = createOAuthClient(USER_ID);
		toUpdate.setClient_id(null);
		
		assertThrows(IllegalArgumentException.class, () -> {
			// method under test
			oauthClientManagerImpl.reverificationRequiredForUpdatedOpenIDConnectClient(userInfo, toUpdate);
		});
	}	
	
	@Test
	public void testReverificationRequiredForUpdatedOpenIDConnectClient_NoRedirUris() throws Exception {
		// 'created' simulates what's in the database already
		OAuthClient created = createOAuthClient(USER_ID);
		created.setClient_id(OAUTH_CLIENT_ID);
		created.setEtag(OAUTH_CLIENT_ETAG);
		created.setSector_identifier(REDIRCT_URIS_HOST);
		
		when(mockOauthClientDao.selectOAuthClientForUpdate(created.getClient_id())).thenReturn(created);
		
		when(mockAuthManager.canAccess(userInfo, OAUTH_CLIENT_ID, ObjectType.OAUTH_CLIENT, ACCESS_TYPE.UPDATE)).
			thenReturn(AuthorizationStatus.authorized());
		
		// 'toUpdate' is the object as retrieved and modified by the client
		OAuthClient toUpdate = createOAuthClient(USER_ID);
		toUpdate.setClient_id(OAUTH_CLIENT_ID);
		toUpdate.setEtag(OAUTH_CLIENT_ETAG);
		toUpdate.setRedirect_uris(Collections.EMPTY_LIST);
		
		assertThrows(IllegalArgumentException.class, () -> {
			// method under test
			oauthClientManagerImpl.reverificationRequiredForUpdatedOpenIDConnectClient(userInfo, toUpdate);
		});
	}
	
	@Test
	public void testReverificationRequiredForUpdatedOpenIDConnectClient_MismatchedEtag() throws Exception {
		// 'created' simulates what's in the database already
		OAuthClient created = createOAuthClient(USER_ID);
		created.setClient_id(OAUTH_CLIENT_ID);
		created.setEtag(OAUTH_CLIENT_ETAG);
		created.setSector_identifier(REDIRCT_URIS_HOST);
		
		when(mockOauthClientDao.selectOAuthClientForUpdate(created.getClient_id())).thenReturn(created);
		
		when(mockAuthManager.canAccess(userInfo, OAUTH_CLIENT_ID, ObjectType.OAUTH_CLIENT, ACCESS_TYPE.UPDATE)).
			thenReturn(AuthorizationStatus.authorized());

		// 'toUpdate' is the object as retrieved and modified by the client
		OAuthClient toUpdate = createOAuthClient(USER_ID);
		toUpdate.setClient_id(OAUTH_CLIENT_ID);
		toUpdate.setEtag("some-other-etag");
		toUpdate.setRedirect_uris(Collections.EMPTY_LIST);
		
		assertThrows(IllegalArgumentException.class, () -> {
			// method under test
			oauthClientManagerImpl.reverificationRequiredForUpdatedOpenIDConnectClient(userInfo, toUpdate);
		});
	}
	
	@Test
	public void testReverificationRequiredForUpdatedOpenIDConnectClient_unauthorized() throws Exception {
		when(mockAuthManager.canAccess(userInfo, OAUTH_CLIENT_ID, ObjectType.OAUTH_CLIENT, ACCESS_TYPE.UPDATE)).
			thenReturn(AuthorizationStatus.accessDenied("unauthorized"));

		// 'toUpdate' is the object as retrieved and modified by the client
		OAuthClient toUpdate = createOAuthClient(USER_ID);
		toUpdate.setClient_id(OAUTH_CLIENT_ID);
		
		assertThrows(UnauthorizedException.class, () -> {
			// method under test
			oauthClientManagerImpl.reverificationRequiredForUpdatedOpenIDConnectClient(userInfo, toUpdate);
		});
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
		when(mockUserManager.getUserInfo(any())).thenReturn(userInfo);
		when(mockAuthManager.canAccess(userInfo, OAUTH_CLIENT_ID, ObjectType.OAUTH_CLIENT, ACCESS_TYPE.UPDATE)).
			thenReturn(AuthorizationStatus.authorized());
		
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
		
		verify(mockNotificationManager).sendTemplatedNotification(userInfo, "message/OAuthClientVerificationRequiredNotification.html.vtl", "OAuth Client Verification Required",
			Map.of("clientName", "some other name")
		);
	}
	
	@Test
	public void testUpdateOpenIDConnectClientWithNoVerificationRequired() throws Exception {
		// 'created' simulates what's in the database already
		OAuthClient created = newCreatedOAuthClient();
		
		// 'toUpdate' is the object as retrieved and modified by the client
		OAuthClient toUpdate = newCreatedOAuthClient();
		
		toUpdate.setClient_name("some other name");
		toUpdate.setClient_uri("some other client uri");
		toUpdate.setPolicy_uri("some new policy URI");
		toUpdate.setTos_uri("some new TOS URI");
				
		when(mockOauthClientDao.selectOAuthClientForUpdate(created.getClient_id())).thenReturn(created);
		when(mockOauthClientDao.updateOAuthClient((OAuthClient)any())).then(returnsFirstArg());
		when(mockAuthManager.canAccess(userInfo, OAUTH_CLIENT_ID, ObjectType.OAUTH_CLIENT, ACCESS_TYPE.UPDATE)).
			thenReturn(AuthorizationStatus.authorized());
		
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
		assertEquals(toUpdate.getSector_identifier(), updated.getSector_identifier());
		assertTrue(updated.getVerified());
		verifyZeroInteractions(mockNotificationManager);
	}
	
	@Test
	public void testUpdateOpenIDConnectClient_unauthorized() throws Exception {
		// 'created' simulates what's in the database already
		OAuthClient created = newCreatedOAuthClient();
		created.setCreatedBy(userInfo.getId().toString());
		when(mockOauthClientDao.selectOAuthClientForUpdate(created.getClient_id())).thenReturn(created);
		
		// 'toUpdate' is the object as retrieved and modified by the client
		OAuthClient toUpdate = newCreatedOAuthClient();
		
		when(mockAuthManager.canAccess(anonymousUserInfo, OAUTH_CLIENT_ID, ObjectType.OAUTH_CLIENT, ACCESS_TYPE.UPDATE)).
			thenReturn(AuthorizationStatus.accessDenied("unauthorized"));
	
		assertThrows(UnauthorizedException.class, () -> {
			// method under test
			oauthClientManagerImpl.updateOpenIDConnectClient(anonymousUserInfo, toUpdate);
		});
		
		verifyZeroInteractions(mockNotificationManager);
	}
	
	@Test
	public void testUpdateOpenIDConnectClient_etagMismatch() throws Exception {
		// 'created' simulates what's in the database already
		OAuthClient created = newCreatedOAuthClient();
		created.setCreatedBy(userInfo.getId().toString());
		when(mockOauthClientDao.selectOAuthClientForUpdate(created.getClient_id())).thenReturn(created);
		when(mockAuthManager.canAccess(userInfo, OAUTH_CLIENT_ID, ObjectType.OAUTH_CLIENT, ACCESS_TYPE.UPDATE)).
			thenReturn(AuthorizationStatus.authorized());
		
		// 'toUpdate' is the object as retrieved and modified by the client
		OAuthClient toUpdate = newCreatedOAuthClient();
		toUpdate.setEtag("mismatched etag");

		assertThrows(ConflictingUpdateException.class, () -> {
			// method under test
			oauthClientManagerImpl.updateOpenIDConnectClient(userInfo, toUpdate);
		});
		
		verifyZeroInteractions(mockNotificationManager);
	}
	
	@Test
	public void testDeleteOpenIDConnectClient() {
		
		OAuthClient client = newCreatedOAuthClient();
		
		when(mockOauthClientDao.getOAuthClient(OAUTH_CLIENT_ID)).thenReturn(client);
		when(mockUserManager.getUserInfo(any())).thenReturn(userInfo);
		when(mockAuthManager.canAccess(userInfo, OAUTH_CLIENT_ID, ObjectType.OAUTH_CLIENT, ACCESS_TYPE.DELETE)).
			thenReturn(AuthorizationStatus.authorized());
		
		// method under test
		oauthClientManagerImpl.deleteOpenIDConnectClient(userInfo, OAUTH_CLIENT_ID);
		verify(mockOauthClientDao).deleteOAuthClient(OAUTH_CLIENT_ID);
		verify(mockUserManager).getUserInfo(USER_ID_LONG);
		verify(mockNotificationManager).sendTemplatedNotification(userInfo, "message/OAuthClientRemovedNotification.html.vtl", "OAuth Client Removed",
			Map.of("clientName", client.getClient_name())
		);
		
		// make sure the ACL was deleted too
		verify(mockAclDAO).delete(OAUTH_CLIENT_ID, ObjectType.OAUTH_CLIENT);
	}
	
	@Test
	public void testDeleteOpenIDConnectClient_Unauthorized() {
		when(mockAuthManager.canAccess(userInfo, OAUTH_CLIENT_ID, ObjectType.OAUTH_CLIENT, ACCESS_TYPE.DELETE)).
			thenReturn(AuthorizationStatus.accessDenied("unauthorized"));

		assertThrows(UnauthorizedException.class, () -> {
			// method under test
			oauthClientManagerImpl.deleteOpenIDConnectClient(userInfo, OAUTH_CLIENT_ID);
		});
		
		verify(mockOauthClientDao, never()).deleteOAuthClient(OAUTH_CLIENT_ID);
		verifyZeroInteractions(mockNotificationManager);
	}
	
	@Test
	public void testCreateClientSecret() {
		
		OAuthClient client = newCreatedOAuthClient();
		
		when(mockOauthClientDao.getOAuthClient(OAUTH_CLIENT_ID)).thenReturn(client);
		when(mockUserManager.getUserInfo(any())).thenReturn(userInfo);
		when(mockAuthManager.canAccess(userInfo, OAUTH_CLIENT_ID, ObjectType.OAUTH_CLIENT, ACCESS_TYPE.UPDATE)).
			thenReturn(AuthorizationStatus.authorized());
		
		// method under test
		OAuthClientIdAndSecret idAndSecret = oauthClientManagerImpl.createClientSecret(userInfo, OAUTH_CLIENT_ID);
		
		assertEquals(OAUTH_CLIENT_ID, idAndSecret.getClient_id());
		assertNotNull(idAndSecret.getClient_secret());
		
		verify(mockOauthClientDao).setOAuthClientSecretHash(eq(OAUTH_CLIENT_ID), anyString(), anyString());
		verify(mockUserManager).getUserInfo(USER_ID_LONG);
		verify(mockNotificationManager).sendTemplatedNotification(userInfo, "message/OAuthClientSecretGeneratedNotification.html.vtl", "OAuth Client Secret Generated", 
			Map.of("clientName", client.getClient_name())
		);
	}

	@Test
	public void testCreateClientSecret_unauthorized() {
		when(mockAuthManager.canAccess(anonymousUserInfo, OAUTH_CLIENT_ID, ObjectType.OAUTH_CLIENT, ACCESS_TYPE.UPDATE)).
			thenReturn(AuthorizationStatus.accessDenied("unauthorized"));
		
		assertThrows(UnauthorizedException.class, () -> {
			// method under test
			oauthClientManagerImpl.createClientSecret(anonymousUserInfo, OAUTH_CLIENT_ID);
		});

		verify(mockOauthClientDao, never()).setOAuthClientSecretHash(eq(OAUTH_CLIENT_ID), anyString(), anyString());
		verifyZeroInteractions(mockNotificationManager);
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
		when(mockOauthClientDao.getSecretSalt(wrongClientId)).thenThrow(new NotFoundException(""));

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
		String etag = OAUTH_CLIENT_ETAG;
		
		IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			// Method under test
			oauthClientManagerImpl.updateOpenIDConnectClientVerifiedStatus(userInfo, clientId, etag, verifiedStatus);
		});
		
		assertEquals("User info is required.", ex.getMessage());
		
		verifyZeroInteractions(mockNotificationManager);
	}
	
	@Test
	public void testUpdateOpenIDConnectClientVerifiedStatusWithWrongClientId() {
		boolean verifiedStatus = true;
		String etag = OAUTH_CLIENT_ETAG;
		
		IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			// Method under test
			oauthClientManagerImpl.updateOpenIDConnectClientVerifiedStatus(userInfo, "", etag, verifiedStatus);
		});
		
		assertEquals("Client ID is required and must not be the empty string.", ex.getMessage());
		
		ex = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			// Method under test
			oauthClientManagerImpl.updateOpenIDConnectClientVerifiedStatus(userInfo, "    ", etag, verifiedStatus);
		});
		
		assertEquals("Client ID is required and must not be a blank string.", ex.getMessage());
		
		verifyZeroInteractions(mockNotificationManager);
	}
	
	@Test
	public void testUpdateOpenIDConnectClientVerifiedStatusWithWrongCredentials() {
		boolean verifiedStatus = true;
		String clientId = OAUTH_CLIENT_ID;
		String etag = OAUTH_CLIENT_ETAG;
		
		when(mockAuthManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(false);
		
		Assertions.assertThrows(UnauthorizedException.class, () -> {
			// Method under test
			oauthClientManagerImpl.updateOpenIDConnectClientVerifiedStatus(userInfo, clientId, etag, verifiedStatus);
		});
		
		verify(mockAuthManager).isACTTeamMemberOrAdmin(userInfo);
		verifyZeroInteractions(mockOauthClientDao);
		verifyZeroInteractions(mockNotificationManager);
		
	}
	
	@Test
	public void testUpdateOpenIDConnectClientVerifiedStatusWithEmptyEtag() {
		boolean verifiedStatus = true;
		String clientId = OAUTH_CLIENT_ID;
		String etag = "  ";
		
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			// Method under test
			oauthClientManagerImpl.updateOpenIDConnectClientVerifiedStatus(userInfo, clientId, etag, verifiedStatus);
		});
		
		verifyZeroInteractions(mockOauthClientDao);
		verifyZeroInteractions(mockNotificationManager);
		
	}	

	@Test
	public void testUpdateOpenIDConnectClientVerifiedStatusWithConflictingEtag() {
		OAuthClient mockClient = newCreatedOAuthClient();
		
		boolean verifiedStatus = true;
		String clientId = mockClient.getClient_id();
		String etag = UUID.randomUUID().toString();
		
		when(mockAuthManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(true);
		when(mockOauthClientDao.selectOAuthClientForUpdate(clientId)).thenReturn(mockClient);
		
		Assertions.assertThrows(ConflictingUpdateException.class, () -> {
			// Method under test
			oauthClientManagerImpl.updateOpenIDConnectClientVerifiedStatus(userInfo, clientId, etag, verifiedStatus);
		});
		
		verify(mockAuthManager).isACTTeamMemberOrAdmin(userInfo);
		verify(mockOauthClientDao).selectOAuthClientForUpdate(clientId);
		verify(mockOauthClientDao, times(0)).updateOAuthClient(any());
		verifyZeroInteractions(mockNotificationManager);
		
	}
	
	@Test
	public void testUpdateOpenIDConnectClientVerifiedStatusNoChange() {
		OAuthClient mockClient = newCreatedOAuthClient();
		
		String clientId = OAUTH_CLIENT_ID;
		boolean verifiedStatus = true;
		
		mockClient.setVerified(verifiedStatus);
		String etag = mockClient.getEtag();
		
		when(mockAuthManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(true);
		when(mockOauthClientDao.selectOAuthClientForUpdate(clientId)).thenReturn(mockClient);
		
		// Method under test
		oauthClientManagerImpl.updateOpenIDConnectClientVerifiedStatus(userInfo, clientId, etag, verifiedStatus);
		
		verify(mockAuthManager).isACTTeamMemberOrAdmin(userInfo);
		verify(mockOauthClientDao).selectOAuthClientForUpdate(clientId);
		verify(mockOauthClientDao, times(0)).updateOAuthClient(any());
		verifyZeroInteractions(mockNotificationManager);
		
	}
	
	@Test
	public void testUpdateOpenIDConnectClientVerifiedStatus() throws Exception {
		OAuthClient originalClient = newCreatedOAuthClient();
		
		String clientId = OAUTH_CLIENT_ID;
		
		Date originalModifiedOn = Date.from(Instant.now().minusSeconds(60));
		String originalEtag = originalClient.getEtag();
		boolean originalVerifiedStatus = false;
		
		originalClient.setModifiedOn(originalModifiedOn);
		originalClient.setEtag(originalEtag);
		originalClient.setVerified(originalVerifiedStatus);
		
		when(mockAuthManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(true);
		when(mockOauthClientDao.selectOAuthClientForUpdate(clientId)).thenReturn(originalClient);
		when(mockOauthClientDao.updateOAuthClient((OAuthClient)any())).then(returnsFirstArg());
		when(mockUserManager.getUserInfo(any())).thenReturn(userInfo);
		
		when(mockHttpResponse.getStatusCode()).thenReturn(200);
		when(mockHttpResponse.getContent()).thenReturn("[\""+REDIRCT_URIS.get(0)+"\"]");
		when(mockHttpClient.get((SimpleHttpRequest)any())).thenReturn(mockHttpResponse);
		
		// Method under test
		oauthClientManagerImpl.updateOpenIDConnectClientVerifiedStatus(userInfo, clientId, originalEtag, !originalVerifiedStatus);
		
		verify(mockAuthManager).isACTTeamMemberOrAdmin(userInfo);
		verify(mockOauthClientDao).selectOAuthClientForUpdate(clientId);
		verify(mockOauthClientDao).updateOAuthClient(oauthClientCaptor.capture());
		
		OAuthClient updated = oauthClientCaptor.getValue();
		
		assertNotEquals(originalModifiedOn, updated.getModifiedOn());
		assertNotEquals(originalEtag, updated.getEtag());
		assertNotEquals(originalVerifiedStatus, updated.getVerified());
		
		verify(mockNotificationManager).sendTemplatedNotification(userInfo, "message/OAuthClientVerifiedNotification.html.vtl", "OAuth Client Verified", 
			Map.of("clientName", CLIENT_NAME)
		);
		
	}
	
	@Test
	public void testUpdateOpenIDConnectClientVerifiedStatusSectorIdentifierFileHasWrongURIs() throws Exception {
		OAuthClient originalClient = newCreatedOAuthClient();
		
		String clientId = OAUTH_CLIENT_ID;
		
		Date originalModifiedOn = Date.from(Instant.now().minusSeconds(60));
		String originalEtag = originalClient.getEtag();
		boolean originalVerifiedStatus = false;
		
		originalClient.setModifiedOn(originalModifiedOn);
		originalClient.setEtag(originalEtag);
		originalClient.setVerified(originalVerifiedStatus);
		
		when(mockAuthManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(true);
		when(mockOauthClientDao.selectOAuthClientForUpdate(clientId)).thenReturn(originalClient);
		
		when(mockHttpResponse.getStatusCode()).thenReturn(200);
		when(mockHttpResponse.getContent()).thenReturn("[\"https://wrong.uri.com\"]");
		when(mockHttpClient.get((SimpleHttpRequest)any())).thenReturn(mockHttpResponse);
		
		// Method under test
		assertThrows(IllegalArgumentException.class, () -> {
			oauthClientManagerImpl.updateOpenIDConnectClientVerifiedStatus(userInfo, clientId, originalEtag, !originalVerifiedStatus);
		});
		
		verify(mockAuthManager).isACTTeamMemberOrAdmin(userInfo);
		verify(mockOauthClientDao).selectOAuthClientForUpdate(clientId);
		verify(mockOauthClientDao, never()).updateOAuthClient(oauthClientCaptor.capture());
		verify(mockNotificationManager, never()).sendTemplatedNotification(eq(userInfo), anyString(),  anyString(), (Map<String,Object>)any());
	}
	
	@Test
	public void testUpdateOpenIDConnectClientVerifiedStatusMissingSectorIdentifierFile() throws Exception {
		OAuthClient originalClient = newCreatedOAuthClient();
		
		String clientId = OAUTH_CLIENT_ID;
		
		Date originalModifiedOn = Date.from(Instant.now().minusSeconds(60));
		String originalEtag = originalClient.getEtag();
		boolean originalVerifiedStatus = false;
		
		originalClient.setModifiedOn(originalModifiedOn);
		originalClient.setEtag(originalEtag);
		originalClient.setVerified(originalVerifiedStatus);
		
		when(mockAuthManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(true);
		when(mockOauthClientDao.selectOAuthClientForUpdate(clientId)).thenReturn(originalClient);
		
		when(mockHttpResponse.getStatusCode()).thenReturn(404);
		when(mockHttpClient.get((SimpleHttpRequest)any())).thenReturn(mockHttpResponse);
		
		// Method under test
		assertThrows(IllegalArgumentException.class, () -> {
			oauthClientManagerImpl.updateOpenIDConnectClientVerifiedStatus(userInfo, clientId, originalEtag, !originalVerifiedStatus);
		});
		
		verify(mockAuthManager).isACTTeamMemberOrAdmin(userInfo);
		verify(mockOauthClientDao).selectOAuthClientForUpdate(clientId);
		verify(mockOauthClientDao, never()).updateOAuthClient(oauthClientCaptor.capture());
		verify(mockNotificationManager, never()).sendTemplatedNotification(eq(userInfo), anyString(),  anyString(), (Map<String,Object>)any());
	}
	
	@Test
	public void testGetClientACLHappyCase() throws Exception {
		AccessControlList expected = new AccessControlList();
		expected.setId(OAUTH_CLIENT_ID);
		expected.setCreatedBy(USER_ID);
		expected.setCreationDate(new Date());
		when(mockAclDAO.get(OAUTH_CLIENT_ID, ObjectType.OAUTH_CLIENT)).thenReturn(expected);
		
		when(mockAuthManager.canAccess(userInfo, OAUTH_CLIENT_ID, ObjectType.OAUTH_CLIENT, ACCESS_TYPE.READ)).
			thenReturn(AuthorizationStatus.authorized());
		
		// method under test
		AccessControlList actual = oauthClientManagerImpl.getAccessControlList(userInfo, OAUTH_CLIENT_ID);
		
		verify(mockAclDAO).get(OAUTH_CLIENT_ID, ObjectType.OAUTH_CLIENT);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testGetClientACLForbidden() throws Exception {		
		when(mockAuthManager.canAccess(anonymousUserInfo, OAUTH_CLIENT_ID, ObjectType.OAUTH_CLIENT, ACCESS_TYPE.READ)).
		thenReturn(AuthorizationStatus.accessDenied("anonymous"));
	
		// method under test
		assertThrows(UnauthorizedException.class, () -> {
			oauthClientManagerImpl.getAccessControlList(anonymousUserInfo, OAUTH_CLIENT_ID);
		});

	}
	
	@Test
	public void testUpdateClientACLHappyPath() throws Exception {
		AccessControlList acl = new AccessControlList();
		acl.setId(OAUTH_CLIENT_ID);
		acl.setCreationDate(new Date());
		ResourceAccess ra = new ResourceAccess();
		ra.setPrincipalId(USER_ID_LONG);
		ra.setAccessType(Collections.singleton(ACCESS_TYPE.CHANGE_PERMISSIONS));
		Set<ResourceAccess> ras = Collections.singleton(ra);
		acl.setResourceAccess(ras);
		
		when(mockAuthManager.canAccess(userInfo, OAUTH_CLIENT_ID, ObjectType.OAUTH_CLIENT, ACCESS_TYPE.CHANGE_PERMISSIONS)).
			thenReturn(AuthorizationStatus.authorized());
		
		OAuthClient oauthClient = createOAuthClient(USER_ID);
		when(mockOauthClientDao.getOAuthClient(OAUTH_CLIENT_ID)).thenReturn(oauthClient);
		
		// method under test
		oauthClientManagerImpl.updateAccessControlList(userInfo, OAUTH_CLIENT_ID, acl);
		
		verify(mockAclDAO).update(acl, ObjectType.OAUTH_CLIENT);
		verify(mockAclDAO).get(OAUTH_CLIENT_ID, ObjectType.OAUTH_CLIENT);
		
	}
	
	@Test
	public void testUpdateClientACLForbidden() throws Exception {		
		AccessControlList acl = new AccessControlList();
		acl.setId(OAUTH_CLIENT_ID);
		when(mockAuthManager.canAccess(anonymousUserInfo, OAUTH_CLIENT_ID, ObjectType.OAUTH_CLIENT, ACCESS_TYPE.CHANGE_PERMISSIONS)).
			thenReturn(AuthorizationStatus.accessDenied("anonymous"));
	
		// method under test
		assertThrows(UnauthorizedException.class, () -> {
			oauthClientManagerImpl.updateAccessControlList(anonymousUserInfo, OAUTH_CLIENT_ID, acl);
		});

	}
	
	private static OAuthClient createOAuthClient(String userId) {
		OAuthClient result = new OAuthClient();
		result.setCreatedBy(userId);
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
	

	// create a fully populated object, to be updated
	private static OAuthClient newCreatedOAuthClient() {
		OAuthClient oauthClient = createOAuthClient(USER_ID);
		oauthClient.setClient_id(OAUTH_CLIENT_ID);
		oauthClient.setCreatedBy(USER_ID);
		oauthClient.setEtag(OAUTH_CLIENT_ETAG);
		oauthClient.setSector_identifier_uri(SECTOR_IDENTIFIER_URI_JSON_FILE_URL);
		oauthClient.setSector_identifier(SECTOR_IDENTIFIER_URI_HOST);
		oauthClient.setVerified(true);
		return oauthClient;
	}
}
