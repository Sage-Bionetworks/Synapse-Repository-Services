package org.sagebionetworks.repo.manager.agent.specialist.gridupdate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.agent.AgentToolContextKey;
import org.sagebionetworks.repo.manager.agent.specialist.ToolResponse;
import org.sagebionetworks.repo.manager.grid.GridManager;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.GridReplicaViewManager;
import org.sagebionetworks.repo.model.agent.GridAgentSessionContext;
import org.sagebionetworks.repo.model.grid.EventSource;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.query.CellValueFilter;
import org.sagebionetworks.repo.model.grid.query.CellValueOperator;
import org.sagebionetworks.repo.model.grid.query.RowSelectionFilter;
import org.sagebionetworks.repo.model.grid.query.result.Row;
import org.sagebionetworks.repo.model.grid.update.GridUpdatePreviewResponse;
import org.sagebionetworks.repo.model.grid.update.GridUpdateRequest;
import org.sagebionetworks.repo.model.grid.update.GridUpdateResponse;
import org.sagebionetworks.repo.model.grid.update.LiteralSetValue;
import org.sagebionetworks.repo.model.grid.update.Update;
import org.sagebionetworks.repo.model.grid.update.UpdateBatch;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;

@ExtendWith(MockitoExtension.class)
public class GridUpdateToolsTest {

	@Mock
	private GridManager mockGridManager;

	@Mock
	private GridReplicaViewManager mockGridViewManager;

	@InjectMocks
	private GridUpdateTools gridUpdateTools;

	private GridAgentSessionContext gridContext;
	private ToolContext toolContext;

	private static final String GRID_SESSION_ID = "grid-123";
	private static final Long USERS_REPLICA_ID = 101L;
	private static final Long AGENTS_REPLICA_ID = 202L;
	private static final Long INTERNAL_REPLICA_ID = 1L;

	@BeforeEach
	public void before() {
		gridContext = new GridAgentSessionContext().setGridSessionId(GRID_SESSION_ID)
				.setUsersReplicaId(USERS_REPLICA_ID).setAgentsReplicaId(AGENTS_REPLICA_ID);
		toolContext = new ToolContext(Map.of(AgentToolContextKey.GRID_SESSION_CONTEXT.getKey(), gridContext));
	}

	/**
	 * The tool is exercised through its {@link ToolCallback} — the same path Spring AI drives. The
	 * update parameter is a raw {@code JSONObject}, so the base class parses and validates the payload
	 * (returning corrective feedback on malformed JSON) while preserving the undefined-vs-null
	 * distinction the tool relies on — an omitted key stays absent, unlike a round-tripped POJO.
	 */
	private ToolCallback updateGridCallback() {
		return gridUpdateTools.getToolCallbacks().stream()
				.filter(callback -> "updateGrid".equals(callback.getToolDefinition().name())).findFirst().orElseThrow();
	}

	@Test
	public void testUpdateGrid() throws Exception {
		GridConnectionInfo internalConnection = new GridConnectionInfo().setReplicaId(INTERNAL_REPLICA_ID);
		GridConnectionInfo agentConnection = new GridConnectionInfo().setReplicaId(AGENTS_REPLICA_ID);
		GridHeader header = new GridHeader();

		when(mockGridManager.getSingletonConnection(GRID_SESSION_ID, EventSource.INTERNAL))
				.thenReturn(Optional.of(internalConnection));
		when(mockGridViewManager.readHeader(GRID_SESSION_ID, INTERNAL_REPLICA_ID, USERS_REPLICA_ID))
				.thenReturn(Optional.of(header));
		when(mockGridManager.getConnection(GRID_SESSION_ID, AGENTS_REPLICA_ID))
				.thenReturn(Optional.of(agentConnection));
		when(mockGridManager.executeGridUpdate(eq(header), eq(agentConnection), any(JSONObject.class)))
				.thenReturn(2L).thenReturn(3L);

		GridUpdateRequest request = new GridUpdateRequest().setUpdate(new UpdateBatch().setBatch(List.of(
				new Update().setSet(List.of(new LiteralSetValue().setColumnName("a").setValue(true)))
						.setFilters(List.of(new RowSelectionFilter().setIsSelected(true))),
				new Update().setSet(List.of(new LiteralSetValue().setColumnName("b").setValue(1)))
						.setFilters(List.of(new CellValueFilter().setColumnName("b")
								.setOperator(CellValueOperator.IS_NULL))))));
		String update = JDOSecondaryPropertyUtils.createJSONFromObject(request);

		// call under test
		String response = updateGridCallback().call(update, toolContext);

		GridUpdateResponse expected = new GridUpdateResponse().setUpdateResults(List.of(2L, 3L))
				.setTotalRowsUpdated(5L);
		assertEquals(JDOSecondaryPropertyUtils.createJSONFromObject(new ToolResponse<>(expected)), response);

		ArgumentCaptor<JSONObject> jsonCaptor = ArgumentCaptor.forClass(JSONObject.class);
		verify(mockGridManager, times(2)).executeGridUpdate(eq(header), eq(agentConnection), jsonCaptor.capture());
		List<JSONObject> captured = jsonCaptor.getAllValues();
		assertEquals(request.getUpdate().getBatch().get(0),
				JDOSecondaryPropertyUtils.createObjectFromJSON(Update.class, captured.get(0).toString()));
		assertEquals(request.getUpdate().getBatch().get(1),
				JDOSecondaryPropertyUtils.createObjectFromJSON(Update.class, captured.get(1).toString()));
	}

