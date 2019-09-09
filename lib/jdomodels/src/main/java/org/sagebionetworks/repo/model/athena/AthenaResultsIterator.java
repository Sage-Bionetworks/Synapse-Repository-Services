package org.sagebionetworks.repo.model.athena;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import com.amazonaws.services.athena.AmazonAthena;
import com.amazonaws.services.athena.model.AmazonAthenaException;
import com.amazonaws.services.athena.model.GetQueryResultsRequest;
import com.amazonaws.services.athena.model.GetQueryResultsResult;
import com.amazonaws.services.athena.model.ResultSet;

/**
 * Custom iterator to retrieve batch of results from an athena query execution
 * 
 * @author Marco
 *
 * @param <T>
 */
public class AthenaResultsIterator<T> implements Iterator<T> {

	// The maximum number of results per page that athena allows
	public static final int MAX_FETCH_PAGE_SIZE = 500;

	private AmazonAthena athenaClient;
	private String queryExecutionId;
	private RowMapper<T> rowMapper;
	private int pageSize;
	private boolean excludeHeader;

	private String nextToken;
	private Iterator<T> currentPage;

	public AthenaResultsIterator(AmazonAthena athenaClient, String queryExecutionId, RowMapper<T> rowMapper, int pageSize,
			boolean excludeHeader) {
		this.athenaClient = athenaClient;
		this.queryExecutionId = queryExecutionId;
		this.rowMapper = rowMapper;
		this.pageSize = pageSize;
		this.excludeHeader = excludeHeader;
	}

	private List<T> nextPage() {

		int maxResults = pageSize;

		// Athena results include the header with column names, we fetch an additional row on the first page
		// if we need to exclude it
		if (excludeHeader && currentPage == null && pageSize < MAX_FETCH_PAGE_SIZE) {
			maxResults += 1;
		}

		// @formatter:off

		GetQueryResultsRequest request = new GetQueryResultsRequest()
				.withQueryExecutionId(queryExecutionId)
				.withMaxResults(maxResults)
				.withNextToken(nextToken);
				
		GetQueryResultsResult result;

		try {
			result = athenaClient.getQueryResults(request);
		} catch (AmazonAthenaException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
		
		nextToken = result.getNextToken();

		ResultSet resultSet = result.getResultSet();

		List<T> results = resultSet.getRows()
				.stream()
				// Athena results include the header with the column names, we skip if this is the first page
				.skip(excludeHeader && currentPage == null ? 1 : 0)
				.map(rowMapper::mapRow)
				.collect(Collectors.toList());
		 
		// @formatter:on

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