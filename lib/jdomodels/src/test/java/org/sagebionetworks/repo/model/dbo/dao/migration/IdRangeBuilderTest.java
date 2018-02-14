package org.sagebionetworks.repo.model.dbo.dao.migration;

import static org.junit.Assert.assertEquals;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.sagebionetworks.repo.model.dbo.migration.IdRangeBuilder;
import org.sagebionetworks.repo.model.migration.IdRange;

import com.google.common.collect.Lists;

public class IdRangeBuilderTest {
	
	long primaryRowId;
	long cardnality;

	@Test
	public void testOneRowLessThanOptimal() {
		long optimalRowsPerRange = 100L;
		// calls under test
		IdRangeBuilder builder = new IdRangeBuilder(optimalRowsPerRange);
		primaryRowId = 1L;
		cardnality = 10L;
		builder.addRow(primaryRowId, cardnality);
		List<IdRange> results = builder.collateResults();
		
		List<IdRange> expected = Lists.newArrayList(
				createRange(1L, 2L)
		);
		assertEquals(expected, results);
	}
	
	@Test
	public void testOneRowMoreThanOptimal() {
		long optimalRowsPerRange = 1;
		// calls under test
		IdRangeBuilder builder = new IdRangeBuilder(optimalRowsPerRange);
		primaryRowId = 12L;
		cardnality = 10L;
		builder.addRow(primaryRowId, cardnality);
		List<IdRange> results = builder.collateResults();
		
		List<IdRange> expected = Lists.newArrayList(
				createRange(12L, 13L)
		);
		assertEquals(expected, results);
	}
	
	@Test
	public void testNoRowsAdded() {
		long optimalRowsPerRange = 1;
		// calls under test
		IdRangeBuilder builder = new IdRangeBuilder(optimalRowsPerRange);
		// now rows added before collate
		List<IdRange> results = builder.collateResults();
		// expect an empty result
		assertEquals(new LinkedList<>(), results);
	}
	
	@Test
	public void testSparseIDsUnderOptimal() {
		long optimalRowsPerRange = 10L;
		IdRangeBuilder builder = new IdRangeBuilder(optimalRowsPerRange);
		builder.addRow(123L, 1L);
		builder.addRow(456L, 1L);
		builder.addRow(789L, 1L);
		List<IdRange> results = builder.collateResults();
		
		List<IdRange> expected = Lists.newArrayList(
				createRange(123L, 790L)
		);
		assertEquals(expected, results);
	}
	
	@Test
	public void testSparseIDsOverOptimal() {
		long optimalRowsPerRange = 1L;
		IdRangeBuilder builder = new IdRangeBuilder(optimalRowsPerRange);
		builder.addRow(123L, 10L);
		builder.addRow(456L, 10L);
		builder.addRow(789L, 10L);
		List<IdRange> results = builder.collateResults();
		
		List<IdRange> expected = Lists.newArrayList(
				createRange(123L, 124L),
				createRange(456L, 457L),
				createRange(789L, 790L)
		);
		assertEquals(expected, results);
	}
	
	
	@Test
	public void testMoreRowsPerRange() {
		long optimalRowsPerRange = 1L;
		IdRangeBuilder builder = new IdRangeBuilder(optimalRowsPerRange);
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
		long optimalRowsPerRange = 4L;
		IdRangeBuilder builder = new IdRangeBuilder(optimalRowsPerRange);
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
		long optimalRowsPerRange = 2L;
		IdRangeBuilder builder = new IdRangeBuilder(optimalRowsPerRange);
		builder.addRow(1L, 1L);
		// single row larger than optimalRowsPerRange
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
		long optimalRowsPerRange = 2L;
		IdRangeBuilder builder = new IdRangeBuilder(optimalRowsPerRange);
		// single row larger than optimalRowsPerRange
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
