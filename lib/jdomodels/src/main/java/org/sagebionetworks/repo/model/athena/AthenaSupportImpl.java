package org.sagebionetworks.repo.model.athena;

import static org.sagebionetworks.repo.model.athena.AthenaResultsIterator.MAX_FETCH_PAGE_SIZE;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amazonaws.services.athena.AmazonAthena;
import com.amazonaws.services.athena.model.GetQueryExecutionRequest;
import com.amazonaws.services.athena.model.QueryExecution;
import com.amazonaws.services.athena.model.QueryExecutionContext;
import com.amazonaws.services.athena.model.QueryExecutionState;
import com.amazonaws.services.athena.model.QueryExecutionStatistics;
import com.amazonaws.services.athena.model.QueryExecutionStatus;
import com.amazonaws.services.athena.model.ResultConfiguration;
import com.amazonaws.services.athena.model.StartQueryExecutionRequest;
import com.amazonaws.services.glue.AWSGlue;
import com.amazonaws.services.glue.model.Database;
import com.amazonaws.services.glue.model.EntityNotFoundException;
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
	private String queryResultsOutputLocation;

	@Autowired
	public AthenaSupportImpl(AWSGlue glueClient, AmazonAthena athenaClient, StackConfiguration stackConfig) {
		this.glueClient = glueClient;
		this.athenaClient = athenaClient;
		this.stackPrefix = (stackConfig.getStack() + stackConfig.getStackInstance()).toLowerCase();
		this.tableNameRegex = String.format(TABLE_NAME_REGEX, stackPrefix);
		this.queryResultsOutputLocation = String.format(QUERY_RESULTS_BUCKET, stackConfig.getLogBucketName(), stackConfig.getStackInstanceNumber());
	}

	@Override
	public List<Table> getPartitionedTables() {
		List<Table> tables = new ArrayList<>();
		for (Database database : getDatabases()) {
			tables.addAll(getPartitionedTables(database));
		}
		return tables;
	}

	@Override
	public Table getTable(String databaseName, String tableName) {
		GetTableRequest request = new GetTableRequest()
				.withDatabaseName(getDatabaseName(databaseName))
				.withName(getTableName(tableName));
		try {
			return glueClient.getTable(request).getTable();
		} catch (EntityNotFoundException e) {
			throw new NotFoundException(e.getMessage(), e);
		}
	}

	@Override
	public QueryExecutionStatistics repairTable(Table table) {

		ValidateArgument.required(table, "table");

		LOG.info("Repairing table {} in database {}...", table.getName(), table.getDatabaseName());
		
		String repairQuery = String.format(TEMPLATE_ATHENA_REPAIR_TABLE, table.getName().toLowerCase());

		String queryExecutionId = executeQuery(table.getDatabaseName(), repairQuery);

		// Just wait for the result
		QueryExecutionStatistics queryStats = waitForQueryResults(queryExecutionId);

		LOG.info("Repairing table {} in database {}...DONE (Scanned: {} bytes, Elapsed Time: {} ms)", table.getName(),
				table.getDatabaseName(), queryStats.getDataScannedInBytes(), queryStats.getEngineExecutionTimeInMillis());

		return queryStats;
	}

	@Override
	public String getDatabaseName(String databaseName) {
		ValidateArgument.required(databaseName, "databaseName");
		return prefixWithStack(databaseName);
	}

	@Override
	public String getTableName(String tableName) {
		ValidateArgument.required(tableName, "tableName");
		return prefixWithStack(tableName);
	}
	

	@Override
	public <T> AthenaQueryResult<T> executeQuery(String databaseName, String query, RowMapper<T> rowMapper, int pageSize) {
		return executeQuery(databaseName, query, rowMapper, pageSize, true);
	}

	
	@Override
	public <T> AthenaQueryResult<T> executeQuery(String databaseName, String query, RowMapper<T> rowMapper, int pageSize, boolean excludeHeader) {
		ValidateArgument.required(databaseName, "databaseName");
		ValidateArgument.required(query, "query");
		ValidateArgument.required(rowMapper, "rowMapper");
		ValidateArgument.requirement(pageSize > 0 && pageSize <= MAX_FETCH_PAGE_SIZE,
				"The batch size should be within the (0, " + MAX_FETCH_PAGE_SIZE + "] range.");
		
		LOG.debug("Executing query {} on database {}...", query, databaseName);

		// Run the query
		String queryExecutionId = executeQuery(databaseName, query);

		QueryExecutionStatistics queryStatistics = waitForQueryResults(queryExecutionId);

		LOG.debug("Executing query {} on database {}...DONE (Byte Scanned: {}, Elapsed Time: {})", query, databaseName,
				queryStatistics.getDataScannedInBytes(), queryStatistics.getEngineExecutionTimeInMillis());
		
		Iterator<T> iterator = new AthenaResultsIterator<>(athenaClient, queryExecutionId, rowMapper, pageSize, excludeHeader);
		
		return new AthenaQueryResult<T>() {

			@Override
			public String getQueryExecutionId() {
				return queryExecutionId;
			}

			@Override
			public QueryExecutionStatistics getQueryExecutionStatistics() {
				return queryStatistics;
			}

			@Override
			public Iterator<T> iterator() {
				return iterator;
			}
		};
	}
	
	private String prefixWithStack(String value) {
		return (stackPrefix + value).toLowerCase();
	}

	private String executeQuery(String databaseName, String query) {
		ValidateArgument.required(databaseName, "databaseName");
		ValidateArgument.required(query, "query");

		QueryExecutionContext queryContext = new QueryExecutionContext()
				.withDatabase(databaseName.toLowerCase());

		ResultConfiguration resultConfiguration = new ResultConfiguration()
				.withOutputLocation(queryResultsOutputLocation);

		StartQueryExecutionRequest request = new StartQueryExecutionRequest()
				.withQueryExecutionContext(queryContext)
				.withResultConfiguration(resultConfiguration)
				.withQueryString(query);

		return athenaClient.startQueryExecution(request).getQueryExecutionId();
	}

	private QueryExecutionStatistics waitForQueryResults(String queryId) {

		QueryExecutionStatistics queryStats = null;
		boolean done = false;

		while (!done) {

			GetQueryExecutionRequest request = new GetQueryExecutionRequest()
					.withQueryExecutionId(queryId);

			QueryExecution result = athenaClient.getQueryExecution(request).getQueryExecution();

			QueryExecutionStatus status = result.getStatus();

			QueryExecutionState state = QueryExecutionState.fromValue(status.getState());

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

		return queryStats;
		
	}

	private List<Table> getPartitionedTables(Database database) {
		List<Table> tables = new ArrayList<>();

		String nextToken = null;

		do {
			GetTablesRequest request = new GetTablesRequest()
					.withDatabaseName(database.getName().toLowerCase())
					.withExpression(tableNameRegex)
					.withNextToken(nextToken);

			GetTablesResult result = glueClient.getTables(request);

			nextToken = result.getNextToken();

			List<Table> page = result.getTableList()
					.stream()
					.filter(table -> table.getPartitionKeys() != null && !table.getPartitionKeys().isEmpty())
					.collect(Collectors.toList());

			tables.addAll(page);

		} while (nextToken != null);

		return tables;
	}

	private List<Database> getDatabases() {
		List<Database> databases = new ArrayList<>();

		String nextToken = null;

		do {
			GetDatabasesRequest request = new GetDatabasesRequest()
					.withNextToken(nextToken);

			GetDatabasesResult result = glueClient.getDatabases(request);

			nextToken = result.getNextToken();

			databases.addAll(result.getDatabaseList());
		} while (nextToken != null);

		return databases;
	}

}
