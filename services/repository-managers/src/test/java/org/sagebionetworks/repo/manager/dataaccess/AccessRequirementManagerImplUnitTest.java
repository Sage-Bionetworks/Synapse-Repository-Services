package org.sagebionetworks.repo.manager.dataaccess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.manager.dataaccess.AccessRequirementManagerImpl.DEFAULT_LIMIT;
import static org.sagebionetworks.repo.manager.dataaccess.AccessRequirementManagerImpl.DEFAULT_OFFSET;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.AccessApprovalDAO;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.AccessRequirementInfoForUpdate;
import org.sagebionetworks.repo.model.AccessRequirementStats;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.LockAccessRequirement;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.PostMessageContentAccessRequirement;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptorResponse;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.SelfSignAccessRequirement;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.dao.NotificationEmailDAO;
import org.sagebionetworks.repo.model.dataaccess.AccessRequirementConversionRequest;
import org.sagebionetworks.repo.util.jrjc.CreatedIssue;
import org.sagebionetworks.repo.util.jrjc.JiraClient;
import org.sagebionetworks.repo.util.jrjc.ProjectInfo;
import org.sagebionetworks.repo.web.NotFoundException;

@ExtendWith(MockitoExtension.class)
public class AccessRequirementManagerImplUnitTest {

	@Mock
	private JiraClient jiraClient;

	private static final String TEST_PRINCIPAL_ID = "1010101";
	private static final String TEST_ENTITY_ID = "syn98786543";

	@Mock
	private AccessRequirementDAO accessRequirementDAO;
	@Mock
	private AccessApprovalDAO accessApprovalDAO;
	@Mock
	private NodeDAO nodeDao;
	@Mock
	private AuthorizationManager authorizationManager;

	@InjectMocks
	private AccessRequirementManagerImpl arm;

	private UserInfo userInfo;
	@Mock
	private NotificationEmailDAO notificationEmailDao;
	@Mock
	CreatedIssue mockProject;
	@Mock
	ProjectInfo mockProjectInfo;

	private Map<String, String> fields;


	@BeforeEach
	public void setUp() throws Exception {
		userInfo = new UserInfo(false, TEST_PRINCIPAL_ID);
	}

