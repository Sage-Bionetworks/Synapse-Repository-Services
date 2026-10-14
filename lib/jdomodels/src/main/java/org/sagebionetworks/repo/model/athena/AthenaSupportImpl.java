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

import software.amazon.awssdk.services.athena.AthenaClient;
import software.amazon.awssdk.services.athena.model.GetQueryExecutionRequest;
import software.amazon.awssdk.services.athena.model.QueryExecution;
import software.amazon.awssdk.services.athena.model.QueryExecutionContext;
import software.amazon.awssdk.services.athena.model.ResultConfiguration;
import software.amazon.awssdk.services.athena.model.StartQueryExecutionRequest;
import software.amazon.awssdk.services.glue.GlueClient;
import software.amazon.awssdk.services.glue.model.Database;
import software.amazon.awssdk.services.glue.model.EntityNotFoundException;
import software.amazon.awssdk.services.glue.model.GetDatabaseRequest;
import software.amazon.awssdk.services.glue.model.GetDatabasesRequest;
import software.amazon.awssdk.services.glue.model.GetDatabasesResponse;
import software.amazon.awssdk.services.glue.model.GetTableRequest;
import software.amazon.awssdk.services.glue.model.GetTablesRequest;
import software.amazon.awssdk.services.glue.model.GetTablesResponse;
import software.amazon.awssdk.services.glue.model.Table;

@Service
public class AthenaSupportImpl implements AthenaSupport {

	private static final Logger LOG = LogManager.getLogger(AthenaSupportImpl.class);

	private static final String TABLE_NAME_REGEX = "^%1$s.+";
	private static final String QUERY_RESULTS_BUCKET = "s3://%1$s/athena/%2$09d";
	private static final long WAIT_INTERVAL = 1000;
	private static final int GLUE_MAX_RESULTS = 1000;

	private static final String TEMPLATE_ATHENA_REPAIR_TABLE = "MSCK REPAIR TABLE %1$s";

	private GlueClient glueClient;
	private AthenaClient athenaClient;

	private String stackPrefix;
	private String tableNameRegex;
	private String queryOutputLocation;

