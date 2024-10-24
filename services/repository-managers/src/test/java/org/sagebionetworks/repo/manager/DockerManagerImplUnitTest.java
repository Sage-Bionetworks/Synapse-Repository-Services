package org.sagebionetworks.repo.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.model.oauth.OAuthScope.download;
import static org.sagebionetworks.repo.model.oauth.OAuthScope.modify;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.oauth.ClaimsJsonUtil;
import org.sagebionetworks.repo.manager.oauth.OIDCTokenManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DockerCommitDao;
import org.sagebionetworks.repo.model.DockerNodeDao;
import org.sagebionetworks.repo.model.EntityId;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.docker.DockerAuthorizationToken;
import org.sagebionetworks.repo.model.docker.DockerCommit;
import org.sagebionetworks.repo.model.docker.DockerCommitSortBy;
import org.sagebionetworks.repo.model.docker.DockerRegistryEventList;
import org.sagebionetworks.repo.model.docker.DockerRepository;
import org.sagebionetworks.repo.model.docker.RegistryEventAction;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.DockerRegistryEventUtil;

import com.google.common.collect.ImmutableList;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;


@ExtendWith(MockitoExtension.class)
public class DockerManagerImplUnitTest {
	
	private static final long PARENT_ID_LONG = 98765L;
	private static final String PARENT_ID = "syn"+PARENT_ID_LONG;
	
	private static final long REPO_ENTITY_ID_LONG = 123456L;
	private static final String REPO_ENTITY_ID = "syn"+REPO_ENTITY_ID_LONG;
	
	private static final long USER_ID = 111L;

	private static final UserInfo USER_INFO = new UserInfo(false, USER_ID);
	
	private static final String REGISTRY_HOST = "docker.synapse.org";
	private static final String SERVICE = REGISTRY_HOST;
	private static final String TYPE = "repository";
	private static final String REPOSITORY_PATH = PARENT_ID+"/reponame";
	private static final String REPOSITORY_NAME = SERVICE+"/"+REPOSITORY_PATH;
	private static final String ACCESS_TYPES_STRING="push,pull";
	
	private static final String TAG = "v1";
	private static final String DIGEST = "sha256:8900ee859c6808c9f83ce51bf44b508df63d1f2e8a839ca230471f1bac90ee19";
	
	private static final String MEDIA_TYPE = DockerManagerImpl.MANIFEST_MEDIA_TYPE;
	
	private static final String OAUTH_ACCESS_TOKEN = "access token";
	
	@Mock
	private NodeDAO nodeDAO;
	
	@Mock
	private DockerNodeDao dockerNodeDao;
	
	@Mock
	private DockerCommitDao dockerCommitDao;
	
	@Mock
	private UserManager userManager;
	
	@Mock
	private EntityManager entityManager;
	
	@Mock
	private AuthorizationManager authorizationManager;
	
	@Mock
	private TransactionalMessenger transactionalMessenger;
	
	@Mock
	private OIDCTokenManager oidcTokenManager;
	
	@Mock
	private Jwt<JwsHeader,Claims> mockJwt;
	
	@Mock
	private StackConfiguration mockConfig;
	
	@InjectMocks
	private DockerManagerImpl dockerManager;

	@BeforeEach
	public void before() throws Exception {
		
	}

	@Test
	public void testAuthorizeDockerAccessNullUserInfo() throws Exception{
		assertThrows(IllegalArgumentException.class, () -> {			
			dockerManager.authorizeDockerAccess(null, OAUTH_ACCESS_TOKEN, SERVICE, new ArrayList<String>());
		});
	}
	
	@Test
	public void testAuthorizeDockerAccessNullService() throws Exception{
		assertThrows(IllegalArgumentException.class, () -> {
			dockerManager.authorizeDockerAccess(USER_INFO, OAUTH_ACCESS_TOKEN, null, new ArrayList<String>());
		});
	}
	
