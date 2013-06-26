package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
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
	private AuthorizationManager authorizationManager;
	private AccessRequirementManagerImpl arm;
	private UserInfo userInfo;

	
	@Before
	public void setUp() throws Exception {
		accessRequirementDAO = Mockito.mock(AccessRequirementDAO.class);
		when(accessRequirementDAO.create((AccessRequirement)any())).thenReturn(null);
		authorizationManager = Mockito.mock(AuthorizationManager.class);
		jiraClient = Mockito.mock(JiraClient.class);
		arm = new AccessRequirementManagerImpl(accessRequirementDAO, authorizationManager, jiraClient);
		userInfo = new UserInfo(false);
		UserGroup individualGroup = new UserGroup();
		individualGroup.setId(TEST_PRINCIPAL_ID);
		userInfo.setIndividualGroup(individualGroup);
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
		when(authorizationManager.canAccess(userInfo, TEST_ENTITY_ID, ACCESS_TYPE.CREATE)).thenReturn(true);
		when(authorizationManager.canAccess(userInfo, TEST_ENTITY_ID, ACCESS_TYPE.UPDATE)).thenReturn(true);
	}
	
	private AccessRequirement createExpectedAR() {
		ACTAccessRequirement expectedAR = new ACTAccessRequirement();
		expectedAR.setAccessType(ACCESS_TYPE.DOWNLOAD);
		expectedAR.setActContactInfo("Access restricted pending review by Synapse Access and Compliance Team.");
		RestrictableObjectDescriptor subjectId = new RestrictableObjectDescriptor();
		subjectId.setId(TEST_ENTITY_ID);
		subjectId.setType(RestrictableObjectType.ENTITY);
		expectedAR.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{subjectId}));
		expectedAR.setEntityType("org.sagebionetworks.repo.model.ACTAccessRequirement");;
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
		// can't just call equals on the objects, because the time stamps are slightly different
		assertEquals(expectedAR.getAccessType(), argument.getValue().getAccessType());
		assertEquals(expectedAR.getCreatedBy(), argument.getValue().getCreatedBy());
		assertEquals(expectedAR.getEntityType(), argument.getValue().getEntityType());
		assertEquals(expectedAR.getModifiedBy(), argument.getValue().getModifiedBy());
		assertEquals(expectedAR.getSubjectIds(), argument.getValue().getSubjectIds());

		// test that jira client was called to create issue
		// we don't test the *content* of the issue because that's tested in JRJCHelperTest
		verify(jiraClient).createIssue((IssueInput)anyObject());
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testCreateLockAccessRequirementNoAuthority() throws Exception {
		when(authorizationManager.canAccess(userInfo, TEST_ENTITY_ID, ACCESS_TYPE.CREATE)).thenReturn(false);
		when(authorizationManager.canAccess(userInfo, TEST_ENTITY_ID, ACCESS_TYPE.UPDATE)).thenReturn(false);
		// this should throw the unauthorized exception
		arm.createLockAccessRequirement(userInfo, TEST_ENTITY_ID);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testCreateLockAccessRequirementAlreadyExists() throws Exception {
		RestrictableObjectDescriptor subjectId = new RestrictableObjectDescriptor();
		subjectId.setId(TEST_ENTITY_ID);
		subjectId.setType(RestrictableObjectType.ENTITY);
		List<AccessRequirement> ars = Arrays.asList(new AccessRequirement[]{createExpectedAR()});
		when(accessRequirementDAO.getForSubject(subjectId)).thenReturn(ars);
		// this should throw the illegal argument exception
		arm.createLockAccessRequirement(userInfo, TEST_ENTITY_ID);
		
	}
}
