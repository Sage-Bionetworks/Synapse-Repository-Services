package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.manager.AccessRequirementManagerImpl.DEFAULT_LIMIT;
import static org.sagebionetworks.repo.manager.AccessRequirementManagerImpl.DEFAULT_OFFSET;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.DOWNLOAD;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.AccessApprovalDAO;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.AccessRequirementInfoForUpdate;
import org.sagebionetworks.repo.model.AccessRequirementStats;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.LockAccessRequirement;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.PostMessageContentAccessRequirement;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptorResponse;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.RestrictionInformationRequest;
import org.sagebionetworks.repo.model.RestrictionInformationResponse;
import org.sagebionetworks.repo.model.RestrictionLevel;
import org.sagebionetworks.repo.model.SelfSignAccessRequirement;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.NotificationEmailDAO;
import org.sagebionetworks.repo.model.dataaccess.AccessRequirementConversionRequest;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.util.jrjc.JiraClient;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import com.atlassian.jira.rest.client.api.OptionalIterable;
import com.atlassian.jira.rest.client.api.domain.BasicIssue;
import com.atlassian.jira.rest.client.api.domain.Field;
import com.atlassian.jira.rest.client.api.domain.IssueType;
import com.atlassian.jira.rest.client.api.domain.Project;
import com.atlassian.jira.rest.client.api.domain.input.IssueInput;