	@Test
	public void testCreatePostMessageContentAccessRequirement() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			arm.createAccessRequirement(userInfo, new PostMessageContentAccessRequirement());
		}) ;
	}

	@Test
	public void testCreateNullAccessType() {
		AccessRequirement toCreate = createExpectedAR();
		toCreate.setAccessType(null);
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			arm.createAccessRequirement(userInfo, toCreate);
		});
	}

	@Test
	public void testCreateNullSubjectIds() {
		AccessRequirement toCreate = createExpectedAR();
		toCreate.setSubjectIds(null);
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			arm.createAccessRequirement(userInfo, toCreate);
		});
	}

	private ManagedACTAccessRequirement createExpectedAR() {
		ManagedACTAccessRequirement expectedAR = new ManagedACTAccessRequirement();
		expectedAR.setAccessType(ACCESS_TYPE.DOWNLOAD);
		RestrictableObjectDescriptor subjectId = new RestrictableObjectDescriptor();
		subjectId.setId(TEST_ENTITY_ID);
		subjectId.setType(RestrictableObjectType.ENTITY);
		expectedAR.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{subjectId}));
		AccessRequirementManagerImpl.populateCreationFields(userInfo, expectedAR);
		return expectedAR;
	}

	@Test
	public void testCreateLockAccessRequirementWithNullUserInfo() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			arm.createLockAccessRequirement(null, TEST_ENTITY_ID);
		});
	}

	@Test
	public void testCreateLockAccessRequirementWithNullEntityId() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			arm.createLockAccessRequirement(userInfo, null);
		});
	}

	@Test
	public void testCreateLockAccessRequirementHappyPath() throws Exception {
		when(authorizationManager.canAccess(userInfo, TEST_ENTITY_ID, ObjectType.ENTITY, ACCESS_TYPE.CREATE)).thenReturn(AuthorizationStatus.authorized());
		when(authorizationManager.canAccess(userInfo, TEST_ENTITY_ID, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationStatus.authorized());
		when(mockProjectInfo.getProjectId()).thenReturn("projectId");
		when(mockProjectInfo.getIssueTypeId()).thenReturn(10000L);

		fields = new HashMap<String, String>();
		fields.put("Synapse Principal ID", "id1");
		fields.put("Synapse User Display Name", "id2");
		fields.put("Synapse Data Object", "id3");
		fields.put("Components", "components");
		when(jiraClient.getFields()).thenReturn(fields);

		when(jiraClient.getProjectInfo(anyString(), anyString())).thenReturn(mockProjectInfo);

		when(mockProject.getKey()).thenReturn("SG-101");
		when(jiraClient.createIssue(anyObject())).thenReturn(mockProject);

		Set<String> ars = new HashSet<String>();
		AccessRequirementStats stats = new AccessRequirementStats();
		stats.setRequirementIdSet(ars);
		when(accessRequirementDAO.getAccessRequirementStats(any(List.class), eq(RestrictableObjectType.ENTITY))).thenReturn(stats);

		arm.createLockAccessRequirement(userInfo, TEST_ENTITY_ID);

		// test that the right AR was created
		ArgumentCaptor<AccessRequirement> argument = ArgumentCaptor.forClass(AccessRequirement.class);
		verify(accessRequirementDAO).create(argument.capture());
		verify(notificationEmailDao).getNotificationEmailForPrincipal(userInfo.getId());
		// can't just call equals on the objects, because the time stamps are slightly different
		AccessRequirement toCreate = argument.getValue();
		assertEquals(ACCESS_TYPE.DOWNLOAD, toCreate.getAccessType());
		assertEquals(userInfo.getId().toString(), toCreate.getCreatedBy());
		assertEquals(LockAccessRequirement.class.getName(), toCreate.getConcreteType());
		assertEquals(userInfo.getId().toString(), toCreate.getModifiedBy());
		assertNotNull(toCreate.getSubjectIds());
		assertEquals(1, toCreate.getSubjectIds().size());
		assertEquals(TEST_ENTITY_ID, toCreate.getSubjectIds().get(0).getId());
		assertEquals(RestrictableObjectType.ENTITY, toCreate.getSubjectIds().get(0).getType());
		assertNotNull(((LockAccessRequirement)toCreate).getJiraKey());

		// test that jira client was called to create issue
		// we don't test the *content* of the issue because that's tested in JRJCHelperTest
		verify(jiraClient).createIssue(anyObject());
	}

	@Test
	public void testCreateLockAccessRequirementWithoutREADPermission() throws Exception {
		when(authorizationManager.canAccess(userInfo, TEST_ENTITY_ID, ObjectType.ENTITY, ACCESS_TYPE.CREATE)).thenReturn(AuthorizationStatus.accessDenied(""));
		// this should throw the unauthorized exception
		Assertions.assertThrows(UnauthorizedException.class, () -> {
			arm.createLockAccessRequirement(userInfo, TEST_ENTITY_ID);
		});
	}

	@Test
	public void testCreateLockAccessRequirementWithoutUPDATEPermission() throws Exception {
		when(authorizationManager.canAccess(userInfo, TEST_ENTITY_ID, ObjectType.ENTITY, ACCESS_TYPE.CREATE)).thenReturn(AuthorizationStatus.authorized());
		when(authorizationManager.canAccess(userInfo, TEST_ENTITY_ID, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationStatus.accessDenied(""));
		// this should throw the unauthorized exception
		Assertions.assertThrows(UnauthorizedException.class, () -> {
			arm.createLockAccessRequirement(userInfo, TEST_ENTITY_ID);
		});
	}

	@Test
	public void testCreateLockAccessRequirementAlreadyExists() throws Exception {
		when(authorizationManager.canAccess(userInfo, TEST_ENTITY_ID, ObjectType.ENTITY, ACCESS_TYPE.CREATE)).thenReturn(AuthorizationStatus.authorized());
		when(authorizationManager.canAccess(userInfo, TEST_ENTITY_ID, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationStatus.authorized());
		Set<String> ars = new HashSet<String>();
		String accessRequirementId = "1";
		ars.add(accessRequirementId);
		AccessRequirementStats stats = new AccessRequirementStats();
		stats.setRequirementIdSet(ars);
		when(accessRequirementDAO.getAccessRequirementStats(any(List.class), eq(RestrictableObjectType.ENTITY))).thenReturn(stats);
		// this should throw the illegal argument exception
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			arm.createLockAccessRequirement(userInfo, TEST_ENTITY_ID);
		});
	}

	@Test
	public void testCreateUploadAccessRequirement() throws Exception {
		AccessRequirement ar = createExpectedAR();
		ar.setAccessType(ACCESS_TYPE.UPLOAD);
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			arm.createAccessRequirement(userInfo, ar);
		});
	}

	@Test
	public void testSetDefaultValuesForManagedAR() {
		ManagedACTAccessRequirement ar = (ManagedACTAccessRequirement) createExpectedAR();
		ar = (ManagedACTAccessRequirement) AccessRequirementManagerImpl.setDefaultValues(ar);
		assertFalse(ar.getIsCertifiedUserRequired());
		assertFalse(ar.getIsValidatedProfileRequired());
		assertFalse(ar.getIsDUCRequired());
		assertFalse(ar.getIsIRBApprovalRequired());
		assertFalse(ar.getAreOtherAttachmentsRequired());
		assertFalse(ar.getIsIDUPublic());
		assertTrue(ar.getIsIDURequired());
	}

	@Test
	public void testSetDefaultValuesForSelfSignAccessRequirement() {
		SelfSignAccessRequirement ar = new SelfSignAccessRequirement();
		ar = (SelfSignAccessRequirement) AccessRequirementManagerImpl.setDefaultValues(ar);
		assertFalse(ar.getIsCertifiedUserRequired());
		assertFalse(ar.getIsValidatedProfileRequired());
	}

	@Test
	public void testCreateACTAccessRequirement() {
		AccessRequirement toCreate = createExpectedAR();
		when(authorizationManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(true);
		arm.createAccessRequirement(userInfo, toCreate);

		// test that the right AR was created
		ArgumentCaptor<AccessRequirement> argument = ArgumentCaptor.forClass(AccessRequirement.class);
		verify(accessRequirementDAO).create(argument.capture());

		// verify that all default fields are set
		ManagedACTAccessRequirement ar = (ManagedACTAccessRequirement) argument.getValue();
		assertFalse(ar.getIsCertifiedUserRequired());
		assertFalse(ar.getIsValidatedProfileRequired());
		assertFalse(ar.getIsDUCRequired());
		assertFalse(ar.getIsIRBApprovalRequired());
		assertFalse(ar.getAreOtherAttachmentsRequired());
		assertFalse(ar.getIsIDUPublic());
		assertTrue(ar.getIsIDURequired());

		assertEquals(AccessRequirementManagerImpl.DEFAULT_EXPIRATION_PERIOD, ar.getExpirationPeriod());
	}

	@Test
	public void testUpdateWithNullUserInfo() {
		AccessRequirement toUpdate = createExpectedAR();
		String accessRequirementId = "1";
		toUpdate.setId(1L);
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			arm.updateAccessRequirement(null, accessRequirementId, toUpdate);
		});
	}

	@Test
	public void testUpdateWithNullAccessRequirementId() {
		AccessRequirement toUpdate = createExpectedAR();
		toUpdate.setId(1L);
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			arm.updateAccessRequirement(userInfo, null, toUpdate);
		});
	}

	@Test
	public void testUpdateWithNullAccessRequirement() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			arm.updateAccessRequirement(userInfo, "1", null);
		});
	}

	@Test
	public void testUpdateWithIdDoesNotMatch() {
		AccessRequirement toUpdate = createExpectedAR();
		toUpdate.setId(1L);
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			arm.updateAccessRequirement(userInfo, "-1", toUpdate);
		});
	}

	@Test
	public void testUpdateWithInvalidAccessRequirement() {
		AccessRequirement toUpdate = createExpectedAR();
		String accessRequirementId = "1";
		toUpdate.setId(1L);
		toUpdate.setAccessType(ACCESS_TYPE.CHANGE_PERMISSIONS);
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			arm.updateAccessRequirement(userInfo, accessRequirementId, toUpdate);
		});
	}

	@Test
	public void testUpdateUnauthorized() {
		AccessRequirement toUpdate = createExpectedAR();
		String accessRequirementId = "1";
		toUpdate.setId(1L);
		when(authorizationManager.canAccess(userInfo, accessRequirementId, ObjectType.ACCESS_REQUIREMENT, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationStatus.accessDenied(""));
		Assertions.assertThrows(UnauthorizedException.class, () -> {
			arm.updateAccessRequirement(userInfo, accessRequirementId, toUpdate);
		});
	}

	@Test
	public void testUpdateNoneExistingAR() {
		AccessRequirement toUpdate = createExpectedAR();
		String accessRequirementId = "1";
		toUpdate.setId(1L);
		when(authorizationManager.canAccess(userInfo, accessRequirementId, ObjectType.ACCESS_REQUIREMENT, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationStatus.authorized());
		when(accessRequirementDAO.getForUpdate(accessRequirementId)).thenThrow(new NotFoundException());
		Assertions.assertThrows(NotFoundException.class, () -> {
			arm.updateAccessRequirement(userInfo, accessRequirementId, toUpdate);
		});
	}

	@Test
	public void testUpdateEtagDoesNotMatch() {
		AccessRequirement toUpdate = createExpectedAR();
		String accessRequirementId = "1";
		toUpdate.setId(1L);
		toUpdate.setEtag("oldEtag");
		toUpdate.setVersionNumber(1L);
		when(authorizationManager.canAccess(userInfo, accessRequirementId, ObjectType.ACCESS_REQUIREMENT, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationStatus.authorized());
		AccessRequirementInfoForUpdate info = new AccessRequirementInfoForUpdate();
		info.setEtag("new Etag");
		info.setCurrentVersion(1L);
		when(accessRequirementDAO.getForUpdate(accessRequirementId)).thenReturn(info );
		Assertions.assertThrows(ConflictingUpdateException.class, () -> {
			arm.updateAccessRequirement(userInfo, accessRequirementId, toUpdate);
		});
	}

	@Test
	public void testUpdateVersionDoesNotMatch() {
		AccessRequirement toUpdate = createExpectedAR();
		String accessRequirementId = "1";
		toUpdate.setId(1L);
		toUpdate.setEtag("etag");
		toUpdate.setVersionNumber(1L);
		when(authorizationManager.canAccess(userInfo, accessRequirementId, ObjectType.ACCESS_REQUIREMENT, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationStatus.authorized());
		AccessRequirementInfoForUpdate info = new AccessRequirementInfoForUpdate();
		info.setEtag("etag");
		info.setCurrentVersion(2L);
		when(accessRequirementDAO.getForUpdate(accessRequirementId)).thenReturn(info );
		Assertions.assertThrows(ConflictingUpdateException.class, () -> {
			arm.updateAccessRequirement(userInfo, accessRequirementId, toUpdate);
		});
	}

	@Test
	public void testUpdateAccessType() {
		AccessRequirement toUpdate = createExpectedAR();
		String accessRequirementId = "1";
		toUpdate.setId(1L);
		toUpdate.setEtag("etag");
		toUpdate.setVersionNumber(1L);
		toUpdate.setAccessType(ACCESS_TYPE.DOWNLOAD);
		when(authorizationManager.canAccess(userInfo, accessRequirementId, ObjectType.ACCESS_REQUIREMENT, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationStatus.authorized());
		AccessRequirementInfoForUpdate info = new AccessRequirementInfoForUpdate();
		info.setEtag("etag");
		info.setCurrentVersion(1L);
		info.setAccessType(ACCESS_TYPE.PARTICIPATE);
		when(accessRequirementDAO.getForUpdate(accessRequirementId)).thenReturn(info );
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			arm.updateAccessRequirement(userInfo, accessRequirementId, toUpdate);
		});
	}

	@Test
	public void testUpdateConcreteType() {
		AccessRequirement toUpdate = createExpectedAR();
		String accessRequirementId = "1";
		toUpdate.setId(1L);
		toUpdate.setEtag("etag");
		toUpdate.setVersionNumber(1L);
		toUpdate.setAccessType(ACCESS_TYPE.DOWNLOAD);
		when(authorizationManager.canAccess(userInfo, accessRequirementId, ObjectType.ACCESS_REQUIREMENT, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationStatus.authorized());
		AccessRequirementInfoForUpdate info = new AccessRequirementInfoForUpdate();
		info.setEtag("etag");
		info.setCurrentVersion(1L);
		info.setAccessType(ACCESS_TYPE.DOWNLOAD);
		info.setConcreteType(TermsOfUseAccessRequirement.class.getName());
		when(accessRequirementDAO.getForUpdate(accessRequirementId)).thenReturn(info );
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			arm.updateAccessRequirement(userInfo, accessRequirementId, toUpdate);
		});
	}

	@Test
	public void testUpdateManagedACTAccessRequirement() {
		AccessRequirement toUpdate = createExpectedAR();
		String accessRequirementId = "1";
		toUpdate.setId(1L);
		toUpdate.setEtag("etag");
		toUpdate.setVersionNumber(1L);
		toUpdate.setAccessType(ACCESS_TYPE.DOWNLOAD);
		when(authorizationManager.canAccess(userInfo, accessRequirementId, ObjectType.ACCESS_REQUIREMENT, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationStatus.authorized());
		AccessRequirementInfoForUpdate info = new AccessRequirementInfoForUpdate();
		info.setEtag("etag");
		info.setCurrentVersion(1L);
		info.setAccessType(ACCESS_TYPE.DOWNLOAD);
		info.setConcreteType(ManagedACTAccessRequirement.class.getName());
		when(accessRequirementDAO.getForUpdate(accessRequirementId)).thenReturn(info );

		arm.updateAccessRequirement(userInfo, "1", toUpdate);

		// test that the right AR was created
		ArgumentCaptor<AccessRequirement> argument = ArgumentCaptor.forClass(AccessRequirement.class);
		verify(accessRequirementDAO).update(argument.capture());

		// verify that all default fields are set
		ManagedACTAccessRequirement ar = (ManagedACTAccessRequirement) argument.getValue();
		assertFalse(ar.getIsCertifiedUserRequired());
		assertFalse(ar.getIsValidatedProfileRequired());
		assertFalse(ar.getIsDUCRequired());
		assertFalse(ar.getIsIRBApprovalRequired());
		assertFalse(ar.getAreOtherAttachmentsRequired());
		assertFalse(ar.getIsIDUPublic());
		assertTrue(ar.getIsIDURequired());
		assertTrue(ar.getVersionNumber().equals(info.getCurrentVersion()+1));

		assertEquals(AccessRequirementManagerImpl.DEFAULT_EXPIRATION_PERIOD, ar.getExpirationPeriod());
	}

	@Test
	public void testGetAccessRequirementsForSubjectWithNullLimit() {
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId("1");
		rod.setType(RestrictableObjectType.ENTITY);
		arm.getAccessRequirementsForSubject(userInfo, rod, null, 0L);
		verify(accessRequirementDAO).getAccessRequirementsForSubject(any(List.class), eq(RestrictableObjectType.ENTITY), eq(DEFAULT_LIMIT), eq(0L));
	}

	@Test
	public void testGetAccessRequirementsForSubjectWithNullOffset() {
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId("1");
		rod.setType(RestrictableObjectType.ENTITY);
		arm.getAccessRequirementsForSubject(userInfo, rod, 10L, null);
		verify(accessRequirementDAO).getAccessRequirementsForSubject(any(List.class), eq(RestrictableObjectType.ENTITY), eq(10L), eq(DEFAULT_OFFSET));
	}

	@Test
	public void testGetAccessRequirementsForSubjectOverMaxLimit() {
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId("1");
		rod.setType(RestrictableObjectType.ENTITY);
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			arm.getAccessRequirementsForSubject(userInfo, rod, 51L, 0L);
		});
	}

	@Test
	public void testGetAccessRequirementsForSubjectZeroLimit() {
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId("1");
		rod.setType(RestrictableObjectType.ENTITY);
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			arm.getAccessRequirementsForSubject(userInfo, rod, 0L, 0L);
		});
	}

	@Test
	public void testGetAccessRequirementsForSubjectNegativeOffset() {
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId("1");
		rod.setType(RestrictableObjectType.ENTITY);
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			arm.getAccessRequirementsForSubject(userInfo, rod, 10L, -1L);
		});
	}

	@Test
	public void testNewLockAccessRequirementWithNullUserInfo(){
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			AccessRequirementManagerImpl.newLockAccessRequirement(null, TEST_ENTITY_ID, "jiraKey");
		});
	}

	@Test
	public void testNewLockAccessRequirementWithNullEntityId(){
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			AccessRequirementManagerImpl.newLockAccessRequirement(userInfo, null, "jiraKey");
		});
	}


	@Test
	public void testNewLockAccessRequirementWithNullJiraKey(){
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			AccessRequirementManagerImpl.newLockAccessRequirement(userInfo, TEST_ENTITY_ID, null);
		});
	}

	@Test
	public void testNewLockAccessRequirement(){
		String jiraKey = "jiraKey";
		LockAccessRequirement ar = AccessRequirementManagerImpl.newLockAccessRequirement(userInfo, TEST_ENTITY_ID, jiraKey);
		assertNotNull(ar);
		assertEquals(jiraKey, ar.getJiraKey());
		assertNotNull(ar.getSubjectIds());
		assertEquals(1, ar.getSubjectIds().size());
		assertEquals(TEST_ENTITY_ID, ar.getSubjectIds().get(0).getId());
		assertEquals(RestrictableObjectType.ENTITY, ar.getSubjectIds().get(0).getType());
		assertNotNull(ar.getCreatedBy());
		assertNotNull(ar.getCreatedOn());
		assertNotNull(ar.getModifiedBy());
		assertNotNull(ar.getModifiedOn());
	}

	@Test
	public void testDeleteAccessRequirementWithNullUserInfo() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			arm.deleteAccessRequirement(null, "1");
		});
	}

	@Test
	public void testDeleteAccessRequirementWithNullUserRequirementId() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			arm.deleteAccessRequirement(userInfo, null);
		});
	}

	@Test
	public void testDeleteAccessRequirementUnauthorized() {
		when(authorizationManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(false);
		Assertions.assertThrows(UnauthorizedException.class, () -> {
			arm.deleteAccessRequirement(userInfo, "1");
		});
	}

	@Test
	public void testDeleteAccessRequirementAuthorized() {
		when(authorizationManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(true);
		arm.deleteAccessRequirement(userInfo, "1");
		verify(accessRequirementDAO).delete("1");
	}

	@Test
	public void testConvertWillNullACTAccessRequirement() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			AccessRequirementManagerImpl.convert(null, "1");
		});
	}

	@Test
	public void testConvertWillNullModifiedBy() {
		ACTAccessRequirement ar = createACTAccessRequirement();
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			AccessRequirementManagerImpl.convert(ar, null);
		});
	}

	@Test
	public void testConvert() {
		ACTAccessRequirement ar = createACTAccessRequirement();
		String modifiedBy = "111";
		ManagedACTAccessRequirement managed = AccessRequirementManagerImpl.convert(ar, modifiedBy);
		assertNotNull(managed);
		assertEquals(ar.getId(), managed.getId());
		assertEquals(ar.getAccessType(), managed.getAccessType());
		assertEquals(ar.getCreatedBy(), managed.getCreatedBy());
		assertEquals(ar.getCreatedOn(), managed.getCreatedOn());
		assertEquals(ar.getSubjectIds(), managed.getSubjectIds());
		assertTrue(managed.getVersionNumber().equals(ar.getVersionNumber()+1));
		assertEquals(modifiedBy, managed.getModifiedBy());
		assertFalse(managed.getEtag().equals(ar.getEtag()));
	}

	private ACTAccessRequirement createACTAccessRequirement() {
		ACTAccessRequirement ar = new ACTAccessRequirement();
		ar.setAccessType(ACCESS_TYPE.DOWNLOAD);
		ar.setActContactInfo("actContactInfo");
		ar.setCreatedBy("1");
		ar.setCreatedOn(new Date());
		ar.setEtag("etag");
		ar.setId(2L);
		ar.setModifiedBy("3");
		ar.setModifiedOn(new Date());
		ar.setOpenJiraIssue(true);
		ar.setSubjectIds(new LinkedList<RestrictableObjectDescriptor>());
		ar.setVersionNumber(1L);
		return ar;
	}

	@Test
	public void testConvertAccessRequirementWithNullUserInfo() {
		AccessRequirementConversionRequest request = new AccessRequirementConversionRequest();
		request.setAccessRequirementId("1");
		request.setEtag("etag");
		request.setCurrentVersion(2L);
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			arm.convertAccessRequirement(null, request);
		});
	}

	@Test
	public void testConvertAccessRequirementWithNullRequest() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			arm.convertAccessRequirement(userInfo, null);
		});
	}

	@Test
	public void testConvertAccessRequirementWithNullRequirementId() {
		AccessRequirementConversionRequest request = new AccessRequirementConversionRequest();
		request.setEtag("etag");
		request.setCurrentVersion(2L);
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			arm.convertAccessRequirement(userInfo, request);
		});
	}

	@Test
	public void testConvertAccessRequirementWithNullEtag() {
		AccessRequirementConversionRequest request = new AccessRequirementConversionRequest();
		request.setAccessRequirementId("1");
		request.setCurrentVersion(2L);
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			arm.convertAccessRequirement(userInfo, request);
		});
	}

	@Test
	public void testConvertAccessRequirementWithNullVersion() {
		AccessRequirementConversionRequest request = new AccessRequirementConversionRequest();
		request.setAccessRequirementId("1");
		request.setEtag("etag");
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			arm.convertAccessRequirement(userInfo, request);
		});
	}

	@Test
	public void testConvertAccessRequirementUnauthorized() {
		AccessRequirementConversionRequest request = new AccessRequirementConversionRequest();
		request.setAccessRequirementId("1");
		request.setEtag("etag");
		request.setCurrentVersion(2L);
		Assertions.assertThrows(UnauthorizedException.class, () -> {
			arm.convertAccessRequirement(userInfo, request);
		});
	}

	@Test
	public void testConvertAccessRequirementNotFound() {
		when(authorizationManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(true);
		when(accessRequirementDAO.getAccessRequirementForUpdate("1")).thenThrow(new NotFoundException());
		AccessRequirementConversionRequest request = new AccessRequirementConversionRequest();
		request.setAccessRequirementId("1");
		request.setEtag("etag");
		request.setCurrentVersion(2L);
		Assertions.assertThrows(NotFoundException.class, () -> {
			arm.convertAccessRequirement(userInfo, request);
		});
	}

	@Test
	public void testConvertAccessRequirementEtagDoesNotMatch() {
		when(authorizationManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(true);
		ACTAccessRequirement current = createACTAccessRequirement();
		when(accessRequirementDAO.getAccessRequirementForUpdate("1")).thenReturn(current);
		AccessRequirementConversionRequest request = new AccessRequirementConversionRequest();
		request.setAccessRequirementId("1");
		request.setEtag("etag does not match");
		request.setCurrentVersion(current.getVersionNumber());
		Assertions.assertThrows(ConflictingUpdateException.class, () -> {
			arm.convertAccessRequirement(userInfo, request);
		});
	}

	@Test
	public void testConvertAccessRequirementVersionDoesNotMatch() {
		when(authorizationManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(true);
		ACTAccessRequirement current = createACTAccessRequirement();
		when(accessRequirementDAO.getAccessRequirementForUpdate("1")).thenReturn(current);
		AccessRequirementConversionRequest request = new AccessRequirementConversionRequest();
		request.setAccessRequirementId("1");
		request.setEtag(current.getEtag());
		request.setCurrentVersion(current.getVersionNumber()-1);
		Assertions.assertThrows(ConflictingUpdateException.class, () -> {
			arm.convertAccessRequirement(userInfo, request);
		});
	}

	@Test
	public void testConvertAccessRequirementNotSupported() {
		when(authorizationManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(true);
		when(accessRequirementDAO.getAccessRequirementForUpdate("1")).thenReturn(new TermsOfUseAccessRequirement());
		AccessRequirementConversionRequest request = new AccessRequirementConversionRequest();
		request.setAccessRequirementId("1");
		request.setEtag("etag");
		request.setCurrentVersion(2L);
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			arm.convertAccessRequirement(userInfo, request);
		});
	}

	@Test
	public void testConvertAccessRequirement() {
		when(authorizationManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(true);
		ACTAccessRequirement current = createACTAccessRequirement();
		when(accessRequirementDAO.getAccessRequirementForUpdate("1")).thenReturn(current);
		AccessRequirementConversionRequest request = new AccessRequirementConversionRequest();
		request.setAccessRequirementId("1");
		request.setEtag(current.getEtag());
		request.setCurrentVersion(current.getVersionNumber());
		arm.convertAccessRequirement(userInfo, request);
		ArgumentCaptor<ManagedACTAccessRequirement> captor = ArgumentCaptor.forClass(ManagedACTAccessRequirement.class);
		verify(accessRequirementDAO).update(captor.capture());
		ManagedACTAccessRequirement updated = captor.getValue();
		assertNotNull(updated);
		assertEquals(current.getId(), updated.getId());
		assertEquals(current.getAccessType(), updated.getAccessType());
		assertEquals(current.getCreatedBy(), updated.getCreatedBy());
		assertEquals(current.getCreatedOn(), updated.getCreatedOn());
		assertEquals(current.getSubjectIds(), updated.getSubjectIds());
		assertTrue(updated.getVersionNumber().equals(current.getVersionNumber()+1));
		assertEquals(userInfo.getId().toString(), updated.getModifiedBy());
		assertFalse(updated.getEtag().equals(current.getEtag()));
	}

	@Test
	public void testGetSubjectsWithNullAccessRequirementID() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			arm.getSubjects(null, null);
		});
	}

	@Test
	public void testGetSubjects() {
		List<RestrictableObjectDescriptor> subjects = new LinkedList<RestrictableObjectDescriptor>();
		when(accessRequirementDAO.getSubjects(1L, NextPageToken.DEFAULT_LIMIT+1, NextPageToken.DEFAULT_OFFSET)).thenReturn(subjects );
		RestrictableObjectDescriptorResponse response = arm.getSubjects("1", null);
		assertNotNull(response);
		assertEquals(subjects, response.getSubjects());
		assertNull(response.getNextPageToken());
	}

	@Test
	public void testDetermineObjectType() {
		assertEquals(RestrictableObjectType.ENTITY, AccessRequirementManagerImpl.determineObjectType(ACCESS_TYPE.DOWNLOAD));
		assertEquals(RestrictableObjectType.TEAM, AccessRequirementManagerImpl.determineObjectType(ACCESS_TYPE.PARTICIPATE));
		try {
			AccessRequirementManagerImpl.determineObjectType(ACCESS_TYPE.READ);
		} catch (IllegalArgumentException e) {
			// as expected
		}
	}

	@Test
	public void testValidateAccessRequirementWillNullAccessType() {
		AccessRequirement ar = createExpectedAR();
		ar.setAccessType(null);
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			AccessRequirementManagerImpl.validateAccessRequirement(ar);
		});
	}

	@Test
	public void testValidateAccessRequirementWillNullSubjectIds() {
		AccessRequirement ar = createExpectedAR();
		ar.setSubjectIds(null);
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			AccessRequirementManagerImpl.validateAccessRequirement(ar);
		});
	}

	@Test
	public void testValidateAccessRequirementForPostMessageContentAccessRequirement() {
		AccessRequirement ar = new PostMessageContentAccessRequirement();
		ar.setAccessType(ACCESS_TYPE.DOWNLOAD);
		ar.setSubjectIds(new LinkedList<RestrictableObjectDescriptor>());
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			AccessRequirementManagerImpl.validateAccessRequirement(ar);
		});
	}

	@Test
	public void testValidateAccessRequirementDownloadOnTeam() {
		AccessRequirement ar = createExpectedAR();
		ar.setAccessType(ACCESS_TYPE.DOWNLOAD);
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setType(RestrictableObjectType.TEAM);
		ar.setSubjectIds(Arrays.asList(rod));
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			AccessRequirementManagerImpl.validateAccessRequirement(ar);
		});
	}

	@Test
	public void testValidateAccessRequirementParticipateOnEntity() {
		AccessRequirement ar = createExpectedAR();
		ar.setAccessType(ACCESS_TYPE.PARTICIPATE);
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setType(RestrictableObjectType.ENTITY);
		ar.setSubjectIds(Arrays.asList(rod));
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			AccessRequirementManagerImpl.validateAccessRequirement(ar);
		});
	}
	
	@Test
	public void testValidateAccessRequirementWithDefaultExpiration() {
		ManagedACTAccessRequirement ar = createExpectedAR();
		
		ar.setExpirationPeriod(AccessRequirementManagerImpl.DEFAULT_EXPIRATION_PERIOD);
		
		AccessRequirementManagerImpl.validateAccessRequirement(ar);
	}
	
	@Test
	public void testValidateAccessRequirementWithInvalidExpiration() {
		ManagedACTAccessRequirement ar = createExpectedAR();
		
		ar.setExpirationPeriod(-1L);
		
		String expectedError = "When supplied, the expiration period should be greater than " + AccessRequirementManagerImpl.DEFAULT_EXPIRATION_PERIOD;
		
		assertEquals(expectedError, assertThrows(IllegalArgumentException.class, () -> {			
			AccessRequirementManagerImpl.validateAccessRequirement(ar);
		}).getMessage());
	}
	
	@Test
	public void testValidateAccessRequirementWithValidExpiration() {
		ManagedACTAccessRequirement ar = createExpectedAR();
		
		ar.setExpirationPeriod(365 * 24 * 60 * 60 * 1000L);
					
		AccessRequirementManagerImpl.validateAccessRequirement(ar);
		
	}
	
	@Test
	public void testValidateAccessRequirementWithValidDescription() {
		ManagedACTAccessRequirement ar = createExpectedAR();
		
		ar.setDescription("Some description");
		
		AccessRequirementManagerImpl.validateAccessRequirement(ar);
	}
	
	@Test
	public void testValidateAccessRequirementWithInvalidDescription() {
		ManagedACTAccessRequirement ar = createExpectedAR();
		
		ar.setDescription(RandomStringUtils.random(51));
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			AccessRequirementManagerImpl.validateAccessRequirement(ar);			
		}).getMessage();
		
		assertEquals("The AR description can be at most 50 characters.", errorMessage);
	}

	@Test
	public void testValidateAccessRequirement() {
		AccessRequirement ar = createExpectedAR();
		AccessRequirementManagerImpl.validateAccessRequirement(ar);
	}
}
