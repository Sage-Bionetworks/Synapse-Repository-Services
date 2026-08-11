package org.sagebionetworks.repo.manager.agent.specialist.gridquery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.GridReplicaViewManager;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.QueryElement;
import org.sagebionetworks.repo.model.agent.GridAgentSessionContext;
import org.sagebionetworks.repo.model.dbo.grid.GridDao;
import org.sagebionetworks.repo.model.grid.EventSource;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.query.result.QueryResult;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;

@ExtendWith(MockitoExtension.class)
public class GridQueryToolsTest {

	@Mock
	private GridReplicaViewManager mockViewManager;

	@Mock
	private GridDao mockGridDao;

	@InjectMocks
	private GridQueryTools gridQueryTools;

	private GridAgentSessionContext gridContext;
	private ToolContext toolContext;

	private static final String GRID_SESSION_ID = "grid-123";
	private static final Long USERS_REPLICA_ID = 101L;
	private static final Long AGENTS_REPLICA_ID = 202L;
	private static final Long INTERNAL_REPLICA_ID = 1L;

	private static final String QUERY_SELECT_ALL = "{\"query\":{\"columnSelection\":[{\"concreteType\":"
			+ "\"org.sagebionetworks.repo.model.grid.query.SelectAll\"}],\"limit\":10}}";

	@BeforeEach
	public void before() {
		gridContext = new GridAgentSessionContext().setGridSessionId(GRID_SESSION_ID)
				.setUsersReplicaId(USERS_REPLICA_ID).setAgentsReplicaId(AGENTS_REPLICA_ID);
		toolContext = new ToolContext(Map.of(AgentToolContextKey.GRID_SESSION_CONTEXT.getKey(), gridContext));
	}

	/**
	 * The tool is exercised through its {@link ToolCallback} — the same path Spring AI drives — so the
	 * base class's argument marshalling (JSON &rarr; {@code QueryRequest}) and result conversion are
	 * covered end to end.
	 */
	private ToolCallback queryGridCallback() {
		return gridQueryTools.getToolCallbacks().stream()
				.filter(callback -> "queryGrid".equals(callback.getToolDefinition().name())).findFirst().orElseThrow();
	}

	@Test
	public void testQueryGrid() {
		GridConnectionInfo internalConnection = new GridConnectionInfo().setReplicaId(INTERNAL_REPLICA_ID);
		GridHeader header = new GridHeader();
		QueryResult expectedResult = new QueryResult();

		when(mockGridDao.getSingletonConnection(GRID_SESSION_ID, EventSource.INTERNAL))
				.thenReturn(Optional.of(internalConnection));
		when(mockViewManager.readHeader(GRID_SESSION_ID, INTERNAL_REPLICA_ID, USERS_REPLICA_ID))
				.thenReturn(Optional.of(header));
		when(mockViewManager.querySinglePageAsQueryResult(eq(header), any(QueryElement.class)))
				.thenReturn(expectedResult);

		// call under test
		String response = queryGridCallback().call(QUERY_SELECT_ALL, toolContext);

		assertEquals(JDOSecondaryPropertyUtils.createJSONFromObject(new ToolResponse<>(expectedResult)), response);

		ArgumentCaptor<QueryElement> elementCaptor = ArgumentCaptor.forClass(QueryElement.class);
		verify(mockViewManager).querySinglePageAsQueryResult(eq(header), elementCaptor.capture());
		assertNotNull(elementCaptor.getValue());
	}

	@Test
	public void testQueryGridWithMalformedJson() {
		// A JSON parse failure is fed back to the model as the tool result string, not thrown, and no
		// downstream work is attempted.
		String malformed = "{ this is not valid json";

		// call under test
		String response = queryGridCallback().call(malformed, toolContext);

		assertTrue(response.contains("was not valid JSON"), response);
		verifyNoInteractions(mockGridDao, mockViewManager);
	}

	@Test
	public void testQueryGridWithNoContext() {
		ToolContext emptyContext = new ToolContext(Map.of());

		// call under test
		String response = queryGridCallback().call(QUERY_SELECT_ALL, emptyContext);

		assertEquals("No grid session context available", new JSONObject(response).getString("errorMessage"));
		verifyNoInteractions(mockGridDao, mockViewManager);
	}

	@Test
	public void testQueryGridWithMissingQuery() {
		// call under test
		String response = queryGridCallback().call("{}", toolContext);

		assertTrue(new JSONObject(response).getString("errorMessage").startsWith("Error executing grid query:"));
		verifyNoInteractions(mockGridDao, mockViewManager);
	}

	@Test
	public void testQueryGridWithEmptyColumnSelection() {
		// A query that binds with no columnSelection (e.g. a double-wrapped request whose nested
		// object is silently dropped) must be rejected rather than silently returning all rows.
		String noSelection = "{\"query\":{\"limit\":10}}";

		// call under test
		String response = queryGridCallback().call(noSelection, toolContext);

		assertTrue(new JSONObject(response).getString("errorMessage").contains("request.query.columnSelection"));
		verifyNoInteractions(mockGridDao, mockViewManager);
	}

	@Test
	public void testQueryGridWithNoInternalConnection() {
		when(mockGridDao.getSingletonConnection(GRID_SESSION_ID, EventSource.INTERNAL)).thenReturn(Optional.empty());

		// call under test
		String response = queryGridCallback().call(QUERY_SELECT_ALL, toolContext);

		assertTrue(new JSONObject(response).getString("errorMessage").contains("Cannot get an internal grid connection."));
		verify(mockViewManager, never()).readHeader(any(), any(), any());
		verify(mockViewManager, never()).querySinglePageAsQueryResult(any(), any());
	}

	@Test
	public void testQueryGridWithNoHeader() {
		GridConnectionInfo internalConnection = new GridConnectionInfo().setReplicaId(INTERNAL_REPLICA_ID);

		when(mockGridDao.getSingletonConnection(GRID_SESSION_ID, EventSource.INTERNAL))
				.thenReturn(Optional.of(internalConnection));
		when(mockViewManager.readHeader(GRID_SESSION_ID, INTERNAL_REPLICA_ID, USERS_REPLICA_ID))
				.thenReturn(Optional.empty());

		// call under test
		String response = queryGridCallback().call(QUERY_SELECT_ALL, toolContext);

		assertTrue(new JSONObject(response).getString("errorMessage").contains("Grid session does not exist"));
		verify(mockViewManager, never()).querySinglePageAsQueryResult(any(), any());
	}

	@Test
	public void testGetToolCallbacksInputSchema() {
		ToolCallback callback = queryGridCallback();

		String inputSchema = callback.getToolDefinition().inputSchema();
		JSONObject schema = new JSONObject(inputSchema);

		// The QueryRequest structure is advertised to the model by the tool's input schema, not the
		// system prompt: the polymorphic unions appear as oneOf and their implementers under $defs.
		assertTrue(schema.has("$defs"));
		assertTrue(inputSchema.contains("oneOf"));
		assertTrue(schema.getJSONObject("$defs").has("org.sagebionetworks.repo.model.grid.query.CellValueFilter"));
		assertTrue(schema.getJSONObject("$defs").has("org.sagebionetworks.repo.model.grid.query.SelectAll"));
	}

	@Test
	public void testGetToolCallbacksDefinition() {
		ToolCallback callback = queryGridCallback();

		assertEquals("queryGrid", callback.getToolDefinition().name());
		assertFalse(callback.getToolDefinition().description().isBlank());
	}
}
