package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.DOWNLOAD;
import static org.sagebionetworks.repo.model.ACCESS_TYPE.UPLOAD;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.NotificationEmailDAO;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.util.jrjc.JiraClient;

import com.atlassian.jira.rest.client.api.OptionalIterable;
import com.atlassian.jira.rest.client.api.domain.BasicIssue;
import com.atlassian.jira.rest.client.api.domain.Field;
import com.atlassian.jira.rest.client.api.domain.IssueType;
import com.atlassian.jira.rest.client.api.domain.Project;
import com.atlassian.jira.rest.client.api.domain.input.IssueInput;


public class AccessRequirementManagerImplUnitTest {

	private JiraClient jiraClient;
	private static final String TEST_PRINCIPAL_ID = "1010101";
	private static final String TEST_ENTITY_ID = "syn98786543";
	
	private AccessRequirementDAO accessRequirementDAO;
	private NodeDAO nodeDao;
	private AuthorizationManager authorizationManager;
	private AccessRequirementManagerImpl arm;
	private UserInfo userInfo;
	private NotificationEmailDAO notificationEmailDao;

	
	@Before
	public void setUp() throws Exception {
		accessRequirementDAO = Mockito.mock(AccessRequirementDAO.class);
		nodeDao = Mockito.mock(NodeDAO.class);
		authorizationManager = Mockito.mock(AuthorizationManager.class);
		notificationEmailDao = Mockito.mock(NotificationEmailDAO.class);
		PrincipalAlias alias = new PrincipalAlias();
		alias.setAlias("foo@bar.com");
		when(notificationEmailDao.getNotificationEmailForPrincipal(anyLong())).thenReturn(alias.getAlias());
		jiraClient = Mockito.mock(JiraClient.class);
		arm = new AccessRequirementManagerImpl(accessRequirementDAO, nodeDao, authorizationManager, jiraClient, notificationEmailDao);
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
	}
	
	private AccessRequirement createExpectedAR() {
		ACTAccessRequirement expectedAR = new ACTAccessRequirement();
		expectedAR.setAccessType(ACCESS_TYPE.DOWNLOAD);
		expectedAR.setActContactInfo("Access restricted pending review by Synapse Access and Compliance Team.");
		RestrictableObjectDescriptor subjectId = new RestrictableObjectDescriptor();
		subjectId.setId(TEST_ENTITY_ID);
		subjectId.setType(RestrictableObjectType.ENTITY);
		expectedAR.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{subjectId}));
		expectedAR.setConcreteType("org.sagebionetworks.repo.model.ACTAccessRequirement");;
		AccessRequirementManagerImpl.populateCreationFields(userInfo, expectedAR);
		return 	expectedAR;
	}

	@Test
	public void testCreateLockAccessRequirementHappyPath() throws Exception {
		arm.createLockAccessRequirement(userInfo, TEST_ENTITY_ID);
		
		// test that the right AR was created
		AccessRequirement expectedAR = createExpectedAR();
		ArgumentCaptor<AccessRequirement> argument = ArgumentCaptor.forClass(AccessRequirement.class);
		verify(accessRequirementDAO).create(argument.capture());
		verify(notificationEmailDao).getNotificationEmailForPrincipal(userInfo.getId());
		// can't just call equals on the objects, because the time stamps are slightly different
		assertEquals(expectedAR.getAccessType(), argument.getValue().getAccessType());
		assertEquals(expectedAR.getCreatedBy(), argument.getValue().getCreatedBy());
		assertEquals(expectedAR.getConcreteType(), argument.getValue().getConcreteType());
		assertEquals(expectedAR.getModifiedBy(), argument.getValue().getModifiedBy());
		assertEquals(expectedAR.getSubjectIds(), argument.getValue().getSubjectIds());

		// test that jira client was called to create issue
		// we don't test the *content* of the issue because that's tested in JRJCHelperTest
		verify(jiraClient).createIssue((IssueInput)anyObject());
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testCreateLockAccessRequirementNoAuthority() throws Exception {
		when(authorizationManager.canAccess(userInfo, TEST_ENTITY_ID, ObjectType.ENTITY, ACCESS_TYPE.CREATE)).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		when(authorizationManager.canAccess(userInfo, TEST_ENTITY_ID, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		// this should throw the unauthorized exception
		arm.createLockAccessRequirement(userInfo, TEST_ENTITY_ID);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testCreateLockAccessRequirementAlreadyExists() throws Exception {
		RestrictableObjectDescriptor subjectId = new RestrictableObjectDescriptor();
		subjectId.setId(TEST_ENTITY_ID);
		subjectId.setType(RestrictableObjectType.ENTITY);
		List<AccessRequirement> ars = Arrays.asList(new AccessRequirement[]{createExpectedAR()});
		when(accessRequirementDAO.getForSubject(any(List.class), eq(RestrictableObjectType.ENTITY))).thenReturn(ars);
		// this should throw the illegal argument exception
		arm.createLockAccessRequirement(userInfo, TEST_ENTITY_ID);
		
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
		mockNode.setNodeType(EntityType.getNodeTypeForClass(FileEntity.class).name());
		when(nodeDao.getNode(TEST_ENTITY_ID)).thenReturn(mockNode);
		when(accessRequirementDAO.unmetAccessRequirements(
				Collections.singletonList(TEST_ENTITY_ID), 
				RestrictableObjectType.ENTITY, 
				Collections.singleton(userInfo.getId()), 
				Collections.singletonList(DOWNLOAD))).
				thenReturn(Collections.singletonList(mockDownloadARId));
		when(accessRequirementDAO.unmetAccessRequirements(
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
		when(accessRequirementDAO.getForSubject(Collections.singletonList(TEST_ENTITY_ID), RestrictableObjectType.ENTITY)).
			thenReturn(arList);
		List<AccessRequirement> result = arm.getUnmetAccessRequirements(userInfo, subjectId).getResults();
		assertEquals(arList, result);
	}
}
