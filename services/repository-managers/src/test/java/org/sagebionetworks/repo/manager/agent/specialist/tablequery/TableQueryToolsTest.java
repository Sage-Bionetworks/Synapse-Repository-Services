package org.sagebionetworks.repo.manager.agent.specialist.tablequery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.agent.AgentToolContextKey;
import org.sagebionetworks.repo.manager.agent.CodeInterpreterFileManager;
import org.sagebionetworks.repo.manager.agent.specialist.ToolResponse;
import org.sagebionetworks.repo.manager.table.TableManagerSupport;
import org.sagebionetworks.repo.manager.table.TableQueryManager;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.agent.TableDescription;
import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.DownloadFromTableRequest;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryOptions;
import org.sagebionetworks.repo.model.table.QueryResult;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.util.csv.CSVWriterStream;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.springaicommunity.agentcore.codeinterpreter.CodeExecutionResult;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;

@ExtendWith(MockitoExtension.class)
public class TableQueryToolsTest {

	@Mock
	private TableQueryManager mockTableQueryManager;

	@Mock
	private TableManagerSupport mockTableManagerSupport;

	@Mock
	private EntityManager mockEntityManager;

	@Mock
	private CodeInterpreterFileManager mockCodeInterpreterFileManager;

	private TableQueryTools tools;
	private UserInfo userInfo;
	private ToolContext toolContext;
	private ToolContext toolContextWithSession;

	@BeforeEach
	public void setup() {
		tools = new TableQueryTools(mockTableQueryManager, mockTableManagerSupport, mockEntityManager, mockCodeInterpreterFileManager);
		userInfo = new UserInfo(false, 101L, AuthorizationConstants.DEFAULT_REALM_ID);
		toolContext = new ToolContext(Map.of(AgentToolContextKey.USER_INFO.getKey(), userInfo));
		toolContextWithSession = new ToolContext(Map.of(AgentToolContextKey.USER_INFO.getKey(), userInfo,
				AgentToolContextKey.CODE_SESSION_ID.getKey(), "session-123"));
	}

	private ToolCallback callback(String name) {
		return tools.getToolCallbacks().stream()
				.filter(c -> name.equals(c.getToolDefinition().name())).findFirst().orElseThrow();
	}

	@Test
	public void testToolCallbackNamesAndSchemas() {
		Set<String> names = tools.getToolCallbacks().stream().map(c -> c.getToolDefinition().name())
				.collect(Collectors.toSet());

		assertEquals(Set.of("describeTable", "queryTable", "writeQueryToSession"), names);

		// A required scalar becomes a typed, required top-level property.
		JSONObject describeSchema = new JSONObject(callback("describeTable").getToolDefinition().inputSchema());
		assertEquals("string", describeSchema.getJSONObject("properties").getJSONObject("tableId").getString("type"));
		assertTrue(describeSchema.getJSONArray("required").toList().contains("tableId"));

		// An optional scalar is a property but is not listed as required.
		JSONObject querySchema = new JSONObject(callback("queryTable").getToolDefinition().inputSchema());
		assertEquals("string", querySchema.getJSONObject("properties").getJSONObject("sql").getString("type"));
		assertEquals("integer", querySchema.getJSONObject("properties").getJSONObject("limit").getString("type"));
		assertFalse(querySchema.getJSONArray("required").toList().contains("limit"));
	}

	@Test
	public void testDescribeTableThroughCallback() throws Exception {
		IdAndVersion idAndVersion = IdAndVersion.parse("syn123");
		TableEntity tableEntity = new TableEntity();
		tableEntity.setId("syn123");
		tableEntity.setName("MyTable");
		when(mockEntityManager.getEntity(userInfo, "syn123")).thenReturn(tableEntity);
		when(mockTableManagerSupport.getTableType(idAndVersion)).thenReturn(TableType.table);
		when(mockTableManagerSupport.getTableSchema(idAndVersion)).thenReturn(List.of());

		// call under test — the model supplies tableId as a named JSON property.
		String response = callback("describeTable").call("{\"tableId\": \"syn123\"}", toolContext);

		TableDescription expected = new TableDescription().setTableType("table").setEntity(tableEntity)
				.setColumnModels(List.of());
		assertEquals(JDOSecondaryPropertyUtils.createJSONFromObject(new ToolResponse<>(expected)), response);
		verify(mockEntityManager).getEntity(userInfo, "syn123");
	}

