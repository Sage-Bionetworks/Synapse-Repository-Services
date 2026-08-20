package org.sagebionetworks.repo.manager.agent.specialist.tablequery;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.agent.AgentToolContextKey;
import org.sagebionetworks.repo.manager.agent.CodeSessionSupplier;
import org.sagebionetworks.repo.manager.agent.CodeInterpreterFileManager;
import org.sagebionetworks.repo.manager.agent.specialist.ToolResponse;
import org.sagebionetworks.repo.manager.agent.tool.JSONEntityTool;
import org.sagebionetworks.repo.manager.agent.tool.JSONEntityToolBase;
import org.sagebionetworks.repo.manager.agent.tool.JSONEntityToolParam;
import org.sagebionetworks.repo.manager.table.TableManagerSupport;
import org.sagebionetworks.repo.manager.table.TableQueryManager;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.agent.TableDescription;
import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.DownloadFromTableRequest;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryOptions;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.util.csv.CSVWriterStream;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.util.progress.ProgressListener;
import org.springaicommunity.agentcore.codeinterpreter.CodeExecutionResult;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Service;

@Service
public class TableQueryTools extends JSONEntityToolBase {

	static final long MAX_QUERY_LIMIT = 100L;

	private static final ProgressCallback NO_OP_PROGRESS = new ProgressCallback() {
		@Override
		public void addProgressListener(ProgressListener listener) {}

		@Override
		public void removeProgressListener(ProgressListener listener) {}

		@Override
		public long getLockTimeoutSeconds() {
			return 300;
		}
	};

	private final TableQueryManager tableQueryManager;
	private final TableManagerSupport tableManagerSupport;
	private final EntityManager entityManager;
	private final CodeInterpreterFileManager codeInterpreterFileManager;

	public TableQueryTools(TableQueryManager tableQueryManager, TableManagerSupport tableManagerSupport,
			EntityManager entityManager, CodeInterpreterFileManager codeInterpreterFileManager) {
		super();
		this.tableQueryManager = tableQueryManager;
		this.tableManagerSupport = tableManagerSupport;
		this.entityManager = entityManager;
		this.codeInterpreterFileManager = codeInterpreterFileManager;
	}

	@JSONEntityTool(description = "Get metadata about a Synapse table or view including its type and full column schema.")
	public ToolResponse<TableDescription> describeTable(
			@JSONEntityToolParam(description = "A Synapse table ID such as 'syn123' or 'syn123.5' for a specific version", required = true) String tableId,
			ToolContext toolContext) {
		UserInfo userInfo = extractUserInfo(toolContext);
		if (userInfo == null) {
			return new ToolResponse<>("No user context available");
		}
		try {
			IdAndVersion idAndVersion = IdAndVersion.parse(tableId);
			String entityId = "syn" + idAndVersion.getId();
			Entity entity;
			if (idAndVersion.getVersion().isPresent()) {
				entity = entityManager.getEntityForVersion(userInfo, entityId, idAndVersion.getVersion().get(), Entity.class);
			} else {
				entity = entityManager.getEntity(userInfo, entityId);
			}
			TableType tableType = tableManagerSupport.getTableType(idAndVersion);
			List<ColumnModel> columns = tableManagerSupport.getTableSchema(idAndVersion);

			TableDescription description = new TableDescription()
					.setTableType(tableType.name())
					.setEntity(entity)
					.setColumnModels(columns);

			return new ToolResponse<>(description);
		} catch (Exception e) {
			return new ToolResponse<>("Error describing table '" + tableId + "': " + e.getMessage());
		}
	}

