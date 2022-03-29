package org.sagebionetworks.repo.manager.discussion;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.dbo.dao.discussion.ForumDAO;
import org.sagebionetworks.repo.model.discussion.Forum;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.web.NotFoundException;

@ExtendWith(MockitoExtension.class)
public class ForumManagerImplTest {
	
	@Mock
	private ForumDAO mockForumDao;
	@Mock
	private AuthorizationManager mockAuthManager;
	@Mock
	private NodeDAO mockNodeDao;
	@InjectMocks
	private ForumManagerImpl forumManager;
	
	private String projectId = "syn123";
	private final ObjectType ENTITY_TYPE = ObjectType.ENTITY;
	private final ACCESS_TYPE READ_ACCESS = ACCESS_TYPE.READ;
	private final AuthorizationStatus SUCCESS = AuthorizationStatus.authorized();
	private final AuthorizationStatus FAILED = AuthorizationStatus.accessDenied("no reasons");
	private UserInfo userInfo = new UserInfo(false /*not admin*/);
	private Forum dto = new Forum();

	@BeforeEach
	public void before() {
		dto.setId("1");
		dto.setProjectId(projectId);
	}

	@Test
	public void testCreateWithNullProjectId() {
		assertThrows(IllegalArgumentException.class, () -> {			
			forumManager.createForum(userInfo, null);
		});
	}

	@Test
	public void testCreateWithNonExistingProjectId() {
		when(mockNodeDao.doesNodeExist(KeyFactory.stringToKey(projectId)))
				.thenReturn(false);
		assertThrows(NotFoundException.class, () -> {	
			forumManager.createForum(userInfo, projectId);
		});
	}

	@Test
	public void testCreateWithNullUserInfo() {
		when(mockNodeDao.doesNodeExist(KeyFactory.stringToKey(projectId)))
			.thenReturn(true);
		assertThrows(IllegalArgumentException.class, () -> {
			forumManager.createForum(null, projectId);
		});
	}

	@Test
	public void testCreateUnauthorized() {
		when(mockNodeDao.doesNodeExist(KeyFactory.stringToKey(projectId)))
				.thenReturn(true);
		when(mockAuthManager.canAccess(
				userInfo, projectId, ENTITY_TYPE, READ_ACCESS)).thenReturn(FAILED);
		
		assertThrows(UnauthorizedException.class, () -> {	
			forumManager.createForum(userInfo, projectId);
		});
	}

	@Test
	public void testCreateAuthorized() {
		when(mockNodeDao.doesNodeExist(KeyFactory.stringToKey(projectId)))
				.thenReturn(true);
		when(mockAuthManager.canAccess(
				userInfo, projectId, ENTITY_TYPE, READ_ACCESS)).thenReturn(SUCCESS);
		when(mockForumDao.createForum(projectId)).thenReturn(dto);
		assertEquals(forumManager.createForum(userInfo, projectId), dto);
	}

	@Test
	public void testGetForumByProjectIdWithNullUserInfo() {
		when(mockNodeDao.doesNodeExist(KeyFactory.stringToKey(projectId)))
				.thenReturn(true);
		
		assertThrows(IllegalArgumentException.class, () -> {	
			forumManager.getForumByProjectId(null, projectId);
		});
	}

	@Test
	public void testGetForumByProjectIdWithNullProjectId() {
		assertThrows(IllegalArgumentException.class, () -> {	
			forumManager.getForumByProjectId(userInfo, null);
		});
	}

	@Test
	public void testGetForumByProjectIdWithNonExistingProjectId() {
		when(mockNodeDao.doesNodeExist(KeyFactory.stringToKey(projectId)))
				.thenReturn(false);
		
		assertThrows(NotFoundException.class, () -> {	
			forumManager.getForumByProjectId(userInfo, projectId);
		});
	}

	@Test
	public void testGetForumByProjectIdUnauthorized() {
		when(mockNodeDao.doesNodeExist(KeyFactory.stringToKey(projectId)))
				.thenReturn(true);
		when(mockAuthManager.canAccess(
				userInfo, projectId, ENTITY_TYPE, READ_ACCESS)).thenReturn(FAILED);
		
		assertThrows(UnauthorizedException.class, () -> {	
			forumManager.getForumByProjectId(userInfo, projectId);
		});
	}

	@Test
	public void testGetForumByProjectIdAuthorized() {
		when(mockNodeDao.doesNodeExist(KeyFactory.stringToKey(projectId)))
				.thenReturn(true);
		when(mockAuthManager.canAccess(
				userInfo, projectId, ENTITY_TYPE, READ_ACCESS)).thenReturn(SUCCESS);
		when(mockForumDao.getForumByProjectId(projectId)).thenReturn(dto);
		assertEquals(forumManager.getForumByProjectId(userInfo, projectId), dto);
	}

	@Test
	public void testGetForumWithNullUserInfo() {
		
		assertThrows(IllegalArgumentException.class, () -> {	
			forumManager.getForum(null, dto.getId());
		});
	}

	@Test
	public void testGetForumWithNullForumId() {
		assertThrows(IllegalArgumentException.class, () -> {	
			forumManager.getForum(userInfo, null);
		});
	}

	@Test
	public void testGetForumWithNonExistingProjectId() {
		when(mockAuthManager.canAccess(userInfo, projectId, ENTITY_TYPE, READ_ACCESS))
				.thenThrow(new NotFoundException(""));
		String forumId = dto.getId();
		when(mockForumDao.getForum(Long.parseLong(forumId))).thenReturn(dto);
		
		assertThrows(NotFoundException.class, () -> {	
			forumManager.getForum(userInfo, forumId);
		});
	}

	@Test
	public void testGetForumUnauthorized() {
		when(mockAuthManager.canAccess(
				userInfo, projectId, ENTITY_TYPE, READ_ACCESS)).thenReturn(FAILED);
		String forumId = dto.getId();
		when(mockForumDao.getForum(Long.parseLong(forumId))).thenReturn(dto);
		
		assertThrows(UnauthorizedException.class, () -> {	
			forumManager.getForum(userInfo, forumId);
		});
	}

	@Test
	public void testGetForum() {
		when(mockAuthManager.canAccess(
				userInfo, projectId, ENTITY_TYPE, READ_ACCESS)).thenReturn(SUCCESS);
		String forumId = dto.getId();
		when(mockForumDao.getForum(Long.parseLong(forumId))).thenReturn(dto);
		assertEquals(forumManager.getForum(userInfo, forumId), dto);
	}
}
