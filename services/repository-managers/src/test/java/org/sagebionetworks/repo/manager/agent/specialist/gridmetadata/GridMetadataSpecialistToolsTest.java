package org.sagebionetworks.repo.manager.agent.specialist.gridmetadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.sagebionetworks.repo.manager.agent.AgentToolContextKey;
import org.sagebionetworks.repo.manager.agent.specialist.ToolResponse;
import org.sagebionetworks.repo.manager.grid.GridManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.agent.GridAgentSessionContext;
import org.sagebionetworks.repo.model.grid.GridReplicaInfo;
import org.sagebionetworks.repo.model.grid.GridReplicaType;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.grid.ListGridReplicasRequest;
import org.sagebionetworks.repo.model.grid.ListGridReplicasResponse;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;

@ExtendWith(MockitoExtension.class)
public class GridMetadataSpecialistToolsTest {

	@Mock
	private GridManager mockGridManager;
	@Mock
	private PrincipalAliasDAO mockPrincipalAliasDAO;

	private GridMetadataSpecialistTools tools;
	private UserInfo userInfo;
	private GridAgentSessionContext gridContext;
	private ToolContext toolContext;

	@BeforeEach
	public void before() {
		tools = new GridMetadataSpecialistTools(mockGridManager, mockPrincipalAliasDAO);
		userInfo = new UserInfo(false, 101L);
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
	public void testGetGridSession() {
		GridSession session = new GridSession().setSessionId("grid-1").setGridJsonSchema$Id("my.org-Schema-1.0.0");
		when(mockGridManager.getGridSession(userInfo, "grid-1")).thenReturn(session);

		// call under test
		ToolResponse<GridSession> result = tools.getGridSession(toolContext);

		assertEquals(new ToolResponse<>(session), result);
		verify(mockGridManager).getGridSession(userInfo, "grid-1");
	}

	@Test
	public void testGetGridSessionWithNoUser() {
		ToolContext noUser = new ToolContext(Map.of(AgentToolContextKey.GRID_SESSION_CONTEXT.getKey(), gridContext));

		// call under test
		ToolResponse<GridSession> result = tools.getGridSession(noUser);

		assertEquals("No user context available", result.getErrorMessage());
		verifyNoInteractions(mockGridManager);
	}

	@Test
	public void testGetGridSessionWithNoGridContext() {
		ToolContext noGrid = new ToolContext(Map.of(AgentToolContextKey.USER_INFO.getKey(), userInfo));

		// call under test
		ToolResponse<GridSession> result = tools.getGridSession(noGrid);

		assertEquals("No grid session context available", result.getErrorMessage());
		verifyNoInteractions(mockGridManager);
	}

	@Test
	public void testGetGridSessionWithManagerError() {
		when(mockGridManager.getGridSession(userInfo, "grid-1"))
				.thenThrow(new IllegalArgumentException("boom"));

		// call under test
		ToolResponse<GridSession> result = tools.getGridSession(toolContext);

		assertNull(result.getResponseBody());
		assertTrue(result.getErrorMessage().contains("boom"));
	}

	@Test
	public void testGetReplicaInfo() {
		GridReplicaInfo info = new GridReplicaInfo().setReplicaId(5L).setCreatedBy("202")
				.setReplicaType(GridReplicaType.USER).setIsConnected(true);
		when(mockGridManager.getReplicaInfo(userInfo, "grid-1", 5L)).thenReturn(info);

		// call under test
		ToolResponse<GridReplicaInfo> result = tools.getReplicaInfo(5L, toolContext);

		assertEquals(new ToolResponse<>(info), result);
		verify(mockGridManager).getReplicaInfo(userInfo, "grid-1", 5L);
	}

	@Test
	public void testGetReplicaInfoWithNoGridContext() {
		ToolContext noGrid = new ToolContext(Map.of(AgentToolContextKey.USER_INFO.getKey(), userInfo));

		// call under test
		ToolResponse<GridReplicaInfo> result = tools.getReplicaInfo(5L, noGrid);

		assertEquals("No grid session context available", result.getErrorMessage());
		verifyNoInteractions(mockGridManager);
	}

	@Test
	public void testGetReplicaInfoWithManagerError() {
		when(mockGridManager.getReplicaInfo(userInfo, "grid-1", 5L))
				.thenThrow(new IllegalArgumentException("no such replica"));

		// call under test
		ToolResponse<GridReplicaInfo> result = tools.getReplicaInfo(5L, toolContext);

		assertNull(result.getResponseBody());
		assertTrue(result.getErrorMessage().contains("no such replica"));
	}

	@Test
	public void testListReplicas() {
		ListGridReplicasResponse response = new ListGridReplicasResponse().setNextPageToken(null);
		when(mockGridManager.listReplicas(eq(userInfo), any(ListGridReplicasRequest.class))).thenReturn(response);

		// call under test
		ToolResponse<ListGridReplicasResponse> result = tools.listReplicas("token-1", toolContext);

		assertEquals(new ToolResponse<>(response), result);
		verify(mockGridManager).listReplicas(userInfo, new ListGridReplicasRequest()
				.setGridSessionId("grid-1").setNextPageToken("token-1"));
	}

	@Test
	public void testListReplicasWithNoGridContext() {
		ToolContext noGrid = new ToolContext(Map.of(AgentToolContextKey.USER_INFO.getKey(), userInfo));

		// call under test
		ToolResponse<ListGridReplicasResponse> result = tools.listReplicas(null, noGrid);

		assertEquals("No grid session context available", result.getErrorMessage());
		verifyNoInteractions(mockGridManager);
	}

	@Test
	public void testGetUserName() {
		when(mockPrincipalAliasDAO.getUserName(202L)).thenReturn("jane.doe");

		// call under test
		String result = tools.getUserName("202", toolContext);

		assertEquals("jane.doe", result);
		verify(mockPrincipalAliasDAO).getUserName(202L);
	}

	@Test
	public void testGetUserNameWithNonNumericId() {
		// call under test
		String result = tools.getUserName("not-a-number", toolContext);

		assertTrue(result.startsWith("Error getting username for user 'not-a-number'"));
		verifyNoInteractions(mockPrincipalAliasDAO);
	}

	@Test
	public void testToolCallbackNamesAndSchemas() {
		Set<String> names = tools.getToolCallbacks().stream().map(c -> c.getToolDefinition().name())
				.collect(Collectors.toSet());

		assertEquals(Set.of("getGridSession", "getReplicaInfo", "listReplicas", "getUserName"), names);

		// A no-argument tool advertises a valid object schema with no properties.
		JSONObject sessionSchema = new JSONObject(callback("getGridSession").getToolDefinition().inputSchema());
		assertEquals("object", sessionSchema.getString("type"));
		assertEquals(0, sessionSchema.getJSONObject("properties").length());

		// A required scalar becomes a typed, required top-level property.
		JSONObject replicaSchema = new JSONObject(callback("getReplicaInfo").getToolDefinition().inputSchema());
		assertEquals("integer",
				replicaSchema.getJSONObject("properties").getJSONObject("replicaId").getString("type"));
		assertTrue(replicaSchema.getJSONArray("required").toList().contains("replicaId"));

		// An optional scalar is a property but is not listed as required.
		JSONObject listSchema = new JSONObject(callback("listReplicas").getToolDefinition().inputSchema());
		assertEquals("string",
				listSchema.getJSONObject("properties").getJSONObject("nextPageToken").getString("type"));
		assertFalse(listSchema.has("required"));
	}

	@Test
	public void testGetReplicaInfoThroughCallback() {
		GridReplicaInfo info = new GridReplicaInfo().setReplicaId(5L).setCreatedBy("202")
				.setReplicaType(GridReplicaType.USER).setIsConnected(true);
		when(mockGridManager.getReplicaInfo(userInfo, "grid-1", 5L)).thenReturn(info);

		// call under test — the model supplies replicaId as a named JSON property.
		String response = callback("getReplicaInfo").call("{\"replicaId\": 5}", toolContext);

		assertEquals(JDOSecondaryPropertyUtils.createJSONFromObject(new ToolResponse<>(info)), response);
		verify(mockGridManager).getReplicaInfo(userInfo, "grid-1", 5L);
	}

	@Test
	public void testGetReplicaInfoThroughCallbackMissingRequired() {
		// call under test — a missing required scalar is fed back as corrective guidance, not thrown.
		String response = callback("getReplicaInfo").call("{}", toolContext);

		assertTrue(response.contains("missing required argument 'replicaId'"), response);
		verifyNoInteractions(mockGridManager);
	}

	@Test
	public void testGetGridSessionThroughCallbackWithNoArguments() {
		GridSession session = new GridSession().setSessionId("grid-1").setGridJsonSchema$Id("my.org-Schema-1.0.0");
		when(mockGridManager.getGridSession(userInfo, "grid-1")).thenReturn(session);

		// call under test — a no-argument tool is invoked with an empty argument object.
		String response = callback("getGridSession").call("{}", toolContext);

		assertEquals(JDOSecondaryPropertyUtils.createJSONFromObject(new ToolResponse<>(session)), response);
		verify(mockGridManager).getGridSession(userInfo, "grid-1");
	}

	@Test
	public void testGetUserNameThroughCallback() {
		when(mockPrincipalAliasDAO.getUserName(202L)).thenReturn("jane.doe");

		// call under test — a plain String return is passed through unchanged by the result converter.
		String response = callback("getUserName").call("{\"userId\": \"202\"}", toolContext);

		assertEquals("jane.doe", response);
		verify(mockPrincipalAliasDAO).getUserName(202L);
	}
}
