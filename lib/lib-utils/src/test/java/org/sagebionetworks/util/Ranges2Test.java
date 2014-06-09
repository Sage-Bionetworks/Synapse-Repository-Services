package org.sagebionetworks.util;

import static org.junit.Assert.*;

import java.util.Iterator;
import java.util.List;

import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import com.google.common.collect.Ranges;

public class Ranges2Test {

	@Test
	public void testNormal() {
		Iterable<Range<Long>> result = Ranges2.rangeIteratable(Ranges.closedOpen(0L, 100L), 25);
		assertEquals(sequence(0, 25, 50, 75, 100), Lists.newArrayList(result));
	}

	@Test
	public void testEmpty() {
		Iterable<Range<Long>> result = Ranges2.rangeIteratable(Ranges.closedOpen(0L, 0L), 25);
		assertFalse(result.iterator().hasNext());

		result = Ranges2.rangeIteratable(Ranges.closedOpen(23L, 23L), 1);
		assertFalse(result.iterator().hasNext());
	}

	@Test
	public void testOneStep() {
		Iterable<Range<Long>> result = Ranges2.rangeIteratable(Ranges.closedOpen(0L, 100L), 100);
		assertEquals(sequence(0, 100), Lists.newArrayList(result));
		result = Ranges2.rangeIteratable(Ranges.closedOpen(0L, 100L), 101);
		assertEquals(sequence(0, 100), Lists.newArrayList(result));
		result = Ranges2.rangeIteratable(Ranges.closedOpen(0L, 100L), 200);
		assertEquals(sequence(0, 100), Lists.newArrayList(result));
		result = Ranges2.rangeIteratable(Ranges.closedOpen(23L, 123L), 100);
		assertEquals(sequence(23, 123), Lists.newArrayList(result));
		result = Ranges2.rangeIteratable(Ranges.closedOpen(23L, 123L), 435);
		assertEquals(sequence(23, 123), Lists.newArrayList(result));
	}

	@Test
	public void testRounding() {
		Iterable<Range<Long>> result = Ranges2.rangeIteratable(Ranges.closedOpen(0L, 100L), 24);
		assertEquals(sequence(0, 20, 40, 60, 80, 100), Lists.newArrayList(result));

		result = Ranges2.rangeIteratable(Ranges.closedOpen(0L, 100L), 23);
		assertEquals(sequence(0, 20, 40, 60, 80, 100), Lists.newArrayList(result));
		result = Ranges2.rangeIteratable(Ranges.closedOpen(0L, 99L), 23);
		assertEquals(sequence(0, 20, 40, 60, 80, 99), Lists.newArrayList(result));
		result = Ranges2.rangeIteratable(Ranges.closedOpen(0L, 98L), 23);
		assertEquals(sequence(0, 20, 40, 60, 79, 98), Lists.newArrayList(result));
		result = Ranges2.rangeIteratable(Ranges.closedOpen(0L, 97L), 23);
		assertEquals(sequence(0, 20, 40, 59, 78, 97), Lists.newArrayList(result));
		result = Ranges2.rangeIteratable(Ranges.closedOpen(0L, 96L), 23);
		assertEquals(sequence(0, 20, 39, 58, 77, 96), Lists.newArrayList(result));
		result = Ranges2.rangeIteratable(Ranges.closedOpen(0L, 95L), 23);
		assertEquals(sequence(0, 19, 38, 57, 76, 95), Lists.newArrayList(result));
		result = Ranges2.rangeIteratable(Ranges.closedOpen(0L, 94L), 23);
		assertEquals(sequence(0, 19, 38, 57, 76, 94), Lists.newArrayList(result));

		result = Ranges2.rangeIteratable(Ranges.closedOpen(34L, 94L), 23);
		assertEquals(sequence(34, 54, 74, 94), Lists.newArrayList(result));
		result = Ranges2.rangeIteratable(Ranges.closedOpen(35L, 94L), 23);
		assertEquals(sequence(35, 55, 75, 94), Lists.newArrayList(result));
		result = Ranges2.rangeIteratable(Ranges.closedOpen(36L, 94L), 23);
		assertEquals(sequence(36, 56, 75, 94), Lists.newArrayList(result));
	}

	@Test
	public void testSmallestStep() {
		Iterable<Range<Long>> result = Ranges2.rangeIteratable(Ranges.closedOpen(23L, 27L), 1);
		assertEquals(sequence(23, 24, 25, 26, 27), Lists.newArrayList(result));
	}

	@Test
	public void testLongSteps() {
		Iterable<Range<Long>> result = Ranges2.rangeIteratable(Ranges.closedOpen(900000000000000L, 990000000000000L), 30000000000000L);
		assertEquals(sequence(900000000000000L, 930000000000000L, 960000000000000L, 990000000000000L), Lists.newArrayList(result));
	}

	private static List<Range<Long>> sequence(long... sequence) {
		List<Range<Long>> result = Lists.newArrayList();
		int index = 0;
		long previous = sequence[index++];
		while (index < sequence.length) {
			long current = sequence[index++];
			result.add(Ranges.closedOpen(previous, current));
			previous = current;
		}
		return result;
	}
}
