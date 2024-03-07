package org.sagebionetworks.repo.manager.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.authentication.TwoFactorAuthManager;
import org.sagebionetworks.repo.manager.entity.decider.AccessContext;
import org.sagebionetworks.repo.manager.entity.decider.UsersEntityAccessInfo;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DataType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.ar.AccessRestrictionStatusDao;
import org.sagebionetworks.repo.model.ar.UserRestrictionStatusWithHasUnmet;
import org.sagebionetworks.repo.model.ar.UsersRequirementStatus;
import org.sagebionetworks.repo.model.ar.UsersRestrictionStatus;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.dbo.entity.UserEntityPermissionsState;
import org.sagebionetworks.repo.model.dbo.entity.UsersEntityPermissionsDao;
import org.sagebionetworks.repo.model.dbo.file.download.v2.FileActionRequired;
import org.sagebionetworks.repo.model.download.EnableTwoFa;
import org.sagebionetworks.repo.model.download.MeetAccessRequirement;
import org.sagebionetworks.repo.model.download.RequestDownload;
import org.sagebionetworks.repo.model.jdo.KeyFactory;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.model.AuthorizationConstants.ERR_MSG_YOU_LACK_ACCESS_TO_REQUESTED_ENTITY_TEMPLATE;

@ExtendWith(MockitoExtension.class)
public class EntityAuthorizationManagerUnitTest {

	@Mock
	private AccessRestrictionStatusDao mockAccessRestrictionStatusDao;
	@Mock
	private UsersEntityPermissionsDao mockUsersEntityPermissionsDao;
	@Mock
	private TwoFactorAuthManager mockTwoFactorAuthManager;

	@InjectMocks
	@Spy
	private EntityAuthorizationManagerImpl entityAuthManager;
	
	private UserInfo userInfo;

	private String entityId;
	private Long entityIdLong;
	private List<Long> entityIds;
	private Long benefactorId;
	private UserEntityPermissionsState permissionsState;
	private Map<Long, UserEntityPermissionsState> mapIdToState;
	
	private UsersRestrictionStatus accessRestrictions;
	private Map<Long, UsersRestrictionStatus> mapIdToAccess;
	
	private UsersRestrictionStatus restrictionStatus;
	private UserRestrictionStatusWithHasUnmet userRestrictionStatusWithHasUnmet;
	private AccessContext accessContext;

	@BeforeEach
	public void before() {
		boolean isAdmin = false;
		userInfo = new UserInfo(isAdmin, 123L);
		userInfo.setAcceptsTermsOfUse(true);
		userInfo.getGroups().add(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());
		userInfo.getGroups().add(userInfo.getId());
		userInfo.getGroups().add(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId());
		entityId = "syn456";
		entityIdLong = KeyFactory.stringToKey(entityId);
		entityIds = KeyFactory.stringToKeySingletonList(entityId);
		benefactorId = 789L;

		permissionsState = new UserEntityPermissionsState(entityIdLong).withBenefactorId(benefactorId).withDoesEntityExist(true);
		mapIdToState = new LinkedHashMap<Long, UserEntityPermissionsState>();
		mapIdToState.put(entityIdLong, permissionsState);
		
		accessRestrictions = new UsersRestrictionStatus().withSubjectId(entityIdLong).withUserId(userInfo.getId());
		mapIdToAccess = new LinkedHashMap<Long, UsersRestrictionStatus>();
		mapIdToAccess.put(entityIdLong, accessRestrictions);
		
		restrictionStatus = new UsersRestrictionStatus().withSubjectId(entityIdLong).withUserId(userInfo.getId());
		userRestrictionStatusWithHasUnmet = new UserRestrictionStatusWithHasUnmet(permissionsState, restrictionStatus);
		accessContext = new AccessContext().withUser(userInfo).withPermissionsState(permissionsState)
				.withUserRestrictionStatusWithHasUnmet(userRestrictionStatusWithHasUnmet);
	}

	@Test
	public void testGetUserPermissionsForEntityWithNoPermissions() {
		when(mockUsersEntityPermissionsDao.getEntityPermissionsAsMap(any(), any())).thenReturn(mapIdToState);
		when(mockAccessRestrictionStatusDao.getEntityStatusAsMap(any(), any(), any())).thenReturn(mapIdToAccess);
		
		
		userInfo.setAcceptsTermsOfUse(false);
		userInfo.getGroups().remove(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());
		// call under test
		UserEntityPermissions permissions = entityAuthManager.getUserPermissionsForEntity(userInfo, entityId);
		UserEntityPermissions expected = createAllFalseUserEntityPermissions();
		assertEquals(expected, permissions);
		
		verify(mockUsersEntityPermissionsDao).getEntityPermissionsAsMap(userInfo.getGroups(), entityIds);
		verify(mockAccessRestrictionStatusDao).getEntityStatusAsMap(entityIds, userInfo.getId(), userInfo.getGroups());
		
	}
	
	
	@Test
	public void testGetUserPermissionsForEntityWithCertifiedUser() {
		when(mockUsersEntityPermissionsDao.getEntityPermissionsAsMap(any(), any())).thenReturn(mapIdToState);
		when(mockAccessRestrictionStatusDao.getEntityStatusAsMap(any(), any(), any())).thenReturn(mapIdToAccess);
		
		
		userInfo.setAcceptsTermsOfUse(false);
		userInfo.getGroups().add(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());
		// call under test
		UserEntityPermissions permissions = entityAuthManager.getUserPermissionsForEntity(userInfo, entityId);
		UserEntityPermissions expected = createAllFalseUserEntityPermissions();
		expected.setIsCertifiedUser(true);
		assertEquals(expected, permissions);
		
		verify(mockUsersEntityPermissionsDao).getEntityPermissionsAsMap(userInfo.getGroups(), entityIds);
		verify(mockAccessRestrictionStatusDao).getEntityStatusAsMap(entityIds, userInfo.getId(), userInfo.getGroups());
		
	}
	
