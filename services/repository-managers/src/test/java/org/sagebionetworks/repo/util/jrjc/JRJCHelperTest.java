package org.sagebionetworks.repo.util.jrjc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class JRJCHelperTest {
	
	private static final String TEST_PRINCIPAL_ID = "1010101";
	private static final String TEST_DISPLAY_NAME = "Foo Bar";
	private static final String TEST_DATA_OBJECT_ID = "syn98786543";
	
	@Mock
	private CreatedIssue mockCreatedIssue;
	@Mock
	private ProjectInfo mockProjectInfo;
	@Mock
	private JiraClient jiraClient;

	private Map<String, String> fields;

	@BeforeEach
	public void setUp() throws Exception {
		when(mockProjectInfo.getProjectId()).thenReturn("projectId");
		when(mockProjectInfo.getIssueTypeId()).thenReturn(10000L);

		when(jiraClient.getProjectInfo(anyString(), anyString())).thenReturn(mockProjectInfo);
	}

	@Test
	public void testCreateRestrictIssue() throws Exception {
		initFields();
		ArgumentCaptor<BasicIssue> issueCaptor = ArgumentCaptor.forClass(BasicIssue.class);

		// Call under test
		JRJCHelper.createRestrictIssue(jiraClient, TEST_PRINCIPAL_ID, TEST_DISPLAY_NAME, TEST_DATA_OBJECT_ID);

		verify(jiraClient).createIssue(issueCaptor.capture());
		BasicIssue issueInput = issueCaptor.getValue();

		assertNotNull(issueInput.getCustomFields());
		Map<String,Object> customFields = issueInput.getCustomFields();
		assertEquals("Request for ACT to add data restriction", issueInput.getSummary());
		assertEquals(TEST_PRINCIPAL_ID, customFields.get("id1"));
		assertEquals(TEST_DISPLAY_NAME, customFields.get("id2"));
		assertEquals(TEST_DATA_OBJECT_ID, customFields.get("id3"));
		List<Map<String,Object>> components = (List<Map<String,Object>>)customFields.get("components");
		Map<String,Object> expectedComponent = new HashMap<String,Object>();
		expectedComponent.put("id", "14865");
		List<Map<String,Object>> expectedComponents = Collections.singletonList(expectedComponent);
		assertEquals(expectedComponents, components);
		assertEquals("projectId", issueInput.getProjectId());
		assertEquals(Long.valueOf((10000L)), issueInput.getIssueTypeId());

	}

	@Test
	public void testCreateFlagIssue() throws Exception {
		initFields();
		ArgumentCaptor<BasicIssue> issueCaptor = ArgumentCaptor.forClass(BasicIssue.class);

		// Call under test
		JRJCHelper.createFlagIssue(jiraClient, TEST_PRINCIPAL_ID, TEST_DISPLAY_NAME, TEST_DATA_OBJECT_ID);

		verify(jiraClient).createIssue(issueCaptor.capture());
		BasicIssue issueInput = issueCaptor.getValue();

		assertNotNull(issueInput.getCustomFields());
		Map<String,Object> fields = issueInput.getCustomFields();
		assertEquals("Request for ACT to review data", issueInput.getSummary());
		assertEquals(TEST_PRINCIPAL_ID, fields.get("id1"));
		assertEquals(TEST_DISPLAY_NAME, fields.get("id2"));
		assertEquals(TEST_DATA_OBJECT_ID, fields.get("id3"));
		assertEquals("projectId", issueInput.getProjectId());
		assertEquals(Long.valueOf(10000L), issueInput.getIssueTypeId());

	}

	@Test
	public void testCreateIssueBadMapping() throws Exception {
		Map<String,String> badFields = new HashMap<String, String>();
		badFields.put("Synapse Principal ID", "id1");
		badFields.put("Synapse User Name", "id2"); // This one does not map
		badFields.put("Synapse Data Object", "id3");
		when(jiraClient.getFields()).thenReturn(badFields);

		// Call under test
		Assertions.assertThrows(JiraClientException.class, () -> {
				JRJCHelper.createFlagIssue(jiraClient, TEST_PRINCIPAL_ID, TEST_DISPLAY_NAME, TEST_DATA_OBJECT_ID);
			}
		);

	}

	private void initFields() {
		when(mockCreatedIssue.getKey()).thenReturn("SG-101");

		when(jiraClient.createIssue(anyObject())).thenReturn(mockCreatedIssue);

		fields = new HashMap<String, String>();
		fields = new HashMap<String, String>();
		fields.put("Synapse Principal ID", "id1");
		fields.put("Synapse User Display Name", "id2");
		fields.put("Synapse Data Object", "id3");
		fields.put("Components", "components");
		when(jiraClient.getFields()).thenReturn(fields);
	}

}
