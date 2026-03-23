package org.sagebionetworks.repo.manager.grid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.UserInfoTestHelper;
import org.sagebionetworks.repo.manager.entity.EntityAuthorizationManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.dbo.grid.GridDao;
import org.sagebionetworks.repo.model.dbo.grid.GridSource;
import org.sagebionetworks.repo.web.NotFoundException;

@ExtendWith(MockitoExtension.class)
public class GridAuthorizationManagerTest {

	@Mock
	private GridDao mockGridDao;
	@Mock
	private EntityAuthorizationManager mockEntityAuthorizationManager;

	@InjectMocks
	private GridAuthorizationManagerImpl manager;

	@Mock
	private UserInfo mockUser;

	private String gridSessionId;
	private Long userId;
	private GridSource gridSource;
	private Long entityId;

	@BeforeEach
	public void before() {
		gridSessionId = "123";
		userId = 456L;
		entityId = 789L;
		gridSource = new GridSource(entityId, EntityType.table);
	}

	@ParameterizedTest
	@EnumSource(value = EntityType.class, names = { "table", "recordset" })
	public void testHasGridSessionAccessWithDownloadTypes(EntityType type) {
		gridSource = new GridSource(entityId, type);
		when(mockUser.getId()).thenReturn(userId);
		when(mockGridDao.getGridSessionOwner(gridSessionId)).thenReturn(Optional.of(userId));
		when(mockGridDao.getSessionSource(gridSessionId)).thenReturn(Optional.of(gridSource));
		when(mockEntityAuthorizationManager.hasAccess(mockUser, entityId.toString(), ACCESS_TYPE.READ,
				ACCESS_TYPE.DOWNLOAD, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationStatus.authorized());

		// call under test
		AuthorizationStatus status = manager.hasGridSessionAccess(mockUser, gridSessionId);
		assertEquals(AuthorizationStatus.authorized(), status);
	}

	@Test
	public void testHasGridSessionAccessWithReadTypes() {
		gridSource = new GridSource(entityId, EntityType.entityview);
		when(mockUser.getId()).thenReturn(userId);
		when(mockGridDao.getGridSessionOwner(gridSessionId)).thenReturn(Optional.of(userId));
		when(mockGridDao.getSessionSource(gridSessionId)).thenReturn(Optional.of(gridSource));
		when(mockEntityAuthorizationManager.hasAccess(mockUser, entityId.toString(), ACCESS_TYPE.READ))
				.thenReturn(AuthorizationStatus.authorized());

		// call under test
		AuthorizationStatus status = manager.hasGridSessionAccess(mockUser, gridSessionId);
		assertEquals(AuthorizationStatus.authorized(), status);
	}

	@ParameterizedTest
	@EnumSource(value = EntityType.class, names = { "table", "recordset", "entityview" }, mode = Mode.EXCLUDE)
	public void testHasGridSessionAccessWithUnsupportedTypes(EntityType type) {
		gridSource = new GridSource(entityId, type);
		when(mockUser.getId()).thenReturn(userId);
		when(mockGridDao.getGridSessionOwner(gridSessionId)).thenReturn(Optional.of(userId));
		when(mockGridDao.getSessionSource(gridSessionId)).thenReturn(Optional.of(gridSource));

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.hasGridSessionAccess(mockUser, gridSessionId);
		}).getMessage();

		assertEquals("Unsupported grid source type: " + type.name(), message);

