package org.sagebionetworks.repo.manager.form;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.AuthorizationStatus;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.form.FormDao;
import org.sagebionetworks.repo.model.form.FormGroup;
import org.sagebionetworks.repo.model.util.AccessControlListUtil;

@ExtendWith(MockitoExtension.class)
public class FormManagerTest {

	@Mock
	FormDao mockFormDao;

	@Mock
	AccessControlListDAO mockAclDao;

	@Mock
	AuthorizationManager mockAuthManager;

	@Captor
	ArgumentCaptor<AccessControlList> aclCaptor;

	@InjectMocks
	FormManagerImpl manager;

	UserInfo user;
	String groupId;
	FormGroup groupToReturn;
	AccessControlList aclToReturn;

	@BeforeEach
	public void before() {
		boolean isAdmin = false;
		user = new UserInfo(isAdmin, 555L);
		groupId = "456";
		groupToReturn = new FormGroup();
		groupToReturn.setGroupId(groupId);
		aclToReturn = AccessControlListUtil.createACL(groupId, user, FormManagerImpl.FORM_GROUP_ADMIN_PERMISSIONS,
				new Date());
	}

	@Test
	public void testCreateGroupNewName() {
		String name = "someName";
		// name does not already exist
		when(mockFormDao.lookupGroupByName(name)).thenReturn(Optional.empty());

		when(mockFormDao.createFormGroup(anyLong(), anyString())).thenReturn(groupToReturn);
		// call under test
		FormGroup group = manager.createGroup(user, name);
		assertEquals(groupToReturn, group);
		verify(mockFormDao).createFormGroup(user.getId(), name);
		verify(mockFormDao).lookupGroupByName(name);
		// should create an ACL
		verify(mockAclDao).create(aclCaptor.capture(), eq(ObjectType.FORM_GROUP));
		verify(mockAuthManager, never()).canAccess(any(UserInfo.class), anyString(), any(ObjectType.class),
				any(ACCESS_TYPE.class));
		AccessControlList acl = aclCaptor.getValue();
		assertNotNull(acl);
		assertEquals(groupToReturn.getGroupId(), acl.getId());
		assertNotNull(acl.getResourceAccess());
		assertEquals(1, acl.getResourceAccess().size());
		ResourceAccess entry = acl.getResourceAccess().iterator().next();
		assertEquals(user.getId(), entry.getPrincipalId());
		assertEquals(FormManagerImpl.FORM_GROUP_ADMIN_PERMISSIONS, entry.getAccessType());
	}

	@Test
	public void testCreateGroupNameExistsAuthorized() {
		String name = "someName";
		// name already exists
		when(mockFormDao.lookupGroupByName(name)).thenReturn(Optional.of(groupToReturn));
		when(mockAuthManager.canAccess(user, groupToReturn.getGroupId(), ObjectType.FORM_GROUP, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationStatus.authorized());

		// call under test
		FormGroup group = manager.createGroup(user, name);
		assertEquals(groupToReturn, group);
		verify(mockAuthManager).canAccess(user, groupToReturn.getGroupId(), ObjectType.FORM_GROUP, ACCESS_TYPE.READ);
		verify(mockFormDao, never()).createFormGroup(anyLong(), anyString());
		verify(mockAclDao, never()).create(any(AccessControlList.class), any(ObjectType.class));
	}

	/**
	 * Group name is taken and the caller does not have access to the existing
	 * group.
	 */
	@Test
	public void testCreateGroupNameExistsUnAuthorized() {
		String name = "someName";
		// name already exists
		when(mockFormDao.lookupGroupByName(name)).thenReturn(Optional.of(groupToReturn));
		when(mockAuthManager.canAccess(user, groupToReturn.getGroupId(), ObjectType.FORM_GROUP, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationStatus.accessDenied("no access for you"));

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			manager.createGroup(user, name);
		});
		assertEquals("The group name: someName is unavailable, please chooser another name.", exception.getMessage());
		verify(mockAuthManager).canAccess(user, groupToReturn.getGroupId(), ObjectType.FORM_GROUP, ACCESS_TYPE.READ);
		verify(mockFormDao, never()).createFormGroup(anyLong(), anyString());
		verify(mockAclDao, never()).create(any(AccessControlList.class), any(ObjectType.class));
	}

	@Test
	public void testCreateGroupUserNull() {
		assertThrows(IllegalArgumentException.class, () -> {
			user = null;
			String name = "notNull";
			manager.createGroup(user, name);
		});
	}

	@Test
	public void testCreateGroupNameNull() {
		assertThrows(IllegalArgumentException.class, () -> {
			String name = null;
			manager.createGroup(user, name);
		});
	}