	@Test
	public void testAuthorizeDockerAccess() throws Exception {
		Claims claims = Jwts.claims();		
		ClaimsJsonUtil.addAccessClaims(ImmutableList.of(download,modify), Collections.EMPTY_MAP, claims);
		when(mockJwt.getBody()).thenReturn(claims);
		
		when(oidcTokenManager.parseJWT(OAUTH_ACCESS_TOKEN)).thenReturn(mockJwt);
		
		List<String> scope = new ArrayList<String>();
		scope.add(TYPE+":"+REPOSITORY_PATH+":"+ACCESS_TYPES_STRING);
		
		// method under test:
		DockerAuthorizationToken token = dockerManager.
				authorizeDockerAccess(USER_INFO, OAUTH_ACCESS_TOKEN, SERVICE, scope);
		
		assertNotNull(token.getToken());
	}
	
	@Test
	public void testAuthorizeDockerListCatalog() throws Exception {
				
		Claims claims = Jwts.claims();		
		ClaimsJsonUtil.addAccessClaims(ImmutableList.of(download,modify), Collections.EMPTY_MAP, claims);
		when(mockJwt.getBody()).thenReturn(claims);
		
		when(oidcTokenManager.parseJWT(OAUTH_ACCESS_TOKEN)).thenReturn(mockJwt);
		
		List<String> scope = new ArrayList<String>();
		scope.add("registry:catalog:*");
		
		// method under test:
		DockerAuthorizationToken token = dockerManager.
				authorizeDockerAccess(USER_INFO, OAUTH_ACCESS_TOKEN, SERVICE, scope);
		
		assertNotNull(token.getToken());
	}
	
	// Docker login calls the authorization service, but without a 'scope' param
	@Test
	public void testDockerLogin() throws Exception {
		// method under test:
		DockerAuthorizationToken token = dockerManager.
				authorizeDockerAccess(USER_INFO, OAUTH_ACCESS_TOKEN, SERVICE, null);
		
		assertNotNull(token.getToken());
	}
	

	
	@Test
	public void testDockerRegistryNotificationPushNEWEntity() {
		when(nodeDAO.getNodeTypeById(any())).thenReturn(EntityType.project);
		when(userManager.getUserInfo(any())).thenReturn(USER_INFO);
		when(entityManager.createEntity(any(), any(), any())).thenReturn(REPO_ENTITY_ID);
		when(mockConfig.getDockerRegistryHosts()).thenReturn(Arrays.asList(REGISTRY_HOST));
		when(dockerNodeDao.getEntityIdForRepositoryName(REPOSITORY_NAME)).thenReturn(null);

		DockerRegistryEventList events = 
				DockerRegistryEventUtil.createDockerRegistryEvent(RegistryEventAction.push, REGISTRY_HOST, USER_ID, REPOSITORY_PATH, TAG, DIGEST, MEDIA_TYPE);
		
		// method under test:
		dockerManager.dockerRegistryNotification(events);
		
		ArgumentCaptor<DockerRepository> repo = ArgumentCaptor.forClass(DockerRepository.class);
		verify(entityManager).createEntity(eq(USER_INFO), repo.capture(), (String)eq(null));
		assertEquals(PARENT_ID, repo.getValue().getParentId());
		assertTrue(repo.getValue().getIsManaged());
		assertEquals(REPOSITORY_NAME, repo.getValue().getRepositoryName());
		// the repo should be created with a null ID and name.
		assertEquals(null, repo.getValue().getId());
		assertEquals(null, repo.getValue().getName());
		
		// verify that commit was added
		ArgumentCaptor<DockerCommit> captureCommit = ArgumentCaptor.forClass(DockerCommit.class);
		verify(dockerCommitDao).createDockerCommit(eq(REPO_ENTITY_ID), eq(USER_ID), captureCommit.capture());
		DockerCommit commit = captureCommit.getValue();
		assertTrue(System.currentTimeMillis()-commit.getCreatedOn().getTime()<1000L);
		assertEquals(DIGEST, commit.getDigest());
		assertEquals(TAG, commit.getTag());
	}

	
	@Test
	public void testDockerRegistryNotificationPushExistingEntity() {
		when(mockConfig.getDockerRegistryHosts()).thenReturn(Arrays.asList(REGISTRY_HOST));
		when(dockerNodeDao.getEntityIdForRepositoryName(REPOSITORY_NAME)).thenReturn(REPO_ENTITY_ID);
		
		DockerRegistryEventList events = 
				DockerRegistryEventUtil.createDockerRegistryEvent(RegistryEventAction.push, REGISTRY_HOST, USER_ID, REPOSITORY_PATH, TAG, DIGEST, MEDIA_TYPE);
		
		// method under test:
		dockerManager.dockerRegistryNotification(events);
		
		// no create operation, since the repo already exists
		verify(entityManager, never()).createEntity(any(), any(), any());
		
		// verify that commit was added
		ArgumentCaptor<DockerCommit> captureCommit = ArgumentCaptor.forClass(DockerCommit.class);
		verify(dockerCommitDao).createDockerCommit(eq(REPO_ENTITY_ID), eq(USER_ID), captureCommit.capture());
		DockerCommit commit = captureCommit.getValue();
		assertTrue(System.currentTimeMillis()-commit.getCreatedOn().getTime()<1000L);
		assertEquals(DIGEST, commit.getDigest());
		assertEquals(TAG, commit.getTag());
	}
	