	@Test
	public void testGetUserPermissionsForEntityWithCanCreate() {
		when(mockUsersEntityPermissionsDao.getEntityPermissionsAsMap(any(), any())).thenReturn(mapIdToState);
		when(mockAccessRestrictionStatusDao.getEntityStatusAsMap(any(), any(), any())).thenReturn(mapIdToAccess);
		
		
		permissionsState.withDoesEntityExist(true);
		permissionsState.withHasCreate(true);
		userInfo.setAcceptsTermsOfUse(true);
		userInfo.getGroups().add(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());
		// call under test
		UserEntityPermissions permissions = entityAuthManager.getUserPermissionsForEntity(userInfo, entityId);
		UserEntityPermissions expected = createAllFalseUserEntityPermissions();
		expected.setIsCertifiedUser(true);
		expected.setCanAddChild(true);
		expected.setCanCertifiedUserAddChild(true);
		expected.setCanUpload(true);
		assertEquals(expected, permissions);
		
		verify(mockUsersEntityPermissionsDao).getEntityPermissionsAsMap(userInfo.getGroups(), entityIds);
		verify(mockAccessRestrictionStatusDao).getEntityStatusAsMap(entityIds, userInfo.getId(), userInfo.getGroups());
		
	}
	
	@Test
	public void testGetUserPermissionsForEntityWithCanCreateButNotCertified() {
		when(mockUsersEntityPermissionsDao.getEntityPermissionsAsMap(any(), any())).thenReturn(mapIdToState);
		when(mockAccessRestrictionStatusDao.getEntityStatusAsMap(any(), any(), any())).thenReturn(mapIdToAccess);
		
		
		permissionsState.withDoesEntityExist(true);
		permissionsState.withHasCreate(true);
		userInfo.setAcceptsTermsOfUse(true);
		userInfo.getGroups().remove(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());
		// call under test
		UserEntityPermissions permissions = entityAuthManager.getUserPermissionsForEntity(userInfo, entityId);
		UserEntityPermissions expected = createAllFalseUserEntityPermissions();
		expected.setIsCertifiedUser(false);
		expected.setCanAddChild(false);
		expected.setCanCertifiedUserAddChild(true);
		expected.setCanUpload(true);
		assertEquals(expected, permissions);
		
		verify(mockUsersEntityPermissionsDao).getEntityPermissionsAsMap(userInfo.getGroups(), entityIds);
		verify(mockAccessRestrictionStatusDao).getEntityStatusAsMap(entityIds, userInfo.getId(), userInfo.getGroups());
		
	}
	
	@Test
	public void testGetUserPermissionsForEntityWithCanChangePermission() {
		when(mockUsersEntityPermissionsDao.getEntityPermissionsAsMap(any(), any())).thenReturn(mapIdToState);
		when(mockAccessRestrictionStatusDao.getEntityStatusAsMap(any(), any(), any())).thenReturn(mapIdToAccess);
		
		
		permissionsState.withDoesEntityExist(true);
		permissionsState.withHasChangePermissions(true);

		// call under test
		UserEntityPermissions permissions = entityAuthManager.getUserPermissionsForEntity(userInfo, entityId);
		UserEntityPermissions expected = createAllFalseUserEntityPermissions();
		expected.setCanUpload(true);
		expected.setIsCertifiedUser(true);
		expected.setCanChangePermissions(true);
		assertEquals(expected, permissions);
		
		verify(mockUsersEntityPermissionsDao).getEntityPermissionsAsMap(userInfo.getGroups(), entityIds);
		verify(mockAccessRestrictionStatusDao).getEntityStatusAsMap(entityIds, userInfo.getId(), userInfo.getGroups());
		
	}
	
	@Test
	public void testGetUserPermissionsForEntityWithCanMoveTrue() {
		when(mockUsersEntityPermissionsDao.getEntityPermissionsAsMap(any(), any())).thenReturn(mapIdToState);
		when(mockAccessRestrictionStatusDao.getEntityStatusAsMap(any(), any(), any())).thenReturn(mapIdToAccess);
		
		
		permissionsState.withDoesEntityExist(true);
		permissionsState.withHasChangePermissions(true);
		permissionsState.withHasUpdate(true);

		// call under test
		UserEntityPermissions permissions = entityAuthManager.getUserPermissionsForEntity(userInfo, entityId);
		UserEntityPermissions expected = createAllFalseUserEntityPermissions();
		expected.setCanUpload(true);
		expected.setIsCertifiedUser(true);
		expected.setCanMove(true);
		expected.setCanChangePermissions(true);
		expected.setCanEdit(true);
		expected.setCanCertifiedUserEdit(true);
		assertEquals(expected, permissions);
		
		verify(mockUsersEntityPermissionsDao).getEntityPermissionsAsMap(userInfo.getGroups(), entityIds);
		verify(mockAccessRestrictionStatusDao).getEntityStatusAsMap(entityIds, userInfo.getId(), userInfo.getGroups());
		
	}
	
