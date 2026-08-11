package org.sagebionetworks.agent.worker;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.agent.specialist.tablequery.TableQuerySpecialist;
import org.sagebionetworks.repo.manager.agent.specialist.tablequery.TableQuerySpecialistFactory;
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.manager.table.TableEntityManager;
import org.sagebionetworks.repo.manager.table.TableTransactionManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryOptions;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.springaicommunity.agentcore.codeinterpreter.AgentCoreCodeInterpreterClient;
import org.springaicommunity.agentcore.codeinterpreter.CodeExecutionResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class TableQuerySpecialistIntegrationTest {

	private static final int MAX_WAIT_MS = 1000 * 60 * 2;

	@Autowired
	private TableQuerySpecialistFactory specialistFactory;

	@Autowired
	private AgentCoreCodeInterpreterClient codeInterpreterClient;

	@Autowired
	private UserManager userManager;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private ColumnModelManager columnManager;

	@Autowired
	private TableEntityManager tableEntityManager;

	@Autowired
	private TableTransactionManager transactionManager;
	
	@Autowired
	private AsynchronousJobWorkerHelper asyncHelper;

	private UserInfo adminUser;
	private String projectId;
	private String tableId;
	private List<ColumnModel> schema;

	@BeforeEach
	public void setup() throws Exception {
		adminUser = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());

		Project project = new Project();
		project.setName("SpecialistTest-" + UUID.randomUUID());
		projectId = entityManager.createEntity(adminUser, project, null);

		schema = createSchema();
		tableId = createTableWithData();
		waitForTableAvailable();
	}

	@AfterEach
	public void cleanup() {
		if (tableId != null) {
			try {
				entityManager.deleteEntity(adminUser, tableId);
			} catch (Exception e) { }
		}
		if (projectId != null) {
			try {
				entityManager.deleteEntity(adminUser, projectId);
			} catch (Exception e) { }
		}
		columnManager.truncateAllColumnData(adminUser);
	}

	@Test
	public void testDescribeTable() {
		TableQuerySpecialist specialist = specialistFactory.create();

		// call under test
		String response = specialist.chat("Describe the table " + tableId, adminUser, null);

		assertNotNull(response);
		assertTrue(response.toLowerCase().contains("name") || response.toLowerCase().contains("column"),
				"Response should mention columns. Got: " + response);
		assertTrue(response.toLowerCase().contains("age") || response.toLowerCase().contains("score"),
				"Response should mention column names from schema. Got: " + response);
	}

	@Test
	public void testQueryTable() {
		TableQuerySpecialist specialist = specialistFactory.create();

		// call under test
		String response = specialist.chat(
				"Query the table " + tableId + " and tell me how many rows it has",
				adminUser, null);

		assertNotNull(response);
		assertTrue(response.contains("3") || response.contains("three"),
				"Response should indicate 3 rows. Got: " + response);
	}

	@Test
	public void testMultiTurnConversation() {
		TableQuerySpecialist specialist = specialistFactory.create();

		String describeResponse = specialist.chat("Describe " + tableId, adminUser, null);
		assertNotNull(describeResponse);

		// call under test — multi-turn, references the same table without re-specifying the ID
		String queryResponse = specialist.chat(
				"What are the distinct values in the name column?",
				adminUser, null);

		assertNotNull(queryResponse);
		assertTrue(queryResponse.contains("Alice") || queryResponse.contains("Bob") || queryResponse.contains("Charlie"),
				"Response should contain at least one name from the data. Got: " + queryResponse);
	}

	@Test
	public void testWriteQueryToSession() {
		String sessionId = codeInterpreterClient.startSession("specialistIT-" + System.nanoTime());
		try {
			TableQuerySpecialist specialist = specialistFactory.create();

			// call under test
			String response = specialist.chat(
					"Write all rows from " + tableId + " where age > 25 to query_specialist/filtered.csv",
					adminUser, sessionId);

			assertNotNull(response);
			assertTrue(response.contains("query_specialist") || response.contains("filtered") || response.contains("csv"),
					"Response should mention the file path. Got: " + response);

			// Verify the file was written to the session
			CodeExecutionResult readResult = codeInterpreterClient.executeCode(sessionId, "python",
					"print(open('query_specialist/filtered.csv').read())");
			assertFalse(readResult.isError(), "Should read the file without error. Got: " + readResult.textOutput());
			assertTrue(readResult.textOutput().contains("Charlie"),
					"File should contain Charlie (age 35). Got: " + readResult.textOutput());
		} finally {
			codeInterpreterClient.stopSession(sessionId);
		}
	}

	private List<ColumnModel> createSchema() {
		List<ColumnModel> columns = new LinkedList<>();

		ColumnModel nameCol = new ColumnModel();
		nameCol.setName("name");
		nameCol.setColumnType(ColumnType.STRING);
		nameCol.setMaximumSize(50L);
		columns.add(columnManager.createColumnModel(adminUser, nameCol));

		ColumnModel ageCol = new ColumnModel();
		ageCol.setName("age");
		ageCol.setColumnType(ColumnType.INTEGER);
		columns.add(columnManager.createColumnModel(adminUser, ageCol));

		ColumnModel scoreCol = new ColumnModel();
		scoreCol.setName("score");
		scoreCol.setColumnType(ColumnType.DOUBLE);
		columns.add(columnManager.createColumnModel(adminUser, scoreCol));

		return columns;
	}

	private String createTableWithData() throws Exception {
		List<String> columnIds = TableModelUtils.getIds(schema);
		String id = asyncHelper.createTable(adminUser, "SpecialistTestTable", projectId, columnIds, false).getId();

		List<Row> rows = new LinkedList<>();
		rows.add(createRow("Alice", "22", "95.5"));
		rows.add(createRow("Bob", "25", "87.3"));
		rows.add(createRow("Charlie", "35", "92.1"));

		RowSet rowSet = new RowSet();
		rowSet.setRows(rows);
		rowSet.setHeaders(TableModelUtils.getSelectColumns(schema));
		rowSet.setTableId(id);

		appendRows(adminUser, id, rowSet);
		return id;
	}

	private Row createRow(String... values) {
		Row row = new Row();
		row.setValues(List.of(values));
		return row;
	}

	private void appendRows(UserInfo user, String tableId, RowSet rowSet) throws Exception {
		transactionManager.executeInTransaction(user, tableId, txContext -> {
			try {
				return tableEntityManager.appendRows(user, tableId, rowSet, txContext);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}

	private void waitForTableAvailable() throws Exception {
		Query query = new Query();
		query.setSql("SELECT * FROM " + tableId);
		query.setLimit(10L);
		QueryOptions options = new QueryOptions().withRunQuery(true).withRunCount(true);
		asyncHelper.assertQueryResult(adminUser, query, options, (result) -> {
			assertNotNull(result.getQueryResult());
		}, MAX_WAIT_MS);
	}
}
