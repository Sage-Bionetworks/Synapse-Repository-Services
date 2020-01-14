package org.sagebionetworks.repo.model.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.sagebionetworks.repo.model.table.QueryOptions.BUNDLE_MASK_QUERY_COLUMN_MODELS;
import static org.sagebionetworks.repo.model.table.QueryOptions.BUNDLE_MASK_QUERY_COUNT;
import static org.sagebionetworks.repo.model.table.QueryOptions.BUNDLE_MASK_QUERY_FACETS;
import static org.sagebionetworks.repo.model.table.QueryOptions.BUNDLE_MASK_QUERY_MAX_ROWS_PER_PAGE;
import static org.sagebionetworks.repo.model.table.QueryOptions.BUNDLE_MASK_QUERY_RESULTS;
import static org.sagebionetworks.repo.model.table.QueryOptions.BUNDLE_MASK_QUERY_SELECT_COLUMNS;
import static org.sagebionetworks.repo.model.table.QueryOptions.*;

import org.junit.Test;

public class QueryOptionsTest {
	
	@Test
	public void testDefaults() {
		// call under test
		QueryOptions options = new QueryOptions();
		boolean expectedValue = false;
		assertAll(expectedValue, options);
	}
	
	@Test
	public void testWithMaskNull() {
		QueryOptions options = new QueryOptions();
		Long partsMask = null;
		// call under test
		options.withMask(partsMask);
		boolean expectedValue = true;
		assertAll(expectedValue, options);
	}
	
	@Test
	public void testWithMaskNegative() {
		QueryOptions options = new QueryOptions();
		Long partsMask = -1L;
		// call under test
		options.withMask(partsMask);
		boolean expectedValue = true;
		assertAll(expectedValue, options);
	}
	
	@Test
	public void testWithMaskAllValues() {
		QueryOptions options = new QueryOptions();
		Long partsMask = 255L;
		// call under test
		options.withMask(partsMask);
		boolean expectedValue = true;
		assertAll(expectedValue, options);
	}
	
	@Test
	public void testWithMaskZero() {
		QueryOptions options = new QueryOptions();
		Long partsMask = 0L;
		// call under test
		options.withMask(partsMask);
		boolean expectedValue = false;
		assertAll(expectedValue, options);
	}
	
	@Test
	public void testQueryMask() {
		// call under test
		QueryOptions options = new QueryOptions().withMask(BUNDLE_MASK_QUERY_RESULTS);
		assertTrue(options.runQuery);
		// the rest of the values should be false
		options.withRunQuery(false);
		boolean expectedValue = false;
		assertAll(expectedValue, options);
	}
	
	@Test
	public void testColumnMask() {
		// call under test
		QueryOptions options = new QueryOptions().withMask(BUNDLE_MASK_QUERY_COLUMN_MODELS);
		assertTrue(options.returnColumnModels());
		// the rest of the values should be false
		options.withReturnColumnModels(false);
		boolean expectedValue = false;
		assertAll(expectedValue, options);
	}
	
	@Test
	public void testCountMask() {
		// call under test
		QueryOptions options = new QueryOptions().withMask(BUNDLE_MASK_QUERY_COUNT);
		assertTrue(options.runCount());
		// the rest of the values should be false
		options.withRunCount(false);
		boolean expectedValue = false;
		assertAll(expectedValue, options);
	}
	
	@Test
	public void testFacetMask() {
		// call under test
		QueryOptions options = new QueryOptions().withMask(BUNDLE_MASK_QUERY_FACETS);
		assertTrue(options.returnFacets());
		// the rest of the values should be false
		options.withReturnFacets(false);
		boolean expectedValue = false;
		assertAll(expectedValue, options);
	}
	
	@Test
	public void testMaxRowsMask() {
		// call under test
		QueryOptions options = new QueryOptions().withMask(BUNDLE_MASK_QUERY_MAX_ROWS_PER_PAGE);
		assertTrue(options.returnMaxRowsPerPage());
		// the rest of the values should be false
		options.withReturnMaxRowsPerPage(false);
		boolean expectedValue = false;
		assertAll(expectedValue, options);
	}
	
	@Test
	public void testSelectColumnsMask() {
		// call under test
		QueryOptions options = new QueryOptions().withMask(BUNDLE_MASK_QUERY_SELECT_COLUMNS);
		assertTrue(options.returnSelectColumns());
		// the rest of the values should be false
		options.withReturnSelectColumns(false);
		boolean expectedValue = false;
		assertAll(expectedValue, options);
	}
	