@RunWith(MockitoJUnitRunner.class)
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
	private EntityHeader mockEntityHeader;
	@Mock
	private AuthorizationManager authorizationManager;

	@InjectMocks
	private AccessRequirementManagerImpl arm;
	private UserInfo userInfo;
	@Mock
	private NotificationEmailDAO notificationEmailDao;
	@Mock
	Project mockProject;

	
	@Before
	public void setUp() throws Exception {
		PrincipalAlias alias = new PrincipalAlias();
		alias.setAlias("foo@bar.com");
		when(notificationEmailDao.getNotificationEmailForPrincipal(anyLong())).thenReturn(alias.getAlias());
		userInfo = new UserInfo(false, TEST_PRINCIPAL_ID);
		Iterable<IssueType> issueTypes = Arrays.asList(new IssueType[]{
				new IssueType(null, 1L, "Flag", false, null, null),
				new IssueType(null, 2L, "Access Restriction", false, null, null) 
		});
		when(mockProject.getIssueTypes()).thenReturn(new OptionalIterable<IssueType>(issueTypes));
		Iterable<Field> fields = Arrays.asList(new Field[]{
				new Field("101", "Synapse Principal ID", null, false, false, false, null),
				new Field("102", "Synapse User Display Name", null, false, false, false, null),
				new Field("103", "Synapse Data Object", null, false, false, false, null)
		});
		when(jiraClient.getFields()).thenReturn(fields);
		when(jiraClient.getProject(anyString())).thenReturn(mockProject);
		when(jiraClient.createIssue((IssueInput)anyObject())).thenReturn(new BasicIssue(null, "SG-101", 101L));
		
		// by default the user is authorized to create, edit.  individual tests may override these settings
		when(authorizationManager.canAccess(userInfo, TEST_ENTITY_ID, ObjectType.ENTITY, ACCESS_TYPE.CREATE)).thenReturn(AuthorizationStatus.authorized());
		when(authorizationManager.canAccess(userInfo, TEST_ENTITY_ID, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationStatus.authorized());

		when(mockEntityHeader.getId()).thenReturn(TEST_ENTITY_ID);
		when(nodeDao.getEntityPath(TEST_ENTITY_ID)).thenReturn(Arrays.asList(mockEntityHeader));
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreatePostMessageContentAccessRequirement() {
		arm.createAccessRequirement(userInfo, new PostMessageContentAccessRequirement());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateNullAccessType() {
		AccessRequirement toCreate = createExpectedAR();
		toCreate.setAccessType(null);
		arm.createAccessRequirement(userInfo, toCreate);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateNullSubjectIds() {
		AccessRequirement toCreate = createExpectedAR();
		toCreate.setSubjectIds(null);
		arm.createAccessRequirement(userInfo, toCreate);
	}
	
	private AccessRequirement createExpectedAR() {
		ManagedACTAccessRequirement expectedAR = new ManagedACTAccessRequirement();
		expectedAR.setAccessType(ACCESS_TYPE.DOWNLOAD);
		RestrictableObjectDescriptor subjectId = new RestrictableObjectDescriptor();
		subjectId.setId(TEST_ENTITY_ID);
		subjectId.setType(RestrictableObjectType.ENTITY);
		expectedAR.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{subjectId}));
		AccessRequirementManagerImpl.populateCreationFields(userInfo, expectedAR);
		return expectedAR;
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateLockAccessRequirementWithNullUserInfo() {
		arm.createLockAccessRequirement(null, TEST_ENTITY_ID);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateLockAccessRequirementWithNullEntityId() {
		arm.createLockAccessRequirement(userInfo, null);
	}

	@Test
	public void testCreateLockAccessRequirementHappyPath() throws Exception {
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
		verify(jiraClient).createIssue((IssueInput)anyObject());
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testCreateLockAccessRequirementWithoutREADPermission() throws Exception {
		when(authorizationManager.canAccess(userInfo, TEST_ENTITY_ID, ObjectType.ENTITY, ACCESS_TYPE.CREATE)).thenReturn(AuthorizationStatus.accessDenied(""));
		// this should throw the unauthorized exception
		arm.createLockAccessRequirement(userInfo, TEST_ENTITY_ID);
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testCreateLockAccessRequirementWithoutUPDATEPermission() throws Exception {
		when(authorizationManager.canAccess(userInfo, TEST_ENTITY_ID, ObjectType.ENTITY, ACCESS_TYPE.CREATE)).thenReturn(AuthorizationStatus.authorized());
		when(authorizationManager.canAccess(userInfo, TEST_ENTITY_ID, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationStatus.accessDenied(""));
		// this should throw the unauthorized exception
		arm.createLockAccessRequirement(userInfo, TEST_ENTITY_ID);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testCreateLockAccessRequirementAlreadyExists() throws Exception {
		Set<String> ars = new HashSet<String>();
		String accessRequirementId = "1";
		ars.add(accessRequirementId);
		AccessRequirementStats stats = new AccessRequirementStats();
		stats.setRequirementIdSet(ars);
		when(accessRequirementDAO.getAccessRequirementStats(any(List.class), eq(RestrictableObjectType.ENTITY))).thenReturn(stats);
		// this should throw the illegal argument exception
		arm.createLockAccessRequirement(userInfo, TEST_ENTITY_ID);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testCreateUploadAccessRequirement() throws Exception {
		AccessRequirement ar = createExpectedAR();
		ar.setAccessType(ACCESS_TYPE.UPLOAD);
		arm.createAccessRequirement(userInfo, ar);
	}
	
	@Test
	public void testUnmetForEntity() throws Exception {
		Long mockDownloadARId = 1L;
		Long mockUploadARId = 2L;
		RestrictableObjectDescriptor subjectId = new RestrictableObjectDescriptor();
		subjectId.setId(TEST_ENTITY_ID);
		subjectId.setType(RestrictableObjectType.ENTITY);
		when(nodeDao.getEntityPath(TEST_ENTITY_ID)).thenReturn(new ArrayList<EntityHeader>()); // an empty list, i.e. this is a top-level object
		Node mockNode = new Node();
		mockNode.setId(KeyFactory.stringToKey(TEST_ENTITY_ID).toString());
		mockNode.setCreatedByPrincipalId(999L); // someone other than TEST_PRINCIPAL_ID
		mockNode.setNodeType(EntityType.file);
		when(nodeDao.getNode(TEST_ENTITY_ID)).thenReturn(mockNode);
		when(accessRequirementDAO.getAllUnmetAccessRequirements(
				Collections.singletonList(TEST_ENTITY_ID), 
				RestrictableObjectType.ENTITY, 
				Collections.singleton(userInfo.getId()), 
				Collections.singletonList(DOWNLOAD))).
				thenReturn(Collections.singletonList(mockDownloadARId));
		AccessRequirement downloadAR = new TermsOfUseAccessRequirement();
		downloadAR.setId(mockDownloadARId);
		AccessRequirement uploadAR = new TermsOfUseAccessRequirement();
		uploadAR.setId(mockUploadARId);
		List<AccessRequirement> arList = Arrays.asList(new AccessRequirement[]{downloadAR, uploadAR});
		when(accessRequirementDAO.getAllAccessRequirementsForSubject(Collections.singletonList(TEST_ENTITY_ID), RestrictableObjectType.ENTITY)).
			thenReturn(arList);
		List<AccessRequirement> result = arm.getAllUnmetAccessRequirements(userInfo, subjectId, DOWNLOAD);
		assertEquals(Collections.singletonList(downloadAR), result);
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

		assertEquals(AccessRequirementManagerImpl.DEFAULT_EXPIRATION_PERIOD, ar.getExpirationPeriod());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateWithNullUserInfo() {
		AccessRequirement toUpdate = createExpectedAR();
		String accessRequirementId = "1";
		toUpdate.setId(1L);
		arm.updateAccessRequirement(null, accessRequirementId, toUpdate);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateWithNullAccessRequirementId() {
		AccessRequirement toUpdate = createExpectedAR();
		toUpdate.setId(1L);
		arm.updateAccessRequirement(userInfo, null, toUpdate);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateWithNullAccessRequirement() {
		arm.updateAccessRequirement(userInfo, "1", null);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateWithIdDoesNotMatch() {
		AccessRequirement toUpdate = createExpectedAR();
		toUpdate.setId(1L);
		arm.updateAccessRequirement(userInfo, "-1", toUpdate);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testUpdateWithInvalidAccessRequirement() {
		AccessRequirement toUpdate = createExpectedAR();
		String accessRequirementId = "1";
		toUpdate.setId(1L);
		toUpdate.setAccessType(ACCESS_TYPE.CHANGE_PERMISSIONS);
		arm.updateAccessRequirement(userInfo, accessRequirementId, toUpdate);
	}

	@Test (expected = UnauthorizedException.class)
	public void testUpdateUnauthorized() {
		AccessRequirement toUpdate = createExpectedAR();
		String accessRequirementId = "1";
		toUpdate.setId(1L);
		when(authorizationManager.canAccess(userInfo, accessRequirementId, ObjectType.ACCESS_REQUIREMENT, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationStatus.accessDenied(""));
		arm.updateAccessRequirement(userInfo, accessRequirementId, toUpdate);
	}

	@Test (expected = NotFoundException.class)
	public void testUpdateNoneExistingAR() {
		AccessRequirement toUpdate = createExpectedAR();
		String accessRequirementId = "1";
		toUpdate.setId(1L);
		when(authorizationManager.canAccess(userInfo, accessRequirementId, ObjectType.ACCESS_REQUIREMENT, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationStatus.authorized());
		when(accessRequirementDAO.getForUpdate(accessRequirementId)).thenThrow(new NotFoundException());
		arm.updateAccessRequirement(userInfo, accessRequirementId, toUpdate);
	}

	@Test (expected = ConflictingUpdateException.class)
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
		arm.updateAccessRequirement(userInfo, accessRequirementId, toUpdate);
	}

	@Test (expected = ConflictingUpdateException.class)
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
		arm.updateAccessRequirement(userInfo, accessRequirementId, toUpdate);
	}

	@Test (expected = IllegalArgumentException.class)
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
		arm.updateAccessRequirement(userInfo, accessRequirementId, toUpdate);
	}

	@Test (expected = IllegalArgumentException.class)
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
		arm.updateAccessRequirement(userInfo, accessRequirementId, toUpdate);
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

	@Test (expected = IllegalArgumentException.class)
	public void testGetAccessRequirementsForSubjectOverMaxLimit() {
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId("1");
		rod.setType(RestrictableObjectType.ENTITY);
		arm.getAccessRequirementsForSubject(userInfo, rod, 51L, 0L);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetAccessRequirementsForSubjectZeroLimit() {
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId("1");
		rod.setType(RestrictableObjectType.ENTITY);
		arm.getAccessRequirementsForSubject(userInfo, rod, 0L, 0L);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetAccessRequirementsForSubjectNegativeOffset() {
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId("1");
		rod.setType(RestrictableObjectType.ENTITY);
		arm.getAccessRequirementsForSubject(userInfo, rod, 10L, -1L);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetRestrictionInformationWithNullUserInfo() {
		RestrictionInformationRequest request = new RestrictionInformationRequest();
		request.setObjectId(TEST_ENTITY_ID);
		request.setRestrictableObjectType(RestrictableObjectType.ENTITY);
		arm.getRestrictionInformation(null, request);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetRestrictionInformationWithNullRequest() {
		arm.getRestrictionInformation(userInfo, null);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetRestrictionInformationWithNullObjectId() {
		RestrictionInformationRequest request = new RestrictionInformationRequest();
		request.setRestrictableObjectType(RestrictableObjectType.ENTITY);
		arm.getRestrictionInformation(userInfo, request);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetRestrictionInformationWithNullObjectType() {
		RestrictionInformationRequest request = new RestrictionInformationRequest();
		request.setObjectId(TEST_ENTITY_ID);
		arm.getRestrictionInformation(userInfo, request);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetRestrictionInformationForEvaluation() {
		RestrictionInformationRequest request = new RestrictionInformationRequest();
		request.setObjectId(TEST_ENTITY_ID);
		request.setRestrictableObjectType(RestrictableObjectType.EVALUATION);
		arm.getRestrictionInformation(userInfo, request);
	}

	@Test
	public void testGetRestrictionInformationWithZeroAR() {
		RestrictionInformationRequest request = new RestrictionInformationRequest();
		request.setObjectId(TEST_ENTITY_ID);
		request.setRestrictableObjectType(RestrictableObjectType.ENTITY);
		AccessRequirementStats stats = new AccessRequirementStats();
		stats.setRequirementIdSet(new HashSet<String>());
		when(accessRequirementDAO.getAccessRequirementStats(Arrays.asList(TEST_ENTITY_ID), RestrictableObjectType.ENTITY)).thenReturn(stats );
		RestrictionInformationResponse info = arm.getRestrictionInformation(userInfo, request);
		assertNotNull(info);
		assertEquals(RestrictionLevel.OPEN, info.getRestrictionLevel());
		assertFalse(info.getHasUnmetAccessRequirement());
		verify(nodeDao).getEntityPath(TEST_ENTITY_ID);
	}

	@Test
	public void testGetRestrictionInformationWithToU() {
		RestrictionInformationRequest request = new RestrictionInformationRequest();
		request.setObjectId(TEST_ENTITY_ID);
		request.setRestrictableObjectType(RestrictableObjectType.ENTITY);
		AccessRequirementStats stats = new AccessRequirementStats();
		Set<String> set = new HashSet<String>();
		set.add("1");
		stats.setRequirementIdSet(set);
		stats.setHasToU(true);
		stats.setHasACT(false);
		stats.setHasLock(false);
		when(accessRequirementDAO.getAccessRequirementStats(Arrays.asList(TEST_ENTITY_ID), RestrictableObjectType.ENTITY)).thenReturn(stats );
		when(accessApprovalDAO.hasUnmetAccessRequirement(set, userInfo.getId().toString())).thenReturn(true);
		RestrictionInformationResponse info = arm.getRestrictionInformation(userInfo, request);
		assertNotNull(info);
		assertEquals(RestrictionLevel.RESTRICTED_BY_TERMS_OF_USE, info.getRestrictionLevel());
		assertTrue(info.getHasUnmetAccessRequirement());
		verify(nodeDao).getEntityPath(TEST_ENTITY_ID);
	}

	@Test
	public void testGetRestrictionInformationWithLock() {
		RestrictionInformationRequest request = new RestrictionInformationRequest();
		request.setObjectId(TEST_ENTITY_ID);
		request.setRestrictableObjectType(RestrictableObjectType.ENTITY);
		AccessRequirementStats stats = new AccessRequirementStats();
		Set<String> set = new HashSet<String>();
		set.add("1");
		stats.setRequirementIdSet(set);
		stats.setHasToU(false);
		stats.setHasACT(false);
		stats.setHasLock(true);
		when(accessRequirementDAO.getAccessRequirementStats(Arrays.asList(TEST_ENTITY_ID), RestrictableObjectType.ENTITY)).thenReturn(stats );
		when(accessApprovalDAO.hasUnmetAccessRequirement(set, userInfo.getId().toString())).thenReturn(true);
		RestrictionInformationResponse info = arm.getRestrictionInformation(userInfo, request);
		assertNotNull(info);
		assertEquals(RestrictionLevel.CONTROLLED_BY_ACT, info.getRestrictionLevel());
		assertTrue(info.getHasUnmetAccessRequirement());
		verify(nodeDao).getEntityPath(TEST_ENTITY_ID);
	}

	@Test
	public void testGetRestrictionInformationWithACT() {
		RestrictionInformationRequest request = new RestrictionInformationRequest();
		request.setObjectId(TEST_ENTITY_ID);
		request.setRestrictableObjectType(RestrictableObjectType.ENTITY);
		AccessRequirementStats stats = new AccessRequirementStats();
		Set<String> set = new HashSet<String>();
		set.add("1");
		stats.setRequirementIdSet(set);
		stats.setHasToU(false);
		stats.setHasACT(true);
		stats.setHasLock(false);
		when(accessRequirementDAO.getAccessRequirementStats(Arrays.asList(TEST_ENTITY_ID), RestrictableObjectType.ENTITY)).thenReturn(stats );
		when(accessApprovalDAO.hasUnmetAccessRequirement(set, userInfo.getId().toString())).thenReturn(false);
		RestrictionInformationResponse info = arm.getRestrictionInformation(userInfo, request);
		assertNotNull(info);
		assertEquals(RestrictionLevel.CONTROLLED_BY_ACT, info.getRestrictionLevel());
		assertFalse(info.getHasUnmetAccessRequirement());
		verify(nodeDao).getEntityPath(TEST_ENTITY_ID);
	}

	@Test
	public void testGetRestrictionInformationWithBoth() {
		RestrictionInformationRequest request = new RestrictionInformationRequest();
		request.setObjectId(TEST_ENTITY_ID);
		request.setRestrictableObjectType(RestrictableObjectType.ENTITY);
		AccessRequirementStats stats = new AccessRequirementStats();
		Set<String> set = new HashSet<String>();
		set.add("1");
		set.add("2");
		stats.setRequirementIdSet(set);
		stats.setHasToU(true);
		stats.setHasACT(true);
		stats.setHasLock(false);
		when(accessRequirementDAO.getAccessRequirementStats(Arrays.asList(TEST_ENTITY_ID), RestrictableObjectType.ENTITY)).thenReturn(stats );
		when(accessApprovalDAO.hasUnmetAccessRequirement(set, userInfo.getId().toString())).thenReturn(false);
		RestrictionInformationResponse info = arm.getRestrictionInformation(userInfo, request);
		assertNotNull(info);
		assertEquals(RestrictionLevel.CONTROLLED_BY_ACT, info.getRestrictionLevel());
		assertFalse(info.getHasUnmetAccessRequirement());
		verify(nodeDao).getEntityPath(TEST_ENTITY_ID);
	}

	@Test (expected = IllegalStateException.class)
	public void testGetRestrictionInformationWithIllegalState() {
		RestrictionInformationRequest request = new RestrictionInformationRequest();
		request.setObjectId(TEST_ENTITY_ID);
		request.setRestrictableObjectType(RestrictableObjectType.ENTITY);
		AccessRequirementStats stats = new AccessRequirementStats();
		Set<String> set = new HashSet<String>();
		set.add("1");
		set.add("2");
		stats.setRequirementIdSet(set);
		stats.setHasToU(false);
		stats.setHasACT(false);
		stats.setHasLock(false);
		when(accessRequirementDAO.getAccessRequirementStats(Arrays.asList(TEST_ENTITY_ID), RestrictableObjectType.ENTITY)).thenReturn(stats);
		arm.getRestrictionInformation(userInfo, request);
	}

	@Test
	public void testGetRestrictionInformationForTeam() {
		RestrictionInformationRequest request = new RestrictionInformationRequest();
		request.setObjectId(TEST_PRINCIPAL_ID);
		request.setRestrictableObjectType(RestrictableObjectType.TEAM);
		AccessRequirementStats stats = new AccessRequirementStats();
		Set<String> set = new HashSet<String>();
		set.add("1");
		stats.setRequirementIdSet(set);
		stats.setHasToU(true);
		stats.setHasACT(false);
		stats.setHasLock(false);
		when(accessRequirementDAO.getAccessRequirementStats(Arrays.asList(TEST_PRINCIPAL_ID), RestrictableObjectType.TEAM)).thenReturn(stats);
		when(accessApprovalDAO.hasUnmetAccessRequirement(set, userInfo.getId().toString())).thenReturn(true);
		RestrictionInformationResponse info = arm.getRestrictionInformation(userInfo, request);
		assertNotNull(info);
		assertEquals(RestrictionLevel.RESTRICTED_BY_TERMS_OF_USE, info.getRestrictionLevel());
		assertTrue(info.getHasUnmetAccessRequirement());
		verify(nodeDao, never()).getEntityPath(TEST_ENTITY_ID);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNewLockAccessRequirementWithNullUserInfo(){
		AccessRequirementManagerImpl.newLockAccessRequirement(null, TEST_ENTITY_ID, "jiraKey");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNewLockAccessRequirementWithNullEntityId(){
		AccessRequirementManagerImpl.newLockAccessRequirement(userInfo, null, "jiraKey");
	}


	@Test(expected = IllegalArgumentException.class)
	public void testNewLockAccessRequirementWithNullJiraKey(){
		AccessRequirementManagerImpl.newLockAccessRequirement(userInfo, TEST_ENTITY_ID, null);
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

	@Test (expected=IllegalArgumentException.class)
	public void testDeleteAccessRequirementWithNullUserInfo() {
		arm.deleteAccessRequirement(null, "1");
	}

	@Test (expected=IllegalArgumentException.class)
	public void testDeleteAccessRequirementWithNullUserRequirementId() {
		arm.deleteAccessRequirement(userInfo, null);
	}

	@Test (expected=UnauthorizedException.class)
	public void testDeleteAccessRequirementUnauthorized() {
		when(authorizationManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(false);
		arm.deleteAccessRequirement(userInfo, "1");
	}

	@Test
	public void testDeleteAccessRequirementAuthorized() {
		when(authorizationManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(true);
		arm.deleteAccessRequirement(userInfo, "1");
		verify(accessRequirementDAO).delete("1");
	}

	@Test (expected=IllegalArgumentException.class)
	public void testConvertWillNullACTAccessRequirement() {
		AccessRequirementManagerImpl.convert(null, "1");
	}

	@Test (expected=IllegalArgumentException.class)
	public void testConvertWillNullModifiedBy() {
		ACTAccessRequirement ar = createACTAccessRequirement();
		AccessRequirementManagerImpl.convert(ar, null);
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

	@Test (expected=IllegalArgumentException.class)
	public void testConvertAccessRequirementWithNullUserInfo() {
		AccessRequirementConversionRequest request = new AccessRequirementConversionRequest();
		request.setAccessRequirementId("1");
		request.setEtag("etag");
		request.setCurrentVersion(2L);
		arm.convertAccessRequirement(null, request);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testConvertAccessRequirementWithNullRequest() {
		arm.convertAccessRequirement(userInfo, null);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testConvertAccessRequirementWithNullRequirementId() {
		AccessRequirementConversionRequest request = new AccessRequirementConversionRequest();
		request.setEtag("etag");
		request.setCurrentVersion(2L);
		arm.convertAccessRequirement(userInfo, request);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testConvertAccessRequirementWithNullEtag() {
		AccessRequirementConversionRequest request = new AccessRequirementConversionRequest();
		request.setAccessRequirementId("1");
		request.setCurrentVersion(2L);
		arm.convertAccessRequirement(userInfo, request);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testConvertAccessRequirementWithNullVersion() {
		AccessRequirementConversionRequest request = new AccessRequirementConversionRequest();
		request.setAccessRequirementId("1");
		request.setEtag("etag");
		arm.convertAccessRequirement(userInfo, request);
	}

	@Test (expected=UnauthorizedException.class)
	public void testConvertAccessRequirementUnauthorized() {
		AccessRequirementConversionRequest request = new AccessRequirementConversionRequest();
		request.setAccessRequirementId("1");
		request.setEtag("etag");
		request.setCurrentVersion(2L);
		arm.convertAccessRequirement(userInfo, request);
	}

	@Test (expected=NotFoundException.class)
	public void testConvertAccessRequirementNotFound() {
		when(authorizationManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(true);
		when(accessRequirementDAO.getAccessRequirementForUpdate("1")).thenThrow(new NotFoundException());
		AccessRequirementConversionRequest request = new AccessRequirementConversionRequest();
		request.setAccessRequirementId("1");
		request.setEtag("etag");
		request.setCurrentVersion(2L);
		arm.convertAccessRequirement(userInfo, request);
	}

	@Test (expected=ConflictingUpdateException.class)
	public void testConvertAccessRequirementEtagDoesNotMatch() {
		when(authorizationManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(true);
		ACTAccessRequirement current = createACTAccessRequirement();
		when(accessRequirementDAO.getAccessRequirementForUpdate("1")).thenReturn(current);
		AccessRequirementConversionRequest request = new AccessRequirementConversionRequest();
		request.setAccessRequirementId("1");
		request.setEtag("etag does not match");
		request.setCurrentVersion(current.getVersionNumber());
		arm.convertAccessRequirement(userInfo, request);
	}

	@Test (expected=ConflictingUpdateException.class)
	public void testConvertAccessRequirementVersionDoesNotMatch() {
		when(authorizationManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(true);
		ACTAccessRequirement current = createACTAccessRequirement();
		when(accessRequirementDAO.getAccessRequirementForUpdate("1")).thenReturn(current);
		AccessRequirementConversionRequest request = new AccessRequirementConversionRequest();
		request.setAccessRequirementId("1");
		request.setEtag(current.getEtag());
		request.setCurrentVersion(current.getVersionNumber()-1);
		arm.convertAccessRequirement(userInfo, request);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testConvertAccessRequirementNotSupported() {
		when(authorizationManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(true);
		when(accessRequirementDAO.getAccessRequirementForUpdate("1")).thenReturn(new TermsOfUseAccessRequirement());
		AccessRequirementConversionRequest request = new AccessRequirementConversionRequest();
		request.setAccessRequirementId("1");
		request.setEtag("etag");
		request.setCurrentVersion(2L);
		arm.convertAccessRequirement(userInfo, request);
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

	@Test (expected = IllegalArgumentException.class)
	public void testGetSubjectsWithNullAccessRequirementID() {
		arm.getSubjects(null, null);
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
			AccessRequirementManagerImpl.determineObjectType(ACCESS_TYPE.UPLOAD);
		} catch (IllegalArgumentException e) {
			// as expected
		}
	}

	@Test (expected = IllegalArgumentException.class)
	public void testValidateAccessRequirementWillNullAccessType() {
		AccessRequirement ar = createExpectedAR();
		ar.setAccessType(null);
		AccessRequirementManagerImpl.validateAccessRequirement(ar);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testValidateAccessRequirementWillNullSubjectIds() {
		AccessRequirement ar = createExpectedAR();
		ar.setSubjectIds(null);
		AccessRequirementManagerImpl.validateAccessRequirement(ar);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testValidateAccessRequirementForPostMessageContentAccessRequirement() {
		AccessRequirement ar = new PostMessageContentAccessRequirement();
		ar.setAccessType(ACCESS_TYPE.DOWNLOAD);
		ar.setSubjectIds(new LinkedList<RestrictableObjectDescriptor>());
		AccessRequirementManagerImpl.validateAccessRequirement(ar);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testValidateAccessRequirementDownloadOnTeam() {
		AccessRequirement ar = createExpectedAR();
		ar.setAccessType(ACCESS_TYPE.DOWNLOAD);
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setType(RestrictableObjectType.TEAM);
		ar.setSubjectIds(Arrays.asList(rod));
		AccessRequirementManagerImpl.validateAccessRequirement(ar);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testValidateAccessRequirementParticipateOnEntity() {
		AccessRequirement ar = createExpectedAR();
		ar.setAccessType(ACCESS_TYPE.PARTICIPATE);
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setType(RestrictableObjectType.ENTITY);
		ar.setSubjectIds(Arrays.asList(rod));
		AccessRequirementManagerImpl.validateAccessRequirement(ar);
	}

	@Test 
	public void testValidateAccessRequirement() {
		AccessRequirement ar = createExpectedAR();
		AccessRequirementManagerImpl.validateAccessRequirement(ar);
	}
}
