package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DockerCommitDao;
import org.sagebionetworks.repo.model.DockerNodeDao;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.docker.DockerAuthorizationToken;
import org.sagebionetworks.repo.model.docker.DockerCommit;
import org.sagebionetworks.repo.model.docker.DockerCommitSortBy;
import org.sagebionetworks.repo.model.docker.DockerRegistryEventList;
import org.sagebionetworks.repo.model.docker.DockerRepository;
import org.sagebionetworks.repo.model.docker.RegistryEventAction;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.util.DockerRegistryEventUtil;
import org.springframework.test.util.ReflectionTestUtils;


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
	
	private DockerManagerImpl dockerManager;
	
	@Mock
	private NodeDAO nodeDAO;
	
	@Mock
	private DockerNodeDao dockerNodeDao;
	
	@Mock
	private DockerCommitDao dockerCommitDao;
	
	@Mock
	private IdGenerator idGenerator;
	
	@Mock
	private UserManager userManager;
	
	@Mock
	private EntityManager entityManager;
	
	@Mock
	private AuthorizationManager authorizationManager;
	
	@Mock
	private TransactionalMessenger transactionalMessenger;
	
	private EntityHeader parentHeader;
	private Node authQueryNode;

	@Before
	public void before() throws Exception {
		MockitoAnnotations.initMocks(this);
		dockerManager = new DockerManagerImpl();
		ReflectionTestUtils.setField(dockerManager, "nodeDAO", nodeDAO);
		ReflectionTestUtils.setField(dockerManager, "dockerNodeDao", dockerNodeDao);
		ReflectionTestUtils.setField(dockerManager, "idGenerator", idGenerator);
		ReflectionTestUtils.setField(dockerManager, "userManager", userManager);
		ReflectionTestUtils.setField(dockerManager, "entityManager", entityManager);
		ReflectionTestUtils.setField(dockerManager, "authorizationManager", authorizationManager);
		ReflectionTestUtils.setField(dockerManager, "dockerCommitDao", dockerCommitDao);
		ReflectionTestUtils.setField(dockerManager, "transactionalMessenger", transactionalMessenger);

		parentHeader = new EntityHeader();
		parentHeader.setId(PARENT_ID);
		parentHeader.setType(Project.class.getName());
		List<EntityHeader> parent = Collections.singletonList(parentHeader);
		when(nodeDAO.getEntityHeader(Collections.singleton(PARENT_ID_LONG))).thenReturn(parent);
		
		when(dockerNodeDao.getEntityIdForRepositoryName(REPOSITORY_NAME)).thenReturn(REPO_ENTITY_ID);
		
		authQueryNode = new Node();
		authQueryNode.setParentId(PARENT_ID);
		authQueryNode.setNodeType(EntityType.dockerrepo);
		when(authorizationManager.canCreate(eq(USER_INFO), eq(authQueryNode))).
			thenReturn(AuthorizationManagerUtil.AUTHORIZED);

		when(authorizationManager.canAccess(
				eq(USER_INFO), eq(REPO_ENTITY_ID), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.READ))).
				thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		
		when(authorizationManager.canAccess(
				eq(USER_INFO), eq(REPO_ENTITY_ID), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.UPDATE))).
				thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		
		when(authorizationManager.canAccess(
				eq(USER_INFO), eq(REPO_ENTITY_ID), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.DOWNLOAD))).
				thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		
		PrincipalAlias pa = new PrincipalAlias();
		pa.setPrincipalId(USER_ID);
		pa.setType(AliasType.USER_NAME);
		
		when(userManager.getUserInfo(USER_ID)).thenReturn(USER_INFO);
		
		when(idGenerator.generateNewId(IdType.ENTITY_ID)).thenReturn(REPO_ENTITY_ID_LONG);
		
		when(entityManager.getEntityType(USER_INFO, REPO_ENTITY_ID)).thenReturn(EntityType.dockerrepo);
	}

	@Test
	public void testValidParentProjectIdInvalidRepoName() {
		assertEquals(null, dockerManager.validParentProjectId("/invalid/"));
	}

	@Test
	public void testValidParentProjectIdInvalidSynID() {
		assertEquals(null, dockerManager.validParentProjectId("uname/myrepo"));
	}

	@Test
	public void testValidParentProjectIdParentNotAProject() {
		parentHeader.setType(Folder.class.getName());
		assertEquals(null, dockerManager.validParentProjectId(PARENT_ID+"/myrepo"));
	}

	@Test
	public void testValidParentProjectIdHappyPath() {
		assertEquals(PARENT_ID, dockerManager.validParentProjectId(PARENT_ID+"/myrepo"));
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testAuthourizeDockerAccessNullUserInfo() throws Exception{
		dockerManager.authorizeDockerAccess(null, SERVICE, new ArrayList<String>());
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testAuthourizeDockerAccessNullService() throws Exception{
		dockerManager.authorizeDockerAccess(USER_INFO, null, new ArrayList<String>());
	}
	
	@Test
	public void testAuthorizeDockerAccess() throws Exception {
		List<String> scope = new ArrayList<String>();
		scope.add(TYPE+":"+REPOSITORY_PATH+":"+ACCESS_TYPES_STRING);
		
		// method under test:
		DockerAuthorizationToken token = dockerManager.
				authorizeDockerAccess(USER_INFO, SERVICE, scope);
		
		assertNotNull(token.getToken());
	}
	
	// Docker login calls the authorization service, but without a 'scope' param
	@Test
	public void testDockerLogin() throws Exception {
		// method under test:
		DockerAuthorizationToken token = dockerManager.
				authorizeDockerAccess(USER_INFO, SERVICE, null);
		
		assertNotNull(token.getToken());
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testGetPermittedAccessTypesNullUserInfo() throws Exception{
		dockerManager.
		getPermittedActions(null, SERVICE, REPOSITORY_PATH, ACCESS_TYPES_STRING);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testGetPermittedAccessTypesNullService() throws Exception{
		dockerManager.
		getPermittedActions(USER_INFO, null, REPOSITORY_PATH, ACCESS_TYPES_STRING);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testGetPermittedAccessTypesNullRepositoryPath() throws Exception{
		dockerManager.
		getPermittedActions(USER_INFO, SERVICE, null, ACCESS_TYPES_STRING);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testGetPermittedAccessTypesNullAction() throws Exception{
		dockerManager.
		getPermittedActions(USER_INFO, SERVICE, REPOSITORY_PATH, null);
	}
	
	@Test
	public void testGetPermittedAccessTypesHappyCase() throws Exception {
		// method under test:
		Set<RegistryEventAction> permitted = dockerManager.
				getPermittedActions(USER_INFO, SERVICE, REPOSITORY_PATH, ACCESS_TYPES_STRING);
		
		assertEquals(new HashSet(Arrays.asList(new RegistryEventAction[]{RegistryEventAction.push, RegistryEventAction.pull})), permitted);
	}

	@Test
	public void testGetPermittedAccessTypesInvalidParent() throws Exception {
		String repositoryPath = "garbage/"+REPOSITORY_NAME;
		
		// method under test:
		Set<RegistryEventAction> permitted = dockerManager.
				getPermittedActions(USER_INFO, SERVICE, repositoryPath, ACCESS_TYPES_STRING);
		
		assertTrue(permitted.isEmpty());
	}

	@Test
	public void testGetPermittedAccessTypesNonexistentChild() throws Exception {
		String repositoryPath = PARENT_ID+"/non-existent-repo";

		when(dockerNodeDao.getEntityIdForRepositoryName(SERVICE+"/"+repositoryPath)).thenReturn(null);

		// method under test:
		Set<RegistryEventAction> permitted = dockerManager.
				getPermittedActions(USER_INFO, SERVICE, repositoryPath, ACCESS_TYPES_STRING);
		
		// client needs both push and pull access to push a not-yet-existing repo to the registry
		assertEquals(new HashSet(Arrays.asList(new RegistryEventAction[]{RegistryEventAction.push, RegistryEventAction.pull})), permitted);
	}

	@Test
	public void testGetPermittedAccessRepoExistsAccessUnauthorized() throws Exception {
		when(authorizationManager.canAccess(
				eq(USER_INFO), eq(REPO_ENTITY_ID), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.UPDATE))).
				thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		
		when(authorizationManager.canAccess(
				eq(USER_INFO), eq(REPO_ENTITY_ID), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.DOWNLOAD))).
				thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		
		// method under test:
		Set<RegistryEventAction> permitted = dockerManager.
				getPermittedActions(USER_INFO, SERVICE, REPOSITORY_PATH, ACCESS_TYPES_STRING);

		// Note, we DO have create access, but that doesn't let us 'push' since the repo already exists
		assertTrue(permitted.toString(), permitted.isEmpty());
	}

	@Test
	public void testGetPermittedAccessDownloadButNoRead() throws Exception {
		when(authorizationManager.canAccess(
				eq(USER_INFO), eq(REPO_ENTITY_ID), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.READ))).
				thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		
		when(authorizationManager.canAccess(
				eq(USER_INFO), eq(REPO_ENTITY_ID), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.DOWNLOAD))).
				thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		
		// method under test:
		Set<RegistryEventAction> permitted = dockerManager.
				getPermittedActions(USER_INFO, SERVICE, REPOSITORY_PATH, "pull");

		// it's allowed because it's *DOWNLOAD* permission, not *READ* permission which we must have
		assertEquals(new HashSet(Arrays.asList(new RegistryEventAction[]{RegistryEventAction.pull})), permitted);

	}

	@Test
	public void testGetPermittedAccessTypesNonexistentChildUnauthorized() throws Exception {
		String repositoryPath = PARENT_ID+"/non-existent-repo";

		when(dockerNodeDao.getEntityIdForRepositoryName(SERVICE+"/"+repositoryPath)).thenReturn(null);

		when(authorizationManager.canCreate(eq(USER_INFO), eq(authQueryNode))).
			thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		
		when(authorizationManager.canAccess(
				eq(USER_INFO), eq(REPO_ENTITY_ID), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.DOWNLOAD))).
				thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		
		// method under test:
		Set<RegistryEventAction> permitted = dockerManager.
				getPermittedActions(USER_INFO, SERVICE, repositoryPath, ACCESS_TYPES_STRING);

		// Note, we DO have update access, but that doesn't let us 'push' since the repo doesn't exist
		assertTrue(permitted.toString(), permitted.isEmpty());
	}
	
	@Test
	public void testDockerRegistryNotificationPushNEWEntity() {
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
		assertEquals(REPO_ENTITY_ID, repo.getValue().getId());
		
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
		DockerRegistryEventList events = 
				DockerRegistryEventUtil.createDockerRegistryEvent(RegistryEventAction.push, REGISTRY_HOST, USER_ID, REPOSITORY_PATH, TAG, DIGEST, MEDIA_TYPE);
		
		// method under test:
		dockerManager.dockerRegistryNotification(events);
		
		// no create operation, since the repo already exists
		verify(entityManager, never()).createEntity((UserInfo)any(), (Entity)any(), (String)any());
		
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
		when(dockerNodeDao.getEntityIdForRepositoryName(REPOSITORY_NAME)).thenReturn(null);

		DockerRegistryEventList events = 
				DockerRegistryEventUtil.createDockerRegistryEvent(RegistryEventAction.push, "quay.io", USER_ID, REPOSITORY_PATH, TAG, DIGEST, MEDIA_TYPE);
		
		// method under test:
		dockerManager.dockerRegistryNotification(events);
		
		verify(entityManager, never()).createEntity((UserInfo)anyObject(), (Entity)anyObject(), anyString());
		verify(dockerCommitDao, never()).createDockerCommit(anyString(), anyLong(), (DockerCommit)anyObject());
	}
	
	@Test
	public void testDockerRegistryNotificationPushNotManifestMediaType() {
		when(dockerNodeDao.getEntityIdForRepositoryName(REPOSITORY_NAME)).thenReturn(null);

		DockerRegistryEventList events = 
				DockerRegistryEventUtil.createDockerRegistryEvent(RegistryEventAction.push, REGISTRY_HOST, USER_ID, REPOSITORY_PATH, TAG, DIGEST, "application/octet-stream");
		
		// method under test:
		dockerManager.dockerRegistryNotification(events);
		
		verify(entityManager, never()).createEntity((UserInfo)anyObject(), (Entity)anyObject(), anyString());
		verify(dockerCommitDao, never()).createDockerCommit(anyString(), anyLong(), (DockerCommit)anyObject());
	}
	
	
	@Test(expected=IllegalArgumentException.class)
	public void testDockerRegistryNotificationPushNEWEntityParentIsFolder() {
		when(dockerNodeDao.getEntityIdForRepositoryName(REPOSITORY_NAME)).thenReturn(null);
		parentHeader.setType(EntityType.folder.name());

		DockerRegistryEventList events = 
				DockerRegistryEventUtil.createDockerRegistryEvent(RegistryEventAction.push, REGISTRY_HOST, USER_ID, REPOSITORY_PATH, TAG, DIGEST, MEDIA_TYPE);
		
		// method under test:
		dockerManager.dockerRegistryNotification(events);
	}

	@Test
	public void testDockerRegistryNotificationPull() {
		DockerRegistryEventList events = 
				DockerRegistryEventUtil.createDockerRegistryEvent(RegistryEventAction.pull, REGISTRY_HOST, USER_ID, REPOSITORY_PATH, TAG, DIGEST, MEDIA_TYPE);
		dockerManager.dockerRegistryNotification(events);
		// no create operation, since the repo already exists
		verify(entityManager, never()).createEntity((UserInfo)anyObject(), (Entity)anyObject(), anyString());
		verify(dockerCommitDao, never()).createDockerCommit(anyString(), anyLong(), (DockerCommit)anyObject());
	}
	@Test
	public void testDockerRegistryNotificationMount() {
		DockerRegistryEventList events = 
				DockerRegistryEventUtil.createDockerRegistryEvent(RegistryEventAction.mount, REGISTRY_HOST, USER_ID, REPOSITORY_PATH, TAG, DIGEST, MEDIA_TYPE);
		dockerManager.dockerRegistryNotification(events);
		// no create operation, since the repo already exists
		verify(entityManager, never()).createEntity((UserInfo)anyObject(), (Entity)anyObject(), anyString());
		verify(dockerCommitDao, never()).createDockerCommit(anyString(), anyLong(), (DockerCommit)anyObject());
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
		
		DockerCommit commit = createCommit();
		
		// method under test
		dockerManager.addDockerCommitToUnmanagedRespository(USER_INFO, REPO_ENTITY_ID, commit);
		
		verify(dockerCommitDao).createDockerCommit(REPO_ENTITY_ID, USER_ID, commit);
		verify(transactionalMessenger).sendMessageAfterCommit(
				eq(REPO_ENTITY_ID), eq(ObjectType.ENTITY), anyString(), eq(ChangeType.UPDATE));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testAddDockerCommitToMANANGEDRespository() {
		// this makes the repo a *managed* repo
		when (dockerNodeDao.getRepositoryNameForEntityId(REPO_ENTITY_ID)).thenReturn(REPOSITORY_NAME);
		DockerCommit commit = createCommit();

		// method under test
		dockerManager.addDockerCommitToUnmanagedRespository(USER_INFO, REPO_ENTITY_ID, commit);
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testAddDockerCommitToUnmanagedRespositoryUNAUTHORIZED() {
		
		when(authorizationManager.canAccess(
				eq(USER_INFO), eq(REPO_ENTITY_ID), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.UPDATE))).
				thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		
		
		DockerCommit commit = createCommit();
		
		// method under test
		dockerManager.addDockerCommitToUnmanagedRespository(USER_INFO, REPO_ENTITY_ID, commit);
	}
	
	@Test
	public void listDockerCommitsHappyCase() {
		List<DockerCommit> commits = new ArrayList<DockerCommit>();
		commits.add(createCommit());
		commits.add(createCommit());
		
		when(dockerCommitDao.listDockerCommits(REPO_ENTITY_ID, DockerCommitSortBy.CREATED_ON, /*ascending*/true, 10, 0)).
			thenReturn(commits);
		
		when(dockerCommitDao.countDockerCommits(REPO_ENTITY_ID)).thenReturn(2L);
		
		// method under test
		PaginatedResults<DockerCommit> pgs = dockerManager.listDockerCommits(
				USER_INFO, REPO_ENTITY_ID, DockerCommitSortBy.CREATED_ON, /*ascending*/true, 10, 0);
		
		assertEquals(2L, pgs.getTotalNumberOfResults());
		assertEquals(commits, pgs.getResults());
	}
	
	@Test(expected=UnauthorizedException.class)
	public void listDockerCommitsUNAUTHORIZED() {
		when(authorizationManager.canAccess(
				eq(USER_INFO), eq(REPO_ENTITY_ID), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.READ))).
				thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
				
		// method under test
		PaginatedResults<DockerCommit> pgs = dockerManager.listDockerCommits(
				USER_INFO, REPO_ENTITY_ID, DockerCommitSortBy.CREATED_ON, /*ascending*/true, 10, 0);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void listDockerCommitsforNONrepo() {
		when(entityManager.getEntityType(USER_INFO, REPO_ENTITY_ID)).thenReturn(EntityType.project);
		// method under test
		PaginatedResults<DockerCommit> pgs = dockerManager.listDockerCommits(
				USER_INFO, REPO_ENTITY_ID, DockerCommitSortBy.CREATED_ON, /*ascending*/true, 10, 0);

	}

}