	@Test
	public void testGetUserPermissionsForEntityWithCanMoveAndNoChangePermissoins() {
		when(mockUsersEntityPermissionsDao.getEntityPermissionsAsMap(any(), any())).thenReturn(mapIdToState);
		when(mockAccessRestrictionStatusDao.getEntityStatusAsMap(any(), any(), any())).thenReturn(mapIdToAccess);
		
		
		permissionsState.withDoesEntityExist(true);
		permissionsState.withHasChangePermissions(false);
		permissionsState.withHasUpdate(true);

		// call under test
		UserEntityPermissions permissions = entityAuthManager.getUserPermissionsForEntity(userInfo, entityId);
		UserEntityPermissions expected = createAllFalseUserEntityPermissions();
		expected.setCanUpload(true);
		expected.setIsCertifiedUser(true);
		expected.setCanMove(false);
		expected.setCanChangePermissions(false);
		expected.setCanEdit(true);
		expected.setCanCertifiedUserEdit(true);
		assertEquals(expected, permissions);
		
		verify(mockUsersEntityPermissionsDao).getEntityPermissionsAsMap(userInfo.getGroups(), entityIds);
		verify(mockAccessRestrictionStatusDao).getEntityStatusAsMap(entityIds, userInfo.getId(), userInfo.getGroups());
		
	}
	
	@Test
	public void testGetUserPermissionsForEntityWithCanMoveAndNoUpdate() {
		when(mockUsersEntityPermissionsDao.getEntityPermissionsAsMap(any(), any())).thenReturn(mapIdToState);
		when(mockAccessRestrictionStatusDao.getEntityStatusAsMap(any(), any(), any())).thenReturn(mapIdToAccess);
		
		
		permissionsState.withDoesEntityExist(true);
		permissionsState.withHasChangePermissions(true);
		permissionsState.withHasUpdate(false);

		// call under test
		UserEntityPermissions permissions = entityAuthManager.getUserPermissionsForEntity(userInfo, entityId);
		UserEntityPermissions expected = createAllFalseUserEntityPermissions();
		expected.setCanUpload(true);
		expected.setIsCertifiedUser(true);
		expected.setCanMove(false);
		expected.setCanChangePermissions(true);
		expected.setCanEdit(false);
		expected.setCanCertifiedUserEdit(false);
		assertEquals(expected, permissions);
		
		verify(mockUsersEntityPermissionsDao).getEntityPermissionsAsMap(userInfo.getGroups(), entityIds);
		verify(mockAccessRestrictionStatusDao).getEntityStatusAsMap(entityIds, userInfo.getId(), userInfo.getGroups());
		
	}
	
	@Test
	public void testGetUserPermissionsForEntityWithCanChangeSettings() {
		when(mockUsersEntityPermissionsDao.getEntityPermissionsAsMap(any(), any())).thenReturn(mapIdToState);
		when(mockAccessRestrictionStatusDao.getEntityStatusAsMap(any(), any(), any())).thenReturn(mapIdToAccess);
		
		
		permissionsState.withDoesEntityExist(true);
		permissionsState.withHasChangeSettings(true);

		// call under test
		UserEntityPermissions permissions = entityAuthManager.getUserPermissionsForEntity(userInfo, entityId);
		UserEntityPermissions expected = createAllFalseUserEntityPermissions();
		expected.setCanUpload(true);
		expected.setIsCertifiedUser(true);
		expected.setCanChangeSettings(true);
		assertEquals(expected, permissions);
		
		verify(mockUsersEntityPermissionsDao).getEntityPermissionsAsMap(userInfo.getGroups(), entityIds);
		verify(mockAccessRestrictionStatusDao).getEntityStatusAsMap(entityIds, userInfo.getId(), userInfo.getGroups());
		
	}
	
	@Test
	public void testGetUserPermissionsForEntityWithCanDelete() {
		when(mockUsersEntityPermissionsDao.getEntityPermissionsAsMap(any(), any())).thenReturn(mapIdToState);
		when(mockAccessRestrictionStatusDao.getEntityStatusAsMap(any(), any(), any())).thenReturn(mapIdToAccess);
		
		
		permissionsState.withDoesEntityExist(true);
		permissionsState.withHasDelete(true);

		// call under test
		UserEntityPermissions permissions = entityAuthManager.getUserPermissionsForEntity(userInfo, entityId);
		UserEntityPermissions expected = createAllFalseUserEntityPermissions();
		expected.setCanUpload(true);
		expected.setIsCertifiedUser(true);
		expected.setCanDelete(true);
		assertEquals(expected, permissions);
		
		verify(mockUsersEntityPermissionsDao).getEntityPermissionsAsMap(userInfo.getGroups(), entityIds);
		verify(mockAccessRestrictionStatusDao).getEntityStatusAsMap(entityIds, userInfo.getId(), userInfo.getGroups());
		
	}
	
	@Test
	public void testGetUserPermissionsForEntityWithCanEdit() {
		when(mockUsersEntityPermissionsDao.getEntityPermissionsAsMap(any(), any())).thenReturn(mapIdToState);
		when(mockAccessRestrictionStatusDao.getEntityStatusAsMap(any(), any(), any())).thenReturn(mapIdToAccess);
		
		
		permissionsState.withDoesEntityExist(true);
		permissionsState.withHasUpdate(true);

		// call under test
		UserEntityPermissions permissions = entityAuthManager.getUserPermissionsForEntity(userInfo, entityId);
		UserEntityPermissions expected = createAllFalseUserEntityPermissions();
		expected.setCanUpload(true);
		expected.setIsCertifiedUser(true);
		expected.setCanEdit(true);
		expected.setCanCertifiedUserEdit(true);
		assertEquals(expected, permissions);
		
		verify(mockUsersEntityPermissionsDao).getEntityPermissionsAsMap(userInfo.getGroups(), entityIds);
		verify(mockAccessRestrictionStatusDao).getEntityStatusAsMap(entityIds, userInfo.getId(), userInfo.getGroups());
		
	}
	