	@Test
	public void testDescribeTableThroughCallbackMissingRequired() {
		// call under test — a missing required scalar is fed back as corrective guidance, not thrown.
		String response = callback("describeTable").call("{}", toolContext);

		assertTrue(response.contains("missing required argument 'tableId'"), response);
		verifyNoInteractions(mockEntityManager);
	}

	@Test
	public void testDescribeTableWithValidId() throws Exception {
		IdAndVersion idAndVersion = IdAndVersion.parse("syn123");
		ColumnModel col1 = new ColumnModel();
		col1.setName("name");
		col1.setColumnType(ColumnType.STRING);
		ColumnModel col2 = new ColumnModel();
		col2.setName("age");
		col2.setColumnType(ColumnType.INTEGER);

		TableEntity tableEntity = new TableEntity();
		tableEntity.setId("syn123");
		tableEntity.setName("MyTable");

		when(mockEntityManager.getEntity(userInfo, "syn123")).thenReturn(tableEntity);
		when(mockTableManagerSupport.getTableType(idAndVersion)).thenReturn(TableType.entityview);
		when(mockTableManagerSupport.getTableSchema(idAndVersion)).thenReturn(List.of(col1, col2));

		// call under test
		ToolResponse<TableDescription> response = tools.describeTable("syn123", toolContext);

		assertNotNull(response.getResponseBody());
		assertNull(response.getErrorMessage());
		assertEquals("entityview", response.getResponseBody().getTableType());
		assertEquals(tableEntity, response.getResponseBody().getEntity());
		assertEquals(2, response.getResponseBody().getColumnModels().size());
		assertEquals("name", response.getResponseBody().getColumnModels().get(0).getName());
		assertEquals(ColumnType.STRING, response.getResponseBody().getColumnModels().get(0).getColumnType());
		assertEquals("age", response.getResponseBody().getColumnModels().get(1).getName());
		assertEquals(ColumnType.INTEGER, response.getResponseBody().getColumnModels().get(1).getColumnType());
		verify(mockEntityManager).getEntity(userInfo, "syn123");
	}

	@Test
	public void testDescribeTableWithVersion() throws Exception {
		IdAndVersion idAndVersion = IdAndVersion.parse("syn456.3");
		ColumnModel col = new ColumnModel();
		col.setName("score");
		col.setColumnType(ColumnType.DOUBLE);

		TableEntity tableEntity = new TableEntity();
		tableEntity.setId("syn456");
		tableEntity.setName("VersionedTable");

		when(mockEntityManager.getEntityForVersion(userInfo, "syn456", 3L, Entity.class)).thenReturn(tableEntity);
		when(mockTableManagerSupport.getTableType(idAndVersion)).thenReturn(TableType.table);
		when(mockTableManagerSupport.getTableSchema(idAndVersion)).thenReturn(List.of(col));

		// call under test
		ToolResponse<TableDescription> response = tools.describeTable("syn456.3", toolContext);

		assertNotNull(response.getResponseBody());
		assertNull(response.getErrorMessage());
		assertEquals("table", response.getResponseBody().getTableType());
		assertEquals(tableEntity, response.getResponseBody().getEntity());
		assertEquals(1, response.getResponseBody().getColumnModels().size());
		verify(mockEntityManager).getEntityForVersion(userInfo, "syn456", 3L, Entity.class);
	}

	@Test
	public void testDescribeTableWithInvalidId() {
		// call under test
		ToolResponse<TableDescription> response = tools.describeTable("not_a_valid_id!", toolContext);

		assertNull(response.getResponseBody());
		assertNotNull(response.getErrorMessage());
		verifyNoInteractions(mockEntityManager);
		verifyNoInteractions(mockTableManagerSupport);
	}

	@Test
	public void testDescribeTableWithNoUserInfo() {
		ToolContext noUserContext = new ToolContext(Map.of());

		// call under test
		ToolResponse<TableDescription> response = tools.describeTable("syn123", noUserContext);

		assertNull(response.getResponseBody());
		assertEquals("No user context available", response.getErrorMessage());
		verifyNoInteractions(mockEntityManager);
		verifyNoInteractions(mockTableManagerSupport);
	}

