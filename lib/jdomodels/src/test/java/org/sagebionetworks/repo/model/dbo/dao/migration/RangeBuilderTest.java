package org.sagebionetworks.repo.model.dbo.dao.migration;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;
import org.sagebionetworks.repo.model.dbo.migration.RangeBuilder;
import org.sagebionetworks.repo.model.migration.IdRange;

import com.google.common.collect.Lists;

public class RangeBuilderTest {

	@Test
	public void testLessRowsPerRange() {
		long rowsPerRange = 100L;
		RangeBuilder builder = new RangeBuilder(rowsPerRange);
		long rowId = 1L;
		long associatedCount = 10L;
		builder.addRow(rowId, associatedCount);
		List<IdRange> results = builder.collateResults();
		
		List<IdRange> expected = Lists.newArrayList(
				createRange(1L, 2L)
		);
		assertEquals(expected, results);
	}
	
	@Test
	public void testMoreRowsPerRange() {
		long rowsPerRange = 1L;
		RangeBuilder builder = new RangeBuilder(rowsPerRange);
		builder.addRow(1L, 1L);
		builder.addRow(2L, 1L);
		builder.addRow(3L, 1L);
		List<IdRange> results = builder.collateResults();
		
		List<IdRange> expected = Lists.newArrayList(
				createRange(1L, 2L),
				createRange(2L, 3L),
				createRange(3L, 4L)
		);
		assertEquals(expected, results);
	}
	
	@Test
	public void testRemainder() {
		long rowsPerRange = 4L;
		RangeBuilder builder = new RangeBuilder(rowsPerRange);
		builder.addRow(1L, 2L);
		builder.addRow(2L, 1L);
		builder.addRow(3L, 5L);
		List<IdRange> results = builder.collateResults();
		
		List<IdRange> expected = Lists.newArrayList(
				createRange(1L, 3L),
				createRange(3L, 4L)
		);
		assertEquals(expected, results);
	}
	
	@Test
	public void testRowTooLargeMiddle() {
		long rowsPerRange = 2L;
		RangeBuilder builder = new RangeBuilder(rowsPerRange);
		builder.addRow(1L, 1L);
		// single row larger than rowsPerRange
		builder.addRow(2L, 100L);
		builder.addRow(3L, 1L);
		List<IdRange> results = builder.collateResults();
		
		List<IdRange> expected = Lists.newArrayList(
				createRange(1L, 2L),
				createRange(2L, 3L),
				createRange(3L, 4L)
		);
		assertEquals(expected, results);
	}
	
	@Test
	public void testRowTooLargeStart() {
		long rowsPerRange = 2L;
		RangeBuilder builder = new RangeBuilder(rowsPerRange);
		// single row larger than rowsPerRange
		builder.addRow(1L, 100L);
		builder.addRow(2L, 1L);
		builder.addRow(3L, 1L);
		List<IdRange> results = builder.collateResults();
		
		List<IdRange> expected = Lists.newArrayList(
				createRange(1L, 2L),
				createRange(2L, 4L)
		);
		assertEquals(expected, results);
	}
	
	/**
	 * Helper to create range.
	 * @param minimumId
	 * @param maximumId
	 * @return
	 */
	IdRange createRange(long minimumId, long maximumId) {
		IdRange range = new IdRange();
		range.setMinimumId(minimumId);
		range.setMaximumId(maximumId);
		return range;
	}
}
