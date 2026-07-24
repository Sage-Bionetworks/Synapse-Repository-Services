package org.sagebionetworks.repo.model.athena;

import java.util.List;
import java.util.stream.Collectors;

import org.sagebionetworks.util.TokenPaginationPage;
import org.sagebionetworks.util.TokenPaginationProvider;

import software.amazon.awssdk.services.athena.AthenaClient;
import software.amazon.awssdk.services.athena.model.GetQueryResultsRequest;
import software.amazon.awssdk.services.athena.model.GetQueryResultsResponse;
import software.amazon.awssdk.services.athena.model.ResultSet;
import software.amazon.awssdk.core.exception.SdkException;

/**
 * {@link TokenPaginationProvider} to retrieve batch of results from an athena query execution
 * 
 * @author Marco
 *
 * @param <T>
 */
public class AthenaResultsProvider<T> implements TokenPaginationProvider<T> {

	// The maximum number of results per page that athena allows
	static final int MAX_PAGE_SIZE = 1000;

	private AthenaClient athenaClient;
	private String queryExecutionId;
	private RowMapper<T> rowMapper;
	private boolean excludeHeader;
	private boolean isFirstPage = true;
	private int pageSize;

	public AthenaResultsProvider(AthenaClient athenaClient, String queryExecutionId, RowMapper<T> rowMapper, boolean excludeHeader, int pageSize) {
		this.athenaClient = athenaClient;
		this.queryExecutionId = queryExecutionId;
		this.rowMapper = rowMapper;
		this.excludeHeader = excludeHeader;
		this.pageSize = pageSize;
	}

	public AthenaResultsProvider(AthenaClient athenaClient, String queryExecutionId, RowMapper<T> rowMapper, boolean excludeHeader) {
		this.athenaClient = athenaClient;
		this.queryExecutionId = queryExecutionId;
		this.rowMapper = rowMapper;
		this.excludeHeader = excludeHeader;
		this.pageSize = MAX_PAGE_SIZE;
	}

	@Override
	public TokenPaginationPage<T> getNextPage(String nextToken) {
		// @formatter:off

		GetQueryResultsRequest request = GetQueryResultsRequest.builder()
				.queryExecutionId(queryExecutionId)
				.maxResults(pageSize)
				.nextToken(nextToken)
				.build();

		GetQueryResultsResponse result;

		try {
			result = athenaClient.getQueryResults(request);
		} catch (SdkException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}

		ResultSet resultSet = result.resultSet();

		List<T> results = resultSet.rows()
				.stream()
				// Athena results include the header with the column names, we skip it if this is the first page
				.skip(excludeHeader && isFirstPage ? 1 : 0)
				.map(rowMapper::mapRow)
				.collect(Collectors.toList());

		isFirstPage = false;

		// @formatter:on

		return new TokenPaginationPage<>(results, result.nextToken());
	}
}