package org.sagebionetworks.repo.manager.form;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
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
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.dbo.form.FormDao;
import org.sagebionetworks.repo.model.form.FormChangeRequest;
import org.sagebionetworks.repo.model.form.FormData;
import org.sagebionetworks.repo.model.form.FormGroup;
import org.sagebionetworks.repo.model.form.FormRejection;
import org.sagebionetworks.repo.model.form.ListRequest;
import org.sagebionetworks.repo.model.form.ListResponse;
import org.sagebionetworks.repo.model.form.StateEnum;
import org.sagebionetworks.repo.model.form.SubmissionStatus;
import org.sagebionetworks.repo.model.util.AccessControlListUtil;
import org.sagebionetworks.repo.web.NotFoundException;

import com.google.common.collect.Sets;

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

	@Captor
	ArgumentCaptor<SubmissionStatus> statusCaptor;

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
	FormChangeRequest changeRequest;

	UserInfo anonymousUser;

	SubmissionStatus submittedStatus;

	ListRequest listRequest;
	FormRejection rejection;

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
		
		changeRequest = new FormChangeRequest();
		changeRequest.setName(validName);
		changeRequest.setFileHandleId(dataFileHandleId);

		anonymousUser = new UserInfo(isAdmin, BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());

		submittedStatus = new SubmissionStatus();
		submittedStatus.setSubmittedOn(new Date(123));
		submittedStatus.setState(StateEnum.SUBMITTED_WAITING_FOR_REVIEW);

		listRequest = new ListRequest();
		listRequest.setGroupId(groupId);
		listRequest.setFilterByState(Sets.newHashSet(StateEnum.ACCEPTED));
		
		String reason = "just because";
		rejection = new FormRejection();
		rejection.setReason(reason);

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
		FormData created = manager.createFormData(user, groupId, changeRequest);
		assertEquals(formData, created);
		verify(mockAclDao).canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.SUBMIT);
		verify(mockAuthManager).canAccessRawFileHandleById(user, dataFileHandleId);
		verify(mockFormDao).createFormData(user.getId(), groupId, validName, dataFileHandleId);
	}

	@Test
	public void testCreateFormDataAnonymous() {
		assertThrows(UnauthorizedException.class, () -> {
			// call under test
			manager.createFormData(anonymousUser, groupId, changeRequest);
		});
	}

	@Test
	public void testCreateFormDataUnauthorizedSubmit() {
		when(mockAclDao.canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.SUBMIT))
				.thenReturn(AuthorizationStatus.accessDenied("no access for you"));
		assertThrows(UnauthorizedException.class, () -> {
			// call under test
			manager.createFormData(user, groupId, changeRequest);
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
			manager.createFormData(user, groupId, changeRequest);
		});
		verify(mockAclDao).canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.SUBMIT);
		verify(mockAuthManager).canAccessRawFileHandleById(user, dataFileHandleId);
		verify(mockFormDao, never()).createFormData(any(Long.class), anyString(), anyString(), anyString());
	}

	@Test
	public void testCreateFormDataInvalidName() {
		String invalidName = StringUtils.repeat('a', FormManagerImpl.MAX_NAME_CHARS + 1);
		changeRequest.setName(invalidName);
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.createFormData(user, groupId, changeRequest);
		});
		verify(mockAclDao, never()).canAccess(any(UserInfo.class), anyString(), any(ObjectType.class),
				any(ACCESS_TYPE.class));
		verify(mockAuthManager, never()).canAccessRawFileHandleById(any(UserInfo.class), anyString());
		verify(mockFormDao, never()).createFormData(any(Long.class), anyString(), anyString(), anyString());
	}

	@Test
	public void testCreateFormDataNullName() {
		String invalidName = null;
		changeRequest.setName(invalidName);
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.createFormData(user, groupId, changeRequest);
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
			manager.createFormData(user, groupId, changeRequest);
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
			manager.createFormData(user, groupId, changeRequest);
		});
		verify(mockAclDao, never()).canAccess(any(UserInfo.class), anyString(), any(ObjectType.class),
				any(ACCESS_TYPE.class));
		verify(mockAuthManager, never()).canAccessRawFileHandleById(any(UserInfo.class), anyString());
		verify(mockFormDao, never()).createFormData(any(Long.class), anyString(), anyString(), anyString());
	}

	@Test
	public void testCreateFormDataNullDataFileHandle() {
		changeRequest.setFileHandleId(null);
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.createFormData(user, groupId, changeRequest);
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
		String groupId = "333";
		when(mockFormDao.getFormDataCreator(formDataId)).thenReturn(user.getId());
		when(mockFormDao.getFormDataState(formDataId)).thenReturn(StateEnum.WAITING_FOR_SUBMISSION);
		when(mockFormDao.getFormDataGroupId(formDataId)).thenReturn(groupId);
		when(mockAclDao.canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.SUBMIT))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockAuthManager.canAccessRawFileHandleById(user, dataFileHandleId))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockFormDao.updateStatus(anyString(), any(SubmissionStatus.class))).thenReturn(formData);

		// call under test
		FormData updated = manager.updateFormData(user, formDataId, changeRequest);
		assertEquals(formData, updated);
		verify(mockFormDao).getFormDataCreator(formDataId);
		verify(mockAclDao).canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.SUBMIT);
		verify(mockAuthManager).canAccessRawFileHandleById(user, dataFileHandleId);
		verify(mockFormDao).updateFormData(formDataId, validName, dataFileHandleId);
		verify(mockFormDao).updateStatus(eq(formDataId), statusCaptor.capture());
		SubmissionStatus status = statusCaptor.getValue();
		assertNotNull(status);
		assertEquals(StateEnum.WAITING_FOR_SUBMISSION, status.getState());
		assertNull(status.getRejectionMessage());
		assertNull(status.getReviewedBy());
		assertNull(status.getReviewedOn());
		assertNull(status.getSubmittedOn());
	}

	@Test
	public void testUpdateFormDataNotCreator() {
		// caller is not the creator.
		when(mockFormDao.getFormDataCreator(formDataId)).thenReturn(user.getId() + 1);

		assertThrows(UnauthorizedException.class, () -> {
			// call under test
			manager.updateFormData(user, formDataId, changeRequest);
		});
		verify(mockFormDao).getFormDataCreator(formDataId);
		verify(mockAclDao, never()).canAccess(any(UserInfo.class), anyString(), any(ObjectType.class),
				any(ACCESS_TYPE.class));
		verify(mockAuthManager, never()).canAccessRawFileHandleById(any(UserInfo.class), anyString());
		verify(mockFormDao, never()).updateFormData(anyString(), anyString(), anyString());
	}

	@Test
	public void testUpdateFormDataUnauthorizedSubmit() {
		String groupId = "333";
		when(mockFormDao.getFormDataCreator(formDataId)).thenReturn(user.getId());
		when(mockFormDao.getFormDataState(formDataId)).thenReturn(StateEnum.WAITING_FOR_SUBMISSION);
		when(mockFormDao.getFormDataGroupId(formDataId)).thenReturn(groupId);
		when(mockAclDao.canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.SUBMIT))
				.thenReturn(AuthorizationStatus.accessDenied("no access"));

		assertThrows(UnauthorizedException.class, () -> {
			// call under test
			manager.updateFormData(user, formDataId, changeRequest);
		});

		verify(mockAclDao).canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.SUBMIT);
		verify(mockAuthManager, never()).canAccessRawFileHandleById(any(UserInfo.class), anyString());
		verify(mockFormDao, never()).updateFormData(anyString(), anyString(), anyString());
	}

	@Test
	public void testUpdateFormDataUnauthorizedFileHandle() {
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
			manager.updateFormData(user, formDataId, changeRequest);
		});

		verify(mockAclDao).canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.SUBMIT);
		verify(mockAuthManager).canAccessRawFileHandleById(user, dataFileHandleId);
		verify(mockFormDao, never()).updateFormData(anyString(), anyString(), anyString());
	}

	@Test
	public void testUpdateFormDataInvalidState() {
		when(mockFormDao.getFormDataCreator(formDataId)).thenReturn(user.getId());
		// can update an accepted from.
		when(mockFormDao.getFormDataState(formDataId)).thenReturn(StateEnum.ACCEPTED);

		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.updateFormData(user, formDataId, changeRequest);
		});

		verify(mockAclDao, never()).canAccess(any(UserInfo.class), anyString(), any(ObjectType.class),
				any(ACCESS_TYPE.class));
		verify(mockAuthManager, never()).canAccessRawFileHandleById(any(UserInfo.class), anyString());
		verify(mockFormDao, never()).updateFormData(anyString(), anyString(), anyString());
	}

	@Test
	public void testUpdateFormDataNullName() {
		// Null name means the name is not updated
		changeRequest.setName(null);
		String groupId = "333";
		when(mockFormDao.getFormDataCreator(formDataId)).thenReturn(user.getId());
		when(mockFormDao.getFormDataState(formDataId)).thenReturn(StateEnum.WAITING_FOR_SUBMISSION);
		when(mockFormDao.getFormDataGroupId(formDataId)).thenReturn(groupId);
		when(mockAclDao.canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.SUBMIT))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockAuthManager.canAccessRawFileHandleById(user, dataFileHandleId))
				.thenReturn(AuthorizationStatus.authorized());

		// call under test
		manager.updateFormData(user, formDataId, changeRequest);

		verify(mockAclDao).canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.SUBMIT);
		verify(mockAuthManager).canAccessRawFileHandleById(user, dataFileHandleId);
		// name should not be updated
		verify(mockFormDao).updateFormData(formDataId, dataFileHandleId);
	}

	@Test
	public void testUpdateFormDataInvalidName() {
		String invalidName = StringUtils.repeat('a', FormManagerImpl.MAX_NAME_CHARS + 1);
		changeRequest.setName(invalidName);

		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.updateFormData(user, formDataId, changeRequest);
		});

		verify(mockAclDao, never()).canAccess(any(UserInfo.class), anyString(), any(ObjectType.class),
				any(ACCESS_TYPE.class));
		verify(mockAuthManager, never()).canAccessRawFileHandleById(any(UserInfo.class), anyString());
		verify(mockFormDao, never()).updateFormData(anyString(), anyString(), anyString());
	}

	@Test
	public void testUpdateFormDataNullId() {
		String formDataId = null;
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.updateFormData(user, formDataId, changeRequest);
		});

		verify(mockAclDao, never()).canAccess(any(UserInfo.class), anyString(), any(ObjectType.class),
				any(ACCESS_TYPE.class));
		verify(mockAuthManager, never()).canAccessRawFileHandleById(any(UserInfo.class), anyString());
		verify(mockFormDao, never()).updateFormData(anyString(), anyString(), anyString());
	}

	@Test
	public void testUpdateFormDataDataFileHandle() {
		changeRequest.setFileHandleId(null);

		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.updateFormData(user, formDataId, changeRequest);
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
		when(mockFormDao.updateStatus(anyString(), any(SubmissionStatus.class))).thenReturn(formData);
		// call under test
		FormData update = manager.submitFormData(user, formDataId);
		assertEquals(update, formData);

		verify(mockFormDao).getFormDataCreator(formDataId);
		verify(mockAclDao).canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.SUBMIT);
		verify(mockFormDao).updateStatus(eq(formDataId), statusCaptor.capture());
		SubmissionStatus status = statusCaptor.getValue();
		assertNotNull(status);
		assertEquals(StateEnum.SUBMITTED_WAITING_FOR_REVIEW, status.getState());
		assertNull(status.getRejectionMessage());
		assertNull(status.getReviewedBy());
		assertNull(status.getReviewedOn());
		assertNotNull(status.getSubmittedOn());
	}

	@Test
	public void testSubmitFormNotCreator() {
		// user is not the creator
		when(mockFormDao.getFormDataCreator(formDataId)).thenReturn(user.getId() + 1);

		assertThrows(UnauthorizedException.class, () -> {
			// call under test
			manager.submitFormData(user, formDataId);
		});

		verify(mockFormDao, never()).updateStatus(anyString(), any(SubmissionStatus.class));
	}

	@Test
	public void testSubmitFormAlreadyAccepted() {
		when(mockFormDao.getFormDataCreator(formDataId)).thenReturn(user.getId());
		when(mockFormDao.getFormDataState(formDataId)).thenReturn(StateEnum.ACCEPTED);

		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.submitFormData(user, formDataId);
		});

		verify(mockFormDao, never()).updateStatus(anyString(), any(SubmissionStatus.class));
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
		verify(mockFormDao, never()).updateStatus(anyString(), any(SubmissionStatus.class));
	}

	@Test
	public void testReviewerAcceptForm() {
		when(mockFormDao.getFormDataGroupId(formDataId)).thenReturn(groupId);
		when(mockAclDao.canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.READ_PRIVATE_SUBMISSION))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockFormDao.getFormDataStatusForUpdate(formDataId)).thenReturn(submittedStatus);
		when(mockFormDao.updateStatus(anyString(), any(SubmissionStatus.class))).thenReturn(formData);

		// call under test
		FormData updated = manager.reviewerAcceptForm(user, formDataId);
		assertEquals(formData, updated);

		verify(mockAclDao).canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.READ_PRIVATE_SUBMISSION);
		verify(mockFormDao).updateStatus(eq(formDataId), statusCaptor.capture());
		SubmissionStatus status = statusCaptor.getValue();
		assertNotNull(status);
		assertEquals(StateEnum.ACCEPTED, status.getState());
		assertEquals(submittedStatus.getSubmittedOn(), status.getSubmittedOn());
		assertNull(status.getRejectionMessage());
		assertEquals(user.getId().toString(), status.getReviewedBy());
		assertNotNull(status.getReviewedOn());
	}

	@Test
	public void testReviewerAcceptFormNoPermission() {
		when(mockFormDao.getFormDataGroupId(formDataId)).thenReturn(groupId);
		when(mockAclDao.canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.READ_PRIVATE_SUBMISSION))
				.thenReturn(AuthorizationStatus.accessDenied("no"));

		assertThrows(UnauthorizedException.class, () -> {
			// call under test
			manager.reviewerAcceptForm(user, formDataId);
		});

		verify(mockAclDao).canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.READ_PRIVATE_SUBMISSION);
		verify(mockFormDao, never()).updateStatus(anyString(), any(SubmissionStatus.class));
	}

	@Test
	public void testReviewerAcceptFormWrongStartingState() {
		when(mockFormDao.getFormDataGroupId(formDataId)).thenReturn(groupId);
		when(mockAclDao.canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.READ_PRIVATE_SUBMISSION))
				.thenReturn(AuthorizationStatus.authorized());
		// wrong starting state
		SubmissionStatus status = new SubmissionStatus();
		status.setState(StateEnum.WAITING_FOR_SUBMISSION);
		when(mockFormDao.getFormDataStatusForUpdate(formDataId)).thenReturn(status);

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.reviewerAcceptForm(user, formDataId);
		}).getMessage();
		assertEquals("Cannot accept a submission that is currently: WAITING_FOR_SUBMISSION", message);

		verify(mockAclDao).canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.READ_PRIVATE_SUBMISSION);
		verify(mockFormDao, never()).updateStatus(anyString(), any(SubmissionStatus.class));
	}

	@Test
	public void testReviewerRejectForm() {
		when(mockFormDao.getFormDataGroupId(formDataId)).thenReturn(groupId);
		when(mockAclDao.canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.READ_PRIVATE_SUBMISSION))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockFormDao.getFormDataStatusForUpdate(formDataId)).thenReturn(submittedStatus);
		when(mockFormDao.updateStatus(anyString(), any(SubmissionStatus.class))).thenReturn(formData);
		String reason = StringUtils.repeat('a', FormManagerImpl.MAX_REASON_CHARS);
		rejection.setReason(reason);

		// call under test
		FormData updated = manager.reviewerRejectForm(user, formDataId, rejection);
		assertEquals(formData, updated);

		verify(mockAclDao).canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.READ_PRIVATE_SUBMISSION);
		verify(mockFormDao).updateStatus(eq(formDataId), statusCaptor.capture());
		SubmissionStatus status = statusCaptor.getValue();
		assertNotNull(status);
		assertEquals(StateEnum.REJECTED, status.getState());
		assertEquals(submittedStatus.getSubmittedOn(), status.getSubmittedOn());
		assertEquals(reason, status.getRejectionMessage());
		assertEquals(user.getId().toString(), status.getReviewedBy());
		assertNotNull(status.getReviewedOn());
	}

	@Test
	public void testReviewerRejectFormWrongState() {
		when(mockFormDao.getFormDataGroupId(formDataId)).thenReturn(groupId);
		when(mockAclDao.canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.READ_PRIVATE_SUBMISSION))
				.thenReturn(AuthorizationStatus.authorized());
		// wrong starting state
		SubmissionStatus status = new SubmissionStatus();
		status.setState(StateEnum.WAITING_FOR_SUBMISSION);
		when(mockFormDao.getFormDataStatusForUpdate(formDataId)).thenReturn(status);

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.reviewerRejectForm(user, formDataId, rejection);
		}).getMessage();
		assertEquals("Cannot reject a submission that is currently: WAITING_FOR_SUBMISSION", message);

		verify(mockAclDao).canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.READ_PRIVATE_SUBMISSION);
		verify(mockFormDao, never()).updateStatus(anyString(), any(SubmissionStatus.class));
	}

	@Test
	public void testReviewerRejectFormNoPermission() {
		when(mockFormDao.getFormDataGroupId(formDataId)).thenReturn(groupId);
		when(mockAclDao.canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.READ_PRIVATE_SUBMISSION))
				.thenReturn(AuthorizationStatus.accessDenied("no"));

		assertThrows(UnauthorizedException.class, () -> {
			// call under test
			manager.reviewerRejectForm(user, formDataId, rejection);
		});

		verify(mockAclDao).canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.READ_PRIVATE_SUBMISSION);
		verify(mockFormDao, never()).updateStatus(anyString(), any(SubmissionStatus.class));
	}

	@Test
	public void testReviewerRejectFormMessagTooLong() {
		String reason = StringUtils.repeat('a', FormManagerImpl.MAX_REASON_CHARS + 1);
		rejection.setReason(reason);
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.reviewerRejectForm(user, formDataId, rejection);
		});

		verify(mockFormDao, never()).updateStatus(anyString(), any(SubmissionStatus.class));
	}

	@Test
	public void testListFormStatusForCreator() {
		NextPageToken expectedToken = new NextPageToken(null);
		List<FormData> page = createPageOfSize((int) (expectedToken.getLimitForQuery()));
		String expectedNextToken = expectedToken.getNextPageTokenForCurrentResults(new ArrayList<>(page));
		when(mockFormDao.listFormDataByCreator(user.getId(), listRequest, expectedToken.getLimitForQuery(),
				expectedToken.getOffset())).thenReturn(page);
		// call under test
		ListResponse response = manager.listFormStatusForCreator(user, listRequest);
		assertNotNull(response);
		assertEquals(page, response.getPage());
		assertEquals(expectedNextToken, response.getNextPageToken());
		verify(mockFormDao).listFormDataByCreator(user.getId(), listRequest, expectedToken.getLimitForQuery(),
				expectedToken.getOffset());
	}

	@Test
	public void testListFormStatusForCreatorNullUser() {
		user = null;
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.listFormStatusForCreator(user, listRequest);
		});

		verify(mockFormDao, never()).listFormDataByCreator(anyLong(), any(ListRequest.class), anyLong(), anyLong());
	}

	@Test
	public void testListFormStatusForCreatorNullRequest() {
		listRequest = null;
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.listFormStatusForCreator(user, listRequest);
		});

		verify(mockFormDao, never()).listFormDataByCreator(anyLong(), any(ListRequest.class), anyLong(), anyLong());
	}

	@Test
	public void testListFormStatusForCreatorNullGroupId() {
		listRequest.setGroupId(null);
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.listFormStatusForCreator(user, listRequest);
		});
		verify(mockFormDao, never()).listFormDataByCreator(anyLong(), any(ListRequest.class), anyLong(), anyLong());
	}

	@Test
	public void testListFormStatusForCreatorNullStateFilter() {
		listRequest.setFilterByState(null);
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.listFormStatusForCreator(user, listRequest);
		});
		verify(mockFormDao, never()).listFormDataByCreator(anyLong(), any(ListRequest.class), anyLong(), anyLong());
	}

	@Test
	public void testListFormStatusForCreatorEmptyStateFilter() {
		listRequest.setFilterByState(new HashSet<>());
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.listFormStatusForCreator(user, listRequest);
		});
		verify(mockFormDao, never()).listFormDataByCreator(anyLong(), any(ListRequest.class), anyLong(), anyLong());
	}

	@Test
	public void testListFormDataForReviewer() {
		NextPageToken expectedToken = new NextPageToken(null);
		List<FormData> page = createPageOfSize((int) (expectedToken.getLimitForQuery()));
		String expectedNextToken = expectedToken.getNextPageTokenForCurrentResults(new ArrayList<>(page));
		when(mockFormDao.listFormDataForReviewer(listRequest, expectedToken.getLimitForQuery(),
				expectedToken.getOffset())).thenReturn(page);
		when(mockAclDao.canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.READ_PRIVATE_SUBMISSION))
				.thenReturn(AuthorizationStatus.authorized());
		// call under test
		ListResponse response = manager.listFormStatusForReviewer(user, listRequest);
		assertNotNull(response);
		assertEquals(page, response.getPage());
		assertEquals(expectedNextToken, response.getNextPageToken());
		verify(mockFormDao).listFormDataForReviewer(listRequest, expectedToken.getLimitForQuery(),
				expectedToken.getOffset());
	}

	@Test
	public void testListFormDataForReviewerUnauthorized() {
		when(mockAclDao.canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.READ_PRIVATE_SUBMISSION))
				.thenReturn(AuthorizationStatus.accessDenied("no"));

		assertThrows(UnauthorizedException.class, () -> {
			// call under test
			manager.listFormStatusForReviewer(user, listRequest);
		});
		verify(mockFormDao, never()).listFormDataForReviewer(any(ListRequest.class), anyLong(), anyLong());
	}

	@Test
	public void testListFormDataForReviewerFilterWaitingForSubmission() {
		listRequest.setFilterByState(Sets.newHashSet(StateEnum.WAITING_FOR_SUBMISSION));
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.listFormStatusForReviewer(user, listRequest);
		});

		verify(mockFormDao, never()).listFormDataByCreator(anyLong(), any(ListRequest.class), anyLong(), anyLong());
	}

	@Test
	public void testListFormDataForReviewerNullUser() {
		user = null;
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.listFormStatusForReviewer(user, listRequest);
		});

		verify(mockFormDao, never()).listFormDataByCreator(anyLong(), any(ListRequest.class), anyLong(), anyLong());
	}

	@Test
	public void testListFormDataForReviewerNullRequest() {
		listRequest = null;
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.listFormStatusForReviewer(user, listRequest);
		});

		verify(mockFormDao, never()).listFormDataByCreator(anyLong(), any(ListRequest.class), anyLong(), anyLong());
	}

	@Test
	public void testListFormDataForReviewerNullGroupId() {
		listRequest.setGroupId(null);
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.listFormStatusForReviewer(user, listRequest);
		});
		verify(mockFormDao, never()).listFormDataByCreator(anyLong(), any(ListRequest.class), anyLong(), anyLong());
	}

	@Test
	public void testListFormDataForReviewerNullStateFilter() {
		listRequest.setFilterByState(null);
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.listFormStatusForReviewer(user, listRequest);
		});
		verify(mockFormDao, never()).listFormDataByCreator(anyLong(), any(ListRequest.class), anyLong(), anyLong());
	}

	@Test
	public void testListFormDataForReviewerEmptyStateFilter() {
		listRequest.setFilterByState(new HashSet<>());
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.listFormStatusForReviewer(user, listRequest);
		});
		verify(mockFormDao, never()).listFormDataByCreator(anyLong(), any(ListRequest.class), anyLong(), anyLong());
	}

	@Test
	public void testCanUserDownloadFormDataCreator() {
		when(mockFormDao.getFormDataCreator(formDataId)).thenReturn(user.getId());
		// call under test
		AuthorizationStatus status = manager.canUserDownloadFormData(user, formDataId);
		assertTrue(status.isAuthorized());
		verify(mockFormDao).getFormDataCreator(formDataId);
		verify(mockFormDao, never()).getFormDataGroupId(anyString());
		verify(mockAclDao, never()).canAccess(any(UserInfo.class), anyString(), any(ObjectType.class), any(ACCESS_TYPE.class));
	}

	@Test
	public void testCanUserDownloadFormDataReviewer() {
		// not the creator
		when(mockFormDao.getFormDataCreator(formDataId)).thenReturn(user.getId() - 1);
		when(mockFormDao.getFormDataGroupId(formDataId)).thenReturn(groupId);
		when(mockAclDao.canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.READ_PRIVATE_SUBMISSION))
				.thenReturn(AuthorizationStatus.authorized());
		// call under test
		AuthorizationStatus status = manager.canUserDownloadFormData(user, formDataId);
		assertTrue(status.isAuthorized());
		verify(mockFormDao).getFormDataCreator(formDataId);
		verify(mockFormDao).getFormDataGroupId(formDataId);
		verify(mockAclDao).canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.READ_PRIVATE_SUBMISSION);
	}
	
	@Test
	public void testCanUserDownloadFormDataNulUser() {
		user = null;
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.canUserDownloadFormData(user, formDataId);
		});
	}
	
	@Test
	public void testCanUserDownloadFormDataNulFormId() {
		formDataId = null;
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.canUserDownloadFormData(user, formDataId);
		});
	}
	
	@Test
	public void testGetFormGroup() {
		when(mockFormDao.getFormGroup(any())).thenReturn(groupToReturn);
		when(mockAclDao.canAccess(any(UserInfo.class), any(), any(), any())).thenReturn(AuthorizationStatus.authorized());
		// call under test
		FormGroup group = manager.getFormGroup(user, groupId);
		assertEquals(groupToReturn, group);
		verify(mockFormDao).getFormGroup(groupId);
		verify(mockAclDao).canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.READ);
	}
	
	@Test
	public void testGetFormGroupWithUnauthorized() {
		when(mockAclDao.canAccess(any(UserInfo.class), any(), any(), any())).thenReturn(AuthorizationStatus.accessDenied("no"));
		assertThrows(UnauthorizedException.class, ()->{
			// call under test
			manager.getFormGroup(user, groupId);
		});
		verify(mockFormDao, never()).getFormGroup(any());
		verify(mockAclDao).canAccess(user, groupId, ObjectType.FORM_GROUP, ACCESS_TYPE.READ);
	}
	
	@Test
	public void testGetFormGroupWithNullUser() {
		user  = null;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.getFormGroup(user, groupId);
		});
	}
	
	@Test
	public void testGetFormGroupWithNullId() {
		groupId  = null;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.getFormGroup(user, groupId);
		});
	}

	/**
	 * Helper to create a page of data of the given size.
	 * 
	 * @param size
	 * @return
	 */
	public static List<FormData> createPageOfSize(int size) {
		List<FormData> page = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			FormData data = new FormData();
			data.setFormDataId("" + i);
			page.add(data);
		}
		return page;
	}
}
