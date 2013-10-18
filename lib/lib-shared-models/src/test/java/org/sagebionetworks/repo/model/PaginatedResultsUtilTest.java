package org.sagebionetworks.repo.model;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

public class PaginatedResultsUtilTest {


	@Test
	public void test() {
		// happy case: ask for 2 items starting at 1, get back [1,2,3) (i.e. including 1, excluding 3)
		assertEquals(Arrays.asList(new Long[]{1L,3l}), Arrays.asList(PaginatedResultsUtil.getStartAndEnd(10, 2, 1)));
		// overlapping the end: ask for 1,2,3,4, get back 1,2 (excl 3)
		assertEquals(Arrays.asList(new Long[]{1L,3l}), Arrays.asList(PaginatedResultsUtil.getStartAndEnd(3, 4, 1)));
		// off the deep end -- get back (0,0)
		assertEquals(Arrays.asList(new Long[]{0L,0l}), Arrays.asList(PaginatedResultsUtil.getStartAndEnd(3, 4, 1000)));
	}

}
