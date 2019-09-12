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

import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.util.TokenPaginationPage;
import org.sagebionetworks.util.TokenPaginationProvider;

import com.amazonaws.services.athena.AmazonAthena;
import com.amazonaws.services.athena.model.Datum;
import com.amazonaws.services.athena.model.GetQueryResultsRequest;
import com.amazonaws.services.athena.model.GetQueryResultsResult;
import com.amazonaws.services.athena.model.ResultSet;
import com.amazonaws.services.athena.model.Row;

@ExtendWith(MockitoExtension.class)
public class AthenaResultsProviderTest {

	private static final String QUERY_ID = "someQueryExecutionId";

	private static final String HEADER_COL = "HeaderColumn";

	@Mock
	private AmazonAthena mockAthenaClient;

	@Mock
	private GetQueryResultsRequest mockQueryRequest;

	@Mock
	private GetQueryResultsResult mockQueryResults;
	
	@Mock
	private RowMapper<String> mockMapper;

	@BeforeEach
	public void before() {
		when(mockAthenaClient.getQueryResults(any())).thenReturn(mockQueryResults);
	}

	@Test
	public void testGetNextFirstPageNoResults() {
		int resultsNumber = 0;
		ResultSet results = getResultSet(resultsNumber);

		when(mockQueryResults.getResultSet()).thenReturn(results);

		boolean excludeHeader = true;
		String nextToken = null;

		TokenPaginationProvider<String> provider = getAthenaResultsProviderInstance(excludeHeader);

		// Call under test
		TokenPaginationPage<String> nextPage = provider.getNextPage(nextToken);

		assertNotNull(nextPage);
		assertEquals(nextToken, nextPage.getNextToken());
		assertTrue(nextPage.getResults().isEmpty());

		verify(mockAthenaClient).getQueryResults(getQueryResultsRequest(nextToken));
		verify(mockQueryResults).getResultSet();
		verify(mockMapper, never()).mapRow(any());
	}

	@Test
	public void testGetNextPageExcludingHeader() {

		int resultsNumber = 10;
		ResultSet results = getResultSet(resultsNumber);

		when(mockQueryResults.getResultSet()).thenReturn(results);
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
		verify(mockQueryResults).getResultSet();
		verify(mockMapper, times(resultsNumber)).mapRow(any());
	}
	
	@Test
	public void testGetNextPageIncludingHeader() {

		int resultsNumber = 10;
		ResultSet results = getResultSet(resultsNumber);

		when(mockQueryResults.getResultSet()).thenReturn(results);
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
		verify(mockQueryResults).getResultSet();
		verify(mockMapper, times(resultsNumber + 1)).mapRow(any());
	}
	
	@Test
	public void testGetNextPageMultiplePagesExcludingHeader() {

		int firstPageResults = AthenaResultsProvider.PAGE_SIZE;
		int secondPageResults = 10;
		
		ResultSet firstPage = getResultSet(firstPageResults);
		ResultSet secondPage = getResultSet(secondPageResults, firstPageResults, false);

		String nextToken = "nextToken";
		
		when(mockQueryResults.getResultSet()).thenReturn(firstPage, secondPage);
		when(mockQueryResults.getNextToken()).thenReturn(nextToken, new String[] { null });
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
		verify(mockQueryResults).getResultSet();
		verify(mockMapper, times(firstPageResults)).mapRow(any());

		// Call under test, second page
		nextPage = provider.getNextPage(nextToken);

		assertNotNull(nextPage);
		assertNull(nextPage.getNextToken());
		assertFalse(nextPage.getResults().isEmpty());
		assertEquals(secondPageResults, nextPage.getResults().size());
		assertNotEquals(HEADER_COL, nextPage.getResults().get(0));
		
		verify(mockAthenaClient).getQueryResults(getQueryResultsRequest(nextToken));
		verify(mockQueryResults, times(2)).getResultSet();
		verify(mockMapper, times(firstPageResults + secondPageResults)).mapRow(any());
		
	}
	

	@Test
	public void testGetNextPageMultiplePagesIncludingHeader() {

		int firstPageResults = AthenaResultsProvider.PAGE_SIZE;
		int secondPageResults = 10;
		
		// Max page size is reached, we need to exclude one row from the first page (the header)
		ResultSet firstPage = getResultSet(firstPageResults - 1);
		ResultSet secondPage = getResultSet(secondPageResults, firstPageResults, false);

		String nextToken = "nextToken";
		
		when(mockQueryResults.getResultSet()).thenReturn(firstPage, secondPage);
		when(mockQueryResults.getNextToken()).thenReturn(nextToken, new String[] { null });
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
		verify(mockQueryResults).getResultSet();
		verify(mockMapper, times(firstPageResults)).mapRow(any());

		// Call under test, second page
		nextPage = provider.getNextPage(nextToken);

		assertNotNull(nextPage);
		assertNull(nextPage.getNextToken());
		assertFalse(nextPage.getResults().isEmpty());
		assertEquals(secondPageResults, nextPage.getResults().size());
		assertNotEquals(HEADER_COL, nextPage.getResults().get(0));
		
		verify(mockAthenaClient).getQueryResults(getQueryResultsRequest(nextToken));
		verify(mockQueryResults, times(2)).getResultSet();
		verify(mockMapper, times(firstPageResults + secondPageResults)).mapRow(any());

		
	}

	
	private String getMapperAnswer(InvocationOnMock invocation) {
		return ((Row)invocation.getArgument(0)).getData().stream().map(Datum::getVarCharValue).collect(Collectors.joining(","));
	}

	private GetQueryResultsRequest getQueryResultsRequest(String nextToken) {
		GetQueryResultsRequest request = new GetQueryResultsRequest().withQueryExecutionId(QUERY_ID)
				.withMaxResults(AthenaResultsProvider.PAGE_SIZE).withNextToken(nextToken);

		return request;
	}

	private TokenPaginationProvider<String> getAthenaResultsProviderInstance(boolean excludeHeader) {
		return new AthenaResultsProvider<>(mockAthenaClient, QUERY_ID, mockMapper, excludeHeader);
	}

	private ResultSet getResultSet(int numberOfRows) {
		return getResultSet(numberOfRows, 0, true);
	}

	private ResultSet getResultSet(int numberOfRows, int startIndex, boolean includeHeader) {
		ResultSet resultSet = new ResultSet();
		if (includeHeader) {
			// Athena always include the header row
			resultSet.withRows(getHeaderRow());
		}
		for (int i = startIndex; i < numberOfRows + startIndex; i++) {
			resultSet.withRows(getRow(String.valueOf(i)));
		}
		return resultSet;
	}

	private Row getHeaderRow() {
		return getRow(HEADER_COL);
	}

	private Row getRow(String value) {
		return new Row().withData(new Datum().withVarCharValue(value));
	}

}