	@Test
	public void testGetUserPermissionsForEntityWithCanEditNotCertified() {
		when(mockUsersEntityPermissionsDao.getEntityPermissionsAsMap(any(), any())).thenReturn(mapIdToState);
		when(mockAccessRestrictionStatusDao.getEntityStatusAsMap(any(), any(), any())).thenReturn(mapIdToAccess);
		
		
		permissionsState.withDoesEntityExist(true);
		permissionsState.withHasUpdate(true);
		userInfo.getGroups().remove(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());

		// call under test
		UserEntityPermissions permissions = entityAuthManager.getUserPermissionsForEntity(userInfo, entityId);
		UserEntityPermissions expected = createAllFalseUserEntityPermissions();
		expected.setCanUpload(true);
		expected.setIsCertifiedUser(false);
		expected.setCanEdit(false);
		expected.setCanCertifiedUserEdit(true);
		assertEquals(expected, permissions);
		
		verify(mockUsersEntityPermissionsDao).getEntityPermissionsAsMap(userInfo.getGroups(), entityIds);
		verify(mockAccessRestrictionStatusDao).getEntityStatusAsMap(entityIds, userInfo.getId(), userInfo.getGroups());
		
	}
	
	@Test
	public void testGetUserPermissionsForEntityWithCanView() {
		when(mockUsersEntityPermissionsDao.getEntityPermissionsAsMap(any(), any())).thenReturn(mapIdToState);
		when(mockAccessRestrictionStatusDao.getEntityStatusAsMap(any(), any(), any())).thenReturn(mapIdToAccess);
		
		
		permissionsState.withDoesEntityExist(true);
		permissionsState.withHasRead(true);

		// call under test
		UserEntityPermissions permissions = entityAuthManager.getUserPermissionsForEntity(userInfo, entityId);
		UserEntityPermissions expected = createAllFalseUserEntityPermissions();
		expected.setCanUpload(true);
		expected.setIsCertifiedUser(true);
		expected.setCanView(true);
		assertEquals(expected, permissions);
		
		verify(mockUsersEntityPermissionsDao).getEntityPermissionsAsMap(userInfo.getGroups(), entityIds);
		verify(mockAccessRestrictionStatusDao).getEntityStatusAsMap(entityIds, userInfo.getId(), userInfo.getGroups());
		
	}
	
	@Test
	public void testGetUserPermissionsForEntityWithCanPublicRead() {
		when(mockUsersEntityPermissionsDao.getEntityPermissionsAsMap(any(), any())).thenReturn(mapIdToState);
		when(mockAccessRestrictionStatusDao.getEntityStatusAsMap(any(), any(), any())).thenReturn(mapIdToAccess);
		
		
		permissionsState.withDoesEntityExist(true);
		permissionsState.withHasPublicRead(true);

		// call under test
		UserEntityPermissions permissions = entityAuthManager.getUserPermissionsForEntity(userInfo, entityId);
		UserEntityPermissions expected = createAllFalseUserEntityPermissions();
		expected.setCanUpload(true);
		expected.setIsCertifiedUser(true);
		expected.setCanPublicRead(true);
		assertEquals(expected, permissions);
		
		verify(mockUsersEntityPermissionsDao).getEntityPermissionsAsMap(userInfo.getGroups(), entityIds);
		verify(mockAccessRestrictionStatusDao).getEntityStatusAsMap(entityIds, userInfo.getId(), userInfo.getGroups());
		
	}
	
	@Test
	public void testGetUserPermissionsForEntityWithCanDownload() {
		when(mockUsersEntityPermissionsDao.getEntityPermissionsAsMap(any(), any())).thenReturn(mapIdToState);
		when(mockAccessRestrictionStatusDao.getEntityStatusAsMap(any(), any(), any())).thenReturn(mapIdToAccess);
		
		
		permissionsState.withDoesEntityExist(true);
		permissionsState.withHasDownload(true);

		// call under test
		UserEntityPermissions permissions = entityAuthManager.getUserPermissionsForEntity(userInfo, entityId);
		UserEntityPermissions expected = createAllFalseUserEntityPermissions();
		expected.setCanUpload(true);
		expected.setIsCertifiedUser(true);
		expected.setCanDownload(true);
		assertEquals(expected, permissions);
		
		verify(mockUsersEntityPermissionsDao).getEntityPermissionsAsMap(userInfo.getGroups(), entityIds);
		verify(mockAccessRestrictionStatusDao).getEntityStatusAsMap(entityIds, userInfo.getId(), userInfo.getGroups());
		
	}
	
	/**
	 * Can upload if you accept the terms of use.
	 */
	@Test
	public void testGetUserPermissionsForEntityWithCanUploadFalse() {
		when(mockUsersEntityPermissionsDao.getEntityPermissionsAsMap(any(), any())).thenReturn(mapIdToState);
		when(mockAccessRestrictionStatusDao.getEntityStatusAsMap(any(), any(), any())).thenReturn(mapIdToAccess);
		
		
		permissionsState.withDoesEntityExist(true);
		userInfo.setAcceptsTermsOfUse(false);

		// call under test
		UserEntityPermissions permissions = entityAuthManager.getUserPermissionsForEntity(userInfo, entityId);
		UserEntityPermissions expected = createAllFalseUserEntityPermissions();
		expected.setCanUpload(false);
		expected.setIsCertifiedUser(true);
		assertEquals(expected, permissions);
		
		verify(mockUsersEntityPermissionsDao).getEntityPermissionsAsMap(userInfo.getGroups(), entityIds);
		verify(mockAccessRestrictionStatusDao).getEntityStatusAsMap(entityIds, userInfo.getId(), userInfo.getGroups());
		
	}
	
	
	/**
	 * Can upload if you accept the terms of use.
	 */
	@Test
	public void testGetUserPermissionsForEntityWithCanUploadTrue() {
		when(mockUsersEntityPermissionsDao.getEntityPermissionsAsMap(any(), any())).thenReturn(mapIdToState);
		when(mockAccessRestrictionStatusDao.getEntityStatusAsMap(any(), any(), any())).thenReturn(mapIdToAccess);
		
		
		permissionsState.withDoesEntityExist(true);
		userInfo.setAcceptsTermsOfUse(true);

		// call under test
		UserEntityPermissions permissions = entityAuthManager.getUserPermissionsForEntity(userInfo, entityId);
		UserEntityPermissions expected = createAllFalseUserEntityPermissions();
		expected.setCanUpload(true);
		expected.setIsCertifiedUser(true);
		assertEquals(expected, permissions);
		
		verify(mockUsersEntityPermissionsDao).getEntityPermissionsAsMap(userInfo.getGroups(), entityIds);
		verify(mockAccessRestrictionStatusDao).getEntityStatusAsMap(entityIds, userInfo.getId(), userInfo.getGroups());
		
	}
	
