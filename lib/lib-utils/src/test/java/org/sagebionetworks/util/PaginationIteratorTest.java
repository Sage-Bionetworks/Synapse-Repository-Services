package org.sagebionetworks.util;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class PaginationIteratorTest {
	
	Integer[] data;
	PaginationProvider<Integer> provider;
	
	@Before
	public void before() {
		
		data = new Integer[] {123,456,789,222,444,555,999};
		
		provider = new PaginationProvider<Integer>() {
			
			@Override
			public List<Integer> getNextPage(long limit, long offset) {
				List<Integer> list = new LinkedList<>();
				for(long i=offset; i<(limit+offset); i++) {
					if(i < data.length) {
						list.add(data[(int) i]);
					}
				}
				return list;
			}
		};
	}
	
	@Test
	public void testIterator() {
		long limit = 3;
		PaginationIterator<Integer> iterator = new PaginationIterator<>(provider, limit);
		List<Integer> results =  new LinkedList<>();
		while(iterator.hasNext()) {
			results.add(iterator.next());
		}
		List<Integer> expected = Arrays.asList(data);
		assertEquals(expected, results);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testNullProvider() {
		long limit = 3;
		provider = null;
		new PaginationIterator<>(provider, limit);
	}

	@Test (expected=IllegalStateException.class)
	public void testNextWithoutHasNext() {
		long limit = 3;
		PaginationIterator<Integer> iterator = new PaginationIterator<>(provider, limit);
		iterator.next();
	}
}
