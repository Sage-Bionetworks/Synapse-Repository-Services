package org.sagebionetworks.repo.model.athena;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.util.TokenPaginationPage;
import org.sagebionetworks.util.TokenPaginationProvider;

import software.amazon.awssdk.services.athena.AthenaClient;
import software.amazon.awssdk.services.athena.model.Datum;
import software.amazon.awssdk.services.athena.model.GetQueryResultsRequest;
import software.amazon.awssdk.services.athena.model.GetQueryResultsResponse;
import software.amazon.awssdk.services.athena.model.ResultSet;
import software.amazon.awssdk.services.athena.model.Row;

@ExtendWith(MockitoExtension.class)
public class AthenaResultsProviderTest {

	private static final String QUERY_ID = "someQueryExecutionId";

	private static final String HEADER_COL = "HeaderColumn";

	@Mock
	private AthenaClient mockAthenaClient;

	@Mock
	private GetQueryResultsRequest mockQueryRequest;

	@Mock
	private GetQueryResultsResponse mockQueryResults;
	
	@Mock
	private RowMapper<String> mockMapper;

	@BeforeEach
	public void before() {
		when(mockAthenaClient.getQueryResults((GetQueryResultsRequest) any())).thenReturn(mockQueryResults);
	}

	@Test
	public void testGetNextFirstPageNoResults() {
		int resultsNumber = 0;
		ResultSet results = getResultSet(resultsNumber);

		when(mockQueryResults.resultSet()).thenReturn(results);

		boolean excludeHeader = true;
		String nextToken = null;

		TokenPaginationProvider<String> provider = getAthenaResultsProviderInstance(excludeHeader);

		// Call under test
		TokenPaginationPage<String> nextPage = provider.getNextPage(nextToken);

		assertNotNull(nextPage);
		assertEquals(nextToken, nextPage.getNextToken());
		assertTrue(nextPage.getResults().isEmpty());

		verify(mockAthenaClient).getQueryResults(getQueryResultsRequest(nextToken));
		verify(mockQueryResults).resultSet();
		verify(mockMapper, never()).mapRow(any());
	}

	@Test
	public void testGetNextPageExcludingHeader() {

		int resultsNumber = 10;
		ResultSet results = getResultSet(resultsNumber);

		when(mockQueryResults.resultSet()).thenReturn(results);
		when(mockMapper.mapRow(any())).then(this::getMapperAnswer);

		boolean excludeHeader = true;
		String nextToken = null;

		TokenPaginationProvider<String> provider = getAthenaResultsProviderInstance(excludeHeader);

		// Call under test
		TokenPaginationPage<String> nextPage = provider.getNextPage(nextToken);

		assertNotNull(nextPage);
		assertEquals(nextToken, nextPage.getNextToken());
		assertFalse(nextPage.getResults().isEmpty());
		// Make sure the header is excluded from the results
		assertEquals(resultsNumber, nextPage.getResults().size());
		assertNotEquals(HEADER_COL, nextPage.getResults().get(0));
		
		verify(mockAthenaClient).getQueryResults(getQueryResultsRequest(nextToken));
		verify(mockQueryResults).resultSet();
		verify(mockMapper, times(resultsNumber)).mapRow(any());
	}
	
	@Test
	public void testGetNextPageIncludingHeader() {

		int resultsNumber = 10;
		ResultSet results = getResultSet(resultsNumber);

		when(mockQueryResults.resultSet()).thenReturn(results);
		when(mockMapper.mapRow(any())).then(this::getMapperAnswer);

		boolean excludeHeader = false;
		String nextToken = null;

		TokenPaginationProvider<String> provider = getAthenaResultsProviderInstance(excludeHeader);

		// Call under test
		TokenPaginationPage<String> nextPage = provider.getNextPage(nextToken);

		assertNotNull(nextPage);
		assertEquals(nextToken, nextPage.getNextToken());
		assertFalse(nextPage.getResults().isEmpty());
		// Make sure the header is included in the results
		assertEquals(resultsNumber + 1, nextPage.getResults().size());
		assertEquals(HEADER_COL, nextPage.getResults().get(0));
		
		verify(mockAthenaClient).getQueryResults(getQueryResultsRequest(nextToken));
		verify(mockQueryResults).resultSet();
		verify(mockMapper, times(resultsNumber + 1)).mapRow(any());
	}
	