	@Test
	public void testGetUserPermissionsForEntityWithCanModerate() {
		when(mockUsersEntityPermissionsDao.getEntityPermissionsAsMap(any(), any())).thenReturn(mapIdToState);
		when(mockAccessRestrictionStatusDao.getEntityStatusAsMap(any(), any(), any())).thenReturn(mapIdToAccess);
		
		
		permissionsState.withDoesEntityExist(true);
		permissionsState.withHasModerate(true);

		// call under test
		UserEntityPermissions permissions = entityAuthManager.getUserPermissionsForEntity(userInfo, entityId);
		UserEntityPermissions expected = createAllFalseUserEntityPermissions();
		expected.setCanUpload(true);
		expected.setIsCertifiedUser(true);
		expected.setCanModerate(true);
		assertEquals(expected, permissions);
		
		verify(mockUsersEntityPermissionsDao).getEntityPermissionsAsMap(userInfo.getGroups(), entityIds);
		verify(mockAccessRestrictionStatusDao).getEntityStatusAsMap(entityIds, userInfo.getId(), userInfo.getGroups());
		
	}
	
	@Test
	public void testGetUserPermissionsForEntityWithCreatedBy() {
		when(mockUsersEntityPermissionsDao.getEntityPermissionsAsMap(any(), any())).thenReturn(mapIdToState);
		when(mockAccessRestrictionStatusDao.getEntityStatusAsMap(any(), any(), any())).thenReturn(mapIdToAccess);
		
		
		permissionsState.withDoesEntityExist(true);
		Long createdBy = 987L;
		permissionsState.withEntityCreatedBy(createdBy);

		// call under test
		UserEntityPermissions permissions = entityAuthManager.getUserPermissionsForEntity(userInfo, entityId);
		UserEntityPermissions expected = createAllFalseUserEntityPermissions();
		expected.setCanUpload(true);
		expected.setIsCertifiedUser(true);
		expected.setOwnerPrincipalId(createdBy);
		assertEquals(expected, permissions);
		
		verify(mockUsersEntityPermissionsDao).getEntityPermissionsAsMap(userInfo.getGroups(), entityIds);
		verify(mockAccessRestrictionStatusDao).getEntityStatusAsMap(entityIds, userInfo.getId(), userInfo.getGroups());
		
	}
	
	@Test
	public void testGetUserPermissionsForEntityWithCanEnableInheritance() {
		when(mockUsersEntityPermissionsDao.getEntityPermissionsAsMap(any(), any())).thenReturn(mapIdToState);
		when(mockAccessRestrictionStatusDao.getEntityStatusAsMap(any(), any(), any())).thenReturn(mapIdToAccess);
		
		
		permissionsState.withDoesEntityExist(true);
		permissionsState.withEntityParentId(654L);
		permissionsState.withHasChangePermissions(true);

		// call under test
		UserEntityPermissions permissions = entityAuthManager.getUserPermissionsForEntity(userInfo, entityId);
		UserEntityPermissions expected = createAllFalseUserEntityPermissions();
		expected.setCanUpload(true);
		expected.setIsCertifiedUser(true);
		expected.setCanChangePermissions(true);
		expected.setCanEnableInheritance(true);
		assertEquals(expected, permissions);
		
		verify(mockUsersEntityPermissionsDao).getEntityPermissionsAsMap(userInfo.getGroups(), entityIds);
		verify(mockAccessRestrictionStatusDao).getEntityStatusAsMap(entityIds, userInfo.getId(), userInfo.getGroups());
		
	}
	
	@Test
	public void testGetUserPermissionsForEntityWithNullDateType() {
		when(mockUsersEntityPermissionsDao.getEntityPermissionsAsMap(any(), any())).thenReturn(mapIdToState);
		when(mockAccessRestrictionStatusDao.getEntityStatusAsMap(any(), any(), any())).thenReturn(mapIdToAccess);
		
		
		permissionsState.withDataType(null);

		// call under test
		UserEntityPermissions permissions = entityAuthManager.getUserPermissionsForEntity(userInfo, entityId);
		UserEntityPermissions expected = createAllFalseUserEntityPermissions();
		expected.setIsCertifiedUser(true);
		expected.setCanUpload(true);
		expected.setIsEntityOpenData(false);
		assertEquals(expected, permissions);
		
		verify(mockUsersEntityPermissionsDao).getEntityPermissionsAsMap(userInfo.getGroups(), entityIds);
		verify(mockAccessRestrictionStatusDao).getEntityStatusAsMap(entityIds, userInfo.getId(), userInfo.getGroups());
		
	}
	
