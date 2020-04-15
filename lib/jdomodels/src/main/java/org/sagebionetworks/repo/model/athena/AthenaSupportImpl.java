package org.sagebionetworks.repo.model.athena;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.TokenPaginationIterator;
import org.sagebionetworks.util.TokenPaginationPage;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amazonaws.services.athena.AmazonAthena;
import com.amazonaws.services.athena.model.GetQueryExecutionRequest;
import com.amazonaws.services.athena.model.QueryExecution;
import com.amazonaws.services.athena.model.QueryExecutionContext;
import com.amazonaws.services.athena.model.ResultConfiguration;
import com.amazonaws.services.athena.model.StartQueryExecutionRequest;
import com.amazonaws.services.glue.AWSGlue;
import com.amazonaws.services.glue.model.Database;
import com.amazonaws.services.glue.model.EntityNotFoundException;
import com.amazonaws.services.glue.model.GetDatabaseRequest;
import com.amazonaws.services.glue.model.GetDatabasesRequest;
import com.amazonaws.services.glue.model.GetDatabasesResult;
import com.amazonaws.services.glue.model.GetTableRequest;
import com.amazonaws.services.glue.model.GetTablesRequest;
import com.amazonaws.services.glue.model.GetTablesResult;
import com.amazonaws.services.glue.model.Table;

@Service
public class AthenaSupportImpl implements AthenaSupport {

	private static final Logger LOG = LogManager.getLogger(AthenaSupportImpl.class);

	private static final String TABLE_NAME_REGEX = "^%1$s.+";
	private static final String QUERY_RESULTS_BUCKET = "s3://%1$s/athena/%2$09d";
	private static final long WAIT_INTERVAL = 1000;
	private static final int GLUE_MAX_RESULTS = 1000;

	private static final String TEMPLATE_ATHENA_REPAIR_TABLE = "MSCK REPAIR TABLE %1$s";

	private AWSGlue glueClient;
	private AmazonAthena athenaClient;

	private String stackPrefix;
	private String tableNameRegex;
	private String queryOutputLocation;

	@Autowired
	public AthenaSupportImpl(AWSGlue glueClient, AmazonAthena athenaClient, StackConfiguration stackConfig) {
		this.glueClient = glueClient;
		this.athenaClient = athenaClient;
		this.stackPrefix = (stackConfig.getStack() + stackConfig.getStackInstance()).toLowerCase();
		this.tableNameRegex = String.format(TABLE_NAME_REGEX, stackPrefix);
		this.queryOutputLocation = String.format(QUERY_RESULTS_BUCKET, stackConfig.getLogBucketName().toLowerCase(),
				stackConfig.getStackInstanceNumber());
	}

	@Override
	public String getOutputResultLocation() {
		return queryOutputLocation;
	}

	@Override
	public Iterator<Database> getDatabases() {
		return new TokenPaginationIterator<Database>((nextToken) -> {
			// @formatter:off
			GetDatabasesRequest request = new GetDatabasesRequest()
					.withMaxResults(GLUE_MAX_RESULTS)
					.withNextToken(nextToken);
			// @formatter:on

			GetDatabasesResult result = glueClient.getDatabases(request);

			return new TokenPaginationPage<>(result.getDatabaseList(), result.getNextToken());
		});
	}

	@Override
	public Iterator<Table> getPartitionedTables(Database database) {
		return new TokenPaginationIterator<>((nextToken) -> {
			// @formatter:off
			GetTablesRequest request = new GetTablesRequest()
					.withDatabaseName(database.getName().toLowerCase())
					.withExpression(tableNameRegex)
					.withMaxResults(GLUE_MAX_RESULTS)
					.withNextToken(nextToken);

			GetTablesResult result = glueClient.getTables(request);

			List<Table> page = result.getTableList()
					.stream()
					.filter(table -> table.getPartitionKeys() != null && !table.getPartitionKeys().isEmpty())
					.collect(Collectors.toList());
			
			// @formatter:on
			return new TokenPaginationPage<>(page, result.getNextToken());
		});
	}