	@Test
	public void testQueryTableWithResults() throws Exception {
		SelectColumn sc1 = new SelectColumn();
		sc1.setName("name");
		SelectColumn sc2 = new SelectColumn();
		sc2.setName("score");

		Row row1 = new Row();
		row1.setValues(List.of("Alice", "95.5"));
		Row row2 = new Row();
		row2.setValues(List.of("Bob", "87.3"));

		RowSet rowSet = new RowSet();
		rowSet.setRows(List.of(row1, row2));
		QueryResult queryResult = new QueryResult();
		queryResult.setQueryResults(rowSet);

		QueryResultBundle bundle = new QueryResultBundle();
		bundle.setQueryResult(queryResult);
		bundle.setSelectColumns(List.of(sc1, sc2));
		bundle.setQueryCount(2L);

		when(mockTableQueryManager.querySinglePage(any(ProgressCallback.class), eq(userInfo), any(Query.class), any(QueryOptions.class)))
				.thenReturn(bundle);

		// call under test
		ToolResponse<QueryResultBundle> response = tools.queryTable("SELECT name, score FROM syn123", null, toolContext);

		assertNotNull(response.getResponseBody());
		assertNull(response.getErrorMessage());
		assertEquals(2L, response.getResponseBody().getQueryCount());
		assertEquals(2, response.getResponseBody().getQueryResult().getQueryResults().getRows().size());

		ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
		verify(mockTableQueryManager).querySinglePage(any(), eq(userInfo), queryCaptor.capture(), any());
		assertEquals("SELECT name, score FROM syn123", queryCaptor.getValue().getSql());
		assertEquals(100L, queryCaptor.getValue().getLimit());
		assertEquals(0L, queryCaptor.getValue().getOffset());
	}

	@Test
	public void testQueryTableWithLimitCap() throws Exception {
		QueryResultBundle bundle = new QueryResultBundle();
		when(mockTableQueryManager.querySinglePage(any(ProgressCallback.class), eq(userInfo), any(Query.class), any(QueryOptions.class)))
				.thenReturn(bundle);

		// call under test
		tools.queryTable("SELECT * FROM syn123", 500L, toolContext);

		ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
		verify(mockTableQueryManager).querySinglePage(any(), eq(userInfo), queryCaptor.capture(), any());
		assertEquals(100L, queryCaptor.getValue().getLimit());
	}

	@Test
	public void testQueryTableWithSmallLimit() throws Exception {
		QueryResultBundle bundle = new QueryResultBundle();
		when(mockTableQueryManager.querySinglePage(any(ProgressCallback.class), eq(userInfo), any(Query.class), any(QueryOptions.class)))
				.thenReturn(bundle);

		// call under test
		tools.queryTable("SELECT * FROM syn123", 5L, toolContext);

		ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
		verify(mockTableQueryManager).querySinglePage(any(), eq(userInfo), queryCaptor.capture(), any());
		assertEquals(5L, queryCaptor.getValue().getLimit());
	}

	@Test
	public void testQueryTableWithNoUserInfo() {
		ToolContext noUserContext = new ToolContext(Map.of());

		// call under test
		ToolResponse<QueryResultBundle> response = tools.queryTable("SELECT * FROM syn123", null, noUserContext);

		assertNull(response.getResponseBody());
		assertEquals("No user context available", response.getErrorMessage());
		verifyNoInteractions(mockTableQueryManager);
	}

	@Test
	public void testQueryTableWithException() throws Exception {
		when(mockTableQueryManager.querySinglePage(any(ProgressCallback.class), eq(userInfo), any(Query.class), any(QueryOptions.class)))
				.thenThrow(new RuntimeException("Table not available"));

		// call under test
		ToolResponse<QueryResultBundle> response = tools.queryTable("SELECT * FROM syn999", null, toolContext);

		assertNull(response.getResponseBody());
		assertEquals("Error executing query: Table not available", response.getErrorMessage());
	}

	@Test
	public void testWriteQueryToSessionWithCsvContent() throws Exception {
		doAnswer(invocation -> {
			CSVWriterStream writer = invocation.getArgument(3);
			writer.writeNext(new String[]{"name", "age"});
			writer.writeNext(new String[]{"Alice", "30"});
			writer.writeNext(new String[]{"Bob", "25"});
			return null;
		}).when(mockTableQueryManager).runQueryDownloadAsCSV(any(ProgressCallback.class), eq(userInfo),
				any(DownloadFromTableRequest.class), any(CSVWriterStream.class));

		ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
		CodeExecutionResult successResult = new CodeExecutionResult("done", false, List.of());
		when(mockCodeInterpreterFileManager.pushLocalFileToSession(eq("session-123"), fileCaptor.capture(), eq("text/csv"), eq("query_specialist/out.csv")))
				.thenAnswer(invocation -> {
					File capturedFile = fileCaptor.getValue();
					String content = Files.readString(capturedFile.toPath());
					assertTrue(content.contains("name,age"));
					assertTrue(content.contains("Alice,30"));
					assertTrue(content.contains("Bob,25"));
					return successResult;
				});

		QueryResultBundle metadata = new QueryResultBundle();
		metadata.setQueryCount(3L);
		when(mockTableQueryManager.querySinglePage(any(ProgressCallback.class), eq(userInfo), any(Query.class), any(QueryOptions.class)))
				.thenReturn(metadata);

		// call under test
		ToolResponse<QueryResultBundle> response = tools.writeQueryToSession(
				"SELECT * FROM syn123", "query_specialist/out.csv", toolContextWithSession);

		assertNotNull(response.getResponseBody());
		assertNull(response.getErrorMessage());
		assertEquals(3L, response.getResponseBody().getQueryCount());
	}