	@Test
	public void testGetUserPermissionsForEntityWithDateTypeOpenDate() {
		when(mockUsersEntityPermissionsDao.getEntityPermissionsAsMap(any(), any())).thenReturn(mapIdToState);
		when(mockAccessRestrictionStatusDao.getEntityStatusAsMap(any(), any(), any())).thenReturn(mapIdToAccess);
		
		
		permissionsState.withDataType(DataType.OPEN_DATA);

		// call under test
		UserEntityPermissions permissions = entityAuthManager.getUserPermissionsForEntity(userInfo, entityId);
		UserEntityPermissions expected = createAllFalseUserEntityPermissions();
		expected.setIsCertifiedUser(true);
		expected.setCanUpload(true);
		expected.setIsEntityOpenData(true);
		assertEquals(expected, permissions);
		
		verify(mockUsersEntityPermissionsDao).getEntityPermissionsAsMap(userInfo.getGroups(), entityIds);
		verify(mockAccessRestrictionStatusDao).getEntityStatusAsMap(entityIds, userInfo.getId(), userInfo.getGroups());
		
	}
	
	@Test
	public void testGetUserPermissionsForEntityWithDateTypeSensitiveDate() {
		when(mockUsersEntityPermissionsDao.getEntityPermissionsAsMap(any(), any())).thenReturn(mapIdToState);
		when(mockAccessRestrictionStatusDao.getEntityStatusAsMap(any(), any(), any())).thenReturn(mapIdToAccess);		
		
		permissionsState.withDataType(DataType.SENSITIVE_DATA);

		// call under test
		UserEntityPermissions permissions = entityAuthManager.getUserPermissionsForEntity(userInfo, entityId);
		UserEntityPermissions expected = createAllFalseUserEntityPermissions();
		expected.setIsCertifiedUser(true);
		expected.setCanUpload(true);
		expected.setIsEntityOpenData(false);
		assertEquals(expected, permissions);
		
		verify(mockUsersEntityPermissionsDao).getEntityPermissionsAsMap(userInfo.getGroups(), entityIds);
		verify(mockAccessRestrictionStatusDao).getEntityStatusAsMap(entityIds, userInfo.getId(), userInfo.getGroups());
		
	}
	