	@Test
	public void testSumFileSizeMask() {
		// call under test
		QueryOptions options = new QueryOptions().withMask(BUNDLE_MASK_SUM_FILE_SIZES);
		assertTrue(options.runSumFileSizes());
		// the rest of the values should be false
		options.withRunSumFileSizes(false);
		boolean expectedValue = false;
		assertAll(expectedValue, options);
	}
	
	@Test
	public void testGetMaskNone() {
		QueryOptions options = new QueryOptions();
		// call under test
		long mask = options.getPartMask();
		assertEquals(0L, mask);
	}
	
	@Test
	public void testGetMaskAll() {
		long inMask = 0xff;
		QueryOptions options = new QueryOptions().withMask(inMask);
		// call under test
		long mask = options.getPartMask();
		assertEquals(inMask, mask);
	}
	
	@Test
	public void testGetMaskColumnModels() {
		QueryOptions options = new QueryOptions().withReturnColumnModels(true);
		// call under test
		long mask = options.getPartMask();
		assertEquals(QueryOptions.BUNDLE_MASK_QUERY_COLUMN_MODELS, mask);
	}
	
	@Test
	public void testGetMaskCount() {
		QueryOptions options = new QueryOptions().withRunCount(true);
		// call under test
		long mask = options.getPartMask();
		assertEquals(QueryOptions.BUNDLE_MASK_QUERY_COUNT, mask);
	}
	
	@Test
	public void testGetMaskFacet() {
		QueryOptions options = new QueryOptions().withReturnFacets(true);
		// call under test
		long mask = options.getPartMask();
		assertEquals(QueryOptions.BUNDLE_MASK_QUERY_FACETS, mask);
	}
	
	@Test
	public void testGetMaskMaxRows() {
		QueryOptions options = new QueryOptions().withReturnMaxRowsPerPage(true);
		// call under test
		long mask = options.getPartMask();
		assertEquals(QueryOptions.BUNDLE_MASK_QUERY_MAX_ROWS_PER_PAGE, mask);
	}
	
	@Test
	public void testGetMaskRunQuery() {
		QueryOptions options = new QueryOptions().withRunQuery(true);
		// call under test
		long mask = options.getPartMask();
		assertEquals(QueryOptions.BUNDLE_MASK_QUERY_RESULTS, mask);
	}
	
	@Test
	public void testGetMaskSelectColumns() {
		QueryOptions options = new QueryOptions().withReturnSelectColumns(true);
		// call under test
		long mask = options.getPartMask();
		assertEquals(QueryOptions.BUNDLE_MASK_QUERY_SELECT_COLUMNS, mask);
	}
	
	@Test
	public void testGetMaskSumFileSizes() {
		QueryOptions options = new QueryOptions().withRunSumFileSizes(true);
		// call under test
		long mask = options.getPartMask();
		assertEquals(QueryOptions.BUNDLE_MASK_SUM_FILE_SIZES, mask);
	}
	
	@Test
	public void testReturnLastUpdatedOn() {
		QueryOptions options = new QueryOptions().withReturnLastUpdatedOn(true);
		// call under test
		long mask = options.getPartMask();
		assertEquals(QueryOptions.BUNDLE_MASK_LAST_UPDATED_ON, mask);
	}
	
	@Test
	public void testReturnLastUpdatedOnMask() {
		// call under test
		QueryOptions options = new QueryOptions().withMask(BUNDLE_MASK_LAST_UPDATED_ON);
		assertTrue(options.returnLastUpdatedOn);
		// the rest of the values should be false
		options.withReturnLastUpdatedOn(false);
		boolean expectedValue = false;
		assertAll(expectedValue, options);
	}
	
	/**
	 * Helper to assert all values match the given value.
	 * @param value
	 * @param options
	 */
	public static void assertAll(boolean value, QueryOptions options) {
		assertEquals(value, options.runQuery());
		assertEquals(value, options.runCount());
		assertEquals(value, options.returnSelectColumns());
		assertEquals(value, options.returnMaxRowsPerPage());
		assertEquals(value, options.returnColumnModels());
		assertEquals(value, options.returnFacets());
		assertEquals(value, options.runSumFileSizes());
		assertEquals(value, options.returnLastUpdatedOn());
	}

}