	@Test
	public void testWriteQueryToSessionWithCsvFieldEscaping() throws Exception {
		doAnswer(invocation -> {
			CSVWriterStream writer = invocation.getArgument(3);
			writer.writeNext(new String[]{"name", "description"});
			writer.writeNext(new String[]{"test", "has,comma"});
			writer.writeNext(new String[]{"other", "has\"quote"});
			return null;
		}).when(mockTableQueryManager).runQueryDownloadAsCSV(any(ProgressCallback.class), eq(userInfo),
				any(DownloadFromTableRequest.class), any(CSVWriterStream.class));

		ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
		CodeExecutionResult successResult = new CodeExecutionResult("done", false, List.of());
		when(mockCodeInterpreterFileManager.pushLocalFileToSession(eq("session-123"), fileCaptor.capture(), eq("text/csv"), eq("out.csv")))
				.thenAnswer(invocation -> {
					File capturedFile = fileCaptor.getValue();
					String content = Files.readString(capturedFile.toPath());
					assertTrue(content.contains("\"has,comma\""));
					assertTrue(content.contains("\"has\"\"quote\""));
					return successResult;
				});

		QueryResultBundle metadata = new QueryResultBundle();
		metadata.setQueryCount(2L);
		when(mockTableQueryManager.querySinglePage(any(ProgressCallback.class), eq(userInfo), any(Query.class), any(QueryOptions.class)))
				.thenReturn(metadata);

		// call under test
		ToolResponse<QueryResultBundle> response = tools.writeQueryToSession(
				"SELECT * FROM syn123", "out.csv", toolContextWithSession);

		assertNotNull(response.getResponseBody());
		assertNull(response.getErrorMessage());
	}

	@Test
	public void testWriteQueryToSessionWithNoUserInfo() {
		ToolContext noUserContext = new ToolContext(Map.of(AgentToolContextKey.CODE_SESSION_ID.getKey(), "session-123"));

		// call under test
		ToolResponse<QueryResultBundle> response = tools.writeQueryToSession(
				"SELECT * FROM syn123", "out.csv", noUserContext);

		assertNull(response.getResponseBody());
		assertEquals("No user context available", response.getErrorMessage());
		verifyNoInteractions(mockCodeInterpreterFileManager);
	}

	@Test
	public void testWriteQueryToSessionWithNoSessionId() {
		// call under test
		ToolResponse<QueryResultBundle> response = tools.writeQueryToSession(
				"SELECT * FROM syn123", "out.csv", toolContext);

		assertNull(response.getResponseBody());
		assertEquals("No code interpreter session ID available", response.getErrorMessage());
		verifyNoInteractions(mockCodeInterpreterFileManager);
	}

	@Test
	public void testWriteQueryToSessionWithPushError() throws Exception {
		CodeExecutionResult writeErrorResult = new CodeExecutionResult("Permission denied", true, List.of());
		when(mockCodeInterpreterFileManager.pushLocalFileToSession(eq("session-123"), any(File.class), eq("text/csv"), eq("query_specialist/out.csv")))
				.thenReturn(writeErrorResult);

		// call under test
		ToolResponse<QueryResultBundle> response = tools.writeQueryToSession(
				"SELECT * FROM syn123", "query_specialist/out.csv", toolContextWithSession);

		assertNull(response.getResponseBody());
		assertTrue(response.getErrorMessage().contains("Permission denied"));
	}

	@Test
	public void testEscapeCsvField() {
		assertEquals("simple", TableQueryTools.escapeCsvField("simple"));
		assertEquals("", TableQueryTools.escapeCsvField(null));
		assertEquals("\"has,comma\"", TableQueryTools.escapeCsvField("has,comma"));
		assertEquals("\"has\"\"quote\"", TableQueryTools.escapeCsvField("has\"quote"));
		assertEquals("\"has\nnewline\"", TableQueryTools.escapeCsvField("has\nnewline"));
	}

}
