package org.sagebionetworks.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NestedMappingIteratorTest {
	
	@Mock
	private Iterator<Long> mockInputIterator;
	
	@Mock
	private Function<Long, Iterator<String>> mockMappingIteratorProvider;
	
	private NestedMappingIterator<Long, String> iterator;
	
	@BeforeEach
	public void before() {
		iterator = new NestedMappingIterator<>(mockInputIterator, mockMappingIteratorProvider);
	}
	
	@Test
	public void testHasNextWithEmptyIterator() {
		
		when(mockInputIterator.hasNext()).thenReturn(false);
		
		boolean result = iterator.hasNext();
		
		assertFalse(result);
		
		verify(mockInputIterator).hasNext();
		verifyNoMoreInteractions(mockInputIterator);
		verifyZeroInteractions(mockMappingIteratorProvider);
		
	}
	
	@Test
	public void testHasNextWithEmptyMapping() {
		
		when(mockInputIterator.hasNext()).thenReturn(true, false);
		when(mockInputIterator.next()).thenReturn(1L);
		when(mockMappingIteratorProvider.apply(1L)).thenReturn(Collections.emptyIterator());
		
		boolean result = iterator.hasNext();
		
		assertFalse(result);
		
		// Two times since the iterator consumes till something is found
		verify(mockInputIterator, times(2)).hasNext();
		verify(mockInputIterator).next();
		verify(mockMappingIteratorProvider).apply(1L);
		
	}
	
	@Test
	public void testHasNextWithNullMapping() {
		
		when(mockInputIterator.hasNext()).thenReturn(true, false);
		when(mockInputIterator.next()).thenReturn(1L);
		when(mockMappingIteratorProvider.apply(1L)).thenReturn(null);
		
		boolean result = iterator.hasNext();
		
		assertFalse(result);
		
		// Two times since the iterator consumes till something is found
		verify(mockInputIterator, times(2)).hasNext();
		verify(mockInputIterator).next();
		verify(mockMappingIteratorProvider).apply(1L);
		
	}
	
	@Test
	public void testHasNextWithMapping() {
		
		when(mockInputIterator.hasNext()).thenReturn(true, false);
		when(mockInputIterator.next()).thenReturn(1L);
		when(mockMappingIteratorProvider.apply(1L)).thenReturn(Arrays.asList("a", "b").iterator());
		
		boolean result = iterator.hasNext();
		
		assertTrue(result);
		
		verify(mockInputIterator).hasNext();
		verify(mockInputIterator).next();
		verify(mockMappingIteratorProvider).apply(1L);
		
	}
	
	@Test
	public void testHasNextWithSkippingEmptyIterators() {
		
		when(mockInputIterator.hasNext()).thenReturn(true, true, false);
		when(mockInputIterator.next()).thenReturn(1L, 2L);
		
		when(mockMappingIteratorProvider.apply(1L)).thenReturn(Collections.emptyIterator());
		when(mockMappingIteratorProvider.apply(2L)).thenReturn(Arrays.asList("a", "b").iterator());
		
		boolean result = iterator.hasNext();
		
		assertTrue(result);
		
		verify(mockInputIterator, times(2)).hasNext();
		verify(mockInputIterator, times(2)).next();
		verify(mockMappingIteratorProvider).apply(1L);
		verify(mockMappingIteratorProvider).apply(2L);
		
	}
	
	@Test
	public void testNextWithNoHasNextInvocation() {
		String errorMessage = assertThrows(IllegalStateException.class, () -> {
			iterator.next();
		}).getMessage();
		
		assertEquals("hasNext() must be called before next()", errorMessage);
	}
	
	@Test
	public void testNextWithEmptyIterator() {
		when(mockInputIterator.hasNext()).thenReturn(false);

		List<String> expected = Collections.emptyList();
		
		// Call under test
		List<String> result = consumeIterator();

		assertEquals(expected, result);
		
		verify(mockInputIterator).hasNext();
		verifyNoMoreInteractions(mockInputIterator);
		verifyZeroInteractions(mockMappingIteratorProvider);
	}
	
	@Test
	public void testNextWithMappingIterator() {
		when(mockInputIterator.hasNext()).thenReturn(true, false);
		when(mockInputIterator.next()).thenReturn(1L);
		
		when(mockMappingIteratorProvider.apply(1L)).thenReturn(Arrays.asList("a", "b", "c").iterator());

		List<String> expected = Arrays.asList("a", "b", "c");
		
		// Call under test
		List<String> result = consumeIterator();

		assertEquals(expected, result);
		
		verify(mockInputIterator, times(2)).hasNext();
		verify(mockInputIterator).next();
		verify(mockMappingIteratorProvider).apply(1L);
	}
	
	@Test
	public void testNextWithEmptyMappingIterator() {
		when(mockInputIterator.hasNext()).thenReturn(true, false);
		when(mockInputIterator.next()).thenReturn(1L);
		
		when(mockMappingIteratorProvider.apply(1L)).thenReturn(Collections.emptyIterator());

		List<String> expected = Collections.emptyList();
		
		// Call under test
		List<String> result = consumeIterator();

		assertEquals(expected, result);
		
		verify(mockInputIterator, times(2)).hasNext();
		verify(mockInputIterator).next();
		verify(mockMappingIteratorProvider).apply(1L);
	}
	
	@Test
	public void testNextWithSkipEmptyMappingIterator() {
		
		when(mockInputIterator.hasNext()).thenReturn(true, true, true, true,true, true, false);
		when(mockInputIterator.next()).thenReturn(1L, 2L, 3L, 4L, 5L, 6L);
		
		when(mockMappingIteratorProvider.apply(1L)).thenReturn(Collections.emptyIterator());
		when(mockMappingIteratorProvider.apply(2L)).thenReturn(Arrays.asList("a", "b").iterator());
		when(mockMappingIteratorProvider.apply(3L)).thenReturn(Collections.emptyIterator());
		when(mockMappingIteratorProvider.apply(4L)).thenReturn(Collections.emptyIterator());
		when(mockMappingIteratorProvider.apply(5L)).thenReturn(Arrays.asList("c").iterator());
		when(mockMappingIteratorProvider.apply(6L)).thenReturn(Collections.emptyIterator());

		List<String> expected = Arrays.asList("a", "b", "c");
		
		// Call under test
		List<String> result = consumeIterator();

		assertEquals(expected, result);
		
		verify(mockInputIterator, times(7)).hasNext();
		verify(mockInputIterator, times(6)).next();
		
		ArgumentCaptor<Long> inputCaptor = ArgumentCaptor.forClass(Long.class);
		
		verify(mockMappingIteratorProvider, times(6)).apply(inputCaptor.capture());
		
		assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L), inputCaptor.getAllValues());
	}
	
	
	private List<String> consumeIterator() {
		List<String> result = new ArrayList<>();
		
		while(iterator.hasNext()) {
			result.add(iterator.next());
		}
		
		return result;
	}
	

}
