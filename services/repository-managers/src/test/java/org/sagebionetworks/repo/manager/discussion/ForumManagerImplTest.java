package org.sagebionetworks.repo.manager.discussion;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.dataaccess.DataAccessAuthorizationManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.SelfSignAccessRequirement;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.dbo.dao.discussion.ForumDAO;
import org.sagebionetworks.repo.model.discussion.Forum;
import org.sagebionetworks.repo.model.discussion.ForumObjectType;
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
	@Mock
	private AccessRequirementDAO mockAccessRequirementDao;
	@Mock
	private DataAccessAuthorizationManager mockDataAccessAuthManager;
	@InjectMocks
	private ForumManagerImpl forumManager;

	private String projectId = "syn123";
	private final ObjectType ENTITY_TYPE = ObjectType.ENTITY;
	private final ACCESS_TYPE READ_ACCESS = ACCESS_TYPE.READ;
	private final AuthorizationStatus SUCCESS = AuthorizationStatus.authorized();
	private final AuthorizationStatus FAILED = AuthorizationStatus.accessDenied("no reasons");
	private UserInfo userInfo = new UserInfo(false /*not admin*/, 1L, AuthorizationConstants.DEFAULT_REALM_ID);
	private Forum dto = new Forum();

	@BeforeEach
	public void before() {
		dto.setId("1");
		dto.setProjectId(projectId);
		dto.setObjectId(projectId);
		dto.setObjectType(ForumObjectType.ENTITY);
	}

	@Test
	public void testCreateWithNullProjectId() {
		//call under test
		assertThrows(IllegalArgumentException.class, () -> {			
			forumManager.createForum(userInfo, null);
		});
	}

	@Test
	public void testCreateWithNonExistingProjectId() {
		when(mockNodeDao.doesNodeExist(KeyFactory.stringToKey(projectId)))
				.thenReturn(false);
		//call under test
		assertThrows(NotFoundException.class, () -> {	
			forumManager.createForum(userInfo, projectId);
		});
	}

	@Test
	public void testCreateWithNullUserInfo() {
		when(mockNodeDao.doesNodeExist(KeyFactory.stringToKey(projectId)))
			.thenReturn(true);
		//call under test
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
		//call under test
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
		when(mockForumDao.createForum(projectId, ForumObjectType.ENTITY)).thenReturn(dto);
		//call under test
		assertEquals(forumManager.createForum(userInfo, projectId), dto);
	}

	@Test
	public void testGetForumByProjectIdWithNullUserInfo() {
		when(mockNodeDao.doesNodeExist(KeyFactory.stringToKey(projectId)))
				.thenReturn(true);
		//call under test
		assertThrows(IllegalArgumentException.class, () -> {	
			forumManager.getForumByProjectId(null, projectId);
		});
	}

	@Test
	public void testGetForumByProjectIdWithNullProjectId() {
		//call under test
		assertThrows(IllegalArgumentException.class, () -> {	
			forumManager.getForumByProjectId(userInfo, null);
		});
	}

	@Test
	public void testGetForumByProjectIdWithNonExistingProjectId() {
		when(mockNodeDao.doesNodeExist(KeyFactory.stringToKey(projectId)))
				.thenReturn(false);
		//call under test
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
		//call under test
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
		when(mockForumDao.getForumByObjectIdAndType(projectId, ForumObjectType.ENTITY)).thenReturn(dto);
		//call under test
		assertEquals(forumManager.getForumByProjectId(userInfo, projectId), dto);
	}

	@Test
	public void testGetForumWithNullUserInfo() {
		//call under test
		assertThrows(IllegalArgumentException.class, () -> {	
			forumManager.getForum(null, dto.getId());
		});
	}

	@Test
	public void testGetForumWithNullForumId() {
		//call under test
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
		//call under test
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
		//call under test
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
		//call under test
		assertEquals(forumManager.getForum(userInfo, forumId), dto);
	}


	@Test
	public void testGetForumByObjectIdAndTypeWithEntityType() {
		when(mockNodeDao.doesNodeExist(KeyFactory.stringToKey(projectId)))
				.thenReturn(true);
		when(mockAuthManager.canAccess(
				userInfo, projectId, ENTITY_TYPE, READ_ACCESS)).thenReturn(SUCCESS);
		when(mockForumDao.getForumByObjectIdAndType(projectId, ForumObjectType.ENTITY)).thenReturn(dto);
		//call under test
		assertEquals(dto, forumManager.getForumByObjectIdAndType(userInfo, projectId, ForumObjectType.ENTITY));
	}

	@Test
	public void testGetForumByObjectIdAndTypeWithAccessRequirementType() {
		String arId = "456";
		Forum arForum = new Forum();
		arForum.setId("2");
		arForum.setObjectId(arId);
		arForum.setObjectType(ForumObjectType.ACCESS_REQUIREMENT);

		when(mockAccessRequirementDao.getConcreteType(arId))
				.thenReturn(ManagedACTAccessRequirement.class.getName());
		when(mockDataAccessAuthManager.canReviewAccessRequirementSubmissions(userInfo, arId))
				.thenReturn(SUCCESS);
		when(mockForumDao.getForumByObjectIdAndType(arId, ForumObjectType.ACCESS_REQUIREMENT))
				.thenReturn(arForum);
		//call under test
		assertEquals(arForum, forumManager.getForumByObjectIdAndType(userInfo, arId, ForumObjectType.ACCESS_REQUIREMENT));
	}

	@Test
	public void testGetForumByObjectIdAndTypeWithNonExistingAccessRequirement() {
		String arId = "-1";
		Forum arForum = new Forum();
		arForum.setId("2");
		arForum.setObjectId(arId);
		arForum.setObjectType(ForumObjectType.ACCESS_REQUIREMENT);

		when(mockAccessRequirementDao.getConcreteType(arId))
				.thenReturn(ManagedACTAccessRequirement.class.getName());
		when(mockDataAccessAuthManager.canReviewAccessRequirementSubmissions(userInfo, arId))
				.thenReturn(SUCCESS);
		when(mockForumDao.getForumByObjectIdAndType(arId, ForumObjectType.ACCESS_REQUIREMENT)).thenThrow(NotFoundException.class);

		//call under test
		assertThrows(NotFoundException.class, () -> {
			forumManager.getForumByObjectIdAndType(userInfo, arId, ForumObjectType.ACCESS_REQUIREMENT);
		});
	}

	@Test
	public void testGetForumByObjectIdAndTypeWithNonManagedAR() {
		String arId = "456";
		when(mockAccessRequirementDao.getConcreteType(arId))
				.thenReturn(SelfSignAccessRequirement.class.getName());

		//call under test
		assertThrows(IllegalArgumentException.class, () -> {
			forumManager.getForumByObjectIdAndType(userInfo, arId, ForumObjectType.ACCESS_REQUIREMENT);
		});
	}

	@Test
	public void testGetForumByObjectIdAndTypeWithAccessRequirementUnauthorized() {
		String arId = "456";
		when(mockAccessRequirementDao.getConcreteType(arId))
				.thenReturn(ManagedACTAccessRequirement.class.getName());
		when(mockDataAccessAuthManager.canReviewAccessRequirementSubmissions(userInfo, arId))
				.thenReturn(FAILED);

		//call under test
		assertThrows(UnauthorizedException.class, () -> {
			forumManager.getForumByObjectIdAndType(userInfo, arId, ForumObjectType.ACCESS_REQUIREMENT);
		});
	}
}