	@Test
	public void testUpdateGridPreservesOmittedValue() throws Exception {
		GridConnectionInfo internalConnection = new GridConnectionInfo().setReplicaId(INTERNAL_REPLICA_ID);
		GridConnectionInfo agentConnection = new GridConnectionInfo().setReplicaId(AGENTS_REPLICA_ID);
		GridHeader header = new GridHeader();

		when(mockGridManager.getSingletonConnection(GRID_SESSION_ID, EventSource.INTERNAL))
				.thenReturn(Optional.of(internalConnection));
		when(mockGridViewManager.readHeader(GRID_SESSION_ID, INTERNAL_REPLICA_ID, USERS_REPLICA_ID))
				.thenReturn(Optional.of(header));
		when(mockGridManager.getConnection(GRID_SESSION_ID, AGENTS_REPLICA_ID))
				.thenReturn(Optional.of(agentConnection));
		when(mockGridManager.executeGridUpdate(eq(header), eq(agentConnection), any(JSONObject.class)))
				.thenReturn(1L);

		// A LiteralSetValue with an omitted 'value' property means "set to undefined". Because the tool
		// receives the raw payload rather than a round-tripped POJO, the 'value' key must reach
		// executeGridUpdate still absent — distinct from an explicit "value": null.
		String update = "{\"update\":{\"batch\":[{\"set\":[{\"concreteType\":"
				+ "\"org.sagebionetworks.repo.model.grid.update.LiteralSetValue\",\"columnName\":\"color\"}],"
				+ "\"filters\":[{\"concreteType\":\"org.sagebionetworks.repo.model.grid.query.RowSelectionFilter\","
				+ "\"isSelected\":true}]}]}}";

		// call under test
		String response = updateGridCallback().call(update, toolContext);

		assertTrue(new JSONObject(response).has("responseBody"));

		ArgumentCaptor<JSONObject> jsonCaptor = ArgumentCaptor.forClass(JSONObject.class);
		verify(mockGridManager).executeGridUpdate(eq(header), eq(agentConnection), jsonCaptor.capture());
		JSONObject setEntry = jsonCaptor.getValue().getJSONArray("set").getJSONObject(0);
		assertEquals("color", setEntry.getString("columnName"));
		assertFalse(setEntry.has("value"), "The omitted value must remain absent in the payload sent to the manager");
	}

	@Test
	public void testUpdateGridWithMalformedJson() {
		// The JSONObject parameter cannot be parsed, so the base class returns its corrective-feedback
		// string (fed back to the model to retry) before the tool body runs — the managers are untouched.
		String malformed = "{ this is not valid json";

		// call under test
		String response = updateGridCallback().call(malformed, toolContext);

		assertTrue(response.contains("was not valid JSON for its input schema"));
		assertTrue(response.contains("Resubmit the call with a corrected argument."));
		verifyNoInteractions(mockGridManager, mockGridViewManager);
	}

	@Test
	public void testUpdateGridWithNoContext() {
		ToolContext emptyContext = new ToolContext(Map.of());

		String update = "{\"update\":{\"batch\":[]}}";

		// call under test
		String response = updateGridCallback().call(update, emptyContext);

		assertEquals("No grid session context available", new JSONObject(response).getString("errorMessage"));
		verifyNoInteractions(mockGridManager, mockGridViewManager);
	}

	@Test
	public void testUpdateGridWithNoInternalConnection() throws Exception {
		when(mockGridManager.getSingletonConnection(GRID_SESSION_ID, EventSource.INTERNAL)).thenReturn(Optional.empty());

		String update = "{\"update\":{\"batch\":[{\"set\":[],\"filters\":[]}]}}";

		// call under test
		String response = updateGridCallback().call(update, toolContext);

		assertTrue(new JSONObject(response).getString("errorMessage").contains("Cannot get an internal grid connection."));
		verify(mockGridViewManager, never()).readHeader(any(), any(), any());
		verify(mockGridManager, never()).executeGridUpdate(any(), any(), any());
	}

