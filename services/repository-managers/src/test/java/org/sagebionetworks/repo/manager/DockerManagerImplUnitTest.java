package org.sagebionetworks.repo.manager;

import static org.junit.Assert.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.docker.DockerAuthorizationToken;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.springframework.test.util.ReflectionTestUtils;


public class DockerManagerImplUnitTest {
	
	private static final long PARENT_ID_LONG = 98765L;
	private static final String PARENT_ID = "syn"+PARENT_ID_LONG;
	
	private static final long REPO_ENTITY_ID_LONG = 123456L;
	private static final String REPO_ENTITY_ID = "syn"+REPO_ENTITY_ID_LONG;
	private static final String ENTITY_NAME = "myrepo";
	
	private static final String USER_NAME = "auser";
	private static final UserInfo USER_INFO = new UserInfo(false);
	
	private static final String SERVICE = "www.synapse.org";
	private static final String TYPE = "repository";
	private static final String REPOSITORY_PATH = PARENT_ID+"/"+ENTITY_NAME;
	private static final String ACCESS_TYPES_STRING="push,pull";
	
	private DockerManagerImpl dockerManager;
	
	@Mock
	private NodeDAO nodeDAO;
	
	@Mock
	private IdGenerator idGenerator;
	
	@Mock
	private UserManager userManager;
	
	@Mock
	private EntityManager entityManager;
	
	@Mock
	private PrincipalAliasDAO principalAliasDAO;
	
	@Mock
	private AuthorizationManager authorizationManager;
	
	private EntityHeader parentHeader;
	private EntityHeader repoEntityHeader;
	private Node authQueryNode;

	@Before
	public void before() throws Exception {
		MockitoAnnotations.initMocks(this);
		dockerManager = new DockerManagerImpl();
		ReflectionTestUtils.setField(dockerManager, "nodeDAO", nodeDAO);
		ReflectionTestUtils.setField(dockerManager, "idGenerator", idGenerator);
		ReflectionTestUtils.setField(dockerManager, "userManager", userManager);
		ReflectionTestUtils.setField(dockerManager, "entityManager", entityManager);
		ReflectionTestUtils.setField(dockerManager, "principalAliasDAO", principalAliasDAO);
		ReflectionTestUtils.setField(dockerManager, "authorizationManager", authorizationManager);

		parentHeader = new EntityHeader();
		parentHeader.setId(PARENT_ID);
		parentHeader.setType(EntityType.project.name());
		List<EntityHeader> parent = Collections.singletonList(parentHeader);
		when(nodeDAO.getEntityHeader(Collections.singleton(PARENT_ID_LONG))).thenReturn(parent);
		
		repoEntityHeader = new EntityHeader();
		repoEntityHeader.setId(REPO_ENTITY_ID);
		repoEntityHeader.setType(EntityType.dockerrepo.name());
		when(nodeDAO.getEntityHeaderByChildName(PARENT_ID, ENTITY_NAME)).thenReturn(repoEntityHeader);
		
		authQueryNode = new Node();
		authQueryNode.setParentId(PARENT_ID);
		authQueryNode.setNodeType(EntityType.dockerrepo);
		when(authorizationManager.canCreate(eq(USER_INFO), eq(authQueryNode))).
			thenReturn(AuthorizationManagerUtil.AUTHORIZED);

		when(authorizationManager.canAccess(
				eq(USER_INFO), eq(REPO_ENTITY_ID), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.UPDATE))).
				thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		
		when(authorizationManager.canAccess(
				eq(USER_INFO), eq(REPO_ENTITY_ID), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.DOWNLOAD))).
				thenReturn(AuthorizationManagerUtil.AUTHORIZED);
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
		parentHeader.setType(EntityType.folder.name());
		assertEquals(null, dockerManager.validParentProjectId(PARENT_ID+"/myrepo"));
	}

	@Test
	public void testValidParentProjectIdHappyPath() {
		assertEquals(PARENT_ID, dockerManager.validParentProjectId(PARENT_ID+"/myrepo"));
	}

	@Test
	public void testAuthorizeDockerAccess() throws Exception {
		String scope =TYPE+":"+REPOSITORY_PATH+":"+ACCESS_TYPES_STRING;
		
		DockerAuthorizationToken token = dockerManager.
				authorizeDockerAccess(USER_NAME, USER_INFO, SERVICE, scope);
		
		assertNotNull(token.getToken());
	}
	
	@Test
	public void testGetPermittedAccessTypesHappyCase() throws Exception {
		List<String> permitted = dockerManager.
				getPermittedAccessTypes(USER_NAME, USER_INFO, SERVICE, TYPE, REPOSITORY_PATH, ACCESS_TYPES_STRING);
		
		assertEquals(Arrays.asList(new String[]{"push", "pull"}), permitted);
	}

	@Test
	public void testGetPermittedAccessTypesInvalidParent() throws Exception {
		String repositoryPath = "garbage/"+ENTITY_NAME;
		
		List<String> permitted = dockerManager.
				getPermittedAccessTypes(USER_NAME, USER_INFO, SERVICE, TYPE, repositoryPath, ACCESS_TYPES_STRING);
		
		assertTrue(permitted.isEmpty());
	}

	@Test
	public void testGetPermittedAccessTypesNonexistentChild() throws Exception {
		String repositoryPath = PARENT_ID+"/non-existent-repo";

		List<String> permitted = dockerManager.
				getPermittedAccessTypes(USER_NAME, USER_INFO, SERVICE, TYPE, repositoryPath, ACCESS_TYPES_STRING);
		
		//OK to push, but can't pull since it doesn't exist
		assertEquals(Arrays.asList(new String[]{"push"}), permitted);
	}

	@Test
	public void testGetPermittedAccessRepoExistsAccessUnauthorized() throws Exception {
		when(authorizationManager.canAccess(
				eq(USER_INFO), eq(REPO_ENTITY_ID), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.UPDATE))).
				thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		
		when(authorizationManager.canAccess(
				eq(USER_INFO), eq(REPO_ENTITY_ID), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.DOWNLOAD))).
				thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		
		List<String> permitted = dockerManager.
				getPermittedAccessTypes(USER_NAME, USER_INFO, SERVICE, TYPE, REPOSITORY_PATH, ACCESS_TYPES_STRING);

		// Note, we DO have create access, but that doesn't let us 'push' since the repo already exists
		assertTrue(permitted.toString(), permitted.isEmpty());
	}

	@Test
	public void testGetPermittedAccessTypesNonexistentChildUnauthorized() throws Exception {
		String repositoryPath = PARENT_ID+"/non-existent-repo";

		when(authorizationManager.canCreate(eq(USER_INFO), eq(authQueryNode))).
			thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		
		when(authorizationManager.canAccess(
				eq(USER_INFO), eq(REPO_ENTITY_ID), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.DOWNLOAD))).
				thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		
		List<String> permitted = dockerManager.
				getPermittedAccessTypes(USER_NAME, USER_INFO, SERVICE, TYPE, repositoryPath, ACCESS_TYPES_STRING);

		// Note, we DO have update access, but that doesn't let us 'push' since the repo doesn't exist
		assertTrue(permitted.toString(), permitted.isEmpty());
	}

//	@Test
//	public void testDockerRegistryNotification() {
//		fail("Not yet implemented");
//	}

}