	@Autowired
	public AthenaSupportImpl(GlueClient glueClient, AthenaClient athenaClient, StackConfiguration stackConfig) {
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
			GetDatabasesRequest request = GetDatabasesRequest.builder()
					.maxResults(GLUE_MAX_RESULTS)
					.nextToken(nextToken)
					.build();
			// @formatter:on

			GetDatabasesResponse result = glueClient.getDatabases(request);

			return new TokenPaginationPage<>(result.databaseList(), result.nextToken());
		});
	}

	@Override
	public Iterator<Table> getPartitionedTables(Database database) {
		return new TokenPaginationIterator<>((nextToken) -> {
			// @formatter:off
			GetTablesRequest request = GetTablesRequest.builder()
					.databaseName(database.name().toLowerCase())
					.expression(tableNameRegex)
					.maxResults(GLUE_MAX_RESULTS)
					.nextToken(nextToken)
					.build();

			GetTablesResponse result = glueClient.getTables(request);

			List<Table> page = result.tableList()
					.stream()
					.filter(table -> table.partitionKeys() != null && !table.partitionKeys().isEmpty())
					.collect(Collectors.toList());

			// @formatter:on
			return new TokenPaginationPage<>(page, result.nextToken());
		});
	}

	@Override
	public Table getTable(Database database, String tableName) {
		ValidateArgument.required(database, "database");
		ValidateArgument.requiredNotEmpty(tableName, "tableName");
		// @formatter:off
		GetTableRequest request = GetTableRequest.builder()
				.databaseName(database.name().toLowerCase())
				.name(getTableName(tableName))
				.build();
		// @formatter:on
		try {
			return glueClient.getTable(request).table();
		} catch (EntityNotFoundException e) {
			throw new NotFoundException(e.getMessage(), e);
		}
	}

	@Override
	public Database getDatabase(String databaseName) {
		ValidateArgument.requiredNotEmpty(databaseName, "databaseName");

		// @formatter:off
		GetDatabaseRequest request = GetDatabaseRequest.builder()
				.name(prefixWithStack(databaseName))
				.build();
		// @formatter:on
		try {
			return glueClient.getDatabase(request).database();
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
			LOG.debug("Repairing table {} in database {}...DONE (Scanned: {} bytes, Elapsed Time: {} ms)", table.name(),
					table.databaseName(), queryStats.getDataScanned(), queryStats.getExecutionTime());
		}

		return queryStats;
	}

	@Override
	public String submitRepairTable(Table table) {
		ValidateArgument.required(table, "table");

		if (LOG.isDebugEnabled()) {
			LOG.debug("Repairing table {} in database {}...", table.name(), table.databaseName());
		}

		String repairQuery = String.format(TEMPLATE_ATHENA_REPAIR_TABLE, table.name().toLowerCase());

		String queryExecutionId = submitQuery(table.databaseName(), repairQuery);

		if (LOG.isDebugEnabled()) {
			LOG.debug("Repairing table {} in database {}...SUBMITTED", table.name(), table.databaseName());
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
			LOG.debug("Executing query {} on database {}...", query, database.name());
		}

		// Run the query
		String queryExecutionId = submitQuery(database, query);

		AthenaQueryStatistics queryStatistics = waitForQueryResults(queryExecutionId);

		if (LOG.isDebugEnabled()) {
			LOG.debug("Executing query {} on database {}...DONE (Byte Scanned: {}, Elapsed Time: {})", query, database.name(),
					queryStatistics.getDataScanned(), queryStatistics.getExecutionTime());
		}

		return retrieveQueryResults(queryExecutionId, queryStatistics, rowMapper, excludeHeader);

	}

	@Override
	public String submitQuery(Database database, String query) {
		ValidateArgument.required(database, "database");
		return submitQuery(database.name(), query);
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
		GetQueryExecutionRequest request = GetQueryExecutionRequest.builder()
				.queryExecutionId(queryExecutionId)
				.build();

		QueryExecution queryExecution = athenaClient.getQueryExecution(request)
					.queryExecution();
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
	
	
	@Override
	public <T> AthenaQueryResultPage<T> getQueryResultsPage(String queryExecutionId, RowMapper<T> rowMapper, String pageToken, int limit) {
		ValidateArgument.required(queryExecutionId, "executionQueryId");
		ValidateArgument.required(rowMapper, "rowMapper");
		ValidateArgument.requirement(limit > 0 && limit <= AthenaResultsProvider.MAX_PAGE_SIZE, "The limit must be greater than 0 and less or equal than " + AthenaResultsProvider.MAX_PAGE_SIZE);
		
		boolean excludeHeader = pageToken == null;
		
		AthenaResultsProvider<T> resultsProvider = new AthenaResultsProvider<>(athenaClient, queryExecutionId, rowMapper, excludeHeader, limit);
		
		TokenPaginationPage<T> page = resultsProvider.getNextPage(pageToken);
		
		AthenaQueryResultPage<T> result = new AthenaQueryResultPage<T>()
				.withResults(page.getResults())
				.withNextPageToken(page.getNextToken())
				.withQueryExecutionId(queryExecutionId);
		
		return result;
		
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
		QueryExecutionContext queryContext = QueryExecutionContext.builder()
				.database(databaseName.toLowerCase())
				.build();

		ResultConfiguration resultConfiguration = ResultConfiguration.builder()
				.outputLocation(queryOutputLocation)
				.build();

		StartQueryExecutionRequest request = StartQueryExecutionRequest.builder()
				.queryExecutionContext(queryContext)
				.resultConfiguration(resultConfiguration)
				.queryString(query)
				.build();
		// @formatter:on
		return athenaClient.startQueryExecution(request).queryExecutionId();
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