	@Override
	public Table getTable(Database database, String tableName) {
		ValidateArgument.required(database, "database");
		ValidateArgument.requiredNotEmpty(tableName, "tableName");
		// @formatter:off
		GetTableRequest request = new GetTableRequest()
				.withDatabaseName(database.getName().toLowerCase())
				.withName(getTableName(tableName));
		// @formatter:on
		try {
			return glueClient.getTable(request).getTable();
		} catch (EntityNotFoundException e) {
			throw new NotFoundException(e.getMessage(), e);
		}
	}

	@Override
	public Database getDatabase(String databaseName) {
		ValidateArgument.requiredNotEmpty(databaseName, "databaseName");

		// @formatter:off
		GetDatabaseRequest request = new GetDatabaseRequest()
				.withName(prefixWithStack(databaseName));
		// @formatter:on
		try {
			return glueClient.getDatabase(request).getDatabase();
		} catch (EntityNotFoundException e) {
			throw new NotFoundException(e.getMessage(), e);
		}
	}
	
	@Override
	public String getDatabaseName(String databaseName) {
		ValidateArgument.requiredNotEmpty(databaseName, "databaseName");
		return prefixWithStack(databaseName);
	}

	@Override
	public AthenaQueryStatistics repairTable(Table table) {

		String queryExecutionId = submitRepairTable(table);

		// Just wait for the result
		AthenaQueryStatistics queryStats = waitForQueryResults(queryExecutionId);

		if (LOG.isDebugEnabled()) {
			LOG.debug("Repairing table {} in database {}...DONE (Scanned: {} bytes, Elapsed Time: {} ms)", table.getName(),
					table.getDatabaseName(), queryStats.getDataScanned(), queryStats.getExecutionTime());
		}
		
		return queryStats;
	}

	@Override
	public String submitRepairTable(Table table) {
		ValidateArgument.required(table, "table");

		if (LOG.isDebugEnabled()) {
			LOG.debug("Repairing table {} in database {}...", table.getName(), table.getDatabaseName());
		}

		String repairQuery = String.format(TEMPLATE_ATHENA_REPAIR_TABLE, table.getName().toLowerCase());

		String queryExecutionId = submitQuery(table.getDatabaseName(), repairQuery);

		if (LOG.isDebugEnabled()) {
			LOG.debug("Repairing table {} in database {}...SUBMITTED", table.getName(), table.getDatabaseName());
		}

		return queryExecutionId;
	}

	@Override
	public String getTableName(String tableName) {
		ValidateArgument.requiredNotEmpty(tableName, "tableName");
		return prefixWithStack(tableName);
	}

	@Override
	public <T> AthenaQueryResult<T> executeQuery(Database database, String query, RowMapper<T> rowMapper) {
		return executeQuery(database, query, rowMapper, true);
	}

	@Override
	public <T> AthenaQueryResult<T> executeQuery(Database database, String query, RowMapper<T> rowMapper, boolean excludeHeader) {
		ValidateArgument.required(database, "database");
		ValidateArgument.required(query, "query");
		ValidateArgument.required(rowMapper, "rowMapper");

		if (LOG.isDebugEnabled()) {
			LOG.debug("Executing query {} on database {}...", query, database.getName());
		}

		// Run the query
		String queryExecutionId = submitQuery(database, query);

		AthenaQueryStatistics queryStatistics = waitForQueryResults(queryExecutionId);

		if (LOG.isDebugEnabled()) {
			LOG.debug("Executing query {} on database {}...DONE (Byte Scanned: {}, Elapsed Time: {})", query, database.getName(),
					queryStatistics.getDataScanned(), queryStatistics.getExecutionTime());
		}

		return retrieveQueryResults(queryExecutionId, queryStatistics, rowMapper, excludeHeader);

	}

	@Override
	public String submitQuery(Database database, String query) {
		ValidateArgument.required(database, "database");
		return submitQuery(database.getName(), query);
	}

