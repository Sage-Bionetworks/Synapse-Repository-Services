package org.sagebionetworks.repo.manager.agent.supervisor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.agent.Agent;
import org.sagebionetworks.repo.manager.agent.AgentToolContextKey;
import org.sagebionetworks.repo.manager.agent.specialist.entitymetadata.EntityMetadataSpecialistFactory;
import org.sagebionetworks.repo.manager.agent.specialist.filesummary.FileSummarySpecialistFactory;
import org.sagebionetworks.repo.manager.agent.specialist.gridmetadata.GridMetadataSpecialistFactory;
import org.sagebionetworks.repo.manager.agent.specialist.gridquery.GridQuerySpecialistFactory;
import org.sagebionetworks.repo.manager.agent.specialist.gridupdate.GridUpdateSpecialistFactory;
import org.sagebionetworks.repo.manager.agent.specialist.jsonschema.JsonSchemaSpecialistFactory;
import org.sagebionetworks.repo.manager.agent.specialist.tablequery.TableQuerySpecialistFactory;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.agent.GridAgentSessionContext;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;

@ExtendWith(MockitoExtension.class)
public class SupervisorToolsTest {

	@Mock
	private TableQuerySpecialistFactory tableQuerySpecialistFactory;
	@Mock
	private JsonSchemaSpecialistFactory jsonSchemaSpecialistFactory;
	@Mock
	private FileSummarySpecialistFactory fileSummarySpecialistFactory;
	@Mock
	private EntityMetadataSpecialistFactory entityMetadataSpecialistFactory;
	@Mock
	private GridQuerySpecialistFactory gridQuerySpecialistFactory;
	@Mock
	private GridUpdateSpecialistFactory gridUpdateSpecialistFactory;
	@Mock
	private GridMetadataSpecialistFactory gridMetadataSpecialistFactory;

	@Mock
	private Agent tableQuerySpecialist;
	@Mock
	private Agent jsonSchemaSpecialist;
	@Mock
	private Agent fileSummarySpecialist;
	@Mock
	private Agent entityMetadataSpecialist;
	@Mock
	private Agent gridQuerySpecialist;
	@Mock
	private Agent gridUpdateSpecialist;
	@Mock
	private Agent gridMetadataSpecialist;

	private SupervisorTools tools;
	private UserInfo userInfo;
	private GridAgentSessionContext gridContext;
	private ToolContext toolContext;

	@BeforeEach
	public void setup() {
		tools = new SupervisorTools(tableQuerySpecialistFactory, jsonSchemaSpecialistFactory, fileSummarySpecialistFactory,
				entityMetadataSpecialistFactory, gridQuerySpecialistFactory, gridUpdateSpecialistFactory,
				gridMetadataSpecialistFactory);
		userInfo = new UserInfo(false, 101L, AuthorizationConstants.DEFAULT_REALM_ID);
		gridContext = new GridAgentSessionContext().setGridSessionId("grid-1").setUsersReplicaId(1L)
				.setAgentsReplicaId(2L);
		toolContext = new ToolContext(Map.of(AgentToolContextKey.USER_INFO.getKey(), userInfo,
				AgentToolContextKey.CODE_SESSION_ID.getKey(), "session-123",
				AgentToolContextKey.GRID_SESSION_CONTEXT.getKey(), gridContext));
	}

	private ToolCallback callback(String name) {
		return tools.getToolCallbacks().stream()
				.filter(c -> name.equals(c.getToolDefinition().name())).findFirst().orElseThrow();
	}

	@Test
	public void testToolCallbackNamesAndSchemas() {
		Set<String> names = tools.getToolCallbacks().stream().map(c -> c.getToolDefinition().name())
				.collect(Collectors.toSet());

		assertEquals(Set.of(SupervisorTools.TOOL_TABLE_QUERY, SupervisorTools.TOOL_JSON_SCHEMA,
				SupervisorTools.TOOL_FILE_SUMMARY, SupervisorTools.TOOL_ENTITY_METADATA, SupervisorTools.TOOL_GRID_QUERY,
				SupervisorTools.TOOL_GRID_UPDATE, SupervisorTools.TOOL_GRID_METADATA), names);

		// The required message scalar becomes a typed, required top-level property.
		JSONObject schema = new JSONObject(callback(SupervisorTools.TOOL_TABLE_QUERY).getToolDefinition().inputSchema());
		assertEquals("string", schema.getJSONObject("properties").getJSONObject("message").getString("type"));
		assertTrue(schema.getJSONArray("required").toList().contains("message"));
	}