		verifyZeroInteractions(mockEntityAuthorizationManager);
	}

	@Test
	public void testHasGridSessionAccessWithNoSource() {
		when(mockUser.getId()).thenReturn(userId);
		when(mockGridDao.getGridSessionOwner(gridSessionId)).thenReturn(Optional.of(userId));
		when(mockGridDao.getSessionSource(gridSessionId)).thenReturn(Optional.empty());

		// call under test
		AuthorizationStatus status = manager.hasGridSessionAccess(mockUser, gridSessionId);
		assertEquals(AuthorizationStatus.authorized(), status);

		verifyZeroInteractions(mockEntityAuthorizationManager);
	}

	@Test
	public void testHasGridSessionAccessWithGridOwnerEmpty() {
		when(mockGridDao.getGridSessionOwner(gridSessionId)).thenReturn(Optional.empty());

		String message = assertThrows(NotFoundException.class, () -> {
			// call under test
			manager.hasGridSessionAccess(mockUser, gridSessionId);
		}).getMessage();

		assertEquals("Grid session not found: " + gridSessionId, message);

		verifyZeroInteractions(mockEntityAuthorizationManager);
	}

	@Test
	public void testHasGridSessionAccessWithNotMemberOfOwnerGroup() {
		Long ownerGroup = 444L;
		when(mockUser.getId()).thenReturn(userId);
		when(mockUser.getGroups()).thenReturn(Set.of(userId, 333L));
		when(mockGridDao.getGridSessionOwner(gridSessionId)).thenReturn(Optional.of(ownerGroup));

		String message = assertThrows(UnauthorizedException.class, () -> {
			// call under test
			manager.hasGridSessionAccess(mockUser, gridSessionId).checkAuthorizationOrElseThrow();
		}).getMessage();

		assertEquals("You are not authorized to access this resource.", message);

		verifyZeroInteractions(mockEntityAuthorizationManager);
	}

	@Test
	public void testHasGridSessionAccessWithMemberOfOwnerGroup() {
		Long ownerGroup = 444L;
		when(mockUser.getId()).thenReturn(userId);
		when(mockUser.getGroups()).thenReturn(Set.of(userId, ownerGroup));
		when(mockGridDao.getGridSessionOwner(gridSessionId)).thenReturn(Optional.of(ownerGroup));

		// call under test
		AuthorizationStatus status = manager.hasGridSessionAccess(mockUser, gridSessionId);
		assertEquals(AuthorizationStatus.authorized(), status);
	}

	@Test
	public void testValidateGridOwnerWithGroupOwner() {
		Long ownerGroup = 444L;
		when(mockUser.getId()).thenReturn(userId);
		when(mockUser.getGroups()).thenReturn(Set.of(userId, ownerGroup));

		// call under test
		Long result = manager.validateGridOwner(mockUser, ownerGroup.toString());
		assertEquals(ownerGroup, result);
	}

	@Test
	public void testValidateGridOwnerWithNotMemberGroup() {
		Long ownerGroup = 444L;
		when(mockUser.getId()).thenReturn(userId);
		when(mockUser.getGroups()).thenReturn(Set.of(userId, 333L));

		String message = assertThrows(UnauthorizedException.class, () -> {
			// call under test
			manager.validateGridOwner(mockUser, ownerGroup.toString());
		}).getMessage();

		assertEquals("Caller must be a member of the owner's team.", message);
	}

	@Test
	public void testValidateGridOwnerWithUserOwner() {
		when(mockUser.getId()).thenReturn(userId);

		// call under test
		Long result = manager.validateGridOwner(mockUser, userId.toString());
		assertEquals(userId, result);
	}

	@Test
	public void testValidateGridOwnerWithNullOwner() {
		when(mockUser.getId()).thenReturn(userId);

		// call under test
		Long result = manager.validateGridOwner(mockUser, null);
		assertEquals(userId, result);
	}

	@Test
	public void testValidateGridOwnerWithAnonymous() {
		when(mockUser.isUserAnonymous()).thenReturn(true);

		String message = assertThrows(UnauthorizedException.class, () -> {
			// call under test
			manager.validateGridOwner(mockUser, null);
		}).getMessage();

		assertEquals("Must login to perform this action", message);
	}

	@Test
	public void testValidateGridOwnerWithInvalidOwner() {
		when(mockUser.isUserAnonymous()).thenReturn(false);
		
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.validateGridOwner(mockUser, "not a number");
		}).getMessage();

		assertEquals("Invalid ownerPrincipalId: 'not a number'", message);
	}

	@Test
	public void testGetRowLevelFilterUserInfoWithTable() {
		gridSource = new GridSource(entityId, EntityType.table);
		when(mockGridDao.getSessionSource(gridSessionId)).thenReturn(Optional.of(gridSource));

		// call under test
		UserInfo user = manager.getRowLevelFilterUserInfo(mockUser, gridSessionId);
		assertEquals(user, mockUser);
	}

	@Test
	public void testGetRowLevelFilterUserInfoWithEntityViewAndUserOwner() {
		when(mockUser.getId()).thenReturn(userId);
		gridSource = new GridSource(entityId, EntityType.entityview);
		when(mockGridDao.getSessionSource(gridSessionId)).thenReturn(Optional.of(gridSource));
		when(mockGridDao.getGridSessionOwner(gridSessionId)).thenReturn(Optional.of(userId));

		// call under test
		UserInfo user = manager.getRowLevelFilterUserInfo(mockUser, gridSessionId);
		assertEquals(user, mockUser);
	}

	@Test
	public void testGetRowLevelFilterUserInfoWithEntityViewAndGroupOwner() {
		Long groupOwnerId = 555L;
		when(mockUser.getId()).thenReturn(userId);
		when(mockUser.getRealmAuthenticatedUsersId()).thenReturn(BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId());
		when(mockUser.getRealmPublicUsersId()).thenReturn(BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId());
		when(mockUser.getRealmId()).thenReturn(AuthorizationConstants.DEFAULT_REALM_ID);
		when(mockUser.getRealmAnonymousUserId()).thenReturn(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());
		gridSource = new GridSource(entityId, EntityType.entityview);
		when(mockGridDao.getSessionSource(gridSessionId)).thenReturn(Optional.of(gridSource));
		when(mockGridDao.getGridSessionOwner(gridSessionId)).thenReturn(Optional.of(groupOwnerId));

		// call under test
		UserInfo user = manager.getRowLevelFilterUserInfo(mockUser, gridSessionId);
		UserInfo expected = UserInfoTestHelper.createUserInfo(false, groupOwnerId);
		expected.setGroups(Set.of(groupOwnerId, BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId(),
				BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId()));
		assertEquals(expected, user);
	}

	@ParameterizedTest
	@EnumSource(value = EntityType.class, names = { "table", "entityview" }, mode = Mode.EXCLUDE)
	public void testGetRowLevelFilterUserInfoWithUnsupportedType(EntityType type) {
		gridSource = new GridSource(entityId, type);
		when(mockGridDao.getSessionSource(gridSessionId)).thenReturn(Optional.of(gridSource));

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.getRowLevelFilterUserInfo(mockUser, gridSessionId);
		}).getMessage();
		assertEquals("Unsupported grid source type: " + type.name(), message);
	}
	
	@Test
	public void testGetRowLevelFilterUserInfoWithOwnerEmpty() {
		gridSource = new GridSource(entityId, EntityType.entityview);
		when(mockGridDao.getSessionSource(gridSessionId)).thenReturn(Optional.of(gridSource));
		when(mockGridDao.getGridSessionOwner(gridSessionId)).thenReturn(Optional.empty());
		
		String message = assertThrows(NotFoundException.class, () -> {
			// call under test
			manager.getRowLevelFilterUserInfo(mockUser, gridSessionId);
		}).getMessage();
		assertEquals("Grid session not found: "+gridSessionId, message);	
	}
	
	@Test
	public void testGetRowLevelFilterUserInfoWithNoSource() {
		gridSource = new GridSource(entityId, EntityType.entityview);
		when(mockGridDao.getSessionSource(gridSessionId)).thenReturn(Optional.empty());
		
		String message = assertThrows(NotFoundException.class, () -> {
			// call under test
			manager.getRowLevelFilterUserInfo(mockUser, gridSessionId);
		}).getMessage();
		assertEquals("Grid does not have a source", message);	
	}

}