	@Override
	public AthenaQueryStatistics waitForQueryResults(String queryExecutionId) {

		AthenaQueryStatistics queryStats = null;

		boolean done = false;

		while (!done) {

			AthenaQueryExecution queryExecution = getQueryExecutionStatus(queryExecutionId);

			AthenaQueryExecutionState state = queryExecution.getState();

			if (AthenaQueryExecutionState.SUCCEEDED.equals(state)) {
				done = true;
				queryStats = queryExecution.getStatistics();
			} else if (AthenaQueryExecutionState.FAILED.equals(state) || AthenaQueryExecutionState.CANCELLED.equals(state)) {
				throw new RuntimeException(
						"Query execution " + queryExecutionId + " " + state.toString() + ": " + queryExecution.getStateChangeReason());
			} else {
				try {
					Thread.sleep(WAIT_INTERVAL);
				} catch (InterruptedException e) {
					throw new RuntimeException(e.getMessage(), e);
				}
			}

		}

		return queryStats;

	}

	@Override
	public AthenaQueryExecution getQueryExecutionStatus(String queryExecutionId) {
		ValidateArgument.required(queryExecutionId, "queryExecutionId");
		// @formatter:off
		GetQueryExecutionRequest request = new GetQueryExecutionRequest()
				.withQueryExecutionId(queryExecutionId);

		QueryExecution queryExecution = athenaClient.getQueryExecution(request)
					.getQueryExecution();
		// @formatter:on

		return new AthenaQueryExecutionAdapter(queryExecution);
	}

	@Override
	public <T> AthenaQueryResult<T> getQueryResults(String queryExecutionId, RowMapper<T> rowMapper, boolean excludeHeader) {
		ValidateArgument.required(queryExecutionId, "executionQueryId");
		ValidateArgument.required(rowMapper, "rowMapper");

		AthenaQueryExecution queryExecution = getQueryExecutionStatus(queryExecutionId);

		if (!AthenaQueryExecutionState.SUCCEEDED.equals(queryExecution.getState())) {
			throw new IllegalStateException("The query with id " + queryExecutionId + " is not completed or did not succeed, state: "
					+ queryExecution.getState().toString());
		}

		return retrieveQueryResults(queryExecutionId, queryExecution.getStatistics(), rowMapper, excludeHeader);
	}

	private <T> AthenaQueryResult<T> retrieveQueryResults(String queryExecutionId, AthenaQueryStatistics queryStatistics,
			RowMapper<T> rowMapper, boolean excludeHeader) {

		AthenaResultsProvider<T> resultsProvider = new AthenaResultsProvider<>(athenaClient, queryExecutionId, rowMapper, excludeHeader);

		Iterator<T> resultsIterator = new TokenPaginationIterator<>(resultsProvider);

		return buildQueryResult(queryExecutionId, queryStatistics, resultsIterator, !excludeHeader);
	}

	private String submitQuery(String databaseName, String query) {
		ValidateArgument.requiredNotEmpty(databaseName, "databaseName");
		ValidateArgument.requiredNotEmpty(query, "query");

		// @formatter:off
		QueryExecutionContext queryContext = new QueryExecutionContext()
				.withDatabase(databaseName.toLowerCase());

		ResultConfiguration resultConfiguration = new ResultConfiguration()
				.withOutputLocation(queryOutputLocation);
		
		StartQueryExecutionRequest request = new StartQueryExecutionRequest()
				.withQueryExecutionContext(queryContext)
				.withResultConfiguration(resultConfiguration)
				.withQueryString(query);
		// @formatter:on
		return athenaClient.startQueryExecution(request).getQueryExecutionId();
	}

	private <T> AthenaQueryResult<T> buildQueryResult(String queryExecutionId, AthenaQueryStatistics queryStatistics,
			Iterator<T> resultsIterator, boolean includeHeader) {
		return new AthenaQueryResult<T>() {

			@Override
			public boolean includeHeader() {
				return includeHeader;
			}

			@Override
			public String getQueryExecutionId() {
				return queryExecutionId;
			}

			@Override
			public AthenaQueryStatistics getQueryExecutionStatistics() {
				return queryStatistics;
			}

			@Override
			public Iterator<T> getQueryResultsIterator() {
				return resultsIterator;
			}
		};
	}

	private String prefixWithStack(String value) {
		return (stackPrefix + value).toLowerCase();
	}

}