	@Test
	public void testAskTableQuerySpecialistThroughCallback() {
		when(tableQuerySpecialistFactory.create()).thenReturn(tableQuerySpecialist);
		when(tableQuerySpecialist.chat("describe syn1", toolContext)).thenReturn("table described");

		// call under test — the supervisor supplies message as a named JSON property.
		String result = callback(SupervisorTools.TOOL_TABLE_QUERY).call("{\"message\": \"describe syn1\"}", toolContext);

		assertEquals("table described", result);
		verify(tableQuerySpecialist).chat("describe syn1", toolContext);
	}

	@Test
	public void testAskTableQuerySpecialistThroughCallbackMissingRequired() {
		// call under test — a missing required scalar is fed back as corrective guidance, not thrown.
		String result = callback(SupervisorTools.TOOL_TABLE_QUERY).call("{}", toolContext);

		assertTrue(result.contains("missing required argument 'message'"), result);
		verifyNoInteractions(tableQuerySpecialistFactory);
	}

	@Test
	public void testAskTableQuerySpecialist() {
		when(tableQuerySpecialistFactory.create()).thenReturn(tableQuerySpecialist);
		when(tableQuerySpecialist.chat("describe syn1", toolContext)).thenReturn("table described");

		// call under test
		String result = tools.askTableQuerySpecialist("describe syn1", toolContext);

		assertEquals("table described", result);
		// A fresh specialist is created and handed the supervisor's tool context unchanged.
		verify(tableQuerySpecialistFactory).create();
		verify(tableQuerySpecialist).chat("describe syn1", toolContext);
	}

	@Test
	public void testAskJsonSchemaSpecialist() {
		when(jsonSchemaSpecialistFactory.create()).thenReturn(jsonSchemaSpecialist);
		when(jsonSchemaSpecialist.chat("describe my.org-S", toolContext)).thenReturn("schema described");

		// call under test
		String result = tools.askJsonSchemaSpecialist("describe my.org-S", toolContext);

		assertEquals("schema described", result);
		verify(jsonSchemaSpecialistFactory).create();
		verify(jsonSchemaSpecialist).chat("describe my.org-S", toolContext);
	}

	@Test
	public void testAskFileSummarySpecialist() {
		when(fileSummarySpecialistFactory.create()).thenReturn(fileSummarySpecialist);
		when(fileSummarySpecialist.chat("summarize out.csv", toolContext)).thenReturn("file summarized");

		// call under test
		String result = tools.askFileSummarySpecialist("summarize out.csv", toolContext);

		assertEquals("file summarized", result);
		verify(fileSummarySpecialistFactory).create();
		verify(fileSummarySpecialist).chat("summarize out.csv", toolContext);
	}

	@Test
	public void testAskEntityMetadataSpecialist() {
		when(entityMetadataSpecialistFactory.create()).thenReturn(entityMetadataSpecialist);
		when(entityMetadataSpecialist.chat("annotations of syn1", toolContext)).thenReturn("annotations described");

		// call under test
		String result = tools.askEntityMetadataSpecialist("annotations of syn1", toolContext);

		assertEquals("annotations described", result);
		verify(entityMetadataSpecialistFactory).create();
		verify(entityMetadataSpecialist).chat("annotations of syn1", toolContext);
	}

	@Test
	public void testAskGridQuerySpecialist() {
		when(gridQuerySpecialistFactory.create()).thenReturn(gridQuerySpecialist);
		when(gridQuerySpecialist.chat("count the rows", toolContext)).thenReturn("grid queried");

		// call under test
		String result = tools.askGridQuerySpecialist("count the rows", toolContext);

		assertEquals("grid queried", result);
		// A fresh specialist is created and handed the supervisor's tool context unchanged.
		verify(gridQuerySpecialistFactory).create();
		verify(gridQuerySpecialist).chat("count the rows", toolContext);
	}

	@Test
	public void testAskGridUpdateSpecialist() {
		when(gridUpdateSpecialistFactory.create()).thenReturn(gridUpdateSpecialist);
		when(gridUpdateSpecialist.chat("set age to 25", toolContext)).thenReturn("grid updated");

		// call under test
		String result = tools.askGridUpdateSpecialist("set age to 25", toolContext);

		assertEquals("grid updated", result);
		verify(gridUpdateSpecialistFactory).create();
		verify(gridUpdateSpecialist).chat("set age to 25", toolContext);
	}

	@Test
	public void testAskGridMetadataSpecialist() {
		when(gridMetadataSpecialistFactory.create()).thenReturn(gridMetadataSpecialist);
		when(gridMetadataSpecialist.chat("who changed row 5", toolContext)).thenReturn("replica described");

		// call under test
		String result = tools.askGridMetadataSpecialist("who changed row 5", toolContext);

		assertEquals("replica described", result);
		// A fresh specialist is created and handed the supervisor's tool context unchanged.
		verify(gridMetadataSpecialistFactory).create();
		verify(gridMetadataSpecialist).chat("who changed row 5", toolContext);
	}
}
