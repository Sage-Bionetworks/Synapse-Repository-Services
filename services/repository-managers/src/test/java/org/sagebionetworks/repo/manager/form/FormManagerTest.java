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

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.dbo.form.FormDao;
import org.sagebionetworks.repo.model.form.FormData;
import org.sagebionetworks.repo.model.form.FormGroup;
import org.sagebionetworks.repo.model.form.StateEnum;
import org.sagebionetworks.repo.model.util.AccessControlListUtil;

import com.amazonaws.services.pinpointsmsvoice.model.NotFoundException;

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
	String groupName;
	String groupId;
	FormGroup groupToReturn;
	AccessControlList aclToReturn;
	String dataFileHandleId;
	String validName;
	String formDataId;
	FormData formData;

	UserInfo anonymousUser;

	@BeforeEach
	public void before() {
		boolean isAdmin = false;
		user = new UserInfo(isAdmin, 555L);
		groupId = "456";
		groupName = "some group";
		groupToReturn = new FormGroup();
		groupToReturn.setGroupId(groupId);
		aclToReturn = AccessControlListUtil.createACL(groupId, user, FormManagerImpl.FORM_GROUP_ADMIN_PERMISSIONS,
				new Date());
		dataFileHandleId = "987";
		validName = "aValidName";
		formData = new FormData();
		formDataId = "321";
		formData.setFormDataId(formDataId);

		anonymousUser = new UserInfo(isAdmin, BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());
	}

	@Test
	public void testValidateName() {
		String name = StringUtils.repeat('a', FormManagerImpl.MAX_NAME_CHARS);
		// call under test
		FormManagerImpl.validateName(name);
	}

	@Test
	public void testValidateNameTooLong() {
		String name = StringUtils.repeat('a', FormManagerImpl.MAX_NAME_CHARS + 1);
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			FormManagerImpl.validateName(name);
		});
		assertEquals("Name must be 256 characters or less", e.getMessage());
	}

	@Test
	public void testValidateNameShort() {
		String name = StringUtils.repeat('a', FormManagerImpl.MIN_NAME_CHARS);
		// call under test
		FormManagerImpl.validateName(name);
	}

	@Test
	public void testValidateNameTooShort() {
		String name = StringUtils.repeat('a', FormManagerImpl.MIN_NAME_CHARS - 1);
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			FormManagerImpl.validateName(name);
		});
		assertEquals("Name must be at least 3 characters", e.getMessage());
	}

	@Test
	public void testCreateGroupNewName() {
		// name does not already exist
		when(mockFormDao.lookupGroupByName(groupName)).thenReturn(Optional.empty());

		when(mockFormDao.createFormGroup(anyLong(), anyString())).thenReturn(groupToReturn);
		// call under test
		FormGroup group = manager.createGroup(user, groupName);
		assertEquals(groupToReturn, group);
		verify(mockFormDao).createFormGroup(user.getId(), groupName);
		verify(mockFormDao).lookupGroupByName(groupName);
		// should create an ACL
		verify(mockAclDao).create(aclCaptor.capture(), eq(ObjectType.FORM_GROUP));
		verify(mockAclDao, never()).canAccess(any(UserInfo.class), anyString(), any(ObjectType.class),
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
	public void testCreateGroupAnonymous() {
		assertThrows(UnauthorizedException.class, () -> {
			// call under test
			manager.createGroup(anonymousUser, groupName);
		});
		verify(mockFormDao, never()).createFormGroup(anyLong(), anyString());
	}

	@Test
	public void testCreateGroupNameExistsAuthorized() {
		String name = "someName";
		// name already exists
		when(mockFormDao.lookupGroupByName(name)).thenReturn(Optional.of(groupToReturn));
		when(mockAclDao.canAccess(user, groupToReturn.getGroupId(), ObjectType.FORM_GROUP, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationStatus.authorized());

		// call under test
		FormGroup group = manager.createGroup(user, name);
		assertEquals(groupToReturn, group);
		verify(mockAclDao).canAccess(user, groupToReturn.getGroupId(), ObjectType.FORM_GROUP, ACCESS_TYPE.READ);
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
		when(mockAclDao.canAccess(user, groupToReturn.getGroupId(), ObjectType.FORM_GROUP, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationStatus.accessDenied("no access for you"));

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			manager.createGroup(user, name);
		});
		assertEquals("The group name: someName is unavailable, please chooser another name.", exception.getMessage());
		verify(mockAclDao).canAccess(user, groupToReturn.getGroupId(), ObjectType.FORM_GROUP, ACCESS_TYPE.READ);
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
	public void testCreateGroupNameTooLong() {
		assertThrows(IllegalArgumentException.class, () -> {
			String name = StringUtils.repeat('a', FormManagerImpl.MAX_NAME_CHARS + 1);
			manager.createGroup(user, name);
		});
	}

	@Test
	public void testGetGroupAclAuthorized() {
		when(mockAclDao.get(groupId, ObjectType.FORM_GROUP)).thenReturn(aclToReturn);
		when(mockAclDao.canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationStatus.authorized());
		// call under test
		AccessControlList acl = manager.getGroupAcl(user, groupId);
		assertEquals(aclToReturn, acl);
		verify(mockAclDao).canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.READ);
		verify(mockAclDao).get(groupId, ObjectType.FORM_GROUP);
	}

	@Test
	public void testGetGroupAclUnauthorized() {
		when(mockAclDao.canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationStatus.accessDenied("no access"));
		assertThrows(UnauthorizedException.class, () -> {
			// call under test
			manager.getGroupAcl(user, groupId);
		});
		verify(mockAclDao).canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.READ);
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
		when(mockAclDao.canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.CHANGE_PERMISSIONS))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockAclDao.canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationStatus.authorized());
		aclToReturn.setId(null);
		// call under test
		AccessControlList acl = manager.updateGroupAcl(user, groupId, aclToReturn);
		assertEquals(aclToReturn, acl);
		verify(mockAclDao).canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.CHANGE_PERMISSIONS);
		verify(mockAclDao).canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.READ);
		verify(mockAclDao).update(aclToReturn, ObjectType.FORM_GROUP);
		// The passed groupId should always be used.
		assertEquals(groupId, acl.getId());
	}

	@Test
	public void testUpdateGroupAclUnauthorized() {
		// need both read and CHANGE_PERMISSIONS.
		when(mockAclDao.canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.CHANGE_PERMISSIONS))
				.thenReturn(AuthorizationStatus.accessDenied("no access"));

		assertThrows(UnauthorizedException.class, () -> {
			// call under test
			manager.updateGroupAcl(user, groupId, aclToReturn);
		});

		verify(mockAclDao).canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.CHANGE_PERMISSIONS);
		verify(mockAclDao, never()).canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.READ);
		verify(mockAclDao, never()).update(any(AccessControlList.class), any(ObjectType.class));
	}

	@Test
	public void testUpdateGroupAclInvalidGroupId() {
		groupId = "notANumber";

		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.updateGroupAcl(user, groupId, aclToReturn);
		});

		verify(mockAclDao, never()).canAccess(any(UserInfo.class), anyString(), any(ObjectType.class),
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

		verify(mockAclDao, never()).canAccess(any(UserInfo.class), anyString(), any(ObjectType.class),
				any(ACCESS_TYPE.class));
		verify(mockAclDao, never()).update(any(AccessControlList.class), any(ObjectType.class));
	}

	@Test
	public void testUpdateGroupAclNullUser() {
		user = null;
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.updateGroupAcl(user, groupId, aclToReturn);
		});
	}

	@Test
	public void testUpdateGroupAclNullGroup() {
		groupId = null;
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.updateGroupAcl(user, groupId, aclToReturn);
		});
	}

	@Test
	public void testUpdateGroupAclNullAcl() {
		aclToReturn = null;
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.updateGroupAcl(user, groupId, aclToReturn);
		});
	}

	@Test
	public void testCreateFormData() {
		when(mockAclDao.canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.SUBMIT))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockAuthManager.canAccessRawFileHandleById(user, dataFileHandleId))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockFormDao.createFormData(user.getId(), groupId, validName, dataFileHandleId)).thenReturn(formData);

		// call under test
		FormData created = manager.createFormData(user, groupId, validName, dataFileHandleId);
		assertEquals(formData, created);
		verify(mockAclDao).canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.SUBMIT);
		verify(mockAuthManager).canAccessRawFileHandleById(user, dataFileHandleId);
		verify(mockFormDao).createFormData(user.getId(), groupId, validName, dataFileHandleId);
	}

	@Test
	public void testCreateFormDataAnonymous() {
		assertThrows(UnauthorizedException.class, () -> {
			// call under test
			manager.createFormData(anonymousUser, groupId, validName, dataFileHandleId);
		});
	}

	@Test
	public void testCreateFormDataUnauthorizedSubmit() {
		when(mockAclDao.canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.SUBMIT))
				.thenReturn(AuthorizationStatus.accessDenied("no access for you"));
		assertThrows(UnauthorizedException.class, () -> {
			// call under test
			manager.createFormData(user, groupId, validName, dataFileHandleId);
		});
		verify(mockAclDao).canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.SUBMIT);
		verify(mockAuthManager, never()).canAccessRawFileHandleById(any(UserInfo.class), anyString());
		verify(mockFormDao, never()).createFormData(any(Long.class), anyString(), anyString(), anyString());
	}

	@Test
	public void testCreateFormDataUnauthorizedFileHandle() {
		// can submit
		when(mockAclDao.canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.SUBMIT))
				.thenReturn(AuthorizationStatus.authorized());
		// does not own file.
		when(mockAuthManager.canAccessRawFileHandleById(user, dataFileHandleId))
				.thenReturn(AuthorizationStatus.accessDenied("not your file"));
		assertThrows(UnauthorizedException.class, () -> {
			// call under test
			manager.createFormData(user, groupId, validName, dataFileHandleId);
		});
		verify(mockAclDao).canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.SUBMIT);
		verify(mockAuthManager).canAccessRawFileHandleById(user, dataFileHandleId);
		verify(mockFormDao, never()).createFormData(any(Long.class), anyString(), anyString(), anyString());
	}

	@Test
	public void testCreateFormDataInvalidName() {
		String invalidName = StringUtils.repeat('a', FormManagerImpl.MAX_NAME_CHARS + 1);
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.createFormData(user, groupId, invalidName, dataFileHandleId);
		});
		verify(mockAclDao, never()).canAccess(any(UserInfo.class), anyString(), any(ObjectType.class),
				any(ACCESS_TYPE.class));
		verify(mockAuthManager, never()).canAccessRawFileHandleById(any(UserInfo.class), anyString());
		verify(mockFormDao, never()).createFormData(any(Long.class), anyString(), anyString(), anyString());
	}

	@Test
	public void testCreateFormDataNullName() {
		String invalidName = null;
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.createFormData(user, groupId, invalidName, dataFileHandleId);
		});
		verify(mockAclDao, never()).canAccess(any(UserInfo.class), anyString(), any(ObjectType.class),
				any(ACCESS_TYPE.class));
		verify(mockAuthManager, never()).canAccessRawFileHandleById(any(UserInfo.class), anyString());
		verify(mockFormDao, never()).createFormData(any(Long.class), anyString(), anyString(), anyString());
	}

	@Test
	public void testCreateFormDataNullUser() {
		user = null;
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.createFormData(user, groupId, validName, dataFileHandleId);
		});
		verify(mockAclDao, never()).canAccess(any(UserInfo.class), anyString(), any(ObjectType.class),
				any(ACCESS_TYPE.class));
		verify(mockAuthManager, never()).canAccessRawFileHandleById(any(UserInfo.class), anyString());
		verify(mockFormDao, never()).createFormData(any(Long.class), anyString(), anyString(), anyString());
	}

	@Test
	public void testCreateFormDataNullGroupId() {
		groupId = null;
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.createFormData(user, groupId, validName, dataFileHandleId);
		});
		verify(mockAclDao, never()).canAccess(any(UserInfo.class), anyString(), any(ObjectType.class),
				any(ACCESS_TYPE.class));
		verify(mockAuthManager, never()).canAccessRawFileHandleById(any(UserInfo.class), anyString());
		verify(mockFormDao, never()).createFormData(any(Long.class), anyString(), anyString(), anyString());
	}

	@Test
	public void testCreateFormDataNullDataFileHandle() {
		dataFileHandleId = null;
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.createFormData(user, groupId, validName, dataFileHandleId);
		});
		verify(mockAclDao, never()).canAccess(any(UserInfo.class), anyString(), any(ObjectType.class),
				any(ACCESS_TYPE.class));
		verify(mockAuthManager, never()).canAccessRawFileHandleById(any(UserInfo.class), anyString());
		verify(mockFormDao, never()).createFormData(any(Long.class), anyString(), anyString(), anyString());
	}

	@Test
	public void testValidateCanUpdateWaitingSubmission() {
		when(mockFormDao.getFormDataState(formDataId)).thenReturn(StateEnum.WAITING_FOR_SUBMISSION);
		// call under test
		manager.validateCanUpdateState(formDataId);
		verify(mockFormDao).getFormDataState(formDataId);
	}

	@Test
	public void testValidateCanUpdateRejected() {
		when(mockFormDao.getFormDataState(formDataId)).thenReturn(StateEnum.REJECTED);
		// call under test
		manager.validateCanUpdateState(formDataId);
		verify(mockFormDao).getFormDataState(formDataId);
	}

	@Test
	public void testValidateCanUpdateAccepted() {
		when(mockFormDao.getFormDataState(formDataId)).thenReturn(StateEnum.ACCEPTED);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.validateCanUpdateState(formDataId);
		}).getMessage();
		assertEquals(FormManagerImpl.CANNOT_UPDATE_ACCEPTED, message);
	}

	@Test
	public void testValidateCanUpdateWaitReivew() {
		when(mockFormDao.getFormDataState(formDataId)).thenReturn(StateEnum.SUBMITTED_WAITING_FOR_REVIEW);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.validateCanUpdateState(formDataId);
		}).getMessage();
		assertEquals(FormManagerImpl.CANNOT_UPDATE_WAITING_REVIEW, message);
	}

	@Test
	public void testValidateCanUpdateNull() {
		formDataId = null;
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.validateCanUpdateState(formDataId);
		});
	}

	@Test
	public void testValidateUserIsCreator() {
		when(mockFormDao.getFormDataCreator(formDataId)).thenReturn(user.getId());
		// call under test
		manager.validateUserIsCreator(user, formDataId);
		verify(mockFormDao).getFormDataCreator(formDataId);
	}

	@Test
	public void testValidateUserIsCreatorNotCreator() {
		when(mockFormDao.getFormDataCreator(formDataId)).thenReturn(user.getId() + 1);

		String message = assertThrows(UnauthorizedException.class, () -> {
			// call under test
			manager.validateUserIsCreator(user, formDataId);
		}).getMessage();

		assertEquals("Cannot update a form created by another user", message);
	}

	@Test
	public void testUpdateFormData() {
		String name = "someName";
		String dataFileHandleId = "456";
		String groupId = "333";
		when(mockFormDao.getFormDataCreator(formDataId)).thenReturn(user.getId());
		when(mockFormDao.getFormDataState(formDataId)).thenReturn(StateEnum.WAITING_FOR_SUBMISSION);
		when(mockFormDao.getFormDataGroupId(formDataId)).thenReturn(groupId);
		when(mockAclDao.canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.SUBMIT))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockAuthManager.canAccessRawFileHandleById(user, dataFileHandleId))
				.thenReturn(AuthorizationStatus.authorized());

		// call under test
		manager.updateFormData(user, formDataId, name, dataFileHandleId);
		verify(mockFormDao).getFormDataCreator(formDataId);
		verify(mockAclDao).canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.SUBMIT);
		verify(mockAuthManager).canAccessRawFileHandleById(user, dataFileHandleId);
		verify(mockFormDao).updateFormData(formDataId, name, dataFileHandleId);
	}

	@Test
	public void testUpdateFormDataNotCreator() {
		String name = "someName";
		String dataFileHandleId = "456";
		// caller is not the creator.
		when(mockFormDao.getFormDataCreator(formDataId)).thenReturn(user.getId() + 1);

		assertThrows(UnauthorizedException.class, () -> {
			// call under test
			manager.updateFormData(user, formDataId, name, dataFileHandleId);
		});
		verify(mockFormDao).getFormDataCreator(formDataId);
		verify(mockAclDao, never()).canAccess(any(UserInfo.class), anyString(), any(ObjectType.class),
				any(ACCESS_TYPE.class));
		verify(mockAuthManager, never()).canAccessRawFileHandleById(any(UserInfo.class), anyString());
		verify(mockFormDao, never()).updateFormData(anyString(), anyString(), anyString());
	}

	@Test
	public void testUpdateFormDataUnauthorizedSubmit() {
		String name = "someName";
		String dataFileHandleId = "456";
		String groupId = "333";
		when(mockFormDao.getFormDataCreator(formDataId)).thenReturn(user.getId());
		when(mockFormDao.getFormDataState(formDataId)).thenReturn(StateEnum.WAITING_FOR_SUBMISSION);
		when(mockFormDao.getFormDataGroupId(formDataId)).thenReturn(groupId);
		when(mockAclDao.canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.SUBMIT))
				.thenReturn(AuthorizationStatus.accessDenied("no access"));

		assertThrows(UnauthorizedException.class, () -> {
			// call under test
			manager.updateFormData(user, formDataId, name, dataFileHandleId);
		});

		verify(mockAclDao).canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.SUBMIT);
		verify(mockAuthManager, never()).canAccessRawFileHandleById(any(UserInfo.class), anyString());
		verify(mockFormDao, never()).updateFormData(anyString(), anyString(), anyString());
	}

	@Test
	public void testUpdateFormDataUnauthorizedFileHandle() {
		String name = "someName";
		String dataFileHandleId = "456";
		String groupId = "333";
		when(mockFormDao.getFormDataCreator(formDataId)).thenReturn(user.getId());
		when(mockFormDao.getFormDataState(formDataId)).thenReturn(StateEnum.WAITING_FOR_SUBMISSION);
		when(mockFormDao.getFormDataGroupId(formDataId)).thenReturn(groupId);
		when(mockAclDao.canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.SUBMIT))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockAuthManager.canAccessRawFileHandleById(user, dataFileHandleId))
				.thenReturn(AuthorizationStatus.accessDenied("not your file"));

		assertThrows(UnauthorizedException.class, () -> {
			// call under test
			manager.updateFormData(user, formDataId, name, dataFileHandleId);
		});

		verify(mockAclDao).canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.SUBMIT);
		verify(mockAuthManager).canAccessRawFileHandleById(user, dataFileHandleId);
		verify(mockFormDao, never()).updateFormData(anyString(), anyString(), anyString());
	}

	@Test
	public void testUpdateFormDataInvalidState() {
		String name = "someName";
		String dataFileHandleId = "456";
		when(mockFormDao.getFormDataCreator(formDataId)).thenReturn(user.getId());
		// can update an accepted from.
		when(mockFormDao.getFormDataState(formDataId)).thenReturn(StateEnum.ACCEPTED);

		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.updateFormData(user, formDataId, name, dataFileHandleId);
		});

		verify(mockAclDao, never()).canAccess(any(UserInfo.class), anyString(), any(ObjectType.class),
				any(ACCESS_TYPE.class));
		verify(mockAuthManager, never()).canAccessRawFileHandleById(any(UserInfo.class), anyString());
		verify(mockFormDao, never()).updateFormData(anyString(), anyString(), anyString());
	}

	@Test
	public void testUpdateFormDataNullName() {
		// Null name means the name is not updated
		String name = null;
		String dataFileHandleId = "456";
		String groupId = "333";
		when(mockFormDao.getFormDataCreator(formDataId)).thenReturn(user.getId());
		when(mockFormDao.getFormDataState(formDataId)).thenReturn(StateEnum.WAITING_FOR_SUBMISSION);
		when(mockFormDao.getFormDataGroupId(formDataId)).thenReturn(groupId);
		when(mockAclDao.canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.SUBMIT))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockAuthManager.canAccessRawFileHandleById(user, dataFileHandleId))
				.thenReturn(AuthorizationStatus.authorized());

		// call under test
		manager.updateFormData(user, formDataId, name, dataFileHandleId);

		verify(mockAclDao).canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.SUBMIT);
		verify(mockAuthManager).canAccessRawFileHandleById(user, dataFileHandleId);
		// name should not be updated
		verify(mockFormDao).updateFormData(formDataId, dataFileHandleId);
	}

	@Test
	public void testUpdateFormDataInvalidName() {
		String invalidName = StringUtils.repeat('a', FormManagerImpl.MAX_NAME_CHARS + 1);
		String dataFileHandleId = "456";

		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.updateFormData(user, formDataId, invalidName, dataFileHandleId);
		});

		verify(mockAclDao, never()).canAccess(any(UserInfo.class), anyString(), any(ObjectType.class),
				any(ACCESS_TYPE.class));
		verify(mockAuthManager, never()).canAccessRawFileHandleById(any(UserInfo.class), anyString());
		verify(mockFormDao, never()).updateFormData(anyString(), anyString(), anyString());
	}

	@Test
	public void testUpdateFormDataNullId() {
		String formDataId = null;
		String name = "aValidName";
		String dataFileHandleId = "456";

		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.updateFormData(user, formDataId, name, dataFileHandleId);
		});

		verify(mockAclDao, never()).canAccess(any(UserInfo.class), anyString(), any(ObjectType.class),
				any(ACCESS_TYPE.class));
		verify(mockAuthManager, never()).canAccessRawFileHandleById(any(UserInfo.class), anyString());
		verify(mockFormDao, never()).updateFormData(anyString(), anyString(), anyString());
	}

	@Test
	public void testUpdateFormDataDataFileHandle() {

		String name = "aValidName";
		String dataFileHandleId = null;

		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.updateFormData(user, formDataId, name, dataFileHandleId);
		});

		verify(mockAclDao, never()).canAccess(any(UserInfo.class), anyString(), any(ObjectType.class),
				any(ACCESS_TYPE.class));
		verify(mockAuthManager, never()).canAccessRawFileHandleById(any(UserInfo.class), anyString());
		verify(mockFormDao, never()).updateFormData(anyString(), anyString(), anyString());
	}

	@Test
	public void testDeleteFormData() {
		when(mockFormDao.getFormDataCreator(formDataId)).thenReturn(user.getId());
		when(mockFormDao.deleteFormData(formDataId)).thenReturn(true);
		// call under test
		manager.deleteFormData(user, formDataId);
		verify(mockFormDao).deleteFormData(formDataId);
	}

	@Test
	public void testDeleteFormDataNotCreator() {
		// not the creator
		when(mockFormDao.getFormDataCreator(formDataId)).thenReturn(user.getId() + 1);

		assertThrows(UnauthorizedException.class, () -> {
			// call under test
			manager.deleteFormData(user, formDataId);
		});

		verify(mockFormDao, never()).deleteFormData(anyString());
	}

	@Test
	public void testDeleteFormDataDoesNotExist() {
		when(mockFormDao.getFormDataCreator(formDataId)).thenReturn(user.getId());
		// return false means the delete form did not exist
		when(mockFormDao.deleteFormData(formDataId)).thenReturn(false);

		assertThrows(NotFoundException.class, () -> {
			// call under test
			manager.deleteFormData(user, formDataId);
		});
	}

	@Test
	public void testDeleteFormDataNullUser() {
		user = null;
		// call under test
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.deleteFormData(user, formDataId);
		});
	}

	@Test
	public void testDeleteFormDataNullId() {
		formDataId = null;
		// call under test
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.deleteFormData(user, formDataId);
		});
	}

	@Test
	public void testValidateCanSubmitStateWaitingSubmission() {
		when(mockFormDao.getFormDataState(formDataId)).thenReturn(StateEnum.WAITING_FOR_SUBMISSION);
		// call under test
		manager.validateCanSubmitState(formDataId);
		verify(mockFormDao).getFormDataState(formDataId);
	}

	@Test
	public void testValidateCanSubmitRejected() {
		when(mockFormDao.getFormDataState(formDataId)).thenReturn(StateEnum.REJECTED);
		// call under test
		manager.validateCanSubmitState(formDataId);
		verify(mockFormDao).getFormDataState(formDataId);
	}

	@Test
	public void testValidateCanSubmitAccepted() {
		when(mockFormDao.getFormDataState(formDataId)).thenReturn(StateEnum.ACCEPTED);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.validateCanSubmitState(formDataId);
		}).getMessage();
		assertEquals(FormManagerImpl.FORM_HAS_ALREADY_BEEN_SUBMITTED, message);
	}

	@Test
	public void testValidateCanSubmitWaitReivew() {
		when(mockFormDao.getFormDataState(formDataId)).thenReturn(StateEnum.SUBMITTED_WAITING_FOR_REVIEW);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.validateCanSubmitState(formDataId);
		}).getMessage();
		assertEquals(FormManagerImpl.FORM_HAS_ALREADY_BEEN_SUBMITTED, message);
	}

	@Test
	public void testSubmitForm() {
		when(mockFormDao.getFormDataCreator(formDataId)).thenReturn(user.getId());
		when(mockFormDao.getFormDataState(formDataId)).thenReturn(StateEnum.WAITING_FOR_SUBMISSION);
		when(mockFormDao.getFormDataGroupId(formDataId)).thenReturn(groupId);
		when(mockAclDao.canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.SUBMIT))
				.thenReturn(AuthorizationStatus.authorized());
		// call under test
		manager.submitFormData(user, formDataId);

		verify(mockAclDao).canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.SUBMIT);
		verify(mockFormDao).submitFormData(user.getId(), formDataId);
	}

	@Test
	public void testSubmitFormNotCreator() {
		// user is not the creator
		when(mockFormDao.getFormDataCreator(formDataId)).thenReturn(user.getId() + 1);

		assertThrows(UnauthorizedException.class, () -> {
			// call under test
			manager.submitFormData(user, formDataId);
		});

		verify(mockFormDao, never()).submitFormData(user.getId(), formDataId);
	}
	
	@Test
	public void testSubmitFormAlreadyAccepted() {
		when(mockFormDao.getFormDataCreator(formDataId)).thenReturn(user.getId());
		when(mockFormDao.getFormDataState(formDataId)).thenReturn(StateEnum.ACCEPTED);

		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.submitFormData(user, formDataId);
		});

		verify(mockFormDao, never()).submitFormData(user.getId(), formDataId);
	}
	
	@Test
	public void testSubmitFormCannotSubmitToGroup() {
		when(mockFormDao.getFormDataCreator(formDataId)).thenReturn(user.getId());
		when(mockFormDao.getFormDataState(formDataId)).thenReturn(StateEnum.WAITING_FOR_SUBMISSION);
		when(mockFormDao.getFormDataGroupId(formDataId)).thenReturn(groupId);
		when(mockAclDao.canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.SUBMIT))
				.thenReturn(AuthorizationStatus.accessDenied("No submit on group"));
		
		assertThrows(UnauthorizedException.class, () -> {
			// call under test
			manager.submitFormData(user, formDataId);
		});

		verify(mockAclDao).canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.SUBMIT);
		verify(mockFormDao, never()).submitFormData(user.getId(), formDataId);
	}
}
