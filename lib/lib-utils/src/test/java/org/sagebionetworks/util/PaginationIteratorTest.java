package org.sagebionetworks.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PaginationIteratorTest {

	@Mock
	private PaginationProvider<Integer> mockProvider;

	@Test
	public void testIterator() {

		when(mockProvider.getNextPage(anyLong(), anyLong())).thenReturn(Arrays.asList(1, 2, 3), Arrays.asList(4, 5, 6),
				Arrays.asList(7), Collections.emptyList());

		long limit = 3;
		PaginationIterator<Integer> iterator = new PaginationIterator<>(mockProvider, limit);
		List<Integer> results = new LinkedList<>();
		while (iterator.hasNext()) {
			results.add(iterator.next());
		}
		// calling hasNext again should not trigger another page.
		assertFalse(iterator.hasNext());
		assertFalse(iterator.hasNext());
		
		assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7), results);
		verify(mockProvider, times(4)).getNextPage(anyLong(), anyLong());
		verify(mockProvider).getNextPage(3, 0);
		verify(mockProvider).getNextPage(3, 3);
		verify(mockProvider).getNextPage(3, 6);
		verify(mockProvider).getNextPage(3, 9);
	}

	@Test
	public void testNullProvider() {
		long limit = 3;
		mockProvider = null;
		assertThrows(IllegalArgumentException.class, () -> {
			new PaginationIterator<>(mockProvider, limit);
		});
	}

	@Test
	public void testNextWithoutHasNext() {
		long limit = 3;
		PaginationIterator<Integer> iterator = new PaginationIterator<>(mockProvider, limit);
		assertThrows(IllegalStateException.class, () -> {
			iterator.next();
		});
	}
}
