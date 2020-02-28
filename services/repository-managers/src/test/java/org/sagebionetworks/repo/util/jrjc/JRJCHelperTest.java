package org.sagebionetworks.repo.util.jrjc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class JRJCHelperTest {
	
	private static final String TEST_PRINCIPAL_ID = "1010101";
	private static final String TEST_DISPLAY_NAME = "Foo Bar";
	private static final String TEST_DATA_OBJECT_ID = "syn98786543";
	
	private JSONObject mockProject;
	private JSONObject mockProjectInfo;
	private Map<String,String> fields;
	JiraClient jiraClient;
	
	@Before
	public void setUp() throws Exception {
		mockProject = Mockito.mock(JSONObject.class);
		when(mockProject.get("key")).thenReturn("SG-101");

		mockProjectInfo = Mockito.mock(JSONObject.class);
		when(mockProjectInfo.get("id")).thenReturn("projectId");
		when(mockProjectInfo.get("issueTypeId")).thenReturn(10000L);

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
		ArgumentCaptor<JSONObject> issueCaptor = ArgumentCaptor.forClass(JSONObject.class);

		// Call under test
		JRJCHelper.createRestrictIssue(jiraClient, TEST_PRINCIPAL_ID, TEST_DISPLAY_NAME, TEST_DATA_OBJECT_ID);

		verify(jiraClient).createIssue(issueCaptor.capture());
		JSONObject issueInput = issueCaptor.getValue();

		assertTrue(issueInput.containsKey("fields"));
		JSONObject fields = (JSONObject) issueInput.get("fields");
		assertEquals("Request for ACT to add data restriction", fields.get("summary"));
		assertEquals(TEST_PRINCIPAL_ID, fields.get("id1"));
		assertEquals(TEST_DISPLAY_NAME, fields.get("id2"));
		assertEquals(TEST_DATA_OBJECT_ID, fields.get("id3"));
		JSONObject o = (JSONObject) fields.get("project");
		assertEquals("projectId", o.get("id"));
		o = (JSONObject) fields.get("issuetype");
		assertEquals(10000L, o.get("id"));

	}

	@Test
	public void testCreateFlagIssue() throws Exception {
		ArgumentCaptor<JSONObject> issueCaptor = ArgumentCaptor.forClass(JSONObject.class);

		// Call under test
		JRJCHelper.createFlagIssue(jiraClient, TEST_PRINCIPAL_ID, TEST_DISPLAY_NAME, TEST_DATA_OBJECT_ID);

		verify(jiraClient).createIssue(issueCaptor.capture());
		JSONObject issueInput = issueCaptor.getValue();

		assertTrue(issueInput.containsKey("fields"));
		JSONObject fields = (JSONObject) issueInput.get("fields");
		assertEquals("Request for ACT to review data", fields.get("summary"));
		assertEquals(TEST_PRINCIPAL_ID, fields.get("id1"));
		assertEquals(TEST_DISPLAY_NAME, fields.get("id2"));
		assertEquals(TEST_DATA_OBJECT_ID, fields.get("id3"));
		JSONObject o = (JSONObject) fields.get("project");
		assertEquals("projectId", o.get("id"));
		o = (JSONObject) fields.get("issuetype");
		assertEquals(10000L, o.get("id"));

	}

}