	@Test
	public void testUpdateGridWithNoAgentConnection() throws Exception {
		GridConnectionInfo internalConnection = new GridConnectionInfo().setReplicaId(INTERNAL_REPLICA_ID);
		GridHeader header = new GridHeader();

		when(mockGridManager.getSingletonConnection(GRID_SESSION_ID, EventSource.INTERNAL))
				.thenReturn(Optional.of(internalConnection));
		when(mockGridViewManager.readHeader(GRID_SESSION_ID, INTERNAL_REPLICA_ID, USERS_REPLICA_ID))
				.thenReturn(Optional.of(header));
		when(mockGridManager.getConnection(GRID_SESSION_ID, AGENTS_REPLICA_ID)).thenReturn(Optional.empty());

		String update = "{\"update\":{\"batch\":[{\"set\":[],\"filters\":[]}]}}";

		// call under test
		String response = updateGridCallback().call(update, toolContext);

		assertTrue(new JSONObject(response).getString("errorMessage").contains("Cannot get an agent grid connection."));
		verify(mockGridManager, never()).executeGridUpdate(any(), any(), any());
	}

	private ToolCallback previewGridUpdateCallback() {
		return gridUpdateTools.getToolCallbacks().stream()
				.filter(callback -> "previewGridUpdate".equals(callback.getToolDefinition().name())).findFirst()
				.orElseThrow();
	}

	@Test
	public void testPreviewGridUpdate() throws Exception {
		GridConnectionInfo internalConnection = new GridConnectionInfo().setReplicaId(INTERNAL_REPLICA_ID);
		GridConnectionInfo agentConnection = new GridConnectionInfo().setReplicaId(AGENTS_REPLICA_ID);
		GridHeader header = new GridHeader();

		Row row1 = new Row().setRowId("1.1").setData(new JSONObject().put("a", true));
		Row row2 = new Row().setRowId("1.2").setData(new JSONObject().put("b", 1));

		when(mockGridManager.getSingletonConnection(GRID_SESSION_ID, EventSource.INTERNAL))
				.thenReturn(Optional.of(internalConnection));
		when(mockGridViewManager.readHeader(GRID_SESSION_ID, INTERNAL_REPLICA_ID, USERS_REPLICA_ID))
				.thenReturn(Optional.of(header));
		when(mockGridManager.getConnection(GRID_SESSION_ID, AGENTS_REPLICA_ID))
				.thenReturn(Optional.of(agentConnection));
		when(mockGridManager.executeGridUpdatePreview(eq(header), eq(agentConnection), any(JSONObject.class)))
				.thenReturn(List.of(row1)).thenReturn(List.of(row2));

		GridUpdateRequest request = new GridUpdateRequest().setUpdate(new UpdateBatch().setBatch(List.of(
				new Update().setSet(List.of(new LiteralSetValue().setColumnName("a").setValue(true)))
						.setFilters(List.of(new RowSelectionFilter().setIsSelected(true))),
				new Update().setSet(List.of(new LiteralSetValue().setColumnName("b").setValue(1)))
						.setFilters(List.of(new CellValueFilter().setColumnName("b")
								.setOperator(CellValueOperator.IS_NULL))))));
		String update = JDOSecondaryPropertyUtils.createJSONFromObject(request);

		// call under test
		String response = previewGridUpdateCallback().call(update, toolContext);

		// The preview rows from every update in the batch are flattened into the response.
		GridUpdatePreviewResponse expected = new GridUpdatePreviewResponse().setPreviewRows(List.of(row1, row2));
		assertEquals(JDOSecondaryPropertyUtils.createJSONFromObject(new ToolResponse<>(expected)), response);

		ArgumentCaptor<JSONObject> jsonCaptor = ArgumentCaptor.forClass(JSONObject.class);
		verify(mockGridManager, times(2)).executeGridUpdatePreview(eq(header), eq(agentConnection),
				jsonCaptor.capture());
		List<JSONObject> captured = jsonCaptor.getAllValues();
		assertEquals(request.getUpdate().getBatch().get(0),
				JDOSecondaryPropertyUtils.createObjectFromJSON(Update.class, captured.get(0).toString()));
		assertEquals(request.getUpdate().getBatch().get(1),
				JDOSecondaryPropertyUtils.createObjectFromJSON(Update.class, captured.get(1).toString()));
		// A preview must never apply a change.
		verify(mockGridManager, never()).executeGridUpdate(any(), any(), any());
	}