	@Test
	public void testGetNextPageMultiplePagesExcludingHeader() {

		int firstPageResults = AthenaResultsProvider.MAX_PAGE_SIZE;
		int secondPageResults = 10;
		
		ResultSet firstPage = getResultSet(firstPageResults);
		ResultSet secondPage = getResultSet(secondPageResults, firstPageResults, false);

		String nextToken = "nextToken";
		
		when(mockQueryResults.resultSet()).thenReturn(firstPage, secondPage);
		when(mockQueryResults.nextToken()).thenReturn(nextToken, new String[] { null });
		when(mockMapper.mapRow(any())).then(this::getMapperAnswer);

		boolean excludeHeader = true;

		TokenPaginationProvider<String> provider = getAthenaResultsProviderInstance(excludeHeader);

		// Call under test, first page
		TokenPaginationPage<String> nextPage = provider.getNextPage(null);

		assertNotNull(nextPage);
		assertEquals(nextToken, nextPage.getNextToken());
		assertFalse(nextPage.getResults().isEmpty());
		// Make sure the header is not included in the results
		assertEquals(firstPageResults, nextPage.getResults().size());
		assertNotEquals(HEADER_COL, nextPage.getResults().get(0));
		
		verify(mockAthenaClient).getQueryResults(getQueryResultsRequest(null));
		verify(mockQueryResults).resultSet();
		verify(mockMapper, times(firstPageResults)).mapRow(any());

		// Call under test, second page
		nextPage = provider.getNextPage(nextToken);

		assertNotNull(nextPage);
		assertNull(nextPage.getNextToken());
		assertFalse(nextPage.getResults().isEmpty());
		assertEquals(secondPageResults, nextPage.getResults().size());
		assertNotEquals(HEADER_COL, nextPage.getResults().get(0));
		
		verify(mockAthenaClient).getQueryResults(getQueryResultsRequest(nextToken));
		verify(mockQueryResults, times(2)).resultSet();
		verify(mockMapper, times(firstPageResults + secondPageResults)).mapRow(any());
		
	}
	

	@Test
	public void testGetNextPageMultiplePagesIncludingHeader() {

		int firstPageResults = AthenaResultsProvider.MAX_PAGE_SIZE;
		int secondPageResults = 10;
		
		// Max page size is reached, we need to exclude one row from the first page (the header)
		ResultSet firstPage = getResultSet(firstPageResults - 1);
		ResultSet secondPage = getResultSet(secondPageResults, firstPageResults, false);

		String nextToken = "nextToken";
		
		when(mockQueryResults.resultSet()).thenReturn(firstPage, secondPage);
		when(mockQueryResults.nextToken()).thenReturn(nextToken, new String[] { null });
		when(mockMapper.mapRow(any())).then(this::getMapperAnswer);

		boolean excludeHeader = false;

		TokenPaginationProvider<String> provider = getAthenaResultsProviderInstance(excludeHeader);

		// Call under test, first page
		TokenPaginationPage<String> nextPage = provider.getNextPage(null);

		assertNotNull(nextPage);
		assertEquals(nextToken, nextPage.getNextToken());
		assertFalse(nextPage.getResults().isEmpty());
		// Make sure the header is included in the results
		assertEquals(firstPageResults, nextPage.getResults().size());
		assertEquals(HEADER_COL, nextPage.getResults().get(0));
		
		verify(mockAthenaClient).getQueryResults(getQueryResultsRequest(null));
		verify(mockQueryResults).resultSet();
		verify(mockMapper, times(firstPageResults)).mapRow(any());

		// Call under test, second page
		nextPage = provider.getNextPage(nextToken);

		assertNotNull(nextPage);
		assertNull(nextPage.getNextToken());
		assertFalse(nextPage.getResults().isEmpty());
		assertEquals(secondPageResults, nextPage.getResults().size());
		assertNotEquals(HEADER_COL, nextPage.getResults().get(0));
		
		verify(mockAthenaClient).getQueryResults(getQueryResultsRequest(nextToken));
		verify(mockQueryResults, times(2)).resultSet();
		verify(mockMapper, times(firstPageResults + secondPageResults)).mapRow(any());

		
	}

	
	private String getMapperAnswer(InvocationOnMock invocation) {
		return ((Row)invocation.getArgument(0)).data().stream().map(Datum::varCharValue).collect(Collectors.joining(","));
	}

	private GetQueryResultsRequest getQueryResultsRequest(String nextToken) {
		GetQueryResultsRequest request = GetQueryResultsRequest.builder().queryExecutionId(QUERY_ID)
				.maxResults(AthenaResultsProvider.MAX_PAGE_SIZE).nextToken(nextToken).build();

		return request;
	}

	private TokenPaginationProvider<String> getAthenaResultsProviderInstance(boolean excludeHeader) {
		return new AthenaResultsProvider<>(mockAthenaClient, QUERY_ID, mockMapper, excludeHeader);
	}

	private ResultSet getResultSet(int numberOfRows) {
		return getResultSet(numberOfRows, 0, true);
	}

	private ResultSet getResultSet(int numberOfRows, int startIndex, boolean includeHeader) {
		ResultSet.Builder resultSetBuilder = ResultSet.builder();
		List<Row> rows = new ArrayList<>(numberOfRows+1);
		if (includeHeader) {
			// Athena always include the header row
			rows.add(getHeaderRow());
		}
		for (int i = startIndex; i < numberOfRows + startIndex; i++) {
			rows.add(getRow(String.valueOf(i)));
		}
		resultSetBuilder.rows(rows);
		return resultSetBuilder.build();
	}

	private Row getHeaderRow() {
		return getRow(HEADER_COL);
	}

	private Row getRow(String value) {
		return Row.builder().data(Datum.builder().varCharValue(value).build()).build();
	}

}
