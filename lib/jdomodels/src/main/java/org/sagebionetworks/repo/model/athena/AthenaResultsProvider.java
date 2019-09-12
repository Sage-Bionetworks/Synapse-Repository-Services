package org.sagebionetworks.repo.model.athena;

import java.util.List;
import java.util.stream.Collectors;

import org.sagebionetworks.util.TokenPaginationPage;
import org.sagebionetworks.util.TokenPaginationProvider;

import com.amazonaws.services.athena.AmazonAthena;
import com.amazonaws.services.athena.model.AmazonAthenaException;
import com.amazonaws.services.athena.model.GetQueryResultsRequest;
import com.amazonaws.services.athena.model.GetQueryResultsResult;
import com.amazonaws.services.athena.model.ResultSet;

/**
 * {@link TokenPaginationProvider} to retrieve batch of results from an athena query execution
 * 
 * @author Marco
 *
 * @param <T>
 */
public class AthenaResultsProvider<T> implements TokenPaginationProvider<T> {

	// The maximum number of results per page that athena allows
	static final int PAGE_SIZE = 500;

	private AmazonAthena athenaClient;
	private String queryExecutionId;
	private RowMapper<T> rowMapper;
	private boolean excludeHeader;
	private boolean isFirstPage = true;

	public AthenaResultsProvider(AmazonAthena athenaClient, String queryExecutionId, RowMapper<T> rowMapper, boolean excludeHeader) {
		this.athenaClient = athenaClient;
		this.queryExecutionId = queryExecutionId;
		this.rowMapper = rowMapper;
		this.excludeHeader = excludeHeader;
	}

	@Override
	public TokenPaginationPage<T> getNextPage(String nextToken) {
		// @formatter:off

		GetQueryResultsRequest request = new GetQueryResultsRequest()
				.withQueryExecutionId(queryExecutionId)
				.withMaxResults(PAGE_SIZE)
				.withNextToken(nextToken);
				
		GetQueryResultsResult result;

		try {
			result = athenaClient.getQueryResults(request);
		} catch (AmazonAthenaException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
		
		ResultSet resultSet = result.getResultSet();

		List<T> results = resultSet.getRows()
				.stream()
				// Athena results include the header with the column names, we skip it if this is the first page
				.skip(excludeHeader && isFirstPage ? 1 : 0)
				.map(rowMapper::mapRow)
				.collect(Collectors.toList());
		
		isFirstPage = false;
		 
		// @formatter:on

		return new TokenPaginationPage<>(results, result.getNextToken());
	}
}