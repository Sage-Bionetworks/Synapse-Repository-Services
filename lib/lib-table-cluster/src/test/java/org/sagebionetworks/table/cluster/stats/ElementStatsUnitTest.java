package org.sagebionetworks.table.cluster.stats;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

@ExtendWith(MockitoExtension.class)
public class ElementStatsUnitTest {

	@InjectMocks
	private ElementStats elementStats;
	
	
	@Test
	public void testAddLongWithNullBothNull() {
		assertEquals(null, ElementStats.addLongsWithNull(null, null));
	}
	
	@Test
	public void testAddLongWithNullFirstNull() {
		assertEquals(Long.valueOf(123), ElementStats.addLongsWithNull(null, 123L));
	}
	
	@Test
	public void testAddLongWithNullSecondNull() {
		assertEquals(Long.valueOf(123), ElementStats.addLongsWithNull(123L, null));
	}
	
	@Test
	public void testAddLongWithNullNeitherNull() {
		assertEquals(Long.valueOf(4), ElementStats.addLongsWithNull(3L, 1L));
	}
	
	@Test
	public void testCloneBuilder() throws JSONObjectAdapterException {
		ElementStats original = ElementStats.builder()
	            .setMaximumSize(1L)
	            .build();
		
		ElementStats cloned = original.cloneBuilder().build();
		
		assertEquals(original.getMaximumSize(), cloned.getMaximumSize());
	}
	
}
