package org.sagebionetworks.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class IntervalStatisticsTest {
	
	double expectedDelta = 0.0;

	@Test
	public void testConstructor_noInitialValue(){
		IntervalStatistics stats = new IntervalStatistics();
		assertEquals(Double.MAX_VALUE, stats.getMinimumValue(),expectedDelta);
		assertEquals(Double.MIN_VALUE, stats.getMaximumValue(),expectedDelta);
		assertEquals(0, stats.getValueSum(), expectedDelta);
		assertEquals(0, stats.getValueCount());
		//now insert a value to make the default min and max are overridden
		double value = 101.1;
		stats.addValue(value);
		assertEquals(value, stats.getMaximumValue(), expectedDelta);
		assertEquals(value, stats.getMinimumValue(), expectedDelta);
		assertEquals(value, stats.getValueSum(), expectedDelta);
		assertEquals(1L, stats.getValueCount());
	}

	@Test
	public void testConstructor_initialValue(){
		double value = 101.1;
		IntervalStatistics stats = new IntervalStatistics(value);
		assertEquals(value, stats.getMaximumValue(), expectedDelta);
		assertEquals(value, stats.getMinimumValue(), expectedDelta);
		assertEquals(value, stats.getValueSum(), expectedDelta);
		assertEquals(1L, stats.getValueCount());
	}
	
	@Test
	public void testAdd(){
		IntervalStatistics stats = new IntervalStatistics(0);
		stats.addValue(1);
		stats.addValue(2);
		stats.addValue(3);
		
		assertEquals(3, stats.getMaximumValue(), expectedDelta);
		assertEquals(0, stats.getMinimumValue(), expectedDelta);
		assertEquals(6, stats.getValueSum(), expectedDelta);
		assertEquals(4L, stats.getValueCount());
	}

}
