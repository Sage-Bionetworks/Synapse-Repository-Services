package org.sagebionetworks.repo.model;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.google.common.collect.Lists;

public class PaginatedResultsUtilTest {

	private static final List<Long> THE_LARGE_LIST = Lists.newArrayList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L);
	private static final List<Long> THE_SMALL_LIST = Lists.newArrayList(1L, 2L, 3L);

	@Test
	public void test() {
		// happy case: ask for 2 items starting at 1, get back [1,2,3) (i.e. including 1, excluding 3)
		assertArrayEquals(new long[] { 1L, 3L }, PaginatedResultsUtil.getStartAndEnd(10, 2, 1));
		// overlapping the end: ask for 1,2,3,4, get back 1,2 (excl 3)
		assertArrayEquals(new long[] { 1L, 3L }, PaginatedResultsUtil.getStartAndEnd(3, 4, 1));
		// off the deep end -- get back (0,0)
		assertArrayEquals(new long[] { 0L, 0L }, PaginatedResultsUtil.getStartAndEnd(3, 4, 1000));
	}

	@Test
	public void testPrePaginate() {
		assertEquals(Arrays.asList(2L, 3L, 4L), PaginatedResultsUtil.prePaginate(THE_LARGE_LIST, 3L, 1L));
		assertEquals(Arrays.asList(2L, 3L), PaginatedResultsUtil.prePaginate(THE_LARGE_LIST, 2L, 1L));
		assertEquals(Arrays.asList(2L, 3L), PaginatedResultsUtil.prePaginate(THE_SMALL_LIST, 4L, 1L));
		assertEquals(Arrays.asList(3L), PaginatedResultsUtil.prePaginate(THE_SMALL_LIST, 4L, 2L));
		assertEquals(Arrays.asList(), PaginatedResultsUtil.prePaginate(THE_SMALL_LIST, 4L, 3L));
		assertEquals(Arrays.asList(), PaginatedResultsUtil.prePaginate(THE_SMALL_LIST, 4L, 1000L));
	}
}
