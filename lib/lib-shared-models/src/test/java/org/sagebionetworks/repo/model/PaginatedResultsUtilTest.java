package org.sagebionetworks.repo.model;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.reflection.model.PaginatedResultsUtil;
import org.sagebionetworks.reflection.model.PaginatedResultsUtil.Paginator;
import org.sagebionetworks.repo.model.entity.query.IntegerValue;
import org.sagebionetworks.schema.adapter.JSONEntity;

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

	@Test
	public void testPaginatedResultsIterator() {
		testPaginatedResultsIterator(new long[] {});
		testPaginatedResultsIterator(new long[] { 1L });
		testPaginatedResultsIterator(new long[] { 1L, 2L, 3L, 4L, 5L, 6L });
	}

	private void testPaginatedResultsIterator(long[] values) {
		final List<IntegerValue> ivalues = Lists.newArrayList();
		for(long value:values){
			IntegerValue ivalue = new IntegerValue();
			ivalue.setValue(value);
			ivalues.add(ivalue);
		}
		Paginator<IntegerValue> paginator = new PaginatedResultsUtil.Paginator<IntegerValue>() {
			@Override
			public PaginatedResults<IntegerValue> getBatch(long limit, long offset) {
				return PaginatedResultsUtil.createPaginatedResults(ivalues, limit, offset);
			}
		};
		checkIterable(values, PaginatedResultsUtil.getPaginatedResultsIterable(paginator, 1));
		checkIterable(values, PaginatedResultsUtil.getPaginatedResultsIterable(paginator, 3));
		checkIterable(values, PaginatedResultsUtil.getPaginatedResultsIterable(paginator, 5));
		checkIterable(values, PaginatedResultsUtil.getPaginatedResultsIterable(paginator, 6));
		checkIterable(values, PaginatedResultsUtil.getPaginatedResultsIterable(paginator, 7));
		checkIterable(values, PaginatedResultsUtil.getPaginatedResultsIterable(paginator, 12));
	}

	private void checkIterable(long[] expected, Iterable<IntegerValue> iterable) {
		int index = 0;
		for (IntegerValue value : iterable) {
			assertEquals(expected[index++], value.getValue().longValue());
		}
	}
}