	@JSONEntityTool(description = "Execute a SQL query against a Synapse table or view. Returns a QueryResultBundle "
			+ "containing query results (up to 100 rows), total row count, select column metadata, column models, "
			+ "and facet statistics. The query limit is capped at 100 rows.")
	public ToolResponse<QueryResultBundle> queryTable(
			@JSONEntityToolParam(description = "A Synapse SQL query such as 'SELECT col1, col2 FROM syn123 WHERE condition'", required = true) String sql,
			@JSONEntityToolParam(description = "Maximum number of rows to return (capped at 100)", required = false) Long limit,
			ToolContext toolContext) {
		UserInfo userInfo = extractUserInfo(toolContext);
		if (userInfo == null) {
			return new ToolResponse<>("No user context available");
		}
		try {
			long effectiveLimit = (limit == null || limit > MAX_QUERY_LIMIT) ? MAX_QUERY_LIMIT : Math.max(1, limit);

			Query query = new Query();
			query.setSql(sql);
			query.setLimit(effectiveLimit);
			query.setOffset(0L);

			QueryOptions options = new QueryOptions()
					.withRunQuery(true)
					.withReturnSelectColumns(true)
					.withRunCount(true)
					.withReturnFacets(true)
					.withReturnColumnModels(true);

			QueryResultBundle result = tableQueryManager.querySinglePage(NO_OP_PROGRESS, userInfo, query, options);
			return new ToolResponse<>(result);
		} catch (Exception e) {
			return new ToolResponse<>("Error executing query: " + e.getMessage());
		}
	}

	@JSONEntityTool(description = "Execute a SQL query and write the full results as a CSV file to the code interpreter session. "
			+ "Use this for large result sets that exceed 100 rows. The results are streamed directly to a file.")
	public ToolResponse<QueryResultBundle> writeQueryToSession(
			@JSONEntityToolParam(description = "A Synapse SQL query string", required = true) String sql,
			@JSONEntityToolParam(description = "File path in the session, e.g. 'query_specialist/results.csv'", required = true) String filePath,
			ToolContext toolContext) {
		UserInfo userInfo = extractUserInfo(toolContext);
		if (userInfo == null) {
			return new ToolResponse<>("No user context available");
		}
		String sessionId = extractSessionId(toolContext);
		if (sessionId == null) {
			return new ToolResponse<>("No code interpreter session ID available");
		}
		File tempFile = null;
		try {
			DownloadFromTableRequest request = new DownloadFromTableRequest();
			request.setSql(sql);
			request.setWriteHeader(true);
			request.setIncludeRowIdAndRowVersion(false);

			tempFile = File.createTempFile("query_", ".csv");
			FileWriter fileWriter = new FileWriter(tempFile);

			CSVWriterStream csvWriter = nextLine -> {
				for (int i = 0; i < nextLine.length; i++) {
					if (i > 0) {
						fileWriter.write(",");
					}
					fileWriter.write(escapeCsvField(nextLine[i]));
				}
				fileWriter.write("\n");
			};

			tableQueryManager.runQueryDownloadAsCSV(NO_OP_PROGRESS, userInfo, request, csvWriter);
			fileWriter.close();

			CodeExecutionResult pushResult = codeInterpreterFileManager.pushLocalFileToSession(sessionId, tempFile, "text/csv", filePath);
			if (pushResult.isError()) {
				return new ToolResponse<>("Error writing file to session: " + pushResult.textOutput());
			}

			Query countQuery = new Query();
			countQuery.setSql(sql);
			countQuery.setLimit(1L);
			countQuery.setOffset(0L);

			QueryOptions countOptions = new QueryOptions()
					.withRunQuery(false)
					.withRunCount(true)
					.withReturnSelectColumns(true)
					.withReturnColumnModels(true);

			QueryResultBundle metadata = tableQueryManager.querySinglePage(NO_OP_PROGRESS, userInfo, countQuery, countOptions);
			return new ToolResponse<>(metadata);
		} catch (Exception e) {
			return new ToolResponse<>("Error writing query to session: " + e.getMessage());
		} finally {
			if (tempFile != null) {
				tempFile.delete();
			}
		}
	}

	private UserInfo extractUserInfo(ToolContext toolContext) {
		return (UserInfo) AgentToolContextKey.USER_INFO.get(toolContext);
	}

	private String extractSessionId(ToolContext toolContext) {
		return CodeSessionSupplier.resolveSessionId(toolContext);
	}

	static String escapeCsvField(String field) {
		if (field == null) {
			return "";
		}
		if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
			return "\"" + field.replace("\"", "\"\"") + "\"";
		}
		return field;
	}

}
