package org.sagebionetworks.util;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.ImmutableList;

@ExtendWith(MockitoExtension.class)
public class TokenPaginationIteratorTest {

	@Mock
	private TokenPaginationProvider<Integer> mockProvider;

	@Mock
	private TokenPaginationPage<Integer> mockPage;

	@InjectMocks
	private TokenPaginationIterator<Integer> iterator;

	@Test
	public void testIteratorWithInvalidProvider() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			new TokenPaginationIterator<>(null);
		});
	}

	@Test
	public void testIteratorWithoutHasNext() {
		Assertions.assertThrows(IllegalStateException.class, () -> {
			iterator.next();
		});
	}

	@Test
	public void testIterator() {

		List<Integer> expectedResult = ImmutableList.of(1, 2, 3, 4);

		when(mockProvider.getNextPage(any())).thenReturn(mockPage);
		when(mockPage.getResults()).thenReturn(expectedResult);
		when(mockPage.getNextToken()).thenReturn(null);

		List<Integer> result = new ArrayList<>();

		while (iterator.hasNext()) {
			result.add(iterator.next());
		}

		assertEquals(expectedResult, result);

		verify(mockPage).getResults();
		verify(mockPage).getNextToken();
		verify(mockProvider).getNextPage(null);

	}

	@Test
	public void testIteratorWithEmptyResults() {

		when(mockProvider.getNextPage(any())).thenReturn(mockPage);
		when(mockPage.getResults()).thenReturn(Collections.emptyList());

		assertFalse(iterator.hasNext());

		verify(mockProvider).getNextPage(null);

	}
	
	@Test
	public void testIteratorWithMultiplePages() {
		List<Integer> firstPage = ImmutableList.of(1, 2, 3);
		List<Integer> secondPage = ImmutableList.of(1, 2);
		
		String nextToken = "nextToken";
		
		when(mockProvider.getNextPage(any())).thenReturn(mockPage);
		when(mockPage.getResults()).thenReturn(firstPage, secondPage);
		when(mockPage.getNextToken()).thenReturn(nextToken, null);
		
		List<Integer> result = new ArrayList<>();

		while (iterator.hasNext()) {
			result.add(iterator.next());
		}
		

		List<Integer> expectedResult = new ArrayList<>();
		
		expectedResult.addAll(firstPage);
		expectedResult.addAll(secondPage);

		assertEquals(expectedResult, result);
		
		verify(mockProvider).getNextPage(null);
		verify(mockProvider).getNextPage(nextToken);
		 
	}

}
