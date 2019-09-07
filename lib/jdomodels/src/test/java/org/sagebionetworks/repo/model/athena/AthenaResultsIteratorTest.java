package org.sagebionetworks.repo.model.athena;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.model.athena.AthenaResultsIterator.MAX_FETCH_PAGE_SIZE;

import java.util.Iterator;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.services.athena.AmazonAthena;
import com.amazonaws.services.athena.model.Datum;
import com.amazonaws.services.athena.model.GetQueryResultsRequest;
import com.amazonaws.services.athena.model.GetQueryResultsResult;
import com.amazonaws.services.athena.model.ResultSet;
import com.amazonaws.services.athena.model.Row;

@ExtendWith(MockitoExtension.class)
public class AthenaResultsIteratorTest {

	private static final String QUERY_ID = "someQueryExecutionId";

	private static final String HEADER_COL = "HeaderColumn";

	private static final RowMapper<String> ROW_MAPPER = new RowMapper<String>() {

		@Override
		public String mapRow(Row row) {
			// Simply join all the columns string values
			return row.getData().stream().map(Datum::getVarCharValue).collect(Collectors.joining(","));
		}
	};

	@Mock
	private AmazonAthena mockAthenaClient;

	@Mock
	private GetQueryResultsResult mockQueryResults;
	
	@Test
	public void testNextWithoutHasNext() {
		boolean excludeHeader = true;
		
		Iterator<String> iterator = getAthenaIteratorInstance(excludeHeader, MAX_FETCH_PAGE_SIZE);
		
		Assertions.assertThrows(IllegalStateException.class, () -> {
			iterator.next();
		});
	}
	
	@Test
	public void testHasNextOnFirstPageExcludingHeader() {

		int pageSize = 10;
				
		ResultSet resultSet = getResultSet(pageSize);

		when(mockAthenaClient.getQueryResults(any())).thenReturn(mockQueryResults);
		when(mockQueryResults.getResultSet()).thenReturn(resultSet);
		when(mockQueryResults.getNextToken()).thenReturn(null);
		
		boolean excludeHeader = true;

		Iterator<String> iterator = getAthenaIteratorInstance(excludeHeader, pageSize);

		assertNotNull(iterator);
		
		boolean result = iterator.hasNext();
		
		assertTrue(result);
		
		GetQueryResultsRequest request = new GetQueryResultsRequest()
				// On the first page when excluding the header it should fetch pageSize + 1
				.withMaxResults(pageSize + 1)
				.withQueryExecutionId(QUERY_ID)
				.withNextToken(null);
		
		verify(mockAthenaClient).getQueryResults(eq(request));
		verify(mockQueryResults).getResultSet();
		verify(mockQueryResults).getNextToken();
	}
	
	@Test
	public void testHasNextOnFirstPageIncludingHeader() {

		int pageSize = 10;
				
		ResultSet resultSet = getResultSet(pageSize);

		when(mockAthenaClient.getQueryResults(any())).thenReturn(mockQueryResults);
		when(mockQueryResults.getResultSet()).thenReturn(resultSet);
		when(mockQueryResults.getNextToken()).thenReturn(null);
		
		boolean excludeHeader = false;

		Iterator<String> iterator = getAthenaIteratorInstance(excludeHeader, pageSize);

		assertNotNull(iterator);
		
		boolean result = iterator.hasNext();
		
		assertTrue(result);
		
		GetQueryResultsRequest request = new GetQueryResultsRequest()
				.withMaxResults(pageSize)
				.withQueryExecutionId(QUERY_ID)
				.withNextToken(null);
		
		verify(mockAthenaClient).getQueryResults(eq(request));
		verify(mockQueryResults).getResultSet();
		verify(mockQueryResults).getNextToken();
	}

	
	@Test
	public void testHasNextOnFirstPageWithMaxPageSize() {
		
		ResultSet resultSet = getResultSet(10);

		when(mockAthenaClient.getQueryResults(any())).thenReturn(mockQueryResults);
		when(mockQueryResults.getResultSet()).thenReturn(resultSet);
		when(mockQueryResults.getNextToken()).thenReturn(null);
		
		boolean excludeHeader = true;

		Iterator<String> iterator = getAthenaIteratorInstance(excludeHeader, MAX_FETCH_PAGE_SIZE);

		assertNotNull(iterator);
		
		boolean result = iterator.hasNext();
		
		assertTrue(result);
		
		GetQueryResultsRequest request = new GetQueryResultsRequest()
				// Even when excluding the header the max fetch size for athena should not be exceeded
				.withMaxResults(MAX_FETCH_PAGE_SIZE)
				.withQueryExecutionId(QUERY_ID)
				.withNextToken(null);
		
		verify(mockAthenaClient).getQueryResults(eq(request));
		verify(mockQueryResults).getResultSet();
		verify(mockQueryResults).getNextToken();
	}

	@Test
	public void testIteratorWithNoResultExcludingHeader() {
		
		ResultSet resultSet = getResultSet(0);

		when(mockAthenaClient.getQueryResults(any())).thenReturn(mockQueryResults);
		when(mockQueryResults.getResultSet()).thenReturn(resultSet);
		when(mockQueryResults.getNextToken()).thenReturn(null);

		boolean excludeHeader = true;

		Iterator<String> iterator = getAthenaIteratorInstance(excludeHeader, MAX_FETCH_PAGE_SIZE);

		assertNotNull(iterator);
		assertFalse(iterator.hasNext());

		verify(mockQueryResults).getResultSet();
		verify(mockQueryResults).getNextToken();

	}

