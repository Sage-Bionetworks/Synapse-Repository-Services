package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.sagebionetworks.repo.manager.AccessRequirementManagerImpl.DEFAULT_LIMIT;
import static org.sagebionetworks.repo.manager.AccessRequirementManagerImpl.DEFAULT_OFFSET;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.DOWNLOAD;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.UPLOAD;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.AccessApprovalDAO;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.AccessRequirementStats;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.LockAccessRequirement;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.PostMessageContentAccessRequirement;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.RestrictionInformationRequest;
import org.sagebionetworks.repo.model.RestrictionInformationResponse;
import org.sagebionetworks.repo.model.RestrictionLevel;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.NotificationEmailDAO;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.util.jrjc.JiraClient;
import org.springframework.test.util.ReflectionTestUtils;

import com.atlassian.jira.rest.client.api.OptionalIterable;
import com.atlassian.jira.rest.client.api.domain.BasicIssue;
import com.atlassian.jira.rest.client.api.domain.Field;
import com.atlassian.jira.rest.client.api.domain.IssueType;
import com.atlassian.jira.rest.client.api.domain.Project;
import com.atlassian.jira.rest.client.api.domain.input.IssueInput;

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
	private AccessRequirementManagerImpl arm;
	private UserInfo userInfo;
	@Mock
	private NotificationEmailDAO notificationEmailDao;

	
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		arm = new AccessRequirementManagerImpl();
		ReflectionTestUtils.setField(arm, "accessRequirementDAO", accessRequirementDAO);
		ReflectionTestUtils.setField(arm, "accessApprovalDAO", accessApprovalDAO);
		ReflectionTestUtils.setField(arm, "nodeDao", nodeDao);
		ReflectionTestUtils.setField(arm, "notificationEmailDao", notificationEmailDao);
		ReflectionTestUtils.setField(arm, "authorizationManager", authorizationManager);
		ReflectionTestUtils.setField(arm, "jiraClient", jiraClient);

		PrincipalAlias alias = new PrincipalAlias();
		alias.setAlias("foo@bar.com");
		when(notificationEmailDao.getNotificationEmailForPrincipal(anyLong())).thenReturn(alias.getAlias());
		userInfo = new UserInfo(false, TEST_PRINCIPAL_ID);
		Project sgProject;
		sgProject = Mockito.mock(Project.class);
		Iterable<IssueType> issueTypes = Arrays.asList(new IssueType[]{
				new IssueType(null, 1L, "Flag", false, null, null),
				new IssueType(null, 2L, "Access Restriction", false, null, null) 
		});
		when(sgProject.getIssueTypes()).thenReturn(new OptionalIterable<IssueType>(issueTypes));
		Iterable<Field> fields = Arrays.asList(new Field[]{
				new Field("101", "Synapse Principal ID", null, false, false, false, null),
				new Field("102", "Synapse User Display Name", null, false, false, false, null),
				new Field("103", "Synapse Data Object", null, false, false, false, null)
		});
		when(jiraClient.getFields()).thenReturn(fields);
		when(jiraClient.getProject(anyString())).thenReturn(sgProject);
		when(jiraClient.createIssue((IssueInput)anyObject())).thenReturn(new BasicIssue(null, "SG-101", 101L));
		
		// by default the user is authorized to create, edit.  individual tests may override these settings
		when(authorizationManager.canAccess(userInfo, TEST_ENTITY_ID, ObjectType.ENTITY, ACCESS_TYPE.CREATE)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		when(authorizationManager.canAccess(userInfo, TEST_ENTITY_ID, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);

		when(mockEntityHeader.getId()).thenReturn(TEST_ENTITY_ID);
		when(nodeDao.getEntityPath(TEST_ENTITY_ID)).thenReturn(Arrays.asList(mockEntityHeader));
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateARForEvaluation() {
		ACTAccessRequirement toCreate = new ACTAccessRequirement();
		toCreate.setAccessType(ACCESS_TYPE.DOWNLOAD);
		toCreate.setActContactInfo("Access restricted pending review by Synapse Access and Compliance Team.");
		RestrictableObjectDescriptor subjectId = new RestrictableObjectDescriptor();
		subjectId.setId(TEST_ENTITY_ID);
		subjectId.setType(RestrictableObjectType.EVALUATION);
		toCreate.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{subjectId}));
		AccessRequirementManagerImpl.populateCreationFields(userInfo, toCreate);
		arm.createAccessRequirement(userInfo, toCreate);
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
		ACTAccessRequirement expectedAR = new ACTAccessRequirement();
		expectedAR.setAccessType(ACCESS_TYPE.DOWNLOAD);
		expectedAR.setActContactInfo("Access restricted pending review by Synapse Access and Compliance Team.");
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
		when(authorizationManager.canAccess(userInfo, TEST_ENTITY_ID, ObjectType.ENTITY, ACCESS_TYPE.CREATE)).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		when(authorizationManager.canAccess(userInfo, TEST_ENTITY_ID, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		// this should throw the unauthorized exception
		arm.createLockAccessRequirement(userInfo, TEST_ENTITY_ID);
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testCreateLockAccessRequirementWithoutUPDATEPermission() throws Exception {
		when(authorizationManager.canAccess(userInfo, TEST_ENTITY_ID, ObjectType.ENTITY, ACCESS_TYPE.CREATE)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		when(authorizationManager.canAccess(userInfo, TEST_ENTITY_ID, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
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
		when(authorizationManager.canCreateAccessRequirement(userInfo, ar)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
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
		when(accessRequirementDAO.getAllUnmetAccessRequirements(
				Collections.singletonList(TEST_ENTITY_ID), 
				RestrictableObjectType.ENTITY, 
				Collections.singleton(userInfo.getId()), 
				Collections.singletonList(UPLOAD))).
				thenReturn(Collections.singletonList(mockUploadARId));
		AccessRequirement downloadAR = new TermsOfUseAccessRequirement();
		downloadAR.setId(mockDownloadARId);
		AccessRequirement uploadAR = new TermsOfUseAccessRequirement();
		uploadAR.setId(mockUploadARId);
		List<AccessRequirement> arList = Arrays.asList(new AccessRequirement[]{downloadAR, uploadAR});
		when(accessRequirementDAO.getAllAccessRequirementsForSubject(Collections.singletonList(TEST_ENTITY_ID), RestrictableObjectType.ENTITY)).
			thenReturn(arList);
		List<AccessRequirement> result = arm.getAllUnmetAccessRequirements(userInfo, subjectId, DOWNLOAD);
		assertEquals(Collections.singletonList(downloadAR), result);
		result = arm.getAllUnmetAccessRequirements(userInfo, subjectId, UPLOAD);
		assertEquals(Collections.singletonList(uploadAR), result);
	}

	@Test
	public void testSetDefaultValues() {
		ACTAccessRequirement ar = (ACTAccessRequirement) createExpectedAR();
		ar = (ACTAccessRequirement) AccessRequirementManagerImpl.setDefaultValues(ar);
		assertFalse(ar.getIsCertifiedUserRequired());
		assertFalse(ar.getIsValidatedProfileRequired());
		assertFalse(ar.getIsDUCRequired());
		assertFalse(ar.getIsIRBApprovalRequired());
		assertFalse(ar.getAreOtherAttachmentsRequired());
		assertFalse(ar.getIsAnnualReviewRequired());
		assertFalse(ar.getIsIDUPublic());
	}

	@Test
	public void testCreateACTAccessRequirement() {
		AccessRequirement toCreate = createExpectedAR();
		when(authorizationManager.canCreateAccessRequirement(userInfo, toCreate)).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		arm.createAccessRequirement(userInfo, toCreate);

		// test that the right AR was created
		ArgumentCaptor<AccessRequirement> argument = ArgumentCaptor.forClass(AccessRequirement.class);
		verify(accessRequirementDAO).create(argument.capture());

		// verify that all default fields are set
		ACTAccessRequirement ar = (ACTAccessRequirement) argument.getValue();
		assertFalse(ar.getIsCertifiedUserRequired());
		assertFalse(ar.getIsValidatedProfileRequired());
		assertFalse(ar.getIsDUCRequired());
		assertFalse(ar.getIsIRBApprovalRequired());
		assertFalse(ar.getAreOtherAttachmentsRequired());
		assertFalse(ar.getIsAnnualReviewRequired());
		assertFalse(ar.getIsIDUPublic());
	}

	@Test
	public void testUpdateACTAccessRequirement() {
		AccessRequirement toUpdate = createExpectedAR();
		toUpdate.setId(1L);
		when(authorizationManager.canAccess(userInfo, "1", ObjectType.ACCESS_REQUIREMENT, ACCESS_TYPE.UPDATE))
				.thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		arm.updateAccessRequirement(userInfo, "1", toUpdate);

		// test that the right AR was created
		ArgumentCaptor<AccessRequirement> argument = ArgumentCaptor.forClass(AccessRequirement.class);
		verify(accessRequirementDAO).update(argument.capture());

		// verify that all default fields are set
		ACTAccessRequirement ar = (ACTAccessRequirement) argument.getValue();
		assertFalse(ar.getIsCertifiedUserRequired());
		assertFalse(ar.getIsValidatedProfileRequired());
		assertFalse(ar.getIsDUCRequired());
		assertFalse(ar.getIsIRBApprovalRequired());
		assertFalse(ar.getAreOtherAttachmentsRequired());
		assertFalse(ar.getIsAnnualReviewRequired());
		assertFalse(ar.getIsIDUPublic());
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
}
