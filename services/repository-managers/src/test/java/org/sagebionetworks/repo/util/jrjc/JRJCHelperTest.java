package org.sagebionetworks.repo.util.jrjc;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.atlassian.jira.rest.client.api.OptionalIterable;
import com.atlassian.jira.rest.client.api.domain.BasicIssue;
import com.atlassian.jira.rest.client.api.domain.Field;
import com.atlassian.jira.rest.client.api.domain.IssueType;
import com.atlassian.jira.rest.client.api.domain.Project;
import com.atlassian.jira.rest.client.api.domain.input.ComplexIssueInputFieldValue;
import com.atlassian.jira.rest.client.api.domain.input.FieldInput;
import com.atlassian.jira.rest.client.api.domain.input.IssueInput;

public class JRJCHelperTest {
	
	private static final String TEST_PRINCIPAL_ID = "1010101";
	private static final String TEST_DISPLAY_NAME = "Foo Bar";
	private static final String TEST_DATA_OBJECT_ID = "syn98786543";
	
	private Project sgProject;
	private Iterable<Field> fields;
	JiraClient jiraClient;
	
	@Before
	public void setUp() throws Exception {
		sgProject = Mockito.mock(Project.class);
		Iterable<IssueType> issueTypes = Arrays.asList(new IssueType[]{
				new IssueType(null, 1L, "Flag", false, null, null),
				new IssueType(null, 2L, "Access Restriction", false, null, null) 
		});
		when(sgProject.getIssueTypes()).thenReturn(new OptionalIterable<IssueType>(issueTypes));
		fields = Arrays.asList(new Field[]{
				new Field("101", "Synapse Principal ID", null, false, false, false, null),
				new Field("102", "Synapse User Display Name", null, false, false, false, null),
				new Field("103", "Synapse Data Object", null, false, false, false, null)
		});
		jiraClient = Mockito.mock(JiraClient.class);
		when(jiraClient.getFields()).thenReturn(fields);
		when(jiraClient.getProject(anyString())).thenReturn(sgProject);
		when(jiraClient.createIssue((IssueInput)anyObject())).thenReturn(new BasicIssue(null, "SG-101", 101L));
	}

	@Test
	public void testCreateRestrictIssue() throws Exception {		
		ArgumentCaptor<IssueInput> issueInputCaptor = ArgumentCaptor.forClass(IssueInput.class);
		JRJCHelper.createRestrictIssue(jiraClient, TEST_PRINCIPAL_ID, TEST_DISPLAY_NAME, TEST_DATA_OBJECT_ID);
		verify(jiraClient).createIssue(issueInputCaptor.capture());
		IssueInput issueInput = issueInputCaptor.getValue();
		Map<String,Object> projectValuesMap = new HashMap<String,Object>();
		projectValuesMap.put("key", "SG");
		Map<String,Object> issueTypeValuesMap = new HashMap<String,Object>();
		issueTypeValuesMap.put("id", "2");
		assertEquals(new FieldInput("summary", "Request for ACT to add data restriction"), issueInput.getField("summary"));
		assertEquals(new FieldInput("project", new ComplexIssueInputFieldValue(projectValuesMap)), issueInput.getField("project"));
		assertEquals(new FieldInput("issuetype", new ComplexIssueInputFieldValue(issueTypeValuesMap)), issueInput.getField("issuetype"));
		assertEquals(new FieldInput("101", TEST_PRINCIPAL_ID), issueInput.getField("101"));
		assertEquals(new FieldInput("102", TEST_DISPLAY_NAME), issueInput.getField("102"));
		assertEquals(new FieldInput("103", TEST_DATA_OBJECT_ID), issueInput.getField("103"));
	}
	
	@Test
	public void testCreateFlagIssue() throws Exception {		
		ArgumentCaptor<IssueInput> issueInputCaptor = ArgumentCaptor.forClass(IssueInput.class);
		JRJCHelper.createFlagIssue(jiraClient, TEST_PRINCIPAL_ID, TEST_DISPLAY_NAME, TEST_DATA_OBJECT_ID);
		verify(jiraClient).createIssue(issueInputCaptor.capture());
		IssueInput issueInput = issueInputCaptor.getValue();
		Map<String,Object> projectValuesMap = new HashMap<String,Object>();
		projectValuesMap.put("key", "SG");
		Map<String,Object> issueTypeValuesMap = new HashMap<String,Object>();
		issueTypeValuesMap.put("id", "1");
		assertEquals(new FieldInput("summary", "Request for ACT to review data"), issueInput.getField("summary"));
		assertEquals(new FieldInput("project", new ComplexIssueInputFieldValue(projectValuesMap)), issueInput.getField("project"));
		assertEquals(new FieldInput("issuetype", new ComplexIssueInputFieldValue(issueTypeValuesMap)), issueInput.getField("issuetype"));
		assertEquals(new FieldInput("101", TEST_PRINCIPAL_ID), issueInput.getField("101"));
		assertEquals(new FieldInput("102", TEST_DISPLAY_NAME), issueInput.getField("102"));
		assertEquals(new FieldInput("103", TEST_DATA_OBJECT_ID), issueInput.getField("103"));
	}

}