	@Test
	public void testPreviewGridUpdatePreservesOmittedValue() throws Exception {
		GridConnectionInfo internalConnection = new GridConnectionInfo().setReplicaId(INTERNAL_REPLICA_ID);
		GridConnectionInfo agentConnection = new GridConnectionInfo().setReplicaId(AGENTS_REPLICA_ID);
		GridHeader header = new GridHeader();

		when(mockGridManager.getSingletonConnection(GRID_SESSION_ID, EventSource.INTERNAL))
				.thenReturn(Optional.of(internalConnection));
		when(mockGridViewManager.readHeader(GRID_SESSION_ID, INTERNAL_REPLICA_ID, USERS_REPLICA_ID))
				.thenReturn(Optional.of(header));
		when(mockGridManager.getConnection(GRID_SESSION_ID, AGENTS_REPLICA_ID))
				.thenReturn(Optional.of(agentConnection));
		when(mockGridManager.executeGridUpdatePreview(eq(header), eq(agentConnection), any(JSONObject.class)))
				.thenReturn(List.of());

		// As with updateGrid, an omitted 'value' (set to undefined) must reach the manager still absent,
		// distinct from an explicit "value": null — the preview shares updateGrid's raw-payload path.
		String update = "{\"update\":{\"batch\":[{\"set\":[{\"concreteType\":"
				+ "\"org.sagebionetworks.repo.model.grid.update.LiteralSetValue\",\"columnName\":\"color\"}],"
				+ "\"filters\":[{\"concreteType\":\"org.sagebionetworks.repo.model.grid.query.RowSelectionFilter\","
				+ "\"isSelected\":true}]}]}}";

		// call under test
		String response = previewGridUpdateCallback().call(update, toolContext);

		assertTrue(new JSONObject(response).has("responseBody"));

		ArgumentCaptor<JSONObject> jsonCaptor = ArgumentCaptor.forClass(JSONObject.class);
		verify(mockGridManager).executeGridUpdatePreview(eq(header), eq(agentConnection), jsonCaptor.capture());
		JSONObject setEntry = jsonCaptor.getValue().getJSONArray("set").getJSONObject(0);
		assertEquals("color", setEntry.getString("columnName"));
		assertFalse(setEntry.has("value"), "The omitted value must remain absent in the payload sent to the manager");
	}

	@Test
	public void testPreviewGridUpdateWithMalformedJson() {
		String malformed = "{ this is not valid json";

		// call under test
		String response = previewGridUpdateCallback().call(malformed, toolContext);

		assertTrue(response.contains("was not valid JSON for its input schema"));
		assertTrue(response.contains("Resubmit the call with a corrected argument."));
		verifyNoInteractions(mockGridManager, mockGridViewManager);
	}

	@Test
	public void testPreviewGridUpdateWithNoContext() {
		ToolContext emptyContext = new ToolContext(Map.of());

		String update = "{\"update\":{\"batch\":[]}}";

		// call under test
		String response = previewGridUpdateCallback().call(update, emptyContext);

		assertEquals("No grid session context available", new JSONObject(response).getString("errorMessage"));
		verifyNoInteractions(mockGridManager, mockGridViewManager);
	}

	@Test
	public void testPreviewGridUpdateWithNoInternalConnection() throws Exception {
		when(mockGridManager.getSingletonConnection(GRID_SESSION_ID, EventSource.INTERNAL))
				.thenReturn(Optional.empty());

		String update = "{\"update\":{\"batch\":[{\"set\":[],\"filters\":[]}]}}";

		// call under test
		String response = previewGridUpdateCallback().call(update, toolContext);

		assertTrue(new JSONObject(response).getString("errorMessage")
				.contains("Cannot get an internal grid connection."));
		verify(mockGridViewManager, never()).readHeader(any(), any(), any());
		verify(mockGridManager, never()).executeGridUpdatePreview(any(), any(), any());
	}

	@Test
	public void testGetToolCallbacksInputSchema() {
		ToolCallback callback = updateGridCallback();

		// Although the tool receives a raw String, its input schema is generated from the declared
		// schemaType (GridUpdateRequest) so the model still sees the full typed structure.
		String inputSchema = callback.getToolDefinition().inputSchema();
		JSONObject schema = new JSONObject(inputSchema);

		assertTrue(schema.has("$defs"));
		assertTrue(inputSchema.contains("oneOf"));
		JSONObject defs = schema.getJSONObject("$defs");
		assertTrue(defs.has("org.sagebionetworks.repo.model.grid.update.LiteralSetValue"));
		assertTrue(defs.has("org.sagebionetworks.repo.model.grid.update.TemplateSetValue"));
		assertTrue(defs.has("org.sagebionetworks.repo.model.grid.query.CellValueFilter"));
	}
}
