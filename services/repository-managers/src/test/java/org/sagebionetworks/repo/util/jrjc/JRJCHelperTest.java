package org.sagebionetworks.repo.util.jrjc;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class JRJCHelperTest {
	
	private static final String TEST_PRINCIPAL_ID = "1010101";
	private static final String TEST_DISPLAY_NAME = "Foo Bar";
	private static final String TEST_DATA_OBJECT_ID = "syn98786543";
	
	private CreatedIssue mockProject;
	private ProjectInfo mockProjectInfo;
	private Map<String,String> fields;
	JiraClient jiraClient;
	
	@Before
	public void setUp() throws Exception {
		mockProject = Mockito.mock(CreatedIssue.class);
		when(mockProject.getKey()).thenReturn("SG-101");

		mockProjectInfo = Mockito.mock(ProjectInfo.class);
		when(mockProjectInfo.getProjectId()).thenReturn("projectId");
		when(mockProjectInfo.getIssueTypeId()).thenReturn(10000L);

		fields = new HashMap<String, String>();
		fields.put("Synapse Principal ID", "id1");
		fields.put("Synapse User Display Name", "id2");
		fields.put("Synapse Data Object", "id3");

		jiraClient = Mockito.mock(JiraClient.class);
		when(jiraClient.getFields()).thenReturn(fields);
		when(jiraClient.getProjectInfo(anyString(), anyString())).thenReturn(mockProjectInfo);
		when(jiraClient.createIssue(anyObject())).thenReturn(mockProject);

	}

	@Test
	public void testCreateRestrictIssue() throws Exception {
		ArgumentCaptor<BasicIssue> issueCaptor = ArgumentCaptor.forClass(BasicIssue.class);

		// Call under test
		JRJCHelper.createRestrictIssue(jiraClient, TEST_PRINCIPAL_ID, TEST_DISPLAY_NAME, TEST_DATA_OBJECT_ID);

		verify(jiraClient).createIssue(issueCaptor.capture());
		BasicIssue issueInput = issueCaptor.getValue();

		assertNotNull(issueInput.getCustomFields());
		Map<String,String> customFields = issueInput.getCustomFields();
		assertEquals("Request for ACT to add data restriction", issueInput.getSummary());
		assertEquals(TEST_PRINCIPAL_ID, customFields.get("id1"));
		assertEquals(TEST_DISPLAY_NAME, customFields.get("id2"));
		assertEquals(TEST_DATA_OBJECT_ID, customFields.get("id3"));
		assertEquals("projectId", issueInput.getProjectId());
		assertEquals(Long.valueOf((10000L)), issueInput.getIssueTypeId());

	}

	@Test
	public void testCreateFlagIssue() throws Exception {
		ArgumentCaptor<BasicIssue> issueCaptor = ArgumentCaptor.forClass(BasicIssue.class);

		// Call under test
		JRJCHelper.createFlagIssue(jiraClient, TEST_PRINCIPAL_ID, TEST_DISPLAY_NAME, TEST_DATA_OBJECT_ID);

		verify(jiraClient).createIssue(issueCaptor.capture());
		BasicIssue issueInput = issueCaptor.getValue();

		assertNotNull(issueInput.getCustomFields());
		Map<String,String> fields = issueInput.getCustomFields();
		assertEquals("Request for ACT to review data", issueInput.getSummary());
		assertEquals(TEST_PRINCIPAL_ID, fields.get("id1"));
		assertEquals(TEST_DISPLAY_NAME, fields.get("id2"));
		assertEquals(TEST_DATA_OBJECT_ID, fields.get("id3"));
		assertEquals("projectId", issueInput.getProjectId());
		assertEquals(Long.valueOf(10000L), issueInput.getIssueTypeId());

	}

}
