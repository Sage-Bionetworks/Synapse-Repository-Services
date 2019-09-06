package org.sagebionetworks.repo.model.athena;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import com.amazonaws.services.athena.AmazonAthena;
import com.amazonaws.services.athena.model.GetQueryResultsRequest;
import com.amazonaws.services.athena.model.GetQueryResultsResult;
import com.amazonaws.services.athena.model.ResultSet;

/**
 * Custom iterator to retrieve batch of results from an athena query execution
 * 
 * @author     Marco
 *
 * @param  <T>
 */
public class AthenaResultsIterator<T> implements Iterator<T> {

	public static final int MAX_ATHENA_BATCH_SIZE = 500;

	private AmazonAthena athenaClient;
	private String queryExecutionId;
	private RowMapper<T> rowMapper;
	private int batchSize;
	private boolean excludeHeader;

	private String nextToken;
	private Iterator<T> currentPage;

	public AthenaResultsIterator(AmazonAthena athenaClient, String queryExecutionId, RowMapper<T> rowMapper, int batchSize,
			boolean excludeHeader) {
		this.athenaClient = athenaClient;
		this.queryExecutionId = queryExecutionId;
		this.rowMapper = rowMapper;
		this.batchSize = batchSize;
		this.excludeHeader = excludeHeader;
	}

	private List<T> nextPage() {

		int pageSize = batchSize;

		// Athena results include the header with column names, we fetch an additional row on the first page
		// if we need to exclude it
		if (excludeHeader && currentPage == null && pageSize < MAX_ATHENA_BATCH_SIZE) {
			pageSize += 1;
		}

		GetQueryResultsRequest request = new GetQueryResultsRequest()
				.withQueryExecutionId(queryExecutionId)
				.withMaxResults(pageSize)
				.withNextToken(nextToken);

		GetQueryResultsResult result = athenaClient.getQueryResults(request);
		nextToken = result.getNextToken();

		ResultSet resultSet = result.getResultSet();

		List<T> results = resultSet.getRows()
				.stream()
				// Athena results include the header with the column names, we skip if this is the first page
				.skip(excludeHeader && currentPage == null ? 1 : 0)
				.map(rowMapper::mapRow)
				.collect(Collectors.toList());

		return results;
	}

	@Override
	public boolean hasNext() {
		// Load the next page only if the iterator is at the end and there is a next token
		if (currentPage == null || (!currentPage.hasNext() && nextToken != null)) {
			currentPage = nextPage().iterator();
		}
		return currentPage.hasNext();
	}

	@Override
	public T next() {
		if (currentPage == null) {
			throw new IllegalStateException("hasNext() must be called before next()");
		}
		return currentPage.next();
	}

}