	@Test
	public void testDockerRegistryNotificationPushUnsupportedHost() {
		DockerRegistryEventList events = 
				DockerRegistryEventUtil.createDockerRegistryEvent(RegistryEventAction.push, "quay.io", USER_ID, REPOSITORY_PATH, TAG, DIGEST, MEDIA_TYPE);
		
		// method under test:
		dockerManager.dockerRegistryNotification(events);
		
		verify(entityManager, never()).createEntity(any(), any(), anyString());
		verify(dockerCommitDao, never()).createDockerCommit(anyString(), anyLong(), any());
	}
	
	@Test
	public void testDockerRegistryNotificationPushNotManifestMediaType() {

		DockerRegistryEventList events = 
				DockerRegistryEventUtil.createDockerRegistryEvent(RegistryEventAction.push, REGISTRY_HOST, USER_ID, REPOSITORY_PATH, TAG, DIGEST, "application/octet-stream");
		
		// method under test:
		dockerManager.dockerRegistryNotification(events);
		
		verify(entityManager, never()).createEntity(any(), any(), anyString());
		verify(dockerCommitDao, never()).createDockerCommit(anyString(), anyLong(), any());
	}
	
	
	@Test
	public void testDockerRegistryNotificationPushNEWEntityParentIsFolder() {
		
		when(mockConfig.getDockerRegistryHosts()).thenReturn(Arrays.asList(REGISTRY_HOST));
		when(dockerNodeDao.getEntityIdForRepositoryName(REPOSITORY_NAME)).thenReturn(null);
		when(nodeDAO.getNodeTypeById(PARENT_ID)).thenReturn(EntityType.folder);

		DockerRegistryEventList events = 
				DockerRegistryEventUtil.createDockerRegistryEvent(RegistryEventAction.push, REGISTRY_HOST, USER_ID, REPOSITORY_PATH, TAG, DIGEST, MEDIA_TYPE);

		assertThrows(IllegalArgumentException.class, () -> {
			// method under test:
			dockerManager.dockerRegistryNotification(events);
		});
	}

	@Test
	public void testDockerRegistryNotificationPull() {
		DockerRegistryEventList events = 
				DockerRegistryEventUtil.createDockerRegistryEvent(RegistryEventAction.pull, REGISTRY_HOST, USER_ID, REPOSITORY_PATH, TAG, DIGEST, MEDIA_TYPE);
		dockerManager.dockerRegistryNotification(events);
		// no create operation, since the repo already exists
		verify(entityManager, never()).createEntity(any(), any(), anyString());
		verify(dockerCommitDao, never()).createDockerCommit(anyString(), anyLong(), any());
	}
	@Test
	public void testDockerRegistryNotificationMount() {
		DockerRegistryEventList events = 
				DockerRegistryEventUtil.createDockerRegistryEvent(RegistryEventAction.mount, REGISTRY_HOST, USER_ID, REPOSITORY_PATH, TAG, DIGEST, MEDIA_TYPE);
		dockerManager.dockerRegistryNotification(events);
		// no create operation, since the repo already exists
		verify(entityManager, never()).createEntity(any(), any(), anyString());
		verify(dockerCommitDao, never()).createDockerCommit(anyString(), anyLong(), any());
	}
	
