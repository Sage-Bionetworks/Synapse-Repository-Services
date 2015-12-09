package org.sagebionetworks.repo.manager.discussion;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.AuthorizationStatus;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.discussion.ForumDAO;
import org.sagebionetworks.repo.model.discussion.Forum;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

public class ForumManagerImplTest {
	private ForumDAO mockForumDao;
	private AuthorizationManager mockAuthManager;
	private NodeDAO mockNodeDao;
	private ForumManagerImpl forumManager;
	private String projectId = "syn123";
	private final ObjectType ENTITY_TYPE = ObjectType.ENTITY;
	private final ACCESS_TYPE READ_ACCESS = ACCESS_TYPE.READ;
	private final AuthorizationStatus SUCCESS = new AuthorizationStatus(true, null);
	private final AuthorizationStatus FAILED = new AuthorizationStatus(false, "no reasons");
	private UserInfo userInfo = new UserInfo(false /*not admin*/);
	private Forum dto = new Forum();

	@Before
	public void before() {
		mockForumDao = Mockito.mock(ForumDAO.class);
		mockAuthManager = Mockito.mock(AuthorizationManager.class);
		mockNodeDao = Mockito.mock(NodeDAO.class);
		forumManager = new ForumManagerImpl();
		ReflectionTestUtils.setField(forumManager, "forumDao", mockForumDao);
		ReflectionTestUtils.setField(forumManager, "authorizationManager", mockAuthManager);
		ReflectionTestUtils.setField(forumManager, "nodeDao", mockNodeDao);

		dto.setId("1");
		dto.setProjectId(projectId);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testCreateWithNullProjectId() {
		forumManager.createForum(userInfo, null);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testGetWithNullProjectId() {
		forumManager.getForumMetadata(userInfo, null);
	}

	@Test (expected=NotFoundException.class)
	public void testCreateWithNonExistingProjectId() {
		Mockito.when(mockNodeDao.doesNodeExist(KeyFactory.stringToKey(projectId)))
				.thenReturn(false);
		forumManager.createForum(userInfo, projectId);
	}

	@Test (expected=NotFoundException.class)
	public void testGetWithNonExistingProjectId() {
		Mockito.when(mockNodeDao.doesNodeExist(KeyFactory.stringToKey(projectId)))
				.thenReturn(false);
		forumManager.getForumMetadata(userInfo, projectId);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testCreateWithNullUserInfo() {
		Mockito.when(mockNodeDao.doesNodeExist(KeyFactory.stringToKey(projectId)))
				.thenReturn(true);
		forumManager.createForum(null, projectId);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testGetWithNullUserInfo() {
		Mockito.when(mockNodeDao.doesNodeExist(KeyFactory.stringToKey(projectId)))
				.thenReturn(true);
		forumManager.getForumMetadata(null, projectId);
	}

	@Test (expected=UnauthorizedException.class)
	public void testCreateUnauthorized() {
		Mockito.when(mockNodeDao.doesNodeExist(KeyFactory.stringToKey(projectId)))
				.thenReturn(true);
		Mockito.when(mockAuthManager.canAccess(
				userInfo, projectId, ENTITY_TYPE, READ_ACCESS)).thenReturn(FAILED);
		forumManager.createForum(userInfo, projectId);
	}

	@Test (expected=UnauthorizedException.class)
	public void testGetUnauthorized() {
		Mockito.when(mockNodeDao.doesNodeExist(KeyFactory.stringToKey(projectId)))
				.thenReturn(true);
		Mockito.when(mockAuthManager.canAccess(
				userInfo, projectId, ENTITY_TYPE, READ_ACCESS)).thenReturn(FAILED);
		forumManager.getForumMetadata(userInfo, projectId);
	}

	@Test
	public void testCreateAuthorized() {
		Mockito.when(mockNodeDao.doesNodeExist(KeyFactory.stringToKey(projectId)))
				.thenReturn(true);
		Mockito.when(mockAuthManager.canAccess(
				userInfo, projectId, ENTITY_TYPE, READ_ACCESS)).thenReturn(SUCCESS);
		Mockito.when(mockForumDao.createForum(projectId)).thenReturn(dto);
		assertEquals(forumManager.createForum(userInfo, projectId), dto);
	}

	@Test
	public void testGetAuthorized() {
		Mockito.when(mockNodeDao.doesNodeExist(KeyFactory.stringToKey(projectId)))
				.thenReturn(true);
		Mockito.when(mockAuthManager.canAccess(
				userInfo, projectId, ENTITY_TYPE, READ_ACCESS)).thenReturn(SUCCESS);
		Mockito.when(mockForumDao.getForumByProjectId(projectId)).thenReturn(dto);
		assertEquals(forumManager.getForumMetadata(userInfo, projectId), dto);
	}

	@Test
	public void testGetForumDoesNotExist() {
		Mockito.when(mockNodeDao.doesNodeExist(KeyFactory.stringToKey(projectId)))
				.thenReturn(true);
		Mockito.when(mockAuthManager.canAccess(
				userInfo, projectId, ENTITY_TYPE, READ_ACCESS)).thenReturn(SUCCESS);
		Mockito.when(mockForumDao.getForumByProjectId(projectId)).thenThrow(new NotFoundException());
		Mockito.when(mockForumDao.createForum(projectId)).thenReturn(dto);
		assertEquals(forumManager.getForumMetadata(userInfo, projectId), dto);
	}
}
