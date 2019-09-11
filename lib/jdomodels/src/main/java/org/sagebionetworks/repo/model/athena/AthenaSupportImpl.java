package org.sagebionetworks.repo.model.athena;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amazonaws.services.athena.AmazonAthena;
import com.amazonaws.services.athena.model.AmazonAthenaException;
import com.amazonaws.services.athena.model.GetQueryExecutionRequest;
import com.amazonaws.services.athena.model.QueryExecution;
import com.amazonaws.services.athena.model.QueryExecutionContext;
import com.amazonaws.services.athena.model.QueryExecutionState;
import com.amazonaws.services.athena.model.QueryExecutionStatistics;
import com.amazonaws.services.athena.model.QueryExecutionStatus;
import com.amazonaws.services.athena.model.ResultConfiguration;
import com.amazonaws.services.athena.model.StartQueryExecutionRequest;
import com.amazonaws.services.glue.AWSGlue;
import com.amazonaws.services.glue.model.AWSGlueException;
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
	private static final String QUERY_RESULTS_BUCKET = "s3://%1$s/%2$09d/athena";
	private static final long WAIT_INTERVAL = 1000;

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
	public List<Table> getPartitionedTables() throws ServiceUnavailableException {
		List<Table> tables = new ArrayList<>();
		for (Database database : getDatabases()) {
			tables.addAll(getPartitionedTables(database));
		}
		return tables;
	}

	@Override
	public Table getTable(Database database, String tableName) throws ServiceUnavailableException {
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
		} catch (AWSGlueException e) {
			throw new ServiceUnavailableException(e.getMessage(), e);
		}
	}

	@Override
	public Database getDatabase(String databaseName) throws ServiceUnavailableException {
		ValidateArgument.requiredNotEmpty(databaseName, "databaseName");

		// @formatter:off
		GetDatabaseRequest request = new GetDatabaseRequest()
				.withName(prefixWithStack(databaseName));
		// @formatter:on
		try {
			return glueClient.getDatabase(request).getDatabase();
		} catch (EntityNotFoundException e) {
			throw new NotFoundException(e.getMessage(), e);
		} catch (AWSGlueException e) {
			throw new ServiceUnavailableException(e.getMessage(), e);
		}
	}

	@Override
	public AthenaQueryStatistics repairTable(Table table) throws ServiceUnavailableException {

		ValidateArgument.required(table, "table");

		LOG.info("Repairing table {} in database {}...", table.getName(), table.getDatabaseName());

		String repairQuery = String.format(TEMPLATE_ATHENA_REPAIR_TABLE, table.getName().toLowerCase());

		String queryExecutionId = queryRequest(table.getDatabaseName(), repairQuery);

		// Just wait for the result
		AthenaQueryStatistics queryStats = waitForQueryResults(queryExecutionId);

		LOG.info("Repairing table {} in database {}...DONE (Scanned: {} bytes, Elapsed Time: {} ms)", table.getName(),
				table.getDatabaseName(), queryStats.getDataScanned(), queryStats.getExecutionTime());

		return queryStats;
	}

	@Override
	public String getTableName(String tableName) {
		ValidateArgument.required(tableName, "tableName");
		return prefixWithStack(tableName);
	}

	@Override
	public <T> AthenaQueryResult<T> executeQuery(Database database, String query, RowMapper<T> rowMapper)
			throws ServiceUnavailableException {
		return executeQuery(database, query, rowMapper, true);
	}

	@Override
	public <T> AthenaQueryResult<T> executeQuery(Database database, String query, RowMapper<T> rowMapper, boolean excludeHeader)
			throws ServiceUnavailableException {
		ValidateArgument.required(database, "database");
		ValidateArgument.required(query, "query");
		ValidateArgument.required(rowMapper, "rowMapper");

		LOG.debug("Executing query {} on database {}...", query, database.getName());

		// Run the query
		String queryExecutionId = queryRequest(database.getName().toLowerCase(), query);

		AthenaQueryStatistics queryStatistics = waitForQueryResults(queryExecutionId);

		LOG.debug("Executing query {} on database {}...DONE (Byte Scanned: {}, Elapsed Time: {})", query, database.getName(),
				queryStatistics.getDataScanned(), queryStatistics.getExecutionTime());

		Iterator<T> resultsIterator = new AthenaResultsIterator<>(athenaClient, queryExecutionId, rowMapper, excludeHeader);

		return buildQueryResult(queryExecutionId, queryStatistics, resultsIterator, !excludeHeader);

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

	private String queryRequest(String databaseName, String query) throws ServiceUnavailableException {
		ValidateArgument.required(databaseName, "databaseName");
		ValidateArgument.required(query, "query");

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

		try {
			return athenaClient.startQueryExecution(request).getQueryExecutionId();
		} catch (AmazonAthenaException e) {
			throw new ServiceUnavailableException(e.getMessage(), e);
		}
	}

	private AthenaQueryStatistics waitForQueryResults(String queryId) throws ServiceUnavailableException {

		QueryExecutionStatistics queryStats = null;
		boolean done = false;

		while (!done) {

			// @formatter:off
			GetQueryExecutionRequest request = new GetQueryExecutionRequest()
					.withQueryExecutionId(queryId);

			QueryExecution result;
			
			try {
				result = athenaClient.getQueryExecution(request)
						.getQueryExecution();
			} catch (AmazonAthenaException e) {
				throw new ServiceUnavailableException(e.getMessage(), e);
			}

			QueryExecutionStatus status = result.getStatus();

			QueryExecutionState state = QueryExecutionState.fromValue(status.getState());
			// @formatter:on

			if (QueryExecutionState.SUCCEEDED.equals(state)) {
				done = true;
				queryStats = result.getStatistics();
			} else if (QueryExecutionState.FAILED.equals(state) || QueryExecutionState.CANCELLED.equals(state)) {
				throw new RuntimeException("Query execution " + queryId + " " + state.toString() + ": " + status.getStateChangeReason());
			} else {
				try {
					Thread.sleep(WAIT_INTERVAL);
				} catch (InterruptedException e) {
					throw new RuntimeException(e.getMessage(), e);
				}
			}

		}

		return new AthenaQueryStatisticsAdapter(queryStats);

	}

	private List<Table> getPartitionedTables(Database database) throws ServiceUnavailableException {
		List<Table> tables = new ArrayList<>();

		String nextToken = null;

		do {
			// @formatter:off
			GetTablesRequest request = new GetTablesRequest()
					.withDatabaseName(database.getName().toLowerCase())
					.withExpression(tableNameRegex)
					.withNextToken(nextToken);

			GetTablesResult result;
			
			try {
				result = glueClient.getTables(request);
			} catch (AWSGlueException e) {
				throw new ServiceUnavailableException(e.getMessage(), e);
			}

			nextToken = result.getNextToken();

			List<Table> page = result.getTableList()
					.stream()
					.filter(table -> table.getPartitionKeys() != null && !table.getPartitionKeys().isEmpty())
					.collect(Collectors.toList());
			// @formatter:on

			tables.addAll(page);

		} while (nextToken != null);

		return tables;
	}

	private List<Database> getDatabases() throws ServiceUnavailableException {
		List<Database> databases = new ArrayList<>();

		String nextToken = null;

		do {
			// @formatter:off
			GetDatabasesRequest request = new GetDatabasesRequest()
					.withNextToken(nextToken);
			// @formatter:on

			GetDatabasesResult result;
			try {
				result = glueClient.getDatabases(request);
			} catch (AWSGlueException e) {
				throw new ServiceUnavailableException(e.getMessage(), e);
			}

			nextToken = result.getNextToken();

			databases.addAll(result.getDatabaseList());
		} while (nextToken != null);

		return databases;
	}

}