	private static DockerCommit createCommit() {
		DockerCommit commit = new DockerCommit();
		Date createdOn = new Date();
		commit.setCreatedOn(createdOn);
		commit.setDigest(DIGEST);
		commit.setTag(TAG);
		return commit;
	}
	
	@Test
	public void testAddDockerCommitToUnmanagedRespository() {
		
		when(authorizationManager.canAccess(
				eq(USER_INFO), eq(REPO_ENTITY_ID), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.UPDATE))).
				thenReturn(AuthorizationStatus.authorized());
		
		DockerCommit commit = createCommit();
		
		// method under test
		dockerManager.addDockerCommitToUnmanagedRespository(USER_INFO, REPO_ENTITY_ID, commit);
		
		verify(dockerCommitDao).createDockerCommit(REPO_ENTITY_ID, USER_ID, commit);
		verify(transactionalMessenger).sendMessageAfterCommit(
				eq(REPO_ENTITY_ID), eq(ObjectType.ENTITY), eq(ChangeType.UPDATE));
	}
	
	@Test
	public void testAddDockerCommitToMANANGEDRespository() {
		// this makes the repo a *managed* repo
		when (dockerNodeDao.getRepositoryNameForEntityId(REPO_ENTITY_ID)).thenReturn(REPOSITORY_NAME);
		DockerCommit commit = createCommit();

		assertThrows(IllegalArgumentException.class, () -> {
			// method under test
			dockerManager.addDockerCommitToUnmanagedRespository(USER_INFO, REPO_ENTITY_ID, commit);
		});
	}
	
	@Test
	public void testAddDockerCommitToUnmanagedRespositoryUNAUTHORIZED() {
		
		when(authorizationManager.canAccess(
				eq(USER_INFO), eq(REPO_ENTITY_ID), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.UPDATE))).
				thenReturn(AuthorizationStatus.accessDenied(""));
		
		DockerCommit commit = createCommit();

		assertThrows(UnauthorizedException.class, () -> {
			// method under test
			dockerManager.addDockerCommitToUnmanagedRespository(USER_INFO, REPO_ENTITY_ID, commit);
		});
	}
	
	@Test
	public void listDockerCommitsHappyCase() {
		
		when(entityManager.getEntityType(USER_INFO, REPO_ENTITY_ID)).thenReturn(EntityType.dockerrepo);
		
		when(authorizationManager.canAccess(
				eq(USER_INFO), eq(REPO_ENTITY_ID), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.READ))).
				thenReturn(AuthorizationStatus.authorized());
		
		
		List<DockerCommit> commits = new ArrayList<DockerCommit>();
		commits.add(createCommit());
		commits.add(createCommit());
		
		when(dockerCommitDao.listDockerTags(REPO_ENTITY_ID, DockerCommitSortBy.CREATED_ON, /*ascending*/true, 10, 0)).
			thenReturn(commits);

		// method under test
		PaginatedResults<DockerCommit> pgs = dockerManager.listDockerTags(
				USER_INFO, REPO_ENTITY_ID, DockerCommitSortBy.CREATED_ON, /*ascending*/true, 10, 0);
		
		assertEquals(2L, pgs.getTotalNumberOfResults());
		assertEquals(commits, pgs.getResults());
	}
	
	@Test
	public void listDockerCommitsUNAUTHORIZED() {
		
		when(authorizationManager.canAccess(
				eq(USER_INFO), eq(REPO_ENTITY_ID), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.READ))).
				thenReturn(AuthorizationStatus.accessDenied(""));

		assertThrows(UnauthorizedException.class, () -> {
			// method under test
			dockerManager.listDockerTags(USER_INFO, REPO_ENTITY_ID, DockerCommitSortBy.CREATED_ON, /*ascending*/true, 10, 0);
		});
	}
	
	@Test
	public void listDockerCommitsforNONrepo() {
		when(entityManager.getEntityType(USER_INFO, REPO_ENTITY_ID)).thenReturn(EntityType.dockerrepo);
		
		when(authorizationManager.canAccess(
				eq(USER_INFO), eq(REPO_ENTITY_ID), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.READ))).
				thenReturn(AuthorizationStatus.authorized());
		
		when(entityManager.getEntityType(USER_INFO, REPO_ENTITY_ID)).thenReturn(EntityType.project);
		
		assertThrows(IllegalArgumentException.class, () -> {
			// method under test
			dockerManager.listDockerTags(USER_INFO, REPO_ENTITY_ID, DockerCommitSortBy.CREATED_ON, /*ascending*/true, 10, 0);
		});
	}
	
	@Test
	public void testGetEntityIdForRepositoryName() {
		when(dockerNodeDao.getEntityIdForRepositoryName(any())).thenReturn(REPO_ENTITY_ID);
		when(authorizationManager.canAccess(any(), any(), any(), any())).thenReturn(AuthorizationStatus.authorized());
		
		EntityId expected = new EntityId().setId(REPO_ENTITY_ID);
		
		EntityId result = dockerManager.getEntityIdForRepositoryName(USER_INFO, REPOSITORY_NAME);
		
		assertEquals(expected, result);
		
		verify(dockerNodeDao).getEntityIdForRepositoryName(REPOSITORY_NAME);
		verify(authorizationManager).canAccess(USER_INFO, REPO_ENTITY_ID, ObjectType.ENTITY, ACCESS_TYPE.READ);
	}
	
	@Test
	public void testGetEntityIdForRepositoryNameAndNotFound() {
		when(dockerNodeDao.getEntityIdForRepositoryName(any())).thenReturn(null);
		
		String result = assertThrows(NotFoundException.class, () -> {			
			dockerManager.getEntityIdForRepositoryName(USER_INFO, REPOSITORY_NAME);
		}).getMessage();
		
		assertEquals("A repository named docker.synapse.org/syn98765/reponame does not exist or it is not a managed repository.", result);
		
		verify(dockerNodeDao).getEntityIdForRepositoryName(REPOSITORY_NAME);
	}
	
	@Test
	public void testGetEntityIdForRepositoryNameAndNotAuthorized() {
		when(dockerNodeDao.getEntityIdForRepositoryName(any())).thenReturn(REPO_ENTITY_ID);
		when(authorizationManager.canAccess(any(), any(), any(), any())).thenReturn(AuthorizationStatus.accessDenied("denied"));
		
		String result = assertThrows(UnauthorizedException.class, () -> {			
			dockerManager.getEntityIdForRepositoryName(USER_INFO, REPOSITORY_NAME);
		}).getMessage();
		
		assertEquals("denied", result);
		
		verify(dockerNodeDao).getEntityIdForRepositoryName(REPOSITORY_NAME);
		verify(authorizationManager).canAccess(USER_INFO, REPO_ENTITY_ID, ObjectType.ENTITY, ACCESS_TYPE.READ);
	}

	@Test
	public void testGetEntityIdForRepositoryNameAndNoUser() {
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			dockerManager.getEntityIdForRepositoryName(null, REPOSITORY_NAME);
		}).getMessage();
		
		assertEquals("The user is required.", result);
	}
	
	@Test
	public void testGetEntityIdForRepositoryNameAndNoRepositoryName() {
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			dockerManager.getEntityIdForRepositoryName(USER_INFO, null);
		}).getMessage();
		
		assertEquals("The repositoryName is required and must not be the empty string.", result);
	}
	
	@Test
	public void testGetEntityIdForRepositoryNameAndEmptyRepositoryName() {
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			dockerManager.getEntityIdForRepositoryName(USER_INFO, "");
		}).getMessage();
		
		assertEquals("The repositoryName is required and must not be the empty string.", result);
	}
	
}