	@Test
	public void testHasAccessWithNullUser() {
		userInfo = null;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			entityAuthManager.hasAccess(userInfo, entityId, ACCESS_TYPE.READ);
		}).getMessage();
		assertEquals("UserInfo is required.", message);
	}
	
	@Test
	public void testHasAccessWithNullEntityId() {
		entityId = null;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			entityAuthManager.hasAccess(userInfo, entityId, ACCESS_TYPE.READ);
		}).getMessage();
		assertEquals("entityId is required.", message);
	}
	
	@Test
	public void testHasAccessWithNullAccessType() {
		ACCESS_TYPE[] accessType = null;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			entityAuthManager.hasAccess(userInfo, entityId, accessType);
		}).getMessage();
		assertEquals("accessTypes is required.", message);
	}
	
	@Test
	public void testHasAccessWithEmptyAccessType() {
		ACCESS_TYPE[] accessType = {};
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			entityAuthManager.hasAccess(userInfo, entityId, accessType);
		}).getMessage();
		assertEquals("At least one ACCESS_TYPE must be provided", message);
	}
	
	
	@Test
	public void testHasAccessWithSingleType() {
		when(mockUsersEntityPermissionsDao.getEntityPermissionsAsMap(any(), any())).thenReturn(mapIdToState);
		permissionsState.withDoesEntityExist(true);
		permissionsState.withHasRead(true);
		// call under test
		AuthorizationStatus status = entityAuthManager.hasAccess(userInfo, entityId, ACCESS_TYPE.READ);
		AuthorizationStatus expected = AuthorizationStatus.authorized();
		assertEquals(expected, status);
		
		verify(mockUsersEntityPermissionsDao).getEntityPermissionsAsMap(userInfo.getGroups(), entityIds);
	}

	@Test
	public void testHasAccessWithSingleTypeFalse() {
		when(mockUsersEntityPermissionsDao.getEntityPermissionsAsMap(any(), any())).thenReturn(mapIdToState);
		permissionsState.withDoesEntityExist(true);
		permissionsState.withHasRead(false);
		// call under test
		AuthorizationStatus status = entityAuthManager.hasAccess(userInfo, entityId, ACCESS_TYPE.READ);
		AuthorizationStatus expected = AuthorizationStatus
				.accessDenied(String.format(ERR_MSG_YOU_LACK_ACCESS_TO_REQUESTED_ENTITY_TEMPLATE, ACCESS_TYPE.READ));
		assertEquals(expected, status);

		verify(mockUsersEntityPermissionsDao).getEntityPermissionsAsMap(userInfo.getGroups(), entityIds);
	}
	
	@Test
	public void testHasAccessWithMultipleTypes() {
		when(mockUsersEntityPermissionsDao.getEntityPermissionsAsMap(any(), any())).thenReturn(mapIdToState);
		permissionsState.withDoesEntityExist(true);
		permissionsState.withHasRead(true);
		permissionsState.withHasDelete(true);
		permissionsState.withHasUpdate(true);
		// call under test
		AuthorizationStatus status = entityAuthManager.hasAccess(userInfo, entityId, ACCESS_TYPE.READ,
				ACCESS_TYPE.DELETE, ACCESS_TYPE.UPDATE);
		AuthorizationStatus expected = AuthorizationStatus.authorized();
		assertEquals(expected, status);

		verify(mockUsersEntityPermissionsDao).getEntityPermissionsAsMap(userInfo.getGroups(), entityIds);
	}
	
	@Test
	public void testHasAccessWithMultipleTypesOneFalse() {
		when(mockUsersEntityPermissionsDao.getEntityPermissionsAsMap(any(), any())).thenReturn(mapIdToState);
		permissionsState.withDoesEntityExist(true);
		permissionsState.withHasRead(true);
		permissionsState.withHasDelete(false);
		permissionsState.withHasUpdate(true);
		// call under test
		AuthorizationStatus status = entityAuthManager.hasAccess(userInfo, entityId, ACCESS_TYPE.READ,
				ACCESS_TYPE.DELETE, ACCESS_TYPE.UPDATE);
		AuthorizationStatus expected = AuthorizationStatus
				.accessDenied(String.format(ERR_MSG_YOU_LACK_ACCESS_TO_REQUESTED_ENTITY_TEMPLATE, ACCESS_TYPE.DELETE));
		assertEquals(expected, status);

		verify(mockUsersEntityPermissionsDao).getEntityPermissionsAsMap(userInfo.getGroups(), entityIds);
	}
	
	@Test
	public void testGetActionsRequiredForDownloadWithAuthorized() {
		EntityAuthorizationManagerImpl manager = Mockito.spy(entityAuthManager);
		
		doReturn(List.of(new UsersEntityAccessInfo(accessContext, AuthorizationStatus.authorized()))).when(manager).batchHasAccess(any(), any(), any());

		List<FileActionRequired> expected = Collections.emptyList();
		
		// Call under test
		List<FileActionRequired> result = manager.getActionsRequiredForDownload(userInfo, entityIds);
		
		assertEquals(expected, result);
		
		verify(manager).batchHasAccess(userInfo, entityIds, ACCESS_TYPE.DOWNLOAD);
	}

	@Test
	public void testGetActionsRequiredForDownloadWithNoRestrictions() {
		EntityAuthorizationManagerImpl manager = Mockito.spy(entityAuthManager);
		
		doReturn(List.of(new UsersEntityAccessInfo(accessContext, AuthorizationStatus.accessDenied("no")))).when(manager).batchHasAccess(any(), any(), any());

		List<FileActionRequired> expected = List.of(
			new FileActionRequired().withFileId(entityIdLong)
				.withAction(new RequestDownload().setBenefactorId(benefactorId))
		);
		
		// Call under test
		List<FileActionRequired> result = manager.getActionsRequiredForDownload(userInfo, entityIds);
		
		assertEquals(expected, result);
		
		verify(manager).batchHasAccess(userInfo, entityIds, ACCESS_TYPE.DOWNLOAD);
		
	}

	@Test
	public void testGetActionsRequiredForDownloadWithNonExistentEntity() {
		permissionsState.withDoesEntityExist(false);
		
		EntityAuthorizationManagerImpl manager = Mockito.spy(entityAuthManager);
		
		doReturn(List.of(new UsersEntityAccessInfo(accessContext, AuthorizationStatus.accessDenied("no")))).when(manager).batchHasAccess(any(), any(), any());

		List<FileActionRequired> expected = Collections.emptyList();
		
		// Call under test
		List<FileActionRequired> result = manager.getActionsRequiredForDownload(userInfo, entityIds);
		
		assertEquals(expected, result);
		
		verify(manager).batchHasAccess(userInfo, entityIds, ACCESS_TYPE.DOWNLOAD);

	}

	@Test
	public void testGetActionsRequiredForDownloadWithMetRestrictions() {
		EntityAuthorizationManagerImpl manager = Mockito.spy(entityAuthManager);
		
		doReturn(List.of(new UsersEntityAccessInfo(accessContext, AuthorizationStatus.accessDenied("no")))).when(manager).batchHasAccess(any(), any(), any());
		
		restrictionStatus.withRestrictionStatus(List.of(new UsersRequirementStatus().withIsUnmet(false).withRequirementId(432L)));
		
		List<FileActionRequired> expected = List.of(
			new FileActionRequired().withFileId(entityIdLong)
				.withAction(new RequestDownload().setBenefactorId(benefactorId))
		);
		
		// Call under test
		List<FileActionRequired> result = manager.getActionsRequiredForDownload(userInfo, entityIds);
		
		assertEquals(expected, result);
		
		verify(manager).batchHasAccess(userInfo, entityIds, ACCESS_TYPE.DOWNLOAD);
	}

	@Test
	public void testGetActionsRequiredForDownloadWithMixedRestrictions() {
		EntityAuthorizationManagerImpl manager = Mockito.spy(entityAuthManager);
		restrictionStatus.withRestrictionStatus(List.of(
				new UsersRequirementStatus().withIsUnmet(false).withRequirementId(432L),
				new UsersRequirementStatus().withIsUnmet(true).withRequirementId(321L)));
		accessContext.withUserRestrictionStatusWithHasUnmet(new UserRestrictionStatusWithHasUnmet(permissionsState, restrictionStatus));

		doReturn(List.of(new UsersEntityAccessInfo(accessContext, AuthorizationStatus.accessDenied("no")))).when(manager).batchHasAccess(any(), any(), any());

		List<FileActionRequired> expected = List.of(
			new FileActionRequired().withFileId(entityIdLong)
				.withAction(new MeetAccessRequirement().setAccessRequirementId(321L))
		);
		
		// Call under test
		List<FileActionRequired> result = manager.getActionsRequiredForDownload(userInfo, entityIds);
		
		assertEquals(expected, result);
		
		verify(manager).batchHasAccess(userInfo, entityIds, ACCESS_TYPE.DOWNLOAD);
	}

	@Test
	public void testGetActionsRequiredForDownloadWithNullRestrictionStatus() {
		accessContext.withUserRestrictionStatusWithHasUnmet(null);
		
		EntityAuthorizationManagerImpl manager = Mockito.spy(entityAuthManager);
		
		doReturn(List.of(new UsersEntityAccessInfo(accessContext, AuthorizationStatus.accessDenied("no")))).when(manager).batchHasAccess(any(), any(), any());
		
		String message = assertThrows(IllegalArgumentException.class, () -> {
			manager.getActionsRequiredForDownload(userInfo, entityIds);
		}).getMessage();

		assertEquals("info.userRestrictionStatusWithHasUnmet() is required.", message);
	}

	@Test
	public void testGetActionsRequiredForDownloadWithMultipleUnmetRestrictions() {
		EntityAuthorizationManagerImpl manager = Mockito.spy(entityAuthManager);
		restrictionStatus.withRestrictionStatus(List.of(
				new UsersRequirementStatus().withIsUnmet(true).withRequirementId(432L),
				new UsersRequirementStatus().withIsUnmet(true).withRequirementId(321L)));
		accessContext.withUserRestrictionStatusWithHasUnmet(new UserRestrictionStatusWithHasUnmet(permissionsState, restrictionStatus));

		doReturn(List.of(new UsersEntityAccessInfo(accessContext, AuthorizationStatus.accessDenied("no")))).when(manager).batchHasAccess(any(), any(), any());

		List<FileActionRequired> expected = List.of(
			new FileActionRequired().withFileId(entityIdLong)
				.withAction(new MeetAccessRequirement().setAccessRequirementId(432L)),
			new FileActionRequired().withFileId(entityIdLong)
				.withAction(new MeetAccessRequirement().setAccessRequirementId(321L))
		);
		
		// Call under test
		List<FileActionRequired> result = manager.getActionsRequiredForDownload(userInfo, entityIds);
		
		assertEquals(expected, result);
		
		verify(manager).batchHasAccess(userInfo, entityIds, ACCESS_TYPE.DOWNLOAD);
	}
	
	@Test
	public void testGetActionsRequiredForDownloadWithUnmetRestrictionsAndUnmetTwoFaRestriction() {
		
		EntityAuthorizationManagerImpl manager = Mockito.spy(entityAuthManager);
		restrictionStatus.withRestrictionStatus(List.of(
				new UsersRequirementStatus().withIsUnmet(true).withRequirementId(432L).withIsTwoFaRequired(false),
				new UsersRequirementStatus().withIsUnmet(true).withRequirementId(789L).withIsTwoFaRequired(true)));
		accessContext.withUserRestrictionStatusWithHasUnmet(new UserRestrictionStatusWithHasUnmet(permissionsState, restrictionStatus));

		doReturn(List.of(new UsersEntityAccessInfo(accessContext, AuthorizationStatus.accessDenied("no")))).when(manager).batchHasAccess(any(), any(), any());

		List<FileActionRequired> expected = List.of(
			new FileActionRequired().withFileId(entityIdLong)
				.withAction(new MeetAccessRequirement().setAccessRequirementId(432L)),
			new FileActionRequired().withFileId(entityIdLong)
				.withAction(new MeetAccessRequirement().setAccessRequirementId(789L)),
			new FileActionRequired().withFileId(entityIdLong)
				.withAction(new EnableTwoFa().setAccessRequirementId(789L))
		);
		
		// Call under test
		List<FileActionRequired> result = manager.getActionsRequiredForDownload(userInfo, entityIds);
		
		assertEquals(expected, result);
		
		verify(manager).batchHasAccess(userInfo, entityIds, ACCESS_TYPE.DOWNLOAD);
	}
	
	@Test
	public void testGetActionsRequiredForDownloadWithMetTwoFaRestriction() {
		
		EntityAuthorizationManagerImpl manager = Mockito.spy(entityAuthManager);
		
		doReturn(List.of(new UsersEntityAccessInfo(accessContext, AuthorizationStatus.accessDenied("no")))).when(manager).batchHasAccess(any(), any(), any());
		
		restrictionStatus.withRestrictionStatus(List.of(new UsersRequirementStatus().withIsUnmet(false).withRequirementId(432L).withIsTwoFaRequired(true)));
		userInfo.setTwoFactorAuthEnabled(true);

		List<FileActionRequired> expected = List.of(
			new FileActionRequired().withFileId(entityIdLong)
				.withAction(new RequestDownload().setBenefactorId(benefactorId))
		);
		
		// Call under test
		List<FileActionRequired> result = manager.getActionsRequiredForDownload(userInfo, entityIds);
		
		assertEquals(expected, result);
		
		verify(manager).batchHasAccess(userInfo, entityIds, ACCESS_TYPE.DOWNLOAD);
	}
	
	@Test
	public void testGetActionsRequiredForDownloadWithUnmetTwoFaRestriction() {
		EntityAuthorizationManagerImpl manager = Mockito.spy(entityAuthManager);
		
		doReturn(List.of(new UsersEntityAccessInfo(accessContext, AuthorizationStatus.accessDenied("no")))).when(manager).batchHasAccess(any(), any(), any());
		
		restrictionStatus.withRestrictionStatus(List.of(new UsersRequirementStatus().withIsUnmet(false).withRequirementId(432L).withIsTwoFaRequired(true)));
		
		List<FileActionRequired> expected = List.of(
			new FileActionRequired().withFileId(entityIdLong)
				.withAction(new EnableTwoFa().setAccessRequirementId(432L))
		);
		
		// Call under test
		List<FileActionRequired> result = manager.getActionsRequiredForDownload(userInfo, entityIds);
		
		assertEquals(expected, result);
		
		verify(manager).batchHasAccess(userInfo, entityIds, ACCESS_TYPE.DOWNLOAD);
	}
	
	
	public UserEntityPermissions createAllFalseUserEntityPermissions() {
		UserEntityPermissions up =  new UserEntityPermissions();
		up.setCanAddChild(false);
		up.setCanCertifiedUserAddChild(false);
		up.setCanCertifiedUserEdit(false);
		up.setCanChangePermissions(false);
		up.setCanChangeSettings(false);
		up.setCanDelete(false);
		up.setCanDownload(false);
		up.setCanEdit(false);
		up.setCanModerate(false);
		up.setCanPublicRead(false);
		up.setCanUpload(false);
		up.setCanView(false);
		up.setIsCertifiedUser(false);
		up.setCanEnableInheritance(false);
		up.setIsCertificationRequired(true);
		up.setIsEntityOpenData(false);
		up.setCanMove(false);
		return up;
	}

}