	@Test
	public void testGetGroupAclAuthorized() {
		when(mockAclDao.get(groupId, ObjectType.FORM_GROUP)).thenReturn(aclToReturn);
		when(mockAuthManager.canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationStatus.authorized());
		// call under test
		AccessControlList acl = manager.getGroupAcl(user, groupId);
		assertEquals(aclToReturn, acl);
		verify(mockAuthManager).canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.READ);
		verify(mockAclDao).get(groupId, ObjectType.FORM_GROUP);
	}

	@Test
	public void testGetGroupAclUnauthorized() {
		when(mockAuthManager.canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationStatus.accessDenied("no access"));
		assertThrows(UnauthorizedException.class, () -> {
			// call under test
			manager.getGroupAcl(user, groupId);
		});
		verify(mockAuthManager).canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.READ);
		verify(mockAclDao, never()).get(anyString(), any(ObjectType.class));
	}

	@Test
	public void testGetGroupAclUserNull() {
		assertThrows(IllegalArgumentException.class, () -> {
			user = null;
			manager.getGroupAcl(user, groupId);
		});
	}

	@Test
	public void testGetGroupAclGroupIdNull() {
		assertThrows(IllegalArgumentException.class, () -> {
			groupId = null;
			manager.getGroupAcl(user, groupId);
		});
	}

	@Test
	public void testUpdateGroupAclAuthorized() {
		when(mockAclDao.get(groupId, ObjectType.FORM_GROUP)).thenReturn(aclToReturn);
		// need both read and CHANGE_PERMISSIONS.
		when(mockAuthManager.canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.CHANGE_PERMISSIONS))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockAuthManager.canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationStatus.authorized());
		aclToReturn.setId(null);
		// call under test
		AccessControlList acl = manager.updateGroupAcl(user, groupId, aclToReturn);
		assertEquals(aclToReturn, acl);
		verify(mockAuthManager).canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.CHANGE_PERMISSIONS);
		verify(mockAuthManager).canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.READ);
		verify(mockAclDao).update(aclToReturn, ObjectType.FORM_GROUP);
		// The passed groupId should always be used.
		assertEquals(groupId, acl.getId());
	}

	@Test
	public void testUpdateGroupAclUnauthorized() {
		// need both read and CHANGE_PERMISSIONS.
		when(mockAuthManager.canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.CHANGE_PERMISSIONS))
				.thenReturn(AuthorizationStatus.accessDenied("no access"));

		assertThrows(UnauthorizedException.class, () -> {
			// call under test
			manager.updateGroupAcl(user, groupId, aclToReturn);
		});

		verify(mockAuthManager).canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.CHANGE_PERMISSIONS);
		verify(mockAuthManager, never()).canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.READ);
		verify(mockAclDao, never()).update(any(AccessControlList.class), any(ObjectType.class));
	}

	@Test
	public void testUpdateGroupAclInvalidGroupId() {
		groupId = "notANumber";

		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.updateGroupAcl(user, groupId, aclToReturn);
		});

		verify(mockAuthManager, never()).canAccess(any(UserInfo.class), anyString(), any(ObjectType.class),
				any(ACCESS_TYPE.class));
		verify(mockAclDao, never()).update(any(AccessControlList.class), any(ObjectType.class));
	}

	@Test
	public void testUpdateGroupAclRemoveSelf() {
		// cannot remove yourself from the ACL.
		aclToReturn.getResourceAccess().clear();

		assertThrows(InvalidModelException.class, () -> {
			// call under test
			manager.updateGroupAcl(user, groupId, aclToReturn);
		});

		verify(mockAuthManager, never()).canAccess(any(UserInfo.class), anyString(), any(ObjectType.class),
				any(ACCESS_TYPE.class));
		verify(mockAclDao, never()).update(any(AccessControlList.class), any(ObjectType.class));
	}

	@Test
	public void testUpdateGroupAclNullUser() {
		user = null;
		assertThrows(InvalidModelException.class, () -> {
			// call under test
			manager.updateGroupAcl(user, groupId, aclToReturn);
		});
	}
	
	@Test
	public void testUpdateGroupAclNullGroup() {
		groupId = null;
		assertThrows(InvalidModelException.class, () -> {
			// call under test
			manager.updateGroupAcl(user, groupId, aclToReturn);
		});
	}
	
	@Test
	public void testUpdateGroupAclNullAcl() {
		aclToReturn = null;
		assertThrows(InvalidModelException.class, () -> {
			// call under test
			manager.updateGroupAcl(user, groupId, aclToReturn);
		});
	}
}