	@Test
	public void testIteratorWithNoResultIncludingHeader() {
		ResultSet resultSet = getResultSet(0);

		when(mockAthenaClient.getQueryResults(any())).thenReturn(mockQueryResults);
		when(mockQueryResults.getResultSet()).thenReturn(resultSet);
		when(mockQueryResults.getNextToken()).thenReturn(null);

		boolean excludeHeader = false;
		
		Iterator<String> iterator = getAthenaIteratorInstance(excludeHeader, MAX_FETCH_PAGE_SIZE);

		assertNotNull(iterator);
		assertTrue(iterator.hasNext());
		assertEquals(HEADER_COL, iterator.next());
		assertFalse(iterator.hasNext());

		verify(mockQueryResults).getResultSet();
		verify(mockQueryResults).getNextToken();
	}

	@Test
	public void testIteratorWithOnePageExcludingHeader() {
		int pageSize = 10;
		ResultSet resultSet = getResultSet(pageSize);

		when(mockAthenaClient.getQueryResults(any())).thenReturn(mockQueryResults);
		when(mockQueryResults.getResultSet()).thenReturn(resultSet);
		when(mockQueryResults.getNextToken()).thenReturn(null);

		boolean excludeHeader = true;

		Iterator<String> iterator = getAthenaIteratorInstance(excludeHeader, pageSize);

		assertNotNull(iterator);
		assertTrue(iterator.hasNext());

		for (int i = 0; i < pageSize; i++) {
			assertTrue(iterator.hasNext());
			assertEquals(String.valueOf(i), iterator.next());
		}

		assertFalse(iterator.hasNext());

		verify(mockQueryResults).getResultSet();
		verify(mockQueryResults).getNextToken();

	}

	@Test
	public void testIteratorWithOnePageIncludingHeader() {
		int pageSize = 10;
		ResultSet resultSet = getResultSet(pageSize);

		when(mockAthenaClient.getQueryResults(any())).thenReturn(mockQueryResults);
		when(mockQueryResults.getResultSet()).thenReturn(resultSet);
		when(mockQueryResults.getNextToken()).thenReturn(null);

		boolean excludeHeader = false;

		Iterator<String> iterator = getAthenaIteratorInstance(excludeHeader, pageSize);

		assertNotNull(iterator);
		assertTrue(iterator.hasNext());
		assertEquals(HEADER_COL, iterator.next());

		for (int i = 0; i < pageSize; i++) {
			assertTrue(iterator.hasNext());
			assertEquals(String.valueOf(i), iterator.next());
		}

		verify(mockQueryResults).getResultSet();
		verify(mockQueryResults).getNextToken();
	}

	@Test
	public void testIteratorWithMultiplePagesExcludingHeader() {
		int pageSize = 10;
		int resultsNumber = pageSize * 2;

		ResultSet firstPage = getResultSet(pageSize);
		ResultSet secondPage = getResultSet(pageSize, pageSize, false);

		when(mockAthenaClient.getQueryResults(any())).thenReturn(mockQueryResults);
		when(mockQueryResults.getResultSet()).thenReturn(firstPage, secondPage);
		when(mockQueryResults.getNextToken()).thenReturn("nextToken", new String[] { null });

		boolean excludeHeader = true;

		Iterator<String> iterator = getAthenaIteratorInstance(excludeHeader, pageSize);

		assertNotNull(iterator);
		assertTrue(iterator.hasNext());

		for (int i = 0; i < resultsNumber; i++) {
			assertTrue(iterator.hasNext());
			assertEquals(String.valueOf(i), iterator.next());
		}

		assertFalse(iterator.hasNext());

		verify(mockQueryResults, times(2)).getResultSet();
		verify(mockQueryResults, times(2)).getNextToken();

	}
	
	@Test
	public void testIteratorWithMultiplePagesIncludingHeader() {
		int pageSize = 10;
		int resultsNumber = pageSize * 2;

		ResultSet firstPage = getResultSet(pageSize);
		ResultSet secondPage = getResultSet(pageSize, pageSize, false);

		when(mockAthenaClient.getQueryResults(any())).thenReturn(mockQueryResults);
		when(mockQueryResults.getResultSet()).thenReturn(firstPage, secondPage);
		when(mockQueryResults.getNextToken()).thenReturn("nextToken", new String[] { null });

		boolean excludeHeader = false;

		Iterator<String> iterator = getAthenaIteratorInstance(excludeHeader, pageSize);

		assertNotNull(iterator);
		assertTrue(iterator.hasNext());
		assertEquals(HEADER_COL, iterator.next());

		for (int i = 0; i < resultsNumber; i++) {
			assertTrue(iterator.hasNext());
			assertEquals(String.valueOf(i), iterator.next());
		}

		assertFalse(iterator.hasNext());

		verify(mockQueryResults, times(2)).getResultSet();
		verify(mockQueryResults, times(2)).getNextToken();

	}
	
	private Iterator<String> getAthenaIteratorInstance(boolean excludeHeader, int pageSize) {
		return new AthenaResultsIterator<>(mockAthenaClient, QUERY_ID, ROW_MAPPER, pageSize, excludeHeader);
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